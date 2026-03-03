# Budget Manager - Architecture & Task Plan

## 1. Project Overview

Budget Manager is a RESTful API application for tracking personal income and expenses. It provides full CRUD operations on financial transactions and delivers monthly summary reports with category-level breakdowns. The application is built with Python, FastAPI, SQLite, and Pydantic, prioritizing simplicity, correctness, and 100% test coverage of core business logic.

---

## 2. Tech Stack

| Layer              | Technology        | Rationale                                                      |
|--------------------|-------------------|----------------------------------------------------------------|
| Web Framework      | FastAPI 0.110+    | Async-capable, automatic OpenAPI docs, native Pydantic support |
| Validation         | Pydantic v2       | Fast, declarative request/response validation                  |
| Database           | SQLite 3          | Zero-config, file-based, ideal for single-user budget tracking |
| DB Access          | Python `sqlite3`  | Stdlib module; no ORM overhead for a single-table schema       |
| Testing            | pytest + httpx    | pytest for structure; httpx.AsyncClient for FastAPI test client |
| Python Version     | 3.11+             | Modern typing, performance improvements                        |

---

## 3. Database Schema

### Table: `transactions`

```sql
CREATE TABLE IF NOT EXISTS transactions (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    type        TEXT    NOT NULL CHECK(type IN ('income', 'expense')),
    amount      REAL    NOT NULL CHECK(amount > 0),
    category    TEXT    NOT NULL,
    description TEXT,
    date        TEXT    NOT NULL,   -- ISO-8601 format: YYYY-MM-DD
    created_at  TEXT    DEFAULT CURRENT_TIMESTAMP
);
```

**Design decisions:**
- `id` uses AUTOINCREMENT to guarantee monotonically increasing IDs even after deletions.
- `type` is constrained at the database level to only accept `'income'` or `'expense'`.
- `amount` is always stored as a positive number; the `type` field determines its sign in calculations.
- `date` is stored as ISO-8601 text (`YYYY-MM-DD`) for SQLite compatibility and sort correctness.
- `created_at` is auto-populated by SQLite on insertion.

---

## 4. REST API Endpoints

### 4.1 Transactions CRUD

| Method | Path                       | Description                  | Request Body         | Success Code | Error Codes        |
|--------|----------------------------|------------------------------|----------------------|--------------|--------------------|
| POST   | `/api/transactions`        | Create a new transaction     | `TransactionCreate`  | 201 Created  | 422 Validation     |
| GET    | `/api/transactions`        | List all transactions        | --                   | 200 OK       | --                 |
| GET    | `/api/transactions/{id}`   | Get a single transaction     | --                   | 200 OK       | 404 Not Found      |
| PUT    | `/api/transactions/{id}`   | Update an existing transaction | `TransactionUpdate` | 200 OK       | 404, 422           |
| DELETE | `/api/transactions/{id}`   | Delete a transaction         | --                   | 204 No Content | 404 Not Found    |

**Query parameters for `GET /api/transactions`:**
- `type` (optional): Filter by `income` or `expense`.
- `category` (optional): Filter by category name.
- `start_date` / `end_date` (optional): Filter by date range (inclusive).
- `skip` (optional, default 0): Pagination offset.
- `limit` (optional, default 100): Pagination limit.

### 4.2 Monthly Summary

| Method | Path                    | Description                              | Query Params          | Success Code |
|--------|-------------------------|------------------------------------------|-----------------------|--------------|
| GET    | `/api/summary/monthly`  | Get income/expense summary for a month   | `year`, `month`       | 200 OK       |

**Query parameters:**
- `year` (required): Integer, e.g. `2026`.
- `month` (required): Integer, 1-12.

---

## 5. Pydantic Models (Schemas)

### 5.1 `TransactionCreate` (Request - POST)

```python
class TransactionCreate(BaseModel):
    type: Literal["income", "expense"]
    amount: float                        # Must be > 0
    category: str                        # Non-empty, max 100 chars
    description: str | None = None       # Optional, max 500 chars
    date: datetime.date                  # ISO-8601 date
```

**Validators:**
- `amount` must be greater than 0.
- `category` must be non-empty and at most 100 characters.
- `description`, if provided, must be at most 500 characters.

### 5.2 `TransactionUpdate` (Request - PUT)

```python
class TransactionUpdate(BaseModel):
    type: Literal["income", "expense"] | None = None
    amount: float | None = None
    category: str | None = None
    description: str | None = None
    date: datetime.date | None = None
```

All fields are optional. At least one field must be provided (validated). Same per-field constraints as `TransactionCreate`.

### 5.3 `TransactionResponse` (Response)

```python
class TransactionResponse(BaseModel):
    id: int
    type: Literal["income", "expense"]
    amount: float
    category: str
    description: str | None
    date: datetime.date
    created_at: str
```

### 5.4 `CategoryBreakdown` (Embedded in summary response)

```python
class CategoryBreakdown(BaseModel):
    category: str
    total: float
    transaction_count: int
```

### 5.5 `MonthlySummary` (Response - GET /api/summary/monthly)

```python
class MonthlySummary(BaseModel):
    year: int
    month: int
    total_income: float
    total_expenses: float
    net_balance: float                              # total_income - total_expenses
    income_by_category: list[CategoryBreakdown]
    expenses_by_category: list[CategoryBreakdown]
    transaction_count: int
```

---

## 6. Project Directory Structure

```
budget_manager/
|-- app/
|   |-- __init__.py              # Package init
|   |-- main.py                  # FastAPI app factory, lifespan, CORS, root route
|   |-- database.py              # SQLite connection management, table creation, query helpers
|   |-- models.py                # Internal data representations (if needed beyond schemas)
|   |-- schemas.py               # Pydantic request/response models
|   |-- routers/
|   |   |-- __init__.py          # Package init
|   |   |-- transactions.py      # CRUD endpoints for /api/transactions
|   |   |-- summary.py           # Monthly summary endpoint /api/summary/monthly
|-- tests/
|   |-- __init__.py              # Package init
|   |-- conftest.py              # Shared fixtures: test DB, test client, sample data
|   |-- test_transactions.py     # Tests for all CRUD operations
|   |-- test_summary.py          # Tests for monthly summary logic
|   |-- test_models.py           # Tests for Pydantic validation and edge cases
|-- requirements.txt             # Python dependencies
|-- README.md                    # Project documentation
|-- TASK_PLAN.md                 # This file
```

---

## 7. Monthly Summary Logic Specification

The `GET /api/summary/monthly?year=YYYY&month=MM` endpoint computes the following:

### 7.1 Data Selection

```sql
SELECT type, amount, category
FROM transactions
WHERE date >= :start_date AND date <= :end_date
```

Where:
- `start_date` = first day of the given month (`YYYY-MM-01`)
- `end_date` = last day of the given month (calculated via `calendar.monthrange`)

### 7.2 Aggregation Logic

```python
total_income = sum(t.amount for t in transactions if t.type == "income")
total_expenses = sum(t.amount for t in transactions if t.type == "expense")
net_balance = total_income - total_expenses
```

### 7.3 Category Breakdown

For each unique category within income transactions:
- Sum the amounts.
- Count the transactions.
- Produce a `CategoryBreakdown` object.

Repeat for expense transactions separately.

### 7.4 Edge Cases

- If no transactions exist for the given month, return zeroed totals and empty category lists.
- `year` and `month` are validated: month must be 1-12, year must be a reasonable positive integer.
- Category breakdowns are sorted alphabetically by category name.

---

## 8. Error Handling Strategy

All errors follow a consistent JSON structure:

```json
{
    "detail": "Human-readable error message"
}
```

| Scenario                          | HTTP Status | Detail Message                              |
|-----------------------------------|-------------|---------------------------------------------|
| Validation failure                | 422         | Pydantic's default validation error format  |
| Transaction not found             | 404         | `"Transaction with id {id} not found"`      |
| Invalid month/year in summary     | 422         | `"Month must be between 1 and 12"`          |
| Update with no fields             | 422         | `"At least one field must be provided"`     |
| Unexpected server error           | 500         | `"Internal server error"`                   |

A global exception handler will catch unhandled exceptions and return 500 with a generic message (no stack traces in production).

---

## 9. Database Access Pattern

- **Connection management:** Use a module-level function `get_db()` that returns a `sqlite3.Connection` with `row_factory = sqlite3.Row` for dict-like access.
- **Initialization:** On application startup (via FastAPI lifespan), call `init_db()` which executes the `CREATE TABLE IF NOT EXISTS` statement.
- **Testing:** The `conftest.py` fixture overrides `get_db()` to return an in-memory SQLite database (`":memory:"`), ensuring test isolation.
- **Row factory:** All query results use `sqlite3.Row` so that rows can be accessed by column name.

---

## 10. Implementation Task Checklist

### Phase 1: Project Setup
- [ ] Create project directory structure (all `__init__.py` files, folders)
- [ ] Create `requirements.txt` with: `fastapi`, `uvicorn[standard]`, `pydantic`, `httpx`, `pytest`, `pytest-asyncio`
- [ ] Create `app/main.py` with FastAPI app instance and lifespan

### Phase 2: Database Layer
- [ ] Implement `app/database.py` with `init_db()`, `get_db()`, and `close_db()`
- [ ] Write the `CREATE TABLE` migration in `init_db()`
- [ ] Implement CRUD helper functions: `create_transaction()`, `get_transaction()`, `get_transactions()`, `update_transaction()`, `delete_transaction()`
- [ ] Implement summary query function: `get_monthly_transactions()`

### Phase 3: Schemas
- [ ] Implement `app/schemas.py` with all Pydantic models
- [ ] Add field validators for `amount > 0`, `category` length, `description` length
- [ ] Add model validator for `TransactionUpdate` requiring at least one field

### Phase 4: API Endpoints
- [ ] Implement `app/routers/transactions.py` with all CRUD routes
- [ ] Implement `app/routers/summary.py` with monthly summary route
- [ ] Register routers in `app/main.py`
- [ ] Add proper HTTP status codes and error responses

### Phase 5: Testing
- [ ] Set up `tests/conftest.py` with test client fixture and in-memory DB
- [ ] Write `tests/test_transactions.py`: test create, read (single + list), update, delete, not-found, validation errors
- [ ] Write `tests/test_summary.py`: test monthly aggregation, category breakdowns, empty month, edge cases
- [ ] Write `tests/test_models.py`: test Pydantic validation (valid inputs, invalid inputs, edge cases)
- [ ] Verify 100% coverage of core business logic with `pytest --cov`

### Phase 6: Polish
- [ ] Add CORS middleware configuration
- [ ] Create `README.md` with setup and usage instructions
- [ ] Final review and cleanup
