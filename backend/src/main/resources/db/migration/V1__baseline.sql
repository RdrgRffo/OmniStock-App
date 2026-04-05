-- OmniStock baseline schema (aligned with JPA entities under com.omnistock.backend.entity)

CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    UNIQUE KEY uk_roles_nombre (nombre)
);

CREATE TABLE IF NOT EXISTS usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(100),
    activo BIT(1) NOT NULL,
    fecha_creacion DATETIME(6),
    fecha_actualizacion DATETIME(6),
    UNIQUE KEY uk_usuario_username (username),
    UNIQUE KEY uk_usuario_email (email)
);

CREATE TABLE IF NOT EXISTS usuarios_roles (
    id_usuario INT NOT NULL,
    id_rol INT NOT NULL,
    PRIMARY KEY (id_usuario, id_rol),
    CONSTRAINT fk_usuarios_roles_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id),
    CONSTRAINT fk_usuarios_roles_rol FOREIGN KEY (id_rol) REFERENCES roles (id)
);

CREATE TABLE IF NOT EXISTS proveedores (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    base_url_api VARCHAR(255) NOT NULL,
    contacto VARCHAR(100),
    email VARCHAR(100),
    horario VARCHAR(100),
    telefono VARCHAR(30),
    website VARCHAR(255),
    pais VARCHAR(3),
    moneda_defecto VARCHAR(3),
    ApiKey VARCHAR(255) NOT NULL,
    endpoint_catalogo VARCHAR(255),
    endpoint_detalle VARCHAR(255),
    endpoint_busqueda VARCHAR(255),
    soporta_sincronizacion_masiva BIT(1),
    activo BIT(1),
    fecha_creacion DATETIME(6),
    fecha_actualizacion DATETIME(6),
    UNIQUE KEY uk_proveedores_nombre (nombre)
);

CREATE TABLE IF NOT EXISTS provider_mapping (
    mapping_id INT AUTO_INCREMENT PRIMARY KEY,
    proveedor_id INT NOT NULL,
    campo_interno VARCHAR(255) NOT NULL,
    campo_externo VARCHAR(255) NOT NULL,
    tipo_transformacion VARCHAR(255),
    fecha_creacion DATETIME(6),
    fecha_actualizacion DATETIME(6),
    CONSTRAINT fk_provider_mapping_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id)
);

CREATE TABLE IF NOT EXISTS producto_maestro (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mpn VARCHAR(255) NOT NULL,
    brand VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    tech_specs TEXT,
    fecha_creacion DATETIME(6),
    fecha_actualizacion DATETIME(6),
    UNIQUE KEY uk_producto_maestro_mpn (mpn),
    KEY idx_producto_maestro_mpn (mpn),
    KEY idx_producto_maestro_brand (brand),
    KEY idx_producto_maestro_model (model),
    KEY idx_producto_maestro_fecha_actualizacion (fecha_actualizacion),
    KEY idx_producto_maestro_category (category),
    KEY idx_producto_maestro_brand_model_mpn (brand, model, mpn)
);

CREATE TABLE IF NOT EXISTS product_supplier (
    id INT AUTO_INCREMENT PRIMARY KEY,
    producto_id INT NOT NULL,
    proveedor_id INT NOT NULL,
    external_provider_id VARCHAR(255),
    price DECIMAL(19, 2) NOT NULL,
    retail_price DECIMAL(19, 2),
    ean VARCHAR(13),
    moq INT,
    product_condition VARCHAR(32),
    currency VARCHAR(3) NOT NULL,
    stock INT NOT NULL,
    last_updated DATETIME(6) NOT NULL,
    is_available BIT(1) NOT NULL,
    data_stale BIT(1) NOT NULL,
    last_error VARCHAR(255),
    UNIQUE KEY uk_product_supplier_producto_proveedor (producto_id, proveedor_id),
    KEY idx_product_supplier_producto (producto_id),
    KEY idx_product_supplier_proveedor (proveedor_id),
    KEY idx_product_supplier_stock (stock),
    CONSTRAINT fk_product_supplier_producto FOREIGN KEY (producto_id) REFERENCES producto_maestro (id),
    CONSTRAINT fk_product_supplier_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id)
);

CREATE TABLE IF NOT EXISTS historial_precios (
    historial_id INT AUTO_INCREMENT PRIMARY KEY,
    producto_id INT,
    proveedor_id INT,
    precio_costo DECIMAL(18, 2) NOT NULL,
    fecha_registro DATETIME(6),
    KEY idx_historial_precios_producto (producto_id),
    KEY idx_historial_precios_proveedor (proveedor_id),
    KEY idx_historial_precios_fecha_registro (fecha_registro),
    CONSTRAINT fk_historial_precios_producto FOREIGN KEY (producto_id) REFERENCES producto_maestro (id),
    CONSTRAINT fk_historial_precios_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id)
);

CREATE TABLE IF NOT EXISTS stock_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id INT NOT NULL,
    proveedor_id INT NOT NULL,
    stock_value INT NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    KEY idx_sh_product_supplier_time (producto_id, proveedor_id, recorded_at),
    CONSTRAINT fk_stock_history_producto FOREIGN KEY (producto_id) REFERENCES producto_maestro (id),
    CONSTRAINT fk_stock_history_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id)
);

CREATE TABLE IF NOT EXISTS currency_rates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(18, 6) NOT NULL,
    updated_at DATETIME(6),
    UNIQUE KEY uq_currency_pair (from_currency, to_currency),
    KEY idx_currency_from (from_currency)
);

CREATE TABLE IF NOT EXISTS global_sync_state (
    id BIGINT PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    updated_at DATETIME(6)
);

INSERT INTO global_sync_state (id, status, updated_at)
VALUES (1, 'IDLE', CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE id = id;

CREATE TABLE IF NOT EXISTS sync_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proveedor_id INT NOT NULL,
    timestamp DATETIME(6) NOT NULL,
    latency_ms BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    items_processed INT,
    KEY idx_sync_logs_supplier (proveedor_id),
    KEY idx_sync_logs_status (status),
    KEY idx_sync_logs_timestamp (timestamp),
    CONSTRAINT fk_sync_logs_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id)
);

CREATE TABLE IF NOT EXISTS budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    budget_number VARCHAR(30) NOT NULL,
    budget_name VARCHAR(200) NOT NULL,
    notes VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    UNIQUE KEY uk_budgets_number (budget_number)
);

CREATE TABLE IF NOT EXISTS budget_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    budget_id BIGINT NOT NULL,
    product_id INT NOT NULL,
    mpn VARCHAR(100),
    product_name VARCHAR(300) NOT NULL,
    brand VARCHAR(100),
    model VARCHAR(200),
    supplier_id INT NOT NULL,
    supplier_name VARCHAR(200) NOT NULL,
    quantity INT NOT NULL,
    stock_available INT,
    unit_price DECIMAL(10, 2) NOT NULL,
    retail_price DECIMAL(10, 2),
    product_url VARCHAR(500),
    notes VARCHAR(500),
    CONSTRAINT fk_budget_lines_budget FOREIGN KEY (budget_id) REFERENCES budgets (id)
);

CREATE TABLE IF NOT EXISTS notificaciones_globales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    dedup_key VARCHAR(255),
    action_path VARCHAR(255),
    scope_tag VARCHAR(40),
    supplier_id INT,
    product_id INT,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    KEY idx_notif_created_at (created_at),
    KEY idx_notif_expires_at (expires_at),
    KEY idx_notif_type (type),
    KEY idx_notif_dedup_key (dedup_key),
    CONSTRAINT fk_notif_supplier FOREIGN KEY (supplier_id) REFERENCES proveedores (id),
    CONSTRAINT fk_notif_product FOREIGN KEY (product_id) REFERENCES producto_maestro (id)
);

CREATE TABLE IF NOT EXISTS notificacion_usuario_estado (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    usuario_id INT NOT NULL,
    read_at DATETIME(6) NOT NULL,
    UNIQUE KEY uq_notif_user_read (notification_id, usuario_id),
    KEY idx_notif_user (usuario_id),
    KEY idx_notif_notification (notification_id),
    CONSTRAINT fk_notif_estado_notif FOREIGN KEY (notification_id) REFERENCES notificaciones_globales (id),
    CONSTRAINT fk_notif_estado_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);
