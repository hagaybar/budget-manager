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
    """Create the transactions table if it doesn't exist."""
    conn = get_db()
    try:
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
        conn.commit()
    finally:
        conn.close()
