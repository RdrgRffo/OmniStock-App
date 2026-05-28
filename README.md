# OmniStock — Stock multicanal y presupuestos B2B

### Plataforma full stack para inventario consolidado y simulación de órdenes de compra

**OmniStock** es una solución integral para la **gestión de stock multicanal** y la **simulación de presupuestos B2B** a partir de catálogos de múltiples proveedores. Originalmente concebida como una aplicación interna, este repositorio representa un **port a plataforma full stack** con una arquitectura moderna, escalable y containerizada, donde participé activamente en el diseño de la arquitectura, la implementación del backend, la contenedorización y la evolución del proyecto.

</div>

---

## Tabla de Contenidos

- [Mi Rol y Enfoque como Desarrollador Backend](#mi-rol-y-enfoque-como-desarrollador-backend)
- [Características Clave e Impacto](#características-clave-e-impacto)
- [Impacto en la Empresa](#impacto-en-la-empresa)
- [Arquitectura del Sistema](#arquitectura-del-sistema)
- [Behavior-Driven Development (BDD)](#behavior-driven-development-bdd)
- [Tests](#tests)
- [Tecnologías Utilizadas](#tecnologías-utilizadas)
- [Endpoints de la API](#endpoints-de-la-api)
- [Cómo Ejecutar el Proyecto](#cómo-ejecutar-el-proyecto)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Flujo de Sincronización de Catálogos](#flujo-de-sincronización-de-catálogos-api-externa--normalización--inventario)
- [Flujo de Simulación de Presupuestos](#flujo-de-simulación-de-presupuestos)
- [Documentación adicional](#documentación-adicional)

---

## Mi Rol y Enfoque como Desarrollador Backend

> **OmniStock es un port de un proyecto de empresa.** Partiendo de una aplicación interna funcional, formé parte de un equipo de tres desarrolladores donde me centré al 100% en el backend. Mi labor fue diseñar e implementar una API REST moderna que reemplazara la lógica dispersa del proyecto original, aplicando principios de arquitectura limpia, resiliencia empresarial y testing riguroso.

### Mi Contribución al Proyecto

Participación activa en la **toma de decisiones arquitectónicas**, el **diseño e implementación del backend**, la **contenedorización con Docker** y la **mejora sustancial** del proyecto original de empresa.

---

#### 1. Diseño de Arquitectura — Arquitectura por Capas (Layered Architecture)

> **Aclaración importante**: Este backend sigue una **arquitectura por capas (Layered Architecture)** clásica, no Clean Architecture ni Domain-Driven Design. No hay separación hexagonal, puertos/adaptadores, ni un núcleo de dominio aislado de la infraestructura. Es una arquitectura pragmática y efectiva para APIs REST empresariales, donde cada capa tiene una responsabilidad bien definida y las dependencias fluyen de arriba abajo.

Participé activamente en las decisiones para la estructura del sistema, garantizando escalabilidad y mantenibilidad:

- **Arquitectura en capas** (Controller → Service → Repository → DTOs) siguiendo principios SOLID
- **Migración** de una lógica dispersa a una API REST moderna con servicios bien definidos
- **Separación de responsabilidades** clara: cada capa con un propósito único y sin solapamiento

**Patrones de diseño implementados:**

| Patrón | Dónde se usa | Propósito |
|--------|-------------|-----------|
| **Facade** | `StockServiceImpl` | Unifica `StockQueryService` + `StockCatalogSyncService` + `MasterProductService` bajo una sola interfaz `IStockService`. El controlador solo conoce el facade. |
| **Strategy** | `TransformationStrategy` (4 implementaciones: `Direct`, `Nested`, `FindInArray`, `Split`) | Cada proveedor tiene un formato JSON distinto. El `UniversalMapperService` selecciona la estrategia según el `ProviderMapping` y normaliza todo a un modelo común. |
| **Template Method** | `SeederRoleSupport` | Los seeders que necesitan roles siguen un flujo común: buscar rol por nombre → si no existe, crearlo → usarlo. La clase base encapsula el boilerplate. |
| **DTO/Projection** | `PriceDispersionProjection`, `TradingOpportunityProjection` (interfaces JPA) | Proyecciones nativas con `@Query` que evitan cargar entidades completas. Mapean directamente columnas SQL a getters camelCase. |
| **Generic Response** | `ApiResponse<T>` | Wrapper genérico con `success`, `message`, `data`, `timestamp`, `metadata`, `errors`. Todos los controladores devuelven `ResponseEntity<ApiResponse<T>>`. |
| **Repository Pattern** | 16 repositorios Spring Data JPA | Abstracción de persistencia con queries derivadas de nombres de métodos y `@Query` nativas para consultas complejas (analytics, dashboard). |

---

#### 2. Backend que construí — Arquitectura Interna

Mi backend no se limita a guardar y recuperar datos; ejecuta procesos críticos con una arquitectura interna bien definida.

##### 2.1. Capa de Controladores (10 controladores REST)

Cada controlador sigue una estructura uniforme:
- Inyección de dependencias por constructor (sin `@Autowired` directo)
- Validación con `@Valid` y `jakarta.validation`
- Respuestas envueltas en `ApiResponse<T>` con `ResponseEntity` para códigos HTTP precisos
- `@PreAuthorize("isAuthenticated()")` o `@PreAuthorize("hasRole('ADMIN')")` en cada endpoint

| Controlador | Endpoints | Funcionalidad |
|-------------|-----------|---------------|
| `AuthController` | 4 | login, register, validate, refresh |
| `UserController` | 2 | perfil propio, admin CRUD |
| `ProductController` | 6 | listar, buscar, detalle, sync-all, sync-status, historial |
| `SupplierController` | 8 | CRUD proveedores, dashboard, mappings |
| `ProviderMappingController` | 3 | CRUD reglas de mapeo |
| `DashboardController` | 3 | summary, supplier-health, export CSV |
| `AnalyticsController` | 10 | price-variation, stockout-rates, price-dispersion, moq, condition-mix, cost-coverage, stock-volatility, catalog-growth, price-stability, trading-opportunities |
| `BudgetController` | 6 | simular, listar, detalle, cambiar estado, eliminar, notify-export |
| `PriceHistoryController` | 8 | historial por producto/proveedor, analytics, comparativa, por-mes, registrar, último-precio, tendencia |
| `NotificationController` | 4 | listar, counts, marcar leída, marcar todas leídas |

##### 2.2. Capa de Servicios (18 servicios)

Organizados por dominio en subpaquetes:

**`service/auth/`**
- `UserService` — CRUD usuarios, cambio de contraseña, perfil
- `RoleService` — Gestión de roles (ADMIN, CLIENTE)
- `EncryptionService` — AES-256/GCM para cifrar credenciales de proveedores

**`service/supplier/`**
- `SupplierService` — CRUD proveedores + dashboard con KPIs (total SKUs, stock medio, precio medio, productos activos, últimos 5 productos)
- `ProviderMappingService` — CRUD reglas de mapeo por proveedor

**`service/transformation/`**
- `UniversalMapperService` — Orquesta las 4 estrategias de transformación
- `TransformationStrategy` (interface) + 4 implementaciones:
  - `DirectTransformationStrategy` — Acceso directo a clave del JSON
  - `NestedTransformationStrategy` — Navegación por rutas punteadas (`manufacturer.part_number`)
  - `FindInArrayTransformationStrategy` — Búsqueda en arrays EAV por clave-valor
  - `SplitTransformationStrategy` — División de strings compuestos (ej: "Dell PowerEdge R740" → brand="Dell", model="PowerEdge R740")

**`service/integration/`**
- `ApiClientService` — Cliente HTTP con Resilience4j (CircuitBreaker + Retry + TimeLimiter)
- `DeadLetterQueueService` — Persistencia y reintento de mensajes fallidos

**`service/product/`**
- `MasterProductService` — CRUD de productos maestros
- `ProductSupplierService` — Relaciones producto-proveedor (ofertas)
- `StockCatalogSyncService` — Sincronización asíncrona con `@Async` y estados parciales
- `StockQueryService` — Consultas de stock con filtros y paginación
- `StockServiceImpl` (Facade) — Implementación de `IStockService` que orquesta los 3 servicios anteriores
- `SyncStateService` — Estado global de sincronización (IDLE/IN_PROGRESS/COMPLETED/ERROR)

**`service/pricing/`**
- `PriceHistoryService` — Historial de precios con analytics (media, min, max, variación, tendencia)

**`service/budget/`**
- `BudgetService` — Simulación, ciclo de vida (BORRADOR → FINALIZADO → APROBADO/RECHAZADO), validación de stock y MOQ

**`service/dashboard/`**
- `DashboardService` — Resumen ejecutivo (totales, disponibilidad, stale products, top movers, zombies)
- `SupplierHealthService` — KPIs de salud operativa (SLA Score, Sync Success Rate, Latency, Error Rate)
- `ExportService` — Exportación ZIP con CSVs del dashboard

**`service/analytics/`**
- `SupplierAnalyticsService` — 10 métricas BI con queries nativas y proyecciones JPA

**`service/notification/`**
- `NotificationService` — Notificaciones in-app con WebSocket (STOMP)
- `NotificationCleanupScheduler` — Limpieza automática de notificaciones antiguas

**`service/shared/`**
- `CurrencyRateService` — Tasas de cambio para conversión de monedas

##### 2.3. Capa de Repositorios (16 repositorios)

Cada repositorio extiende `JpaRepository<Entity, IdType>`. Los más complejos usan `@Query` con SQL nativo:

```java
// Ejemplo: Price Dispersion Index — query nativa con proyección
@Query(value = """
    SELECT mp.id AS productId, mp.mpn AS mpn, mp.category AS category,
           COUNT(DISTINCT ps.proveedor_id) AS supplierCount,
           MIN(ps.price) AS minPrice, MAX(ps.price) AS maxPrice,
           AVG(ps.price) AS avgPrice,
           ROUND((MAX(ps.price) - MIN(ps.price)) / AVG(ps.price) * 100, 2) AS dispersionPct
    FROM product_supplier ps
    JOIN producto_maestro mp ON mp.id = ps.producto_id
    WHERE ps.price IS NOT NULL AND ps.price > 0
    GROUP BY mp.id, mp.mpn, mp.category
    HAVING COUNT(DISTINCT ps.proveedor_id) >= 2
    ORDER BY dispersionPct DESC
    LIMIT :maxRows
    """, nativeQuery = true)
List<PriceDispersionProjection> findPriceDispersionIndexProjected(@Param("maxRows") int maxRows);
```

**Queries nativas destacadas:**
- `findPriceDispersionIndexProjected` — Dispersión de precios entre proveedores
- `findTradingOpportunitiesProjected` — Oportunidades de trading (margen positivo)
- `findStockoutRatesProjected` — Tasa de ruptura de stock por proveedor
- `findCatalogGrowthProjected` — Crecimiento semanal del catálogo
- `findPriceStabilityProjected` — Score de estabilidad de precio por SKU/proveedor
- `findStockVolatilityProjected` — Índice de volatilidad de stock (24h)

##### 2.4. Capa de Entidades (17 entidades JPA)

| Entidad | Tabla | Propósito |
|---------|-------|-----------|
| `MasterProduct` | `producto_maestro` | Producto único identificado por MPN |
| `ProductSupplier` | `product_supplier` | Oferta de un proveedor para un producto (precio, stock, condición) |
| `Supplier` | `proveedor` | Proveedor externo con API URL y credenciales cifradas |
| `PriceHistory` | `historial_precios` | Historial de cambios de precio |
| `StockHistory` | `stock_history` | Historial de cambios de stock |
| `User` | `usuarios` | Usuarios del sistema |
| `Role` | `roles` | Roles (ADMIN, CLIENTE) |
| `Budget` | `presupuestos` | Presupuesto con estado y totales |
| `BudgetLine` | `presupuestos_lineas` | Línea individual de presupuesto |
| `AppNotification` | `notificaciones` | Notificación in-app |
| `AppNotificationType` | `tipos_notificacion` | Tipo de notificación (enum) |
| `CurrencyRate` | `tasas_cambio` | Tasas de cambio EUR/USD/GBP |
| `DeadLetterQueue` | `dead_letter_queue` | Mensajes fallidos de sincronización |
| `GlobalSyncState` | `sync_state` | Estado global de sincronización |
| `ProviderMapping` | `mapeo_proveedores` | Reglas de mapeo de campos por proveedor |
| `SyncLog` | `sync_log` | Log de sincronización |
| `UserNotificationStatus` | `usuarios_notificaciones` | Estado de lectura por usuario |

---

#### 3. Resiliencia Empresarial

Construí el sistema pensando en que los proveedores externos fallan — y cuando fallan, OmniStock no se cae:

- **Circuit Breaker** con Resilience4j: si un proveedor externo falla (5 llamadas fallidas en 10s), el sistema abre el circuito y responde con gracia durante 30s antes de reintentar
- **Retry con backoff exponencial**: ante errores transitorios (timeouts, 503), reintenta hasta 3 veces con espera progresiva (500ms → 1s → 2s)
- **TimeLimiter**: cada llamada a API externa tiene timeout de 10s; si excede, se lanza `TimeoutException` y el Circuit Breaker lo contabiliza
- **Caché con Caffeine Cache**: las consultas frecuentes (dashboard summary, supplier health) se cachean en memoria con TTL de 5 minutos, reduciendo latencia de 200ms a <5ms
- **Dead Letter Queue**: los mensajes que fallan en sincronización no se pierden — se almacenan en tabla `dead_letter_queue` con el payload original, timestamp y causa del error, para reintento o revisión manual
- **@Async**: la sincronización de catálogos se ejecuta en un hilo separado, permitiendo que el endpoint responda inmediatamente con `202 Accepted` mientras el proceso continúa en background

---

#### 4. Seguridad de Nivel Empresarial

- **JWT con claims personalizados**: `sub` (userId), `username`, `fullName`, `role`, `iat`, `exp` (150 min), `issuer` ("omnistock-api"), `audience` ("omnistock-frontend")
- **Filtro JWT personalizado** (`JwtAuthenticationFilter`): intercepta cada request, extrae el token del header `Authorization: Bearer <token>`, valida firma HMAC-SHA256, y establece el `SecurityContext`
- **Encriptación AES-256/GCM** para datos sensibles de proveedores (API keys, contraseñas) — cifrado autenticado con tag de 128 bits
- **Hash de contraseñas** con BCrypt (strength=12)
- **Protección por rol** en cada endpoint via Spring Security (`@PreAuthorize`):
  - `ROLE_ADMIN`: acceso completo (CRUD usuarios, proveedores, productos)
  - `ROLE_CLIENTE`: solo lectura (dashboard, analytics, presupuestos propios)
- **Refresh tokens**: el token JWT expira a los 150 min; el endpoint `/auth/refresh` permite renovarlo sin reautenticación

---

#### 5. Testing Riguroso (311 tests, 74% cobertura)

Mi filosofía es: si no está testeado, no está terminado.

**Estrategia de testing:**
- **Tests unitarios puros**: cada servicio se testea de forma aislada con Mockito, sin levantar Spring Context
- **Tests de arquitectura**: ArchUnit verifica que las dependencias entre capas sean correctas (Controller no depende de Repository directamente)
- **Cobertura de caminos**: cada test cubre: flujo feliz, caso borde (null, vacío, valores límite), y excepción esperada
- **Nomenclatura BDD**: todos los mensajes de test son descriptivos en español (ej: `"debe lanzar excepción cuando el username ya existe"`)
- **Seeders excluidos**: los seeders se excluyen intencionadamente de la cobertura (son código de inicialización, no lógica de negocio)
- **H2 en tests**: los tests de integración usan H2 en lugar de MariaDB para evitar dependencias externas

**Distribución de tests por servicio:**

| Servicio | Tests | Cobertura | Casos clave |
|----------|-------|-----------|-------------|
| `UserService` | 18 | 100% | CRUD, duplicados, contraseña, perfil |
| `SupplierService` | 14 | 100% | CRUD, dashboard, encriptación |
| `ProviderMappingService` | 10 | 100% | CRUD mapeos, validación estrategias |
| `StockCatalogSyncService` | 10 | 100% | Paginación, estados parciales, errores |
| `ProductSupplierService` | 12 | 100% | Relaciones, ofertas, stock |
| `SyncStateService` | 9 | 100% | Estados IDLE/IN_PROGRESS/COMPLETED/ERROR |
| `ApiClientService` | ~10 | 100% | Circuit Breaker, retry, timeouts |
| `DeadLetterQueueService` | 12 | 100% | Encolar, reintentar, limpiar |
| `PriceHistoryService` | 12 | 100% | Historial, analytics, tendencias |
| `DashboardService` | ~10 | 100% | Summary, stale, top movers |
| `SupplierHealthService` | ~8 | 100% | SLA, latency, error rate |
| `ExportService` | 8 | 100% | Exportación ZIP con CSVs |
| `NotificationService` | 14 | 100% | Crear, leer, marcar, limpiar |
| `EncryptionService` | 9 | 100% | Cifrar/descifrar AES-256 |
| `CurrencyRateService` | 3 | 100% | Tasas, conversión |
| `ArchitectureTest` | ~5 | — | ArchUnit (no aplica cobertura) |

---

#### 6. Código Limpio y Mantenible

- **Excepciones personalizadas**: `RegistrationConflictException`, `ResourceNotFoundException`, `SyncInProgressException` — todas con mensajes en español y código HTTP específico
- **`ApiResponse<T>` genérico**: wrapper uniforme para toda la API con `success`, `message`, `data`, `timestamp`, `metadata` (path, page info), `errors`
- **GlobalExceptionHandler**: `@ControllerAdvice` que captura cualquier excepción no manejada y la envuelve en `ApiResponse` con el código HTTP adecuado
- **Logging estructurado**: SLF4J con niveles DEBUG/INFO/WARN/ERROR según la criticidad, incluyendo IDs de sincronización y tiempos de ejecución
- **Inyección por constructor**: todos los servicios usan constructor injection (sin `@Autowired` directo), facilitando testing y haciendo explícitas las dependencias
- **Perfiles Spring**: `application.properties` (dev), `application-prod.properties` (producción), `application-resilience.properties` (Resilience4j), `application-test.properties` (H2)

---

### En números

| Métrica | Backend (mi foco) | Frontend |
|---------|-------------------|----------|
| **Tests** | 311 | 175 |
| **Cobertura** | 74% (JaCoCo) | 100% servicios / ~18.85% global |
| **Servicios críticos** | 18 servicios (16 testeados) | 8 servicios + 7 contextos/componentes |
| **Endpoints REST** | 54 | — |
| **Entidades JPA** | 17 | — |
| **Repositorios** | 16 | — |
| **Queries nativas** | 12 (analytics + dashboard) | — |
| **Patrones de diseño** | 5 (Facade, Strategy, Template, DTO/Projection, Generic Response) | — |
| **Tecnologías de resiliencia** | Circuit Breaker, Retry, TimeLimiter, Caché, DLQ, @Async | Axios interceptors (retry básico) |
| **Seguridad** | JWT (HMAC-SHA256) + AES-256/GCM + BCrypt | JWT en localStorage + rutas protegidas |
| **Líneas de código (Java)** | ~8,500 (excluyendo tests) | — |


---

## Características Clave e Impacto

### 🏗️ Diseño de Arquitectura
- Participación activa en la toma de decisiones para la estructura del sistema, garantizando escalabilidad y mantenibilidad
- Diseño en capas (Controller → Service → Repository → DTOs) siguiendo principios SOLID y clean architecture
- Migración de una aplicación interna monolítica a una API REST moderna con servicios asíncronos y colas de mensajes

### 📦 Contenedorización
- Configuración completa de entornos aislados con Docker Compose (8 servicios: MariaDB, Backend, Frontend, Mock Server, Prometheus, Alloy, Grafana, Log Reset)
- Multi-stage build en Dockerfile para optimizar el tamaño de la imagen final
- Reverse proxy con Nginx para servir el frontend estático

### 🔒 Seguridad
- Autenticación JWT con refresh tokens (150 min de expiración)
- Hash de contraseñas con BCrypt
- Almacenamiento cifrado de credenciales de proveedores (AES-256)
- Validación de entrada con Spring Validation
- Dos roles bien definidos: `ROLE_ADMIN` (acceso completo) y `ROLE_CLIENTE` (solo lectura)

### 📊 Monitorización
- **Prometheus** scrapea métricas del backend vía `/actuator/prometheus` cada 15s
- **Grafana Alloy** recolecta y reenvía métricas a Prometheus
- **Grafana** con dashboard preconfigurado "OmniStock — Backend Metrics" (18 paneles: JVM, HTTP, GC, BD, logs)
- Dashboard cargado automáticamente vía provisioning al iniciar el contenedor

### 📋 Documentación Automática
- Swagger UI y OpenAPI generados automáticamente desde el código (springdoc-openapi)
- Endpoints documentados con descripciones en español

---

## 📊 Impacto en la Empresa

### Beneficios Cuantificables

| Métrica | Impacto |
|---------|---------|
| **Centralización de datos** | Elimina silos de información entre departamentos |
| **Automatización de sincronización** | Reduce horas hombre en actualización manual de catálogos |
| **Presupuestos en línea** | Acelera el ciclo de cotización de días a minutos |
| **Visibilidad del inventario** | Dashboard en tiempo real reduce roturas de stock |
| **Analytics predictivos** | Identifica productos obsoletos y oportunidades de compra |
| **Exportación automatizada** | Elimina errores de transcripción en órdenes de compra |
| **Seguridad empresarial** | Cumplimiento con estándares de encriptación y control de acceso |

### Valor Diferencial

1. **Evolución del proyecto**: Partiendo de una aplicación interna funcional, se diseñó e implementó una API REST que permite integración con sistemas externos
2. **Resiliencia**: El sistema sigue funcionando aunque un proveedor externo esté caído (Circuit Breaker)
3. **Escalabilidad**: Arquitectura preparada para crecer horizontalmente
4. **Trazabilidad**: Historial completo de precios y cambios de estado
5. **Seguridad**: Encriptación de extremo a extremo (AES-256 + JWT)
6. **Monitoreo**: Dashboard de Grafana con métricas en tiempo real (JVM, HTTP, conexiones BD, logs)

---

## Arquitectura del Sistema

### Diagrama de Arquitectura

```txt
                    ┌─────────────┐
                    │   Usuario   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ Nginx (80)  │
                    │  frontend   │
                    │  :8081      │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │ /api       │ /          │
       ┌──────▼──────┐    │    ┌───────┴────────┐
       │   Backend   │    │    │  Frontend SPA  │
       │  Spring Boot│    │    │  (React+Vite)  │
       │   :8080     │    │    └────────────────┘
       └──────┬──────┘    │
              │            │
    ┌─────────┼─────────┐
    │         │         │
┌───▼───┐ ┌──▼───┐ ┌───▼────┐
│MariaDB│ │Mock  │ │Prometeus│
│:3306  │ │Server│ │:9090   │
└───────┘ │:3001-4│ └───┬────┘
          └────────┘     │
                    ┌────▼────┐
                    │ Grafana │
                    │ :3000   │
                    └─────────┘
```

### Stack Tecnológico

#### Backend
- **Java 21 + Spring Boot 3.2.5** — Framework principal con Spring Security, Spring Data JPA y Hibernate
- **MariaDB LTS (11.8)** — Base de datos relacional; H2 para tests de integración
- **Resilience4j** — Circuit Breaker, Retry con backoff exponencial y TimeLimiter para llamadas a APIs externas
- **Caffeine Cache** — Caché en memoria para catálogos y dashboards (TTL 5 min)
- **Micrometer + Prometheus** — Métricas exportadas vía `/actuator/prometheus`
- **Grafana 11 + Alloy** — Dashboard preconfigurado "OmniStock — Backend Metrics" (18 paneles)
- **Flyway** — Migraciones de base de datos versionadas
- **SpringDoc OpenAPI** — Documentación Swagger automática
- **JWT (HMAC-SHA256) + BCrypt + AES-256/GCM** — Seguridad multicapa
- **JUnit 5 + Mockito + ArchUnit + JaCoCo** — Testing (311 tests, 74% cobertura)

#### Frontend
- **React 18 + TypeScript** — Framework UI con Vite como build tool
- **React Router v6** — Enrutamiento SPA
- **TanStack React Query** — Data fetching y caché del lado del cliente
- **Axios** — Cliente HTTP con interceptores JWT
- **Recharts** — Gráficos del dashboard
- **STOMP + SockJS** — WebSockets para notificaciones en tiempo real
- **ExcelJS** — Exportación de órdenes de compra a Excel
- **Framer Motion** — Animaciones
- **Vitest + Testing Library** — Testing (175 tests)

#### Infraestructura y DevOps
- **Docker + Docker Compose** — 8 servicios containerizados (MariaDB, Backend, Frontend, Mock Server, Prometheus, Alloy, Grafana, Log Reset)
- **Nginx** — Reverse proxy para servir el frontend estático
- **GitHub Actions** — CI pipeline con build, test, lint y smoke tests

### Capas del Proyecto

| Capa | Contenido |
|------|-----------|
| **App (código de negocio)** | `backend/` (Spring Boot) + `frontend/` (React + Vite) |
| **Infra (operación/despliegue)** | `infra/` (json-mock-server, monitoring) |
| **Orquestación** | `docker-compose.yml` (8 servicios) |

### Dependencias Críticas

1. **MariaDB** — sin base de datos, el backend no levanta
2. **json-mock-server** — para sincronizaciones con proveedores mock
3. **Red Docker** — conectividad entre contenedores via `omnistock-net`

---

## Behavior-Driven Development (BDD)

El desarrollo del backend se guió por principios de **Behavior-Driven Development**, asegurando que cada funcionalidad implementada tuviera su correspondiente batería de tests que validaran el comportamiento esperado.

### Principios Aplicados

1. **Los tests describen comportamiento, no implementación**: Cada test lleva un mensaje declarativo en español que describe qué debería ocurrir (ej: "debe lanzar excepción cuando el username ya existe")
2. **Cobertura de caminos felices y casos borde**: No solo se prueba el flujo principal, sino también errores, valores límite y condiciones excepcionales
3. **Aislamiento total**: Todos los tests usan mocks (Mockito) para aislar la unidad bajo prueba
4. **Nomenclatura Given-When-Then implícita**: La estructura de cada test sigue el patrón: preparación (given) → ejecución (when) → verificación (then)

### Resultados de Cobertura

| Módulo | Tests | Cobertura |
|--------|-------|-----------|
| **Backend (Spring Boot)** | **311 tests** | **74%** (JaCoCo) |
| Frontend — Servicios | 175 tests | 100% |
| Frontend — Global | 175 tests | ~18.85% |

> **Nota**: Los seeders del backend fueron excluidos intencionadamente de los tests por ser código de inicialización de datos, no lógica de negocio.

---

## Tests

### Backend (311 tests — 74% cobertura)

| Servicio | Tests | Cobertura |
|----------|-------|-----------|
| SupplierService | 14 | CRUD, dashboard, encriptación |
| ProviderMappingService | 10 | CRUD mapeos |
| UserService | 18 | CRUD usuarios, auth, perfil |
| ExportService | 8 | Exportación ZIP con CSV |
| DeadLetterQueueService | 12 | Operaciones DLQ |
| StockCatalogSyncService | 10 | Sincronización con paginación |
| ProductSupplierService | 12 | Relaciones producto-proveedor |
| SyncStateService | 9 | Estado de sincronización |
| ApiClientService | ~10 | Cliente API con circuit breaker |
| PriceHistoryService | 12 | Historial de precios |
| NotificationService | 14 | Notificaciones |
| EncryptionService | 9 | Encriptación/desencriptación |
| CurrencyRateService | 3 | Tasas de cambio |
| UniversalMapperService | (modificado) | Mapeo universal |

### Frontend (175 tests — 100% servicios)

| Archivo | Tests | Descripción |
|---------|-------|-------------|
| authService.test.ts | 2 | Servicio de login |
| dashboardService.test.ts | 2 | Dashboard summary |
| analyticsService.test.ts | 10 | Analytics API |
| supplierHealthService.test.ts | 2 | Health de proveedores |
| productService.test.ts | 7 | CRUD productos |
| supplierService.test.ts | 9 | CRUD proveedores |
| userService.test.ts | 5 | CRUD usuarios |
| notificationService.test.ts | 5 | Notificaciones |
| budgetService.test.ts | 8 | Presupuestos |
| api.test.ts | 12 | Interceptores Axios |
| api.coverage.test.ts | 3 | Cobertura api.ts |
| AuthContext.test.tsx | 10 | Contexto de autenticación |
| BudgetContext.test.tsx | 10 | Contexto de presupuesto |
| exportPurchaseOrder.test.ts | 5 | Exportación Excel |
| formatting.test.ts | 7 | Formateo de moneda |
| useDebounce.test.ts | 6 | Hook debounce |
| useFieldErrors.test.ts | 7 | Hook errores de campo |
| Componentes UI | ~50 | Badge, Pagination, DataTable, etc. |

---

## 🛠️ Tecnologías Utilizadas

OmniStock se apoya en un stack moderno y probado:

**Backend** — Java 21, Spring Boot 3.2.5, Spring Security, Spring Data JPA, Hibernate, MariaDB LTS (11.8), Resilience4j, Caffeine Cache, Flyway, SpringDoc OpenAPI, JWT (HMAC-SHA256), BCrypt, AES-256/GCM, JUnit 5, Mockito, ArchUnit, JaCoCo.

**Frontend** — React 18, TypeScript, Vite, React Router v6, TanStack React Query, Axios, Recharts, STOMP + SockJS, ExcelJS, Framer Motion, Vitest, Testing Library.

**Infraestructura** — Docker, Docker Compose, Nginx, Prometheus, Grafana 11, Alloy, GitHub Actions.

---

## 📋 Endpoints de la API

### Autenticación (`/api/v1/auth`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/auth/login` | Iniciar sesión (devuelve JWT) |
| POST | `/auth/register` | Registrar nuevo usuario (rol CLIENTE) |
| GET | `/auth/validate` | Validar si un token sigue siendo válido |
| POST | `/auth/refresh` | Renovar token JWT |

### Productos (`/api/v1/productos`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/productos/` | Listar productos (paginado, ordenable) |
| GET | `/productos/search` | Buscar productos con filtros (query, categoría, proveedor, specs) |
| GET | `/productos/{id}` | Detalle completo del producto con ofertas por proveedor |
| GET | `/productos/{id}/historial/{proveedorId}` | Historial de precios del producto con un proveedor |
| POST | `/productos/sync-all` | Iniciar sincronización masiva de catálogos (background) |
| GET | `/productos/sync-status` | Estado actual de la sincronización (IDLE/IN_PROGRESS/COMPLETED/ERROR) |

### Proveedores (`/api/v1/proveedores`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/proveedores/list` | Lista simple (id, nombre) para desplegables |
| GET | `/proveedores/` | Listar todos los proveedores con todos los campos |
| GET | `/proveedores/{id}` | Detalle del proveedor |
| GET | `/proveedores/{id}/dashboard` | Dashboard con KPIs del proveedor |
| POST | `/proveedores/` | Crear un nuevo proveedor |
| PUT | `/proveedores/{id}` | Actualizar datos del proveedor |
| DELETE | `/proveedores/{id}` | Eliminar proveedor |
| GET | `/proveedores/{id}/mappings` | Mapeos de campos del proveedor |
| POST | `/proveedores/{id}/mappings` | Crear regla de mapeo |
| PUT | `/mappings/{id}` | Actualizar regla de mapeo |
| DELETE | `/mappings/{id}` | Eliminar regla de mapeo |

### Dashboard (`/api/v1/dashboard`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/dashboard/summary` | Resumen ejecutivo: totales, disponibilidad, gráficos, zombies, proveedores caídos |
| GET | `/dashboard/supplier-health` | KPIs de salud operativa de proveedores (SLA Score, Sync Success Rate, Latency, Error Rate) |
| GET | `/dashboard/export/csv` | Descargar ZIP con CSVs del dashboard |

### Analytics BI (`/api/v1/analytics`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/analytics/price-variation` | Variación mensual del índice de precios por proveedor (LAG) |
| GET | `/analytics/stockout-rates` | Tasa de ruptura de stock por proveedor |
| GET | `/analytics/price-dispersion` | Dispersión de precios entre proveedores por SKU |
| GET | `/analytics/moq` | Distribución de MOQ por proveedor |
| GET | `/analytics/condition-mix` | Mezcla de condiciones de producto por proveedor |
| GET | `/analytics/cost-coverage` | Cobertura de proveedor por SKU (riesgo proveedor único) |
| GET | `/analytics/stock-volatility` | Índice de volatilidad de stock (24h) |
| GET | `/analytics/catalog-growth` | Tendencia de crecimiento del catálogo (semanal) |
| GET | `/analytics/price-stability` | Score de estabilidad de precio por SKU/proveedor |
| GET | `/analytics/trading-opportunities` | Oportunidades de trading: productos con margen positivo entre coste y PVP |

### Presupuestos (`/api/v1/budget`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/budget/simulate` | Simular presupuesto con líneas de producto |
| GET | `/budget/user` | Listar presupuestos del usuario autenticado |
| GET | `/budget/{id}` | Obtener detalle de un presupuesto por ID |
| PUT | `/budget/{id}/status` | Actualizar estado del presupuesto |
| DELETE | `/budget/{id}` | Eliminar presupuesto |
| POST | `/budget/{id}/notify-export` | Notificar exportación de presupuesto |

### Historial de Precios (`/api/v1/historial-precios`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/historial-precios/producto/{id}/proveedor/{id}` | Historial de precios de un producto con un proveedor |
| GET | `/historial-precios/producto/{id}` | Historial de precios de un producto (todos los proveedores) |
| GET | `/historial-precios/producto/{id}/proveedor/{id}/analytics` | Análisis BI de precios (media, min, max, variación) |
| GET | `/historial-precios/producto/{id}/comparativa-proveedores` | Comparativa de precios entre proveedores |
| GET | `/historial-precios/producto/{id}/proveedor/{id}/por-mes` | Historial agrupado por mes |
| POST | `/historial-precios/registrar` | Registrar cambio de precio manualmente |
| GET | `/historial-precios/producto/{id}/proveedor/{id}/ultimo-precio` | Último precio registrado |
| GET | `/historial-precios/producto/{id}/proveedor/{id}/tendencia` | Tendencia de precios (ALZA/BAJA/ESTABLE) |

### Usuarios (`/api/v1/admin/usuarios` y `/api/v1/usuarios`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/admin/usuarios` | Listar todos los usuarios |
| POST | `/admin/usuarios` | Crear un nuevo usuario |
| GET | `/admin/usuarios/{id}` | Obtener usuario por ID |
| PUT | `/admin/usuarios/{id}` | Actualizar usuario |
| DELETE | `/admin/usuarios/{id}` | Eliminar usuario |
| PUT | `/usuarios/perfil` | Actualizar perfil del usuario autenticado |

### Notificaciones (`/api/v1/notificaciones`)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/notificaciones/` | Listar notificaciones (por tab, con límite) |
| GET | `/notificaciones/counts` | Conteos de notificaciones no leídas |
| PUT | `/notificaciones/{id}/read` | Marcar notificación como leída |
| PUT | `/notificaciones/read-all` | Marcar todas como leídas |

### Utilidades

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check del sistema |
| GET | `/actuator/prometheus` | Métricas Prometheus |
| GET | `/v3/api-docs/**` | Documentación OpenAPI |
| GET | `/swagger-ui/**` | Interfaz Swagger UI |

---

## 🚀 Cómo Ejecutar el Proyecto

### Requisitos
- Docker Desktop (con Docker Compose)
- Git

### Pasos

```bash
# 1. Clonar el repositorio
git clone <repo-url>
cd OmniStock

# 2. Copiar variables de entorno
copy .env.example .env

# 3. Configurar las variables en .env (ver sección de variables)

# 4. Levantar todos los servicios (BD, Backend, Frontend, Monitorización)
docker compose up -d --build

# 5. Acceder a la aplicación
# Frontend: http://localhost:8081
# Backend health: http://localhost:8080/actuator/health
# Swagger UI: http://localhost:8080/swagger-ui.html

# 6. Ver el dashboard de métricas
# Grafana: http://localhost:3000 (usuario/contraseña del .env)
# Prometheus: http://localhost:9090
```

### Comandos útiles

```bash
# Ver logs de todos los servicios
docker compose logs -f

# Ver logs de un servicio específico
docker compose logs -f backend
docker compose logs -f frontend

# Detener servicios
docker compose down

# Detener y eliminar volúmenes (borra datos)
docker compose down -v

# Reconstruir un servicio específico
docker compose up -d --build backend
```

### Despliegue desde cero (limpiar todo y reiniciar)

```powershell
# 1. Detener servicios
docker-compose down

# 2. Borrar datos de MariaDB y logs
Remove-Item .\data\mariadb\* -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item .\logs\* -Recurse -Force -ErrorAction SilentlyContinue

# 3. Reconstruir y levantar
docker-compose build --no-cache
docker-compose up -d --build

# 4. Verificar que todo está funcionando
docker-compose logs --tail=50 backend
```

### Verificación con Postman Collection

El proyecto incluye una colección de Postman (`OmniStock.postman_collection.json`) que prueba los **14 endpoints principales** de la API, junto con un archivo de entorno (`OmniStock.postman_environment.json`) con las variables preconfiguradas.

#### Requisitos

- **Postman** (app desktop) o **Newman** (CLI): `npm install -g newman`
- Los servicios deben estar levantados: `docker compose up -d`

#### Usar con Postman (GUI)

1. Abre Postman → **Import** → selecciona ambos archivos:
   - `OmniStock.postman_collection.json`
   - `OmniStock.postman_environment.json`
2. Selecciona el environment **"OmniStock Local"** en el desplegable superior derecho
3. Ejecuta la colección: **Run collection** → **Run OmniStock**
4. El flujo es:
   - **Paso 1**: `POST /auth/login` → autocompleta el `token` automáticamente
   - **Pasos 2-14**: Endpoints autenticados (usan el token del paso 1)

#### Usar con Newman (CLI)

```bash
# Con environment (recomendado)
npx newman run OmniStock.postman_collection.json -e OmniStock.postman_environment.json

# Sin environment (usa valores por defecto)
npx newman run OmniStock.postman_collection.json
```

#### Variables del Environment

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `baseUrl` | `http://localhost:8080` | URL base de la API |
| `username` | `admin` | Usuario para login |
| `password` | `admin123` | Contraseña (tipo: secret) |
| `token` | *(vacío)* | Se autocompleta tras el login vía script de tests |

> **Nota**: El token se autocompleta automáticamente gracias al script de tests de Postman que extrae el JWT de la respuesta del login y lo asigna a la variable `token`.

### Test Suite de Despliegue (PowerShell)

El proyecto incluye un script PowerShell (`test-deploy.ps1`) que automatiza la verificación completa del despliegue sin necesidad de Postman:

```powershell
# Ejecutar el test suite
.\test-deploy.ps1
```

#### ¿Qué hace el script?

1. **Login**: Autentica con credenciales por defecto (`admin@omnistock.com` / `Admin123!`)
2. **Extrae JWT**: Captura el token de la respuesta del login automáticamente
3. **Prueba 14 endpoints** autenticados:
   - `GET /dashboard/summary` — Resumen ejecutivo
   - `GET /dashboard/supplier-health` — Salud de proveedores
   - `GET /analytics/stockout-rates` — Ruptura de stock
   - `GET /analytics/price-dispersion` — Dispersión de precios
   - `GET /analytics/moq` — MOQ distribution
   - `GET /analytics/condition-mix` — Mezcla de condiciones
   - `GET /analytics/cost-coverage` — Cobertura de SKU
   - `GET /analytics/stock-volatility` — Volatilidad de stock
   - `GET /analytics/catalog-growth` — Crecimiento de catálogo
   - `GET /analytics/price-stability` — Estabilidad de precios
   - `GET /analytics/trading-opportunities` — Oportunidades de trading
   - `GET /analytics/price-variation` — Variación de índice de precios
   - `GET /productos/` — Listado de productos
   - `GET /proveedores/list` — Lista de proveedores
4. **Valida respuestas**: Cada endpoint debe devolver `success: true`
5. **Reporte visual**: Resultados con código de colores (verde ✅ / rojo ❌)

#### Ejemplo de salida

```
╔══════════════════════════════════════════════════════════════╗
║     OMNISTOCK - TEST SUITE DE DESPLIEGUE                    ║
╚══════════════════════════════════════════════════════════════╝

[LOGIN] Autenticando...
  ✔ Token obtenido correctamente

[01/14] GET /dashboard/summary → 200 OK ✅ (8.23s)
[02/14] GET /dashboard/supplier-health → 200 OK ✅ (0.58s)
[03/14] GET /analytics/stockout-rates → 200 OK ✅ (0.45s)
...
[14/14] GET /proveedores/list → 200 OK ✅ (0.35s)

╔══════════════════════════════════════════════════════════════╗
║     RESULTADO: 14/14 TESTS PASARON                          ║
╚══════════════════════════════════════════════════════════════╝
```



---

## 📁 Estructura del Proyecto

```
OmniStock/
├── .github/workflows/          # CI pipeline (GitHub Actions)
│   └── ci.yml                  # Backend (Maven test + package) + Frontend (lint + test + build)
├── backend/                    # Backend API (Spring Boot)
│   ├── src/main/java/          # Código fuente Java
│   │   └── com/omnistock/backend/
│   │       ├── configuration/  # Configuración (Security, CORS)
│   │       ├── controller/     # Controladores REST (10 controladores)
│   │       ├── dtos/           # DTOs (auth, product, supplier, dashboard, analytics, pricing, notification, budget)
│   │       ├── entity/         # Entidades JPA (14 entidades)
│   │       ├── repository/     # Repositorios Spring Data JPA (14 repos)
│   │       ├── security/       # JWT Service, JWT Filter
│   │       ├── service/        # Lógica de negocio por dominio
│   │       │   ├── auth/          # Usuarios, roles, cifrado
│   │       │   ├── supplier/      # Proveedores y mapeos API
│   │       │   ├── product/       # Inventario, sync, catálogo (StockQueryService + StockCatalogSyncService + StockServiceImpl facade)
│   │       │   ├── pricing/       # Historial de precios
│   │       │   ├── budget/        # Presupuestos
│   │       │   ├── dashboard/     # KPIs y exportación
│   │       │   ├── analytics/     # Métricas de proveedores
│   │       │   ├── notification/  # Notificaciones in-app
│   │       │   ├── transformation/# Mapeo JSON proveedores (4 estrategias)
│   │       │   ├── integration/   # Cliente HTTP catálogos
│   │       │   └── shared/        # Tipos de cambio (FX)
│   │       └── util/          # ApiResponse wrapper genérico
│   ├── src/main/resources/    # application.properties, db/migration/
│   ├── src/test/              # Tests unitarios e integración (199 tests)
│   └── Dockerfile             # Imagen Docker multi-stage
├── frontend/                   # Frontend (React + TypeScript)
│   ├── src/
│   │   ├── components/        # Componentes UI reutilizables
│   │   ├── features/          # Módulos por funcionalidad
│   │   │   ├── auth/          # Autenticación (context, pages, services)
│   │   │   ├── inventory/     # Inventario (pages, components)
│   │   │   ├── suppliers/     # Proveedores (pages, services)
│   │   │   ├── dashboard/     # Dashboard + Oportunidades (components, pages)
│   │   │   ├── budget/        # Presupuestos (context, pages, services)
│   │   │   │   ├── context/   # BudgetContext.tsx, BudgetContextStore.ts, useBudget.ts
│   │   │   │   ├── pages/     # BudgetPage.tsx, BudgetListPage.tsx, BudgetDetailPage.tsx
│   │   │   │   └── services/  # budgetService.ts
│   │   │   ├── users/         # Usuarios (pages, services)
│   │   │   ├── priceHistory/  # Historial de precios (pages)
│   │   │   ├── analytics/     # Analytics BI (components, constants, pages)
│   │   │   └── notifications/ # Notificaciones (components, context, pages, services)
│   │   ├── pages/             # Páginas (NotFound)
│   │   ├── router/            # ProtectedRoute
│   │   ├── services/          # Axios instance con interceptors JWT
│   │   ├── constants/         # Rutas API, enlaces externos
│   │   └── types.ts           # Interfaces TypeScript
│   ├── src/test/              # Tests Vitest (38 tests)
│   ├── nginx/                 # Configuración Nginx
│   └── Dockerfile             # Imagen Docker Nginx
├── infra/                     # Configuración de infraestructura
│   ├── monitoring/            # Prometheus, Grafana, Alloy
│   │   ├── prometheus/        # prometheus.yml (scrape backend cada 15s)
│   │   ├── alloy/             # config.alloy (remote write a Prometheus)
│   │   └── grafana/           # Dashboards + provisioning
│   │       ├── dashboards/    # omnistock-backend.json (18 paneles)
│   │       └── provisioning/  # Datasource Prometheus + provider dashboards
│   └── json-mock-server/      # Mock APIs de proveedores (4 APIs: plana, jerárquica, EAV, legacy)
├── docker-compose.yml         # Orquestación de servicios (8 servicios)
├── .env                       # Variables de entorno (local)
├── .env.example               # Template de variables de entorno
├── BusinessLogic.md           # Permisos, renderizado, arquitectura y estado actual
├── ToDo.md                    # Estado del refactor y checklist de validación
└── PROVIDER_FIELD_MAPPING_GUIDE.md  # Guía de mapeo de campos de proveedores
```

---

## 🔄 Flujo de Sincronización de Catálogos: API Externa → Normalización → Inventario

Este es el flujo recomendado para sincronizar catálogos de proveedores externos de forma automatizada.

### ¿Qué se almacena realmente en la BD?

Cuando haces un `POST /api/v1/productos/sync-all`, los catálogos de los proveedores se obtienen vía API, se normalizan y se persisten en MariaDB. No se almacena el JSON crudo, sino los campos estructurados del modelo:

| Campo | Ejemplo | Se almacena |
|-------|---------|-------------|
| `mpn` | `ABC-123` | ✅ |
| `brand` | `Dell` | ✅ |
| `model` | `PowerEdge R740` | ✅ |
| `category` | `Servidor` | ✅ |
| `techSpec` | `"RAM: 64GB, CPU: 2.5GHz"` | ✅ |
| `ean` | `8712345678904` | ✅ |
| `stock` | `150` | ✅ |
| `price` | `1250.00` | ✅ |
| `currency` | `EUR` | ✅ |
| `condition` | `NEW` | ✅ |
| `supplierId` | `1` | ✅ (FK) |
| `supplierSku` | `SUP-001` | ✅ |
| `imageUrl` | `https://...` | ✅ |
| `description` | `Servidor Dell PowerEdge...` | ✅ |
| `minQuantity` | `5` | ✅ (MOQ) |
| `lastUpdated` | `2025-03-15T10:30:00` | ✅ |
| `rawJson` | `{...}` | ❌ (no se almacena) |

### Estrategias de Mapeo Soportadas

El sistema soporta 4 estrategias de mapeo para adaptarse a diferentes formatos de API de proveedores:

| Estrategia | Descripción | Ejemplo de API |
|------------|-------------|----------------|
| `DIRECT` | El campo está en la raíz del JSON | `{ "mpn": "ABC-123" }` |
| `NESTED` | El campo está dentro de un objeto anidado | `{ "product": { "mpn": "ABC-123" } }` |
| `FIND_IN_ARRAY` | El campo está dentro de un array, filtrado por clave-valor | `{ "attributes": [{"name":"MPN","value":"ABC-123"}] }` |
| `SPLIT` | El campo se compone de dos valores separados por un delimitador | `{ "title": "Dell PowerEdge R740" }` → brand=`Dell`, model=`PowerEdge R740` |

### Paso a Paso: Sincronizar un Catálogo

#### 1. Configurar un Proveedor

```json
POST /api/v1/proveedores/
{
  "name": "Proveedor A",
  "apiUrl": "http://json-mock-server:3001/api/products",
  "apiKey": "mock-api-key-001",
  "contactEmail": "proveedor@example.com",
  "currency": "EUR"
}
```

#### 2. Configurar los Mapeos de Campos

```json
POST /api/v1/proveedores/1/mappings
[
  {
    "sourceField": "mpn",
    "targetField": "mpn",
    "strategy": "DIRECT"
  },
  {
    "sourceField": "product.name",
    "targetField": "brand",
    "strategy": "NESTED"
  },
  {
    "sourceField": "attributes",
    "targetField": "category",
    "strategy": "FIND_IN_ARRAY",
    "filterKey": "name",
    "filterValue": "Category"
  },
  {
    "sourceField": "title",
    "targetField": "model",
    "strategy": "SPLIT",
    "delimiter": " ",
    "position": 1
  }
]
```

#### 3. Iniciar la Sincronización

```json
POST /api/v1/productos/sync-all
→ 202 Accepted
{
  "success": true,
  "message": "Sincronización iniciada correctamente",
  "data": {
    "syncId": "sync-1710518400000",
    "status": "IN_PROGRESS"
  }
}
```

#### 4. Consultar el Estado

```json
GET /api/v1/productos/sync-status
→ 200 OK
{
  "success": true,
  "data": {
    "status": "IN_PROGRESS",
    "progress": {
      "total": 3,
      "completed": 1,
      "failed": 0,
      "errors": []
    }
  }
}
```

#### 5. Consultar los Productos Sincronizados

```json
GET /api/v1/productos/?page=0&size=10
→ 200 OK
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "mpn": "ABC-123",
        "brand": "Dell",
        "model": "PowerEdge R740",
        "category": "Servidor",
        "price": 1250.00,
        "currency": "EUR",
        "stock": 150,
        "supplierName": "Proveedor A"
      }
    ],
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 🔄 Flujo de Simulación de Presupuestos

Este flujo describe cómo un usuario puede explorar productos, comparar ofertas de diferentes proveedores, añadirlos a un carrito de presupuesto y exportar una orden de compra.

### Paso a Paso: Simular un Presupuesto

#### 1. Explorar Productos

El usuario navega por el listado de productos sincronizados, pudiendo filtrar por categoría, proveedor, rango de precio y especificaciones técnicas.

```
GET /api/v1/productos/search?query=servidor&minPrice=1000&maxPrice=2000
```

#### 2. Comparar Ofertas

Al seleccionar un producto, se muestran todas las ofertas disponibles de diferentes proveedores, ordenadas por precio.

```
GET /api/v1/productos/1
→ Muestra: Proveedor A → 1250.00€ | Proveedor B → 1180.00€ | Proveedor C → 1300.00€
```

#### 3. Añadir al Carrito de Presupuesto

El usuario selecciona la mejor oferta y la añade al carrito con una cantidad deseada.

```json
POST /api/v1/budget/simulate
{
  "lines": [
    {
      "productSupplierId": 1,
      "quantity": 10,
      "unitPrice": 1180.00
    },
    {
      "productSupplierId": 5,
      "quantity": 5,
      "unitPrice": 450.00
    }
  ]
}
```

#### 4. Revisar y Finalizar el Presupuesto

El sistema calcula automáticamente los totales, valida disponibilidad de stock y MOQ, y permite al usuario finalizar el presupuesto.

```json
PUT /api/v1/budget/1/status
{
  "status": "FINALIZADO"
}
```

#### 5. Exportar Orden de Compra

Una vez finalizado, el usuario puede exportar el presupuesto a Excel con un solo clic desde el frontend. La exportación incluye:

- **Cabecera**: Número de presupuesto, fecha, estado, usuario
- **Líneas**: Producto, proveedor, cantidad, precio unitario, subtotal
- **Resumen**: Subtotal, IVA, descuentos, total
- **Formato profesional**: Encabezados con color, bordes, columnas ajustadas al contenido

---

## 🧪 Testing

### Backend

```bash
# Ejecutar todos los tests
cd backend
./mvnw test

# Ejecutar tests con cobertura (JaCoCo)
./mvnw verify

# Ver reporte de cobertura
# Abrir backend/target/site/jacoco/index.html en el navegador

# Ejecutar un test específico
./mvnw test -Dtest=SupplierServiceTest

# Ejecutar tests de un paquete
./mvnw test -Dtest="com.omnistock.backend.service.*"
```

### Frontend

```bash
# Ejecutar todos los tests
cd frontend
npx vitest run

# Ejecutar tests en modo watch (para desarrollo)
npx vitest

# Ejecutar tests con cobertura
npx vitest run --coverage

# Ver reporte de cobertura
# Abrir frontend/coverage/index.html en el navegador
```

### CI Pipeline (GitHub Actions)

El pipeline de CI ejecuta automáticamente en cada push y pull request:

1. **Backend**: `mvn test` + `mvn package` (compila y testea)
2. **Frontend**: `npm ci` + `npm run lint` + `npx vitest run` + `npm run build`
3. **Postman / Smoke Tests** (depende de backend + frontend):
   - Construye las imágenes Docker del backend y frontend
   - Levanta todos los servicios con `docker compose up -d --wait`
   - Espera a que el backend esté healthy (`/actuator/health`)
   - Ejecuta `test-deploy.ps1` (14 endpoints: login + analytics + productos + proveedores + dashboard)
   - Sube los resultados como artifact (`postman-test-results`)
   - Hace cleanup con `docker compose down -v`
4. **Resultado**: Si algún test falla, el pipeline falla y se notifica al equipo

---

## 📄 Licencia

Este proyecto está bajo la licencia MIT. Consulta el archivo `LICENSE` para más detalles.

---

## 📚 Documentación Adicional

| Documento | Descripción |
|-----------|-------------|
| [`BusinessLogic.md`](BusinessLogic.md) | Permisos, renderizado, arquitectura y decisiones de negocio |
| [`PROVIDER_FIELD_MAPPING_GUIDE.md`](PROVIDER_FIELD_MAPPING_GUIDE.md) | Guía detallada de mapeo de campos de proveedores |
| [`OmniStock.postman_collection.json`](OmniStock.postman_collection.json) | Colección de Postman con 14 endpoints para verificar el despliegue |
| [`OmniStock.postman_environment.json`](OmniStock.postman_environment.json) | Variables de entorno de Postman (baseUrl, credenciales) |
| [`test-deploy.ps1`](test-deploy.ps1) | Script PowerShell que automatiza la verificación completa del despliegue (login JWT + 14 tests) |
| [`docker-compose.yml`](docker-compose.yml) | Orquestación de servicios (8 servicios) |
| `infra/monitoring/grafana/dashboards/omnistock-backend.json` | Dashboard de Grafana preconfigurado (18 paneles) |
| `infra/monitoring/prometheus/prometheus.yml` | Configuración de scraping de Prometheus |
| `infra/monitoring/alloy/config.alloy` | Configuración de Grafana Alloy |
| `infra/json-mock-server/` | Mock APIs de proveedores (4 APIs diferentes) |
