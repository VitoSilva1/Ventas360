#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
PIDS=()

if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

mkdir -p "$LOG_DIR"

cleanup() {
  echo
  echo "Deteniendo servicios..."
  for pid in "${PIDS[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
}

trap cleanup INT TERM EXIT

start_service() {
  local directory="$1"
  local name="$2"

  echo "Iniciando $name..."
  (
    cd "$ROOT_DIR/$directory"
    exec ./mvnw spring-boot:run
  ) > "$LOG_DIR/$name.log" 2>&1 &
  PIDS+=("$!")
}

start_frontend() {
  echo "Iniciando frontend Angular..."
  (
    cd "$ROOT_DIR/frontend"
    exec npm start -- --host localhost
  ) > "$LOG_DIR/frontend.log" 2>&1 &
  PIDS+=("$!")
}

wait_for_service() {
  local url="$1"
  local name="$2"
  local attempts=60

  echo "Esperando $name en $url..."
  until curl -fsS "$url" >/dev/null 2>&1; do
    attempts=$((attempts - 1))
    if (( attempts == 0 )); then
      echo "No fue posible iniciar $name. Revisa: $LOG_DIR/$name.log"
      exit 1
    fi
    sleep 1
  done
  echo "$name disponible"
}

start_service "backend/service-registry" "service-registry"
wait_for_service "http://localhost:8761/actuator/health" "service-registry"

start_service "backend/auth-service" "auth-service"
start_service "backend/product-service" "product-service"
start_service "backend/sales-service" "sales-service"

wait_for_service "http://localhost:8081/actuator/health" "auth-service"
wait_for_service "http://localhost:8083/actuator/health" "product-service"
wait_for_service "http://localhost:8082/actuator/health" "sales-service"
wait_for_service "http://localhost:8761/eureka/apps/AUTH-SERVICE" "auth-service en Eureka"
wait_for_service "http://localhost:8761/eureka/apps/PRODUCT-SERVICE" "product-service en Eureka"
wait_for_service "http://localhost:8761/eureka/apps/SALES-SERVICE" "sales-service en Eureka"

start_service "backend/api-gateway" "api-gateway"
wait_for_service "http://localhost:8080/actuator/health" "api-gateway"
wait_for_service "http://localhost:8080/products" "product route through api-gateway"
wait_for_service "http://localhost:8080/sales" "sales route through api-gateway"

start_frontend
wait_for_service "http://localhost:4200" "frontend"

echo
echo "Backend y frontend iniciados correctamente."
echo "API Gateway: http://localhost:8080"
echo "Productos:    http://localhost:8080/products"
echo "Frontend:     http://localhost:4200"
echo "Logs:         $LOG_DIR"
echo "Presiona Ctrl+C para detener todos los servicios."

wait
