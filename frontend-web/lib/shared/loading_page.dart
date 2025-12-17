import 'package:flutter/material.dart';
import 'dart:async';
import '../core/logger.dart';

class LoadingPage extends StatefulWidget {
  const LoadingPage({super.key});

  @override
  State<LoadingPage> createState() => _LoadingPageState();
}

class _LoadingPageState extends State<LoadingPage> with SingleTickerProviderStateMixin {
  String _displayText = 'LC';
  final List<String> _steps = [
    'LC',
    'L C',
    'Li Ca',
    'Lim Car',
    'Limi Cart',
    'Limit Cart',
    'Limite Cart',
    'Limited Cart',
  ];
  Timer? _typingTimer;
  Timer? _debounceTimer;
  int _stepIndex = 0;
  late AnimationController _cursorController;

  @override
  void initState() {
    super.initState();
    AppLogger.info('LoadingPage: initState called');
    _cursorController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    );
    
    // Debounce animation start to improve performance on quick loads
    _debounceTimer = Timer(const Duration(milliseconds: 300), () {
      if (mounted) {
        _cursorController.repeat(reverse: true);
        _startTyping();
      }
    });
  }

  void _startTyping() {
    AppLogger.info('LoadingPage: Starting typing animation');
    _typingTimer = Timer.periodic(const Duration(milliseconds: 150), (timer) {
      if (_stepIndex < _steps.length - 1) {
        if (mounted) {
          setState(() {
            _stepIndex++;
            _displayText = _steps[_stepIndex];
          });
          AppLogger.warn('LoadingPage: Typing step $_stepIndex: $_displayText');
        } else {
          AppLogger.warn('LoadingPage: Timer ticked but widget not mounted');
        }
      } else {
        AppLogger.info('LoadingPage: Typing animation complete');
        timer.cancel();
      }
    });
  }

  @override
  void dispose() {
    AppLogger.info('LoadingPage: dispose called');
    _debounceTimer?.cancel();
    _typingTimer?.cancel();
    _cursorController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // AppLogger.debug('LoadingPage: build called'); // Uncomment if build logs are needed, can be noisy
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Animated cart icon
            TweenAnimationBuilder<double>(
              tween: Tween(begin: 0.0, end: 1.0),
              duration: const Duration(milliseconds: 800),
              curve: Curves.easeOutCubic,
              builder: (context, value, child) {
                return Transform.scale(
                  scale: 0.3 + (value * 0.7),
                  child: Opacity(
                    opacity: value,
                    child: Container(
                      padding: const EdgeInsets.all(20),
                      decoration: BoxDecoration(
                        color: const Color(0xFF4F46E5).withOpacity(0.1),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(
                        Icons.shopping_cart_rounded,
                        size: 60,
                        color: Color(0xFF4F46E5),
                      ),
                    ),
                  ),
                );
              },
            ),
            
            const SizedBox(height: 40),
            
            // Typing text animation
            SizedBox(
              height: 50,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Text(
                    _displayText,
                    style: const TextStyle(
                      fontSize: 40,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF1F2937),
                      letterSpacing: 2,
                      height: 1.2,
                    ),
                  ),
                  // Blinking cursor
                  if (_stepIndex < _steps.length - 1)
                    FadeTransition(
                      opacity: _cursorController,
                      child: Container(
                        margin: const EdgeInsets.only(left: 2),
                        width: 3,
                        height: 40,
                        color: const Color(0xFF4F46E5),
                      ),
                    ),
                ],
              ),
            ),
            
            const SizedBox(height: 60),
            
            // Loading indicator
            SizedBox(
              width: 40,
              height: 40,
              child: CircularProgressIndicator(
                strokeWidth: 3,
                valueColor: AlwaysStoppedAnimation<Color>(
                  const Color(0xFF4F46E5).withOpacity(0.8),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
