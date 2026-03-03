"""Shared fixtures for budget_manager test suite."""

import os
import sqlite3
import tempfile
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture(scope="function")
def test_db():
    """Create a temporary SQLite database for each test.

    Yields (conn, db_path) so tests can inspect the database directly
    if needed.  The database file is deleted after the test.
    """
    fd, db_path = tempfile.mkstemp(suffix=".db")
    os.close(fd)

    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")

    # Mirror the exact schema from app/database.py
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

    yield conn, db_path

    conn.close()
    os.unlink(db_path)


@pytest.fixture(scope="function")
def client(test_db):
    """Create a TestClient whose database calls go to the temp DB.

    Because the routers import ``get_db`` with
    ``from app.database import get_db`` (a direct call, *not* FastAPI
    ``Depends``), we must monkey-patch the name **in every module that
    imported it**.  That means patching both
    ``app.routers.transactions.get_db`` and ``app.routers.summary.get_db``.
    """
    _conn, db_path = test_db

    def _test_get_db() -> sqlite3.Connection:
        """Return a fresh connection to the temporary test database."""
        conn = sqlite3.connect(db_path)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA foreign_keys = ON")
        return conn

    with (
        patch("app.routers.transactions.get_db", _test_get_db),
        patch("app.routers.summary.get_db", _test_get_db),
    ):
        with TestClient(app) as c:
            yield c
