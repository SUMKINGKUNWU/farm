-- Farm Exchange initial PostgreSQL schema.
-- Amounts use integer minor units. Do not use floating point for money.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(64) NOT NULL UNIQUE,
    nickname VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    trade_password_hash VARCHAR(255),
    trade_password_set_at TIMESTAMPTZ,
    trade_password_failed_count INTEGER NOT NULL DEFAULT 0,
    trade_password_locked_until TIMESTAMPTZ,
    role VARCHAR(32) NOT NULL DEFAULT 'PLAYER',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_app_users_role CHECK (role IN ('PLAYER', 'ADMIN')),
    CONSTRAINT chk_app_users_status CHECK (status IN ('ACTIVE', 'FROZEN', 'BANNED'))
);

CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES app_users(id),
    balance BIGINT NOT NULL DEFAULT 10000,
    locked_balance BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_wallets_balance CHECK (balance >= 0),
    CONSTRAINT chk_wallets_locked_balance CHECK (locked_balance >= 0)
);

CREATE TABLE items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    base_price BIGINT NOT NULL,
    current_price BIGINT NOT NULL,
    grow_seconds INTEGER,
    output_item_id UUID REFERENCES items(id),
    output_quantity BIGINT,
    bulk_quantity_threshold BIGINT,
    bulk_amount_threshold BIGINT,
    trade_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_items_type CHECK (item_type IN ('SEED', 'ANIMAL', 'FEED', 'HARVEST', 'TOKEN', 'CONSUMABLE')),
    CONSTRAINT chk_items_prices CHECK (base_price >= 0 AND current_price >= 0),
    CONSTRAINT chk_items_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE player_inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    item_id UUID NOT NULL REFERENCES items(id),
    available_quantity BIGINT NOT NULL DEFAULT 0,
    locked_quantity BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0,
    UNIQUE (user_id, item_id),
    CONSTRAINT chk_inventory_available CHECK (available_quantity >= 0),
    CONSTRAINT chk_inventory_locked CHECK (locked_quantity >= 0)
);

CREATE TABLE expansion_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_type VARCHAR(32) NOT NULL UNIQUE,
    initial_slots INTEGER NOT NULL,
    max_slots INTEGER NOT NULL DEFAULT 16,
    first_expand_cost BIGINT NOT NULL,
    cost_multiplier INTEGER NOT NULL DEFAULT 2,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_expansion_slot_type CHECK (slot_type IN ('FARM', 'RANCH')),
    CONSTRAINT chk_expansion_limits CHECK (initial_slots > 0 AND max_slots >= initial_slots AND max_slots <= 16),
    CONSTRAINT chk_expansion_cost CHECK (first_expand_cost > 0 AND cost_multiplier >= 2)
);

CREATE TABLE farm_plots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    slot_index INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'EMPTY',
    level INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0,
    UNIQUE (user_id, slot_index),
    CONSTRAINT chk_farm_plots_status CHECK (status IN ('EMPTY', 'GROWING', 'READY', 'LOCKED')),
    CONSTRAINT chk_farm_plots_index CHECK (slot_index BETWEEN 1 AND 16)
);

CREATE TABLE ranch_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    slot_index INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'EMPTY',
    level INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0,
    UNIQUE (user_id, slot_index),
    CONSTRAINT chk_ranch_slots_status CHECK (status IN ('EMPTY', 'GROWING', 'READY', 'LOCKED')),
    CONSTRAINT chk_ranch_slots_index CHECK (slot_index BETWEEN 1 AND 16)
);

CREATE TABLE growth_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    slot_type VARCHAR(32) NOT NULL,
    slot_id UUID NOT NULL,
    input_item_id UUID NOT NULL REFERENCES items(id),
    output_item_id UUID NOT NULL REFERENCES items(id),
    output_quantity BIGINT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ready_at TIMESTAMPTZ NOT NULL,
    harvested_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'GROWING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_growth_slot_type CHECK (slot_type IN ('FARM', 'RANCH')),
    CONSTRAINT chk_growth_status CHECK (status IN ('GROWING', 'READY', 'HARVESTED', 'CANCELLED')),
    CONSTRAINT chk_growth_output CHECK (output_quantity > 0),
    CONSTRAINT chk_growth_time CHECK (ready_at > started_at)
);

CREATE TABLE tax_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_type VARCHAR(32) NOT NULL UNIQUE,
    rate_basis_points INTEGER NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES app_users(id),
    updated_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_tax_trade_type CHECK (trade_type IN ('MARKET', 'PRIVATE', 'BULK')),
    CONSTRAINT chk_tax_rate CHECK (rate_basis_points >= 0 AND rate_basis_points <= 5000)
);

CREATE TABLE asset_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    asset_type VARCHAR(32) NOT NULL,
    item_id UUID REFERENCES items(id),
    change_amount BIGINT NOT NULL,
    balance_after BIGINT,
    reason VARCHAR(64) NOT NULL,
    ref_type VARCHAR(64),
    ref_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_asset_type CHECK (asset_type IN ('COIN', 'ITEM', 'TOKEN'))
);

CREATE TABLE tax_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_type VARCHAR(32) NOT NULL,
    ref_type VARCHAR(64) NOT NULL,
    ref_id UUID NOT NULL,
    payer_user_id UUID REFERENCES app_users(id),
    receiver_user_id UUID REFERENCES app_users(id),
    tax_rate_basis_points INTEGER NOT NULL,
    trade_amount BIGINT NOT NULL,
    tax_amount BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_tax_records_type CHECK (trade_type IN ('MARKET', 'PRIVATE', 'BULK')),
    CONSTRAINT chk_tax_records_amount CHECK (trade_amount >= 0 AND tax_amount >= 0)
);

CREATE TABLE market_trades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    item_id UUID NOT NULL REFERENCES items(id),
    side VARCHAR(16) NOT NULL,
    quantity BIGINT NOT NULL,
    unit_price BIGINT NOT NULL,
    gross_amount BIGINT NOT NULL,
    tax_amount BIGINT NOT NULL,
    net_amount BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_market_side CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT chk_market_amount CHECK (quantity > 0 AND unit_price >= 0 AND gross_amount >= 0 AND tax_amount >= 0)
);

CREATE TABLE private_trade_offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_user_id UUID NOT NULL REFERENCES app_users(id),
    buyer_user_id UUID NOT NULL REFERENCES app_users(id),
    item_id UUID NOT NULL REFERENCES items(id),
    quantity BIGINT NOT NULL,
    price_amount BIGINT NOT NULL,
    tax_amount BIGINT NOT NULL DEFAULT 0,
    bulk_token_id UUID,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'WAIT_ACCEPT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_private_offer_users CHECK (seller_user_id <> buyer_user_id),
    CONSTRAINT chk_private_offer_amount CHECK (quantity > 0 AND price_amount >= 0 AND tax_amount >= 0),
    CONSTRAINT chk_private_offer_status CHECK (status IN ('WAIT_ACCEPT', 'SETTLING', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'FAILED'))
);

CREATE TABLE bulk_trade_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    token_code VARCHAR(64) NOT NULL UNIQUE,
    allowed_item_type VARCHAR(32),
    single_trade_limit BIGINT,
    total_limit BIGINT,
    used_amount BIGINT NOT NULL DEFAULT 0,
    remaining_uses INTEGER NOT NULL DEFAULT 1,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_bulk_token_status CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED', 'REVOKED')),
    CONSTRAINT chk_bulk_token_usage CHECK (used_amount >= 0 AND remaining_uses >= 0)
);

ALTER TABLE private_trade_offers
    ADD CONSTRAINT fk_private_trade_bulk_token
    FOREIGN KEY (bulk_token_id) REFERENCES bulk_trade_tokens(id);

CREATE TABLE market_price_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES items(id),
    price BIGINT NOT NULL,
    volume_24h BIGINT NOT NULL DEFAULT 0,
    trade_count_24h BIGINT NOT NULL DEFAULT 0,
    change_basis_points INTEGER NOT NULL DEFAULT 0,
    snapshot_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_price_snapshot CHECK (price >= 0 AND volume_24h >= 0 AND trade_count_24h >= 0)
);

CREATE TABLE admin_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id UUID REFERENCES app_users(id),
    action VARCHAR(96) NOT NULL,
    target_type VARCHAR(64),
    target_id UUID,
    before_data JSONB,
    after_data JSONB,
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_inventory_user ON player_inventory(user_id);
CREATE INDEX idx_growth_user_status ON growth_instances(user_id, status);
CREATE INDEX idx_growth_slot ON growth_instances(slot_type, slot_id, status);
CREATE INDEX idx_asset_ledger_user_time ON asset_ledger(user_id, created_at DESC);
CREATE INDEX idx_tax_records_ref ON tax_records(ref_type, ref_id);
CREATE INDEX idx_market_trades_item_time ON market_trades(item_id, created_at DESC);
CREATE INDEX idx_private_trade_seller_status ON private_trade_offers(seller_user_id, status);
CREATE INDEX idx_private_trade_buyer_status ON private_trade_offers(buyer_user_id, status);
CREATE INDEX idx_price_snapshots_item_time ON market_price_snapshots(item_id, snapshot_at DESC);
CREATE INDEX idx_admin_audit_time ON admin_audit_logs(created_at DESC);

INSERT INTO expansion_config (slot_type, initial_slots, max_slots, first_expand_cost, cost_multiplier)
VALUES
    ('FARM', 4, 16, 1000, 2),
    ('RANCH', 2, 16, 2000, 2);

INSERT INTO tax_config (trade_type, rate_basis_points, updated_reason)
VALUES
    ('MARKET', 300, 'Initial market tax rate 3%'),
    ('PRIVATE', 500, 'Initial private trade tax rate 5%'),
    ('BULK', 300, 'Initial bulk trade tax rate follows market tax');

