ALTER TABLE meals
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_meals_date_deleted ON meals (meal_date, is_deleted);

CREATE TABLE IF NOT EXISTS search_miss_logs (
    miss_id BIGSERIAL PRIMARY KEY,
    keyword VARCHAR(150) NOT NULL,
    hit_count INT NOT NULL DEFAULT 1,
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_search_miss_logs_keyword ON search_miss_logs (keyword);
CREATE INDEX IF NOT EXISTS idx_search_miss_logs_resolved_deleted ON search_miss_logs (is_resolved, is_deleted);
