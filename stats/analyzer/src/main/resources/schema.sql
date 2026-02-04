-- Таблица сходства мероприятий
CREATE TABLE IF NOT EXISTS event_similarity (
    id BIGSERIAL PRIMARY KEY,
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL CHECK (score >= 0 AND score <= 1),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(event_a, event_b),
    CONSTRAINT event_order CHECK (event_a < event_b)
);

-- Индексы для быстрого поиска
CREATE INDEX IF NOT EXISTS idx_event_similarity_a_b ON event_similarity(event_a, event_b);
CREATE INDEX IF NOT EXISTS idx_event_similarity_score ON event_similarity(score DESC);

-- Таблица взаимодействий пользователей
CREATE TABLE IF NOT EXISTS user_interaction (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    max_weight DOUBLE PRECISION NOT NULL DEFAULT 0,
    last_action_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, event_id)
);

-- Индексы для быстрого поиска
CREATE INDEX IF NOT EXISTS idx_user_interaction_user_event ON user_interaction(user_id, event_id);
CREATE INDEX IF NOT EXISTS idx_user_interaction_user_time ON user_interaction(user_id, last_action_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_interaction_event ON user_interaction(event_id);