import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'constants.dart';

class ApiClient {
  late final Dio _dio;

  ApiClient() {
    const base = AppConstants.apiBaseUrl;
    final normalizedBase = base.endsWith('/') ? base.substring(0, base.length - 1) : base;
    final apiBase = normalizedBase.endsWith('/api') ? normalizedBase : '$normalizedBase/api';

    _dio = Dio(BaseOptions(
      baseUrl: apiBase,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 10),
      headers: {
        'Content-Type': 'application/json',
      },
    ));

    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final prefs = await SharedPreferences.getInstance();
        final token = prefs.getString('jwt_token');
        
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        return handler.next(options);
      },
      onError: (DioException e, handler) {
        // Handle 401 Unauthorized globally if needed
        return handler.next(e);
      },
    ));
  }

  Dio get client => _dio;
}
