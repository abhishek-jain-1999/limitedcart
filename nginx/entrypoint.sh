#!/bin/sh
set -e

# Install openssl for password hashing (required on nginx:alpine)
apk add --no-cache openssl

if [ -n "$NGINX_BASIC_AUTH_USER" ] && [ -n "$NGINX_BASIC_AUTH_PASSWORD" ]; then
    echo "Generating .htpasswd for user: $NGINX_BASIC_AUTH_USER"
    # Generate MD5-based password hash using openssl
    PASS=$(openssl passwd -apr1 "$NGINX_BASIC_AUTH_PASSWORD")
    echo "${NGINX_BASIC_AUTH_USER}:${PASS}" > /etc/nginx/.htpasswd
fi

# Execute the CMD passed to the container (usually nginx)
exec "$@"
