# Stage 1: Build the Flutter web app
FROM ghcr.io/cirruslabs/flutter:stable AS build

WORKDIR /app

# Copy pubspec files first to cache dependencies
COPY pubspec.yaml pubspec.lock ./
RUN flutter pub get

# Copy the rest of the application code
COPY . .

# Build the web application
ARG BASE_URL=http://localhost
RUN flutter build web --dart-define=BASE_URL=${BASE_URL} --release

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Copy the build output from the previous stage
COPY --from=build /app/build/web /usr/share/nginx/html

# Copy custom Nginx configuration to templates directory for env variable substitution
COPY nginx.conf /etc/nginx/templates/default.conf.template

# Expose port 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
