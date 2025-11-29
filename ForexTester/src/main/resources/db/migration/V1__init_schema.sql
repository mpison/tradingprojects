-- === TABLE CREATION ===

CREATE TABLE IF NOT EXISTS brokers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    website VARCHAR(255),
    regulation_info TEXT,
    api_endpoint VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS asset_classes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS instrument_types (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS symbols (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    broker_id BIGINT REFERENCES brokers(id),
    asset_class_id BIGINT REFERENCES asset_classes(id),
    instrument_type_id BIGINT REFERENCES instrument_types(id),
    symbol_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100),
    base_currency VARCHAR(10),
    quote_currency VARCHAR(10),
    exchange_name VARCHAR(100),
    UNIQUE(symbol_code, broker_id)
);

CREATE TABLE IF NOT EXISTS contract_specifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol_id BIGINT REFERENCES symbols(id),
    contract_size DECIMAL(18,8),
    min_lot_size DECIMAL(10,5),
    max_lot_size DECIMAL(10,5),
    lot_step DECIMAL(10,5),
    tick_size DECIMAL(10,8),
    tick_value DECIMAL(18,8),
    leverage DECIMAL(10,2),
    margin_currency VARCHAR(10),
    swap_long DECIMAL(10,6),
    swap_short DECIMAL(10,6),
    commission_per_lot DECIMAL(10,6),
    is_commission_percent BOOLEAN DEFAULT FALSE,
    triple_swap_day INTEGER CHECK (triple_swap_day BETWEEN 0 AND 6) DEFAULT 3
);

CREATE TABLE IF NOT EXISTS trading_sessions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol_id BIGINT REFERENCES symbols(id),
    day_of_week INTEGER CHECK (day_of_week BETWEEN 0 AND 6),
    session_start TIME NOT NULL,
    session_end TIME NOT NULL
);