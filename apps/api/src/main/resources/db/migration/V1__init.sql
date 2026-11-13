CREATE TABLE users (
    id            uuid PRIMARY KEY,
    email         varchar(255) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    role          varchar(255) NOT NULL,
    created_at    timestamp(6) with time zone NOT NULL
);

CREATE TABLE refresh_tokens (
    id         uuid PRIMARY KEY,
    token      uuid NOT NULL UNIQUE,
    expires_at timestamp(6) with time zone NOT NULL,
    user_id    uuid NOT NULL REFERENCES users (id)
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

CREATE TABLE logs (
    id          uuid PRIMARY KEY,
    file_name   varchar(255) NOT NULL,
    user_id     uuid NOT NULL,
    uploaded_at timestamp(6) with time zone NOT NULL,
    version     bigint
);
CREATE INDEX idx_logs_user_id ON logs (user_id);

CREATE TABLE log_chunk (
    id      uuid PRIMARY KEY,
    content text NOT NULL,
    log_id  uuid NOT NULL REFERENCES logs (id)
);
CREATE INDEX idx_log_chunk_log_id ON log_chunk (log_id);

CREATE TABLE log_entries (
    id           uuid PRIMARY KEY,
    user_id      uuid NOT NULL,
    service_name varchar(255) NOT NULL,
    level        varchar(255) NOT NULL,
    message      text NOT NULL,
    timestamp    timestamp(6) with time zone NOT NULL
);
CREATE INDEX idx_log_entries_user_ts ON log_entries (user_id, timestamp DESC);

CREATE TABLE idempotency_keys (
    key_id     varchar(255) PRIMARY KEY,
    response   bytea NOT NULL,
    created_at timestamp(6) with time zone NOT NULL
);
