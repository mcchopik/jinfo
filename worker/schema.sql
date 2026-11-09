-- Таблица для хранения публичных ключей offline-аккаунтов
CREATE TABLE IF NOT EXISTS offline_keys (
    uuid TEXT PRIMARY KEY,
    pubkey TEXT NOT NULL,
    created INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS profiles (
    uuid TEXT PRIMARY KEY,
    data TEXT NOT NULL,
    skin TEXT,
    updated INTEGER NOT NULL
);

-- Таблица для жалоб на профили
CREATE TABLE IF NOT EXISTS reports (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    profile_uuid TEXT NOT NULL,
    reporter_uuid TEXT NOT NULL,
    time INTEGER NOT NULL,
    FOREIGN KEY (profile_uuid) REFERENCES profiles(uuid) ON DELETE CASCADE
);

-- Индекс для быстрой проверки: жаловался ли этот игрок на этот профиль за последние сутки
CREATE INDEX IF NOT EXISTS idx_reports_profile_reporter_time
    ON reports (profile_uuid, reporter_uuid, time);

-- Индекс для очистки старых жалоб
CREATE INDEX IF NOT EXISTS idx_reports_time ON reports (time);