PRAGMA journal_mode=WAL;

CREATE TABLE IF NOT EXISTS jobs (
  id TEXT PRIMARY KEY,
  command TEXT NOT NULL,
  state TEXT NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0,
  max_retries INTEGER NOT NULL DEFAULT 3,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  run_at TEXT NOT NULL,
  priority INTEGER NOT NULL DEFAULT 0,
  last_error TEXT,
  locked_by TEXT,
  locked_until TEXT
);

CREATE TABLE IF NOT EXISTS workers (
  name TEXT PRIMARY KEY,
  last_heartbeat TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS config (
  key TEXT PRIMARY KEY,
  value INTEGER NOT NULL
);
INSERT OR IGNORE INTO config(key,value) VALUES('backoff_base', 2);
INSERT OR IGNORE INTO config(key,value) VALUES('default_max_retries', 3);

CREATE TABLE IF NOT EXISTS control (
  key TEXT PRIMARY KEY,
  value INTEGER NOT NULL
);
INSERT OR IGNORE INTO control(key,value) VALUES('stop_all', 0);