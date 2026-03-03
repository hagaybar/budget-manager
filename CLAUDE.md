# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Personal budget manager built with FastAPI, SQLite, and Pydantic v2. Currency is Israeli New Shekel (₪). Web UI is a single-page vanilla JS app served as a static file.

## Commands

```bash
# Run dev server
uvicorn app.main:app --reload

# Run all tests
pytest tests/ -v

# Run tests with coverage
pytest tests/ -v --cov=app --cov-report=term-missing

# Run a single test file
pytest tests/test_transactions.py -v

# Run a single test class
pytest tests/test_transactions.py::TestCreateTransaction -v
```

## Architecture

- **app/main.py** — FastAPI app factory with lifespan, CORS, static file mount, router registration
- **app/database.py** — SQLite connection factory (`get_db()`), table creation. No ORM; uses parameterized SQL directly
- **app/schemas.py** — Pydantic v2 models: separate Create/Update/Response schemas with field and model validators
- **app/models.py** — TransactionType enum
- **app/routers/** — Modular routers: `transactions.py`, `summary.py`, `recurring.py`, `backup.py`
- **app/static/index.html** — Entire web UI (vanilla JS, responsive, collapsible sections on mobile)
- **tests/** — pytest with class-based test grouping, fresh temp DB per test via fixtures

### Key Patterns

- **Database isolation in tests**: `conftest.py` patches `get_db()` across all routers to use a temp SQLite DB. Helper functions `_make_transaction()` and `_add_tx()` reduce boilerplate.
- **Router prefixes**: `/api/transactions`, `/api/summary`, `/api/recurring`, `/api/backup`
- **SQLite pragmas**: Foreign keys enabled via `PRAGMA foreign_keys = ON`
- **Validation**: Pydantic handles input validation; `TransactionUpdate` requires at least one field via `@model_validator`
- **Dates**: ISO-8601 `YYYY-MM-DD` strings throughout

### Database Tables

Two tables: `transactions` (with optional `recurring_id` FK) and `recurring_transactions` (weekly/monthly frequency, active/inactive toggle). Schema defined in `database.py`.
