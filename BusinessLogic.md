# Business Logic — Permisos, Renderizado y Estado Actual

## Sistema de Roles y Permisos

### Roles definidos

| Rol | Descripción |
|-----|-------------|
| `ROLE_ADMIN` | Acceso completo a todas las funcionalidades del sistema |
| `ROLE_CLIENTE` | Acceso de lectura a inventario, analytics, oportunidades, presupuesto y dashboard de proveedor |

### Mapa de permisos por endpoint (Backend — SecurityConfig.java)

#### Rutas públicas (sin autenticación)
- `GET /actuator/health`, `/actuator/health/**`, `/actuator/prometheus`
- `POST /auth/**`, `/api/v1/auth/**` (login, register, refresh, validate)
- `GET /v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` (OpenAPI/Swagger)

#### Productos (`/api/v1/productos/**`)
- `GET` → `hasAnyRole("ADMIN", "CLIENTE")`
- `POST`, `PUT`, `DELETE` → `hasRole("ADMIN")`

#### Proveedores (`/api/v1/proveedores/**`)
- `GET /list` → `hasAnyRole("ADMIN", "CLIENTE")`
- `GET /*/dashboard` → `hasAnyRole("ADMIN", "CLIENTE")`
- Resto (`GET /`, `GET /{id}`, `POST`, `PUT`, `DELETE`) → `hasRole("ADMIN")`

#### Usuarios
- `/api/admin/**` → `hasRole("ADMIN")`
- `PUT /api/usuarios/perfil` → `authenticated()` (cualquier usuario autenticado)

#### Notificaciones
- `/api/v1/notificaciones/**` → `authenticated()`

#### Dashboard
- `GET /summary` → `authenticated()`
- `GET /supplier-health` → `hasRole("ADMIN")`
- `GET /export/csv` → `authenticated()`

#### Analytics (`/api/v1/analytics/**`)
- Todos los endpoints → `isAuthenticated()` (ADMIN y CLIENTE)

#### Presupuestos (`/api/v1/budget/**`)
- Todos los endpoints → `authenticated()`

#### Mapeos de proveedor (`/api/v1/proveedores/{id}/mappings`, `/api/v1/mappings/{id}`)
- Todos → `hasRole("ADMIN")`

### Flujo de autenticación JWT

1. **Login**: `POST /api/v1/auth/login` → `AuthenticationManager.authenticate()` → genera JWT con claims: `sub` (username), `roles` (lista de strings), `fullName`, `jti`, `iat`, `exp`, `iss` ("OmniStock"), `aud` ("WebApiOmniStock")
2. **Validación**: `JwtAuthenticationFilter` extrae token del header `Authorization: Bearer`, parsea claims, construye `UserDetails` con roles del claim `roles` (sin consultar BD)
3. **Refresh**: `POST /api/v1/auth/refresh` → extrae username del token actual, genera uno nuevo
4. **Expiración**: 150 minutos (configurado en `JwtService.EXPIRATION_TIME_MS`)

### Flujo de autorización en el frontend

1. `AuthContext` mantiene estado: `token`, `username`, `roles`, `isAdmin`, `isAuthenticated`
2. `ProtectedRoute` verifica `isAuthenticated` → redirige a `/login` si no autenticado
3. `api.ts` (Axios interceptor) añade `Authorization: Bearer <token>` a cada request
4. En 401/403 → limpia localStorage y redirige a `/login`
5. Componentes usan `useAuth().isAdmin` para mostrar/ocultar elementos de UI (botones de crear/editar/eliminar)

### Reglas de renderizado condicional

| Componente | Condición de renderizado |
|------------|-------------------------|
| Botón "Nuevo producto" | `isAdmin` |
| Botón "Editar proveedor" | `isAdmin` |
| Botón "Eliminar proveedor" | `isAdmin` |
| Gestión de usuarios (ruta /users) | `isAdmin` |
| Formularios de crear/editar | `isAdmin` |
| Botón "Sincronizar" | `isAdmin` |
| Panel de Supplier Health | `isAdmin` |
| Listado de productos | `isAuthenticated` |
| Dashboard | `isAuthenticated` |
| Historial de precios | `isAuthenticated` |
| Carrito / Presupuesto | `isAuthenticated` |
| Notificaciones | `isAuthenticated` |
| Analytics / Oportunidades | `isAuthenticated` |

## Arquitectura de componentes (Frontend)

### Layout
```
AppLayout
├── Sidebar (navegación)
├── TopBar (usuario, notificaciones)
└── <Outlet /> (contenido de la ruta)
```

### Protección de rutas
```
<Routes>
  <Route path="/login" element={<LoginPage />} />  ← pública
  <Route element={<ProtectedRoute />}>              ← requiere auth
    <Route path="/" element={<InventoryDashboardPage />} />
    <Route path="/product/:id" element={<ProductDetailPage />} />
    <Route path="/analytics" element={<AnalyticsPage />} />
    <Route path="/oportunidades" element={<OpportunitiesPage />} />
    <Route path="/dashboard" element={<DashboardPage />} />
    <Route path="/suppliers" element={<SupplierListPage />} />
    <Route path="/suppliers/new" element={<SupplierFormPage />} />
    <Route path="/suppliers/:id" element={<SupplierDetailPage />} />
    <Route path="/suppliers/:id/edit" element={<SupplierFormPage />} />
    <Route path="/suppliers/:id/dashboard" element={<SupplierDashboardPage />} />
    <Route path="/users" element={<UserListPage />} />
    <Route path="/users/new" element={<UserFormPage />} />
    <Route path="/users/edit/:id" element={<UserFormPage />} />
    <Route path="/historial-precios/:productoId/:proveedorId" element={<PriceHistoryPage />} />
    <Route path="/presupuesto" element={<BudgetPage />} />
    <Route path="/presupuesto/nuevo" element={<BudgetPage />} />
    <Route path="/presupuestos" element={<BudgetListPage />} />
    <Route path="/presupuesto/:id" element={<BudgetDetailPage />} />
    <Route path="/notificaciones" element={<NotificationsPage />} />
  </Route>
  <Route path="*" element={<NotFoundPage />} />
</Routes>
```

### Providers (orden de anidamiento)
```
QueryClientProvider
  └── Router (BrowserRouter)
       └── AuthProvider
            └── BudgetProvider
                 └── ErrorBoundary
                      └── AppRoutes
```

## Manejo de errores

### Backend
- `ApiResponse<T>` wrapper genérico con: `success`, `message`, `data`, `timestamp`, `metadata`, `errors`
- Métodos estáticos: `ApiResponse.success(data, message)`, `ApiResponse.error(message)`
- Excepciones de validación → `errors` map con errores por campo
- Errores de analytics → capturados con try-catch, metadata con detalle de excepción

### Frontend
- `ErrorBoundary` global captura errores no controlados
- Axios interceptor maneja 401/403 globalmente
- `react-hot-toast` para notificaciones toast

---

## Arquitectura del Sistema

### Diagrama de flujo
```txt
Usuario
  |
  v
Nginx (frontend:8081)
  |-- "/api" ------> Backend Spring Boot (8080)
                         |
                         |-- JDBC -----------> MariaDB (mariadb-local:3306)
                         |
                         |-- HTTP -----------> json-mock-server (3001..3004)
                         |
                         |-- metrics --------> Prometheus -> Grafana
```

### Capas del proyecto

| Capa | Contenido |
|------|-----------|
| **App (código de negocio)** | `backend/` (Spring Boot) + `frontend/` (React + Vite) |
| **Infra (operación/despliegue)** | `infra/` (json-mock-server, monitoring) |
| **Orquestación** | `docker-compose.yml` (8 servicios) |

### Dependencias críticas

1. **MariaDB** — sin DB, backend no levanta.
2. **json-mock-server** — para sincronizaciones con proveedores mock.
3. **Red Docker** — conectividad entre contenedores via `omnistock-net`.

### Riesgos actuales

- CI frontend tiene `continue-on-error` en tests, lo que puede ocultar fallos.

### Quick wins

1. Hacer obligatorio test frontend (quitar `continue-on-error` cuando haya suite madura).
2. Definir política de tags release (`vX.Y.Z`) para despliegue reproducible.
3. Añadir smoke tests post-deploy (health endpoints y ruta front).

### ¿Microservicios?

Actualmente es **Frontend separado + Backend monolítico modular** + componentes de infraestructura. No es microservicios estrictos (backend no está partido en varios servicios independientes). Si crece, puede evolucionar, pero hoy funciona como un servicio único.
