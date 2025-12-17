import 'dart:convert';
import 'dart:developer' as developer;

/// Centralized logger for the application
/// Only logs when debug mode is enabled or force is true
class AppLogger {
  static const String _name = 'LimitedCart';
  static bool _debugMode = false;

  /// Enable/disable debug mode
  static void setDebugMode(bool enabled) {
    _debugMode = enabled;
    log('Debug mode ${enabled ? 'ENABLED' : 'DISABLED'}', force: true);
  }

  /// Check if debug mode is enabled
  static bool get isDebugMode => _debugMode;

  /// Log a message
  /// [message] - The message to log
  /// [force] - Force logging even if debug mode is off
  /// [level] - Log level: 0=finest, 500=fine, 800=info, 900=warning, 1000=error
  static void log(
    String message, {
    bool force = false,
    int level = 800,
    Object? error,
    StackTrace? stackTrace,
  }) {
    if (!_debugMode && !force) return;

    developer.log(
      message,
      name: _name,
      level: level,
      error: error,
      stackTrace: stackTrace,
    );
  }

  /// Log info message
  static void info(String message, {bool force = false}) {
    log('ℹ️ $message', force: force, level: 800);
  }

  /// Log warning message
  static void warn(String message, {bool force = false}) {
    log('⚠️ $message', force: force, level: 900);
  }

  /// Log error message
  static void error(
    String message, {
    bool force = false,
    Object? error,
    StackTrace? stackTrace,
  }) {
    log('❌ $message', force: force, level: 1000, error: error, stackTrace: stackTrace);
  }

  /// Log success message
  static void success(String message, {bool force = false}) {
    log('✅ $message', force: force, level: 800);
  }

  /// Log stream data in a beautiful format
  /// [label] - Label for the stream data
  /// [data] - The data received from stream
  static void logStream(String label, dynamic data, {bool force = false}) {
    if (!_debugMode && !force) return;

    final buffer = StringBuffer();
    buffer.writeln('📡 STREAM: $label');
    buffer.writeln('┌────────────────────────────────────────');
    
    if (data is Map) {
      data.forEach((key, value) {
        buffer.writeln('│ $key: ${_formatValue(value)}');
      });
    } else if (data is List) {
      buffer.writeln('│ Count: ${data.length}');
      for (var i = 0; i < data.length && i < 5; i++) {
        buffer.writeln('│ [$i]: ${_formatValue(data[i])}');
      }
      if (data.length > 5) {
        buffer.writeln('│ ... ${data.length - 5} more items');
      }
    } else {
      buffer.writeln('│ ${_formatValue(data)}');
    }
    
    buffer.writeln('└────────────────────────────────────────');
    
    log(buffer.toString(), force: force, level: 800);
  }

  /// Format value for pretty printing
  static String _formatValue(dynamic value) {
    if (value == null) return 'null';
    if (value is String) return '"$value"';
    if (value is num || value is bool) return value.toString();
    if (value is Map || value is List) {
      try {
        return JsonEncoder.withIndent('  ').convert(value);
      } catch (e) {
        return value.toString();
      }
    }
    return value.toString();
  }

  /// Log API request
  static void logRequest(String method, String url, {Map<String, dynamic>? body}) {
    if (!_debugMode) return;
    
    final buffer = StringBuffer();
    buffer.writeln('🌐 API REQUEST');
    buffer.writeln('┌────────────────────────────────────────');
    buffer.writeln('│ Method: $method');
    buffer.writeln('│ URL: $url');
    if (body != null && body.isNotEmpty) {
      buffer.writeln('│ Body:');
      buffer.writeln('│ ${_formatValue(body)}');
    }
    buffer.writeln('└────────────────────────────────────────');
    
    log(buffer.toString(), level: 500);
  }

  /// Log API response
  static void logResponse(int statusCode, dynamic data) {
    if (!_debugMode) return;
    
    final icon = statusCode >= 200 && statusCode < 300 ? '✅' : '❌';
    final buffer = StringBuffer();
    buffer.writeln('$icon API RESPONSE');
    buffer.writeln('┌────────────────────────────────────────');
    buffer.writeln('│ Status: $statusCode');
    buffer.writeln('│ Data:');
    buffer.writeln('│ ${_formatValue(data)}');
    buffer.writeln('└────────────────────────────────────────');
    
    log(buffer.toString(), level: statusCode >= 400 ? 900 : 500);
  }
}
