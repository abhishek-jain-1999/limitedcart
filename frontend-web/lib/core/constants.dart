class AppConstants {
  // In production, this should be configurable via --dart-define
  static const String apiBaseUrl =  String.fromEnvironment('BASE_URL', defaultValue: 'http://localhost');
}
