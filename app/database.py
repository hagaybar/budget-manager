"""SQLite database connection management and table creation."""

import sqlite3
import os
from contextlib import contextmanager

DATABASE_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "budget_manager.db")


def get_db() -> sqlite3.Connection:
    """Return a new SQLite connection with row_factory set to sqlite3.Row."""
    conn = sqlite3.connect(DATABASE_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def create_tables() -> None:
    """Create all tables if they don't exist."""
    conn = get_db()
    try:
        # Create recurring_transactions table FIRST (referenced by transactions FK)
        conn.execute("""
            CREATE TABLE IF NOT EXISTS recurring_transactions (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                type          TEXT    NOT NULL CHECK(type IN ('income', 'expense')),
                amount        REAL    NOT NULL CHECK(amount > 0),
                category      TEXT    NOT NULL,
                description   TEXT    DEFAULT '',
                frequency     TEXT    NOT NULL CHECK(frequency IN ('weekly', 'monthly')),
                day_of_week   INTEGER CHECK(day_of_week IS NULL OR (day_of_week >= 0 AND day_of_week <= 6)),
                day_of_month  INTEGER CHECK(day_of_month IS NULL OR (day_of_month >= 1 AND day_of_month <= 31)),
                start_date    TEXT    NOT NULL,
                end_date      TEXT,
                is_active     INTEGER DEFAULT 1 CHECK(is_active IN (0, 1)),
                created_at    TEXT    DEFAULT (datetime('now'))
            )
        """)

        # Original transactions table
        conn.execute("""
            CREATE TABLE IF NOT EXISTS transactions (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                type        TEXT    NOT NULL CHECK(type IN ('income', 'expense')),
                amount      REAL    NOT NULL CHECK(amount > 0),
                category    TEXT    NOT NULL,
                description TEXT    DEFAULT '',
                date        TEXT    NOT NULL,
                created_at  TEXT    DEFAULT (datetime('now'))
            )
        """)

        # Add recurring_id column to transactions (idempotent for existing DBs)
        try:
            conn.execute("""
                ALTER TABLE transactions
                ADD COLUMN recurring_id INTEGER REFERENCES recurring_transactions(id)
            """)
        except Exception:
            pass  # Column already exists

        conn.commit()
    finally:
        conn.close()
