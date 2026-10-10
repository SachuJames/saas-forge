#!/bin/bash
set -e
echo "== TenantKit demo =="
echo "Starting postgres, keycloak, backend, frontend..."
docker compose up -d --build
echo "Waiting for backend..."
for i in $(seq 1 60); do
  if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "Backend is up"
    break
  fi
  sleep 5
done
echo ""
echo "Demo users (Keycloak at http://localhost:8180):"
echo "  admin / admin  (realm role: admin)"
echo "  demo  / demo   (realm role: user)"
echo ""
echo "Frontend: http://localhost:3000"
echo "API:      http://localhost:8080"
