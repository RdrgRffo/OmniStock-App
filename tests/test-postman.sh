#!/usr/bin/env bash
# =============================================================================
# OmniStock - Test de Despliegue Completo (Postman Smoke Tests) - Linux/macOS
# Usa el puerto dinámico de HOST_PORT_API (asignado por find_free_ports.sh)
#
# Uso:
#   bash ./tests/test-postman.sh
# =============================================================================

set -euo pipefail

# Colores
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
GRAY='\033[0;90m'
NC='\033[0m'

API_PORT="${HOST_PORT_API:-8080}"
BASE_URL="http://localhost:$API_PORT"
PASS=0
FAIL=0
RESULTS=()

echo -e "${CYAN}"
echo "========== OMNISTOCK - TEST DE DESPLIEGUE =========="
echo -e "${NC}"

# ──────────────────────────────────────────────────────────────
# PASO 1: LOGIN
# ──────────────────────────────────────────────────────────────
echo -ne "${GRAY}[1/14] Auth - Login... ${NC}"
LOGIN_RESPONSE=$(curl -sf -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}' 2>/dev/null) || {
    echo -e "${RED}ERROR: No se pudo conectar al backend${NC}"
    exit 1
}

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.token' 2>/dev/null)
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
    echo -e "${GREEN}OK (token obtenido)${NC}"
    PASS=$((PASS + 1))
    RESULTS+=("  OK Auth - Login: 200 OK")
else
    echo -e "${RED}ERROR: No se pudo obtener token${NC}"
    exit 1
fi

# ──────────────────────────────────────────────────────────────
# FUNCION PARA TESTEAR
# ──────────────────────────────────────────────────────────────
test_endpoint() {
    local num=$1
    local name=$2
    local url=$3

    echo -ne "${GRAY}[$num] $name... ${NC}"

    RESPONSE=$(curl -sf -H "Authorization: Bearer $TOKEN" "$url" 2>/dev/null) || {
        echo -e "${RED}ERROR: Fallo la peticion${NC}"
        FAIL=$((FAIL + 1))
        RESULTS+=("  FAIL $name: FALLO")
        return
    }

    SUCCESS=$(echo "$RESPONSE" | jq -r '.success' 2>/dev/null)
    if [ "$SUCCESS" = "true" ]; then
        DATA_COUNT=$(echo "$RESPONSE" | jq '.data | if type == "array" then length else 1 end' 2>/dev/null)
        echo -e "${GREEN}OK ($DATA_COUNT registros)${NC}"
        PASS=$((PASS + 1))
        RESULTS+=("  OK $name: 200 OK ($DATA_COUNT registros)")
    else
        echo -e "${RED}ERROR: success=false${NC}"
        FAIL=$((FAIL + 1))
        RESULTS+=("  FAIL $name: FALLO")
    fi
}

# ──────────────────────────────────────────────────────────────
# ENDPOINTS
# ──────────────────────────────────────────────────────────────
test_endpoint "2/14" "Analytics - Price Variation"     "$BASE_URL/api/v1/analytics/price-variation?months=6&outlierThreshold=50"
test_endpoint "3/14" "Analytics - Stockout Rates"      "$BASE_URL/api/v1/analytics/stockout-rates"
test_endpoint "4/14" "Analytics - Price Dispersion"    "$BASE_URL/api/v1/analytics/price-dispersion?maxRows=10"
test_endpoint "5/14" "Analytics - Price Stability"     "$BASE_URL/api/v1/analytics/price-stability?days=90&maxRows=10"
test_endpoint "6/14" "Analytics - Stock Volatility"    "$BASE_URL/api/v1/analytics/stock-volatility?days=30&maxRows=10"
test_endpoint "7/14" "Analytics - Catalog Growth"      "$BASE_URL/api/v1/analytics/catalog-growth?weeks=12"
test_endpoint "8/14" "Analytics - MOQ Distribution"    "$BASE_URL/api/v1/analytics/moq"
test_endpoint "9/14" "Analytics - Condition Mix"       "$BASE_URL/api/v1/analytics/condition-mix"
test_endpoint "10/14" "Analytics - Cost Coverage"      "$BASE_URL/api/v1/analytics/cost-coverage?maxRows=10"
test_endpoint "11/14" "Products - List All"            "$BASE_URL/api/v1/productos"
test_endpoint "12/14" "Suppliers - List All"           "$BASE_URL/api/v1/proveedores/list"
test_endpoint "13/14" "Dashboard - Summary"            "$BASE_URL/api/v1/dashboard/summary"
test_endpoint "14/14" "Trading Opportunities"          "$BASE_URL/api/v1/analytics/trading-opportunities"

# ──────────────────────────────────────────────────────────────
# RESUMEN
# ──────────────────────────────────────────────────────────────
echo -e "${CYAN}"
echo "================== R E S U M E N =================="
echo -e "${NC}"
for r in "${RESULTS[@]}"; do
    echo -e "$r"
done
echo -e "${GRAY}------------------------------------------------${NC}"
TOTAL=$((PASS + FAIL))
if [ "$FAIL" -eq 0 ]; then
    echo -e "${GREEN}Total: $TOTAL  |  Pasados: $PASS  |  Fallados: $FAIL${NC}"
else
    echo -e "${YELLOW}Total: $TOTAL  |  Pasados: $PASS  |  Fallados: $FAIL${NC}"
fi
echo -e "${CYAN}================================================${NC}"
if [ "$FAIL" -eq 0 ]; then
    echo -e "${GREEN}"
    echo "TODOS LOS ENDPOINTS FUNCIONAN CORRECTAMENTE!"
    echo -e "${NC}"
else
    echo -e "${YELLOW}"
    echo "$FAIL endpoint(s) fallaron."
    echo -e "${NC}"
fi
