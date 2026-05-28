# OmniStock - Test de Despliegue Completo (Postman Smoke Tests)
# Usa el puerto dinámico de HOST_PORT_API (asignado por find_free_ports.ps1)
$apiPort = if ($env:HOST_PORT_API) { $env:HOST_PORT_API } else { 8080 }
$BASE_URL = "http://localhost:$apiPort"
$PASS = 0
$FAIL = 0
$RESULTS = @()

Write-Host "`n========== OMNISTOCK - TEST DE DESPLIEGUE ==========`n" -ForegroundColor Cyan

# PASO 1: LOGIN
Write-Host "[1/14] Auth - Login... " -NoNewline
try {
    $loginBody = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    if ($loginResponse.success -and $loginResponse.data.token) {
        $TOKEN = $loginResponse.data.token
        Write-Host "OK (token obtenido)" -ForegroundColor Green
        $PASS++
        $RESULTS += "  OK Auth - Login: 200 OK"
    } else {
        Write-Host "ERROR: No se pudo obtener token" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    exit 1
}

# FUNCION PARA TESTEAR
function Test-Endpoint {
    param([string]$Num, [string]$Name, [string]$Method, [string]$Url)
    Write-Host ("[$Num] $Name... ") -NoNewline
    try {
        $headers = @{ Authorization = "Bearer $TOKEN" }
        $json = Invoke-RestMethod -Uri $Url -Method $Method -Headers $headers
        if ($json.success -eq $true) {
            $dataCount = 0
            if ($json.data -is [System.Array]) { $dataCount = $json.data.Count }
            elseif ($json.data) { $dataCount = 1 }
            Write-Host ("OK ($dataCount registros)") -ForegroundColor Green
            $script:PASS++
            $script:RESULTS += ("  OK " + $Name + ": 200 OK (" + $dataCount + " registros)")
        } else {
            Write-Host "ERROR: success=false" -ForegroundColor Red
            $script:FAIL++
            $script:RESULTS += ("  FAIL " + $Name + ": FALLO")
        }
    } catch {
        Write-Host ("ERROR: " + $_) -ForegroundColor Red
        $script:FAIL++
        $script:RESULTS += ("  FAIL " + $Name + ": FALLO")
    }
}

# ENDPOINTS
Test-Endpoint "2/14" "Analytics - Price Variation" "GET" "$BASE_URL/api/v1/analytics/price-variation?months=6&outlierThreshold=50"
Test-Endpoint "3/14" "Analytics - Stockout Rates" "GET" "$BASE_URL/api/v1/analytics/stockout-rates"
Test-Endpoint "4/14" "Analytics - Price Dispersion" "GET" "$BASE_URL/api/v1/analytics/price-dispersion?maxRows=10"
Test-Endpoint "5/14" "Analytics - Price Stability" "GET" "$BASE_URL/api/v1/analytics/price-stability?days=90&maxRows=10"
Test-Endpoint "6/14" "Analytics - Stock Volatility" "GET" "$BASE_URL/api/v1/analytics/stock-volatility?days=30&maxRows=10"
Test-Endpoint "7/14" "Analytics - Catalog Growth" "GET" "$BASE_URL/api/v1/analytics/catalog-growth?weeks=12"
Test-Endpoint "8/14" "Analytics - MOQ Distribution" "GET" "$BASE_URL/api/v1/analytics/moq"
Test-Endpoint "9/14" "Analytics - Condition Mix" "GET" "$BASE_URL/api/v1/analytics/condition-mix"
Test-Endpoint "10/14" "Analytics - Cost Coverage" "GET" "$BASE_URL/api/v1/analytics/cost-coverage?maxRows=10"
Test-Endpoint "11/14" "Products - List All" "GET" "$BASE_URL/api/v1/productos"
Test-Endpoint "12/14" "Suppliers - List All" "GET" "$BASE_URL/api/v1/proveedores/list"
Test-Endpoint "13/14" "Dashboard - Summary" "GET" "$BASE_URL/api/v1/dashboard/summary"
Test-Endpoint "14/14" "Trading Opportunities" "GET" "$BASE_URL/api/v1/analytics/trading-opportunities"

# RESUMEN
Write-Host "`n================== R E S U M E N ==================" -ForegroundColor Cyan
foreach ($r in $RESULTS) { Write-Host $r }
Write-Host "------------------------------------------------" -ForegroundColor Gray
Write-Host "Total: $($PASS+$FAIL)  |  Pasados: $PASS  |  Fallados: $FAIL" -ForegroundColor $(if ($FAIL -eq 0) {"Green"} else {"Yellow"})
Write-Host "================================================" -ForegroundColor Cyan
if ($FAIL -eq 0) { Write-Host "`nTODOS LOS ENDPOINTS FUNCIONAN CORRECTAMENTE!" -ForegroundColor Green }
else { Write-Host "`n$FAIL endpoint(s) fallaron." -ForegroundColor Yellow }
