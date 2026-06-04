#!/bin/sh
set -e

echo "Waiting for PostgreSQL to be ready..."
until pg_isready -h postgres -U postgres -q 2>/dev/null; do
  sleep 1
done

echo "Creating databases..."
psql -h postgres -U postgres <<-EOSQL
  SELECT 'CREATE DATABASE hotelos_reception'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hotelos_reception')\gexec
  SELECT 'CREATE DATABASE hotelos_housekeeping'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hotelos_housekeeping')\gexec
  SELECT 'CREATE DATABASE hotelos_roomservice'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hotelos_roomservice')\gexec
  SELECT 'CREATE DATABASE hotelos_maintenance'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hotelos_maintenance')\gexec
EOSQL

echo "Databases created successfully."
