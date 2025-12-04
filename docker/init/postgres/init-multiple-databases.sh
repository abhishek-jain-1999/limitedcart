#!/bin/bash
set -e

echo "Creating application databases..."
for db in auth_db products_db inventory_db orders_db payment_db worker_db temporal_db; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE $db' WHERE NOT EXISTS (
        SELECT FROM pg_database WHERE datname = '$db'
    )\gexec
EOSQL
done
echo "All databases created."
