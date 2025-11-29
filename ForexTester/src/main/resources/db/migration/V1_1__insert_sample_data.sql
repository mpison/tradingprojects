
-- === INSERT ENUM DATA ===

INSERT INTO asset_classes (name) VALUES 
  ('Forex'), ('Stocks'), ('ETFs'), ('Crypto'), ('Commodities'),
  ('Indices'), ('Bonds'), ('Options'), ('Futures')
ON CONFLICT DO NOTHING;

INSERT INTO instrument_types (name) VALUES 
  ('Spot'), ('CFD'), ('Option'), ('Future'), ('Swap'),
  ('Forward'), ('Perpetual'), ('Synthetic')
ON CONFLICT DO NOTHING;

-- === INSERT EURUSD for FBS ===

INSERT INTO brokers (name, website, regulation_info, api_endpoint)
VALUES ('FBS', 'https://fbs.com', 'IFSC Belize, CySEC', 'https://api.fbs.com')
ON CONFLICT DO NOTHING;


DO $$
DECLARE
    _broker_id BIGINT;
    _asset_id BIGINT;
    _instr_id BIGINT;
    _symbol_id BIGINT;
BEGIN
    SELECT id INTO _broker_id FROM brokers WHERE name = 'FBS';
    SELECT id INTO _asset_id FROM asset_classes WHERE name = 'Forex';
    SELECT id INTO _instr_id FROM instrument_types WHERE name = 'Spot';

    INSERT INTO symbols (
        broker_id, asset_class_id, instrument_type_id, 
        symbol_code, display_name, base_currency, quote_currency, exchange_name
    )
    VALUES (
        _broker_id, _asset_id, _instr_id,
        'EURUSD', 'Euro vs US Dollar', 'EUR', 'USD', 'FBS Forex Market'
    )
    ON CONFLICT DO NOTHING;

    SELECT id INTO _symbol_id FROM symbols 
    WHERE symbol_code = 'EURUSD' AND broker_id = _broker_id;

    INSERT INTO contract_specifications (
        symbol_id, contract_size, min_lot_size, max_lot_size, lot_step,
        tick_size, tick_value, leverage, margin_currency,
        swap_long, swap_short, commission_per_lot, is_commission_percent, triple_swap_day
    )
    VALUES (
        _symbol_id, 100000, 0.01, 100.00, 0.01,
        0.0001, 10.00, 500, 'USD',
        -3.25, 1.15, 7.00, FALSE, 3
    )
    ON CONFLICT DO NOTHING;

    FOR i IN 1..5 LOOP
        INSERT INTO trading_sessions (symbol_id, day_of_week, session_start, session_end)
        VALUES (_symbol_id, i, TIME '00:00', TIME '23:59')
        ON CONFLICT DO NOTHING;
    END LOOP;
END $$;
