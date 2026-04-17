CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    health_profile JSONB,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS food_items (
    food_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    manufacturer VARCHAR(100),
    barcode VARCHAR(50) UNIQUE,
    image_url VARCHAR(512),
    calories DECIMAL(6, 2) NOT NULL,
    carbs DECIMAL(6, 2) NOT NULL DEFAULT 0,
    protein DECIMAL(6, 2) NOT NULL DEFAULT 0,
    fat DECIMAL(6, 2) NOT NULL DEFAULT 0,
    sugars DECIMAL(6, 2) NOT NULL DEFAULT 0,
    sodium DECIMAL(6, 2) NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_food_name ON food_items (name);

CREATE TABLE IF NOT EXISTS additives (
    additive_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    purpose VARCHAR(50),
    danger_level INT NOT NULL,
    daily_acceptable_intake VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS food_additives_map (
    map_id BIGSERIAL PRIMARY KEY,
    food_id BIGINT NOT NULL REFERENCES food_items(food_id),
    additive_id BIGINT NOT NULL REFERENCES additives(additive_id),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_food_additives_map_food_additive UNIQUE (food_id, additive_id)
);

CREATE INDEX IF NOT EXISTS idx_food_additives_map_food_id ON food_additives_map (food_id);
CREATE INDEX IF NOT EXISTS idx_food_additives_map_additive_id ON food_additives_map (additive_id);

CREATE TABLE IF NOT EXISTS meals (
    meal_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    meal_date DATE NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    total_calories DECIMAL(8, 2),
    health_score INT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_meals_user_id ON meals (user_id);
CREATE INDEX IF NOT EXISTS idx_meals_date ON meals (meal_date);

CREATE TABLE IF NOT EXISTS meal_details (
    detail_id BIGSERIAL PRIMARY KEY,
    meal_id BIGINT NOT NULL REFERENCES meals(meal_id),
    food_id BIGINT NOT NULL REFERENCES food_items(food_id),
    quantity DECIMAL(5, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS violation_logs (
    log_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(user_id),
    input_text TEXT NOT NULL,
    violation_category VARCHAR(50),
    confidence_score DECIMAL(3, 2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS hallucination_logs (
    log_id BIGSERIAL PRIMARY KEY,
    model_version VARCHAR(50),
    prompt_context TEXT,
    generated_response TEXT,
    failed_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    audit_id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL REFERENCES users(user_id),
    action_type VARCHAR(50) NOT NULL,
    target_entity VARCHAR(50) NOT NULL,
    target_id BIGINT,
    payload_snapshot JSONB,
    ip_address VARCHAR(45) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
