import 'dart:async';
import 'dart:convert';
import 'dart:html' as html;
import 'package:shared_preferences/shared_preferences.dart';
import 'logger.dart';
import 'constants.dart';

/// Represents a parsed SSE message
class _SseMessage {
  final String? event;
  final String? data;

  _SseMessage({this.event, this.data});

  bool get hasData => data != null && data!.isNotEmpty;
}

/// Web-specific SSE client using XMLHttpRequest for custom headers support
class SseClient {
  html.HttpRequest? _xhr;
  StreamController<Map<String, dynamic>>? _controller;
  
  // Track what we've already processed
  int _lastProcessedLength = 0;
  
  // Current message being assembled
  String? _currentEvent;
  final List<String> _currentDataLines = [];

  Stream<Map<String, dynamic>> connect(String orderId) {
    _controller = StreamController<Map<String, dynamic>>.broadcast();
    _connectToStream();
    return _controller!.stream;
  }

  Future<void> _connectToStream() async {
    final url = _buildUrl();
    final token = await _getAuthToken();
    
    AppLogger.info('SSE connecting to: $url');
    
    _xhr = html.HttpRequest();
    _xhr!.open('GET', url);
    _setHeaders(token);
    _attachEventListeners();
    _xhr!.send();
    
    AppLogger.success('SSE connection initiated');
  }

  String _buildUrl() {
    const base = AppConstants.apiBaseUrl;
    final normalizedBase = base.endsWith('/') ? base.substring(0, base.length - 1) : base;
    final apiBase = normalizedBase.endsWith('/api') ? normalizedBase : '$normalizedBase/api';
    return '$apiBase/notifications/stream';
  }

  Future<String?> _getAuthToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('jwt_token');
  }

  void _setHeaders(String? token) {
    _xhr!.setRequestHeader('Accept', 'text/event-stream');
    _xhr!.setRequestHeader('Cache-Control', 'no-cache');
    if (token != null) {
      _xhr!.setRequestHeader('Authorization', 'Bearer $token');
    }
  }

  void _attachEventListeners() {
    _xhr!.onProgress.listen(_handleProgress);
    _xhr!.onError.listen(_handleError);
    _xhr!.onLoadEnd.listen(_handleClose);
  }

  void _handleProgress(html.ProgressEvent event) {
    try {
      // ProgressEvent properties:
      // - event.loaded: bytes loaded so far
      // - event.total: total bytes (if known)
      // - event.lengthComputable: whether total is known
      // BUT: It does NOT contain the actual text data!
      
      // Optional: Log event properties for debugging
      // AppLogger.debug('Progress: ${event.loaded}/${event.total} bytes');
      
      // The actual SSE data comes from xhr.responseText, not the event
      final fullText = _xhr?.responseText ?? '';
      if (fullText.length <= _lastProcessedLength) return;
      
      // Extract text from our last processed position to end
      final unprocessedText = fullText.substring(_lastProcessedLength);
      
      AppLogger.info("New chunk: $unprocessedText");
      
      // Try to process complete messages
      final processedLength = _processChunk(unprocessedText);
      
      // Only advance pointer by the amount we successfully processed
      _lastProcessedLength += processedLength;
      
    } catch (e) {
      AppLogger.error('Error in progress handler', error: e);
    }
  }

  int _processChunk(String chunk) {
    // SSE message pattern: event:<value1>\n (optional whitespace) data:<value2>
    final regex = RegExp(
      r'event:\s*(\S+)\s*\n\s*data:\s*(.+?)(?=\n\s*event:|\n\s*\n|$)',
      multiLine: true,
      dotAll: true,
    );
    
    final matches = regex.allMatches(chunk);
    
    if (matches.isEmpty) {
      AppLogger.info('No complete SSE messages yet - buffering');
      return 0; // Don't advance pointer, wait for more data
    }
    
    // Process all complete matches
    int lastMatchEnd = 0;
    for (final match in matches) {
      final eventType = match.group(1)?.trim();
      final eventData = match.group(2)?.trim();
      
      if (eventType != null && eventData != null) {
        _handleSseMessage(eventType, eventData);
      }
      
      lastMatchEnd = match.end;
    }
    
    // Return how many characters we successfully processed
    // This allows partial messages to stay in the buffer
    return lastMatchEnd;
  }

  void _handleSseMessage(String eventType, String data) {
    AppLogger.info('SSE event type: $eventType');
    
    // Check if data is JSON
    if (_looksLikeJson(data)) {
      _parseAndEmitJson(eventType, data);
    } else {
      // String data (e.g., "Connected to notification stream")
      AppLogger.info('Non-JSON SSE message: $data');
    }
  }

  void _parseAndEmitJson(String eventType, String data) {
    try {
      final parsed = json.decode(data);
      
      if (parsed is Map<String, dynamic>) {
        final status = parsed['status'] ?? 'unknown';
        AppLogger.info('SSE message: event=$eventType, status=$status');
        _controller?.add(parsed);
      } else {
        AppLogger.warn('SSE data is not a Map<String, dynamic>');
      }
    } catch (e) {
      AppLogger.error('Failed to parse SSE JSON: $data', error: e);
    }
  }

  bool _looksLikeJson(String data) {
    final trimmed = data.trim();
    return trimmed.startsWith('{') || trimmed.startsWith('[');
  }

  void _handleError(html.Event event) {
    AppLogger.error('SSE connection error');
    _controller?.addError('SSE connection error');
  }

  void _handleClose(html.Event event) {
    AppLogger.warn('SSE stream ended');
    _controller?.close();
  }

  void dispose() {
    AppLogger.info('Closing SSE client');
    _xhr?.abort();
    _xhr = null;
    _controller?.close();
    _lastProcessedLength = 0;
    _currentEvent = null;
    _currentDataLines.clear();
  }
}
