# Budget Manager Enhancement Plan

## Overview

This document describes the implementation plan for two enhancements to the Budget Manager application:

1. **Currency Change**: Replace all `$` (dollar) symbols with `₪` (Israeli New Shekel) across the UI and test assertions.
2. **Recurring Transactions**: Add a full recurring-transaction subsystem -- database table, API endpoints, generation logic, UI section, and tests.

---

## Table of Contents

- [1. Currency Change ($ to NIS)](#1-currency-change--to-nis)
  - [1.1 Files to Modify](#11-files-to-modify)
  - [1.2 Detailed Changes](#12-detailed-changes)
- [2. Recurring Transactions](#2-recurring-transactions)
  - [2.1 Database Schema](#21-database-schema)
  - [2.2 Pydantic Schemas](#22-pydantic-schemas)
  - [2.3 API Endpoints](#23-api-endpoints)
  - [2.4 Generation Logic](#24-generation-logic)
  - [2.5 Router Registration](#25-router-registration)
  - [2.6 UI Changes](#26-ui-changes)
  - [2.7 Test Plan](#27-test-plan)
- [3. File Change Summary](#3-file-change-summary)
- [4. Implementation Order](#4-implementation-order)

---

## 1. Currency Change ($ to NIS)

### 1.1 Files to Modify

| File | Change Type |
|------|-------------|
| `app/static/index.html` | Replace all `$` currency prefixes with `₪` |
| `tests/test_ui_e2e.py` | Update assertions that check for `$` in rendered text |

**No backend changes required.** The backend stores and returns raw numeric amounts. Currency formatting is purely a UI concern.

### 1.2 Detailed Changes

#### `app/static/index.html`

**A) Default summary placeholders (3 occurrences)**

Replace each hardcoded default text:

```
Line 483: "$0.00"  -->  "₪0.00"
Line 487: "$0.00"  -->  "₪0.00"
Line 491: "$0.00"  -->  "₪0.00"
```

These are inside the `data-testid="total-income"`, `data-testid="total-expenses"`, and `data-testid="net-balance"` elements.

**B) `renderTransactions()` function (1 occurrence)**

In the template literal that builds each transaction row:

```javascript
// BEFORE
<span class="tx-amount ${tx.type}">$${formatAmount(tx.amount)}</span>

// AFTER
<span class="tx-amount ${tx.type}">₪${formatAmount(tx.amount)}</span>
```

**C) `renderSummary()` function (3 occurrences)**

```javascript
// BEFORE
totalIncomeEl.textContent = `$${formatAmount(summary.total_income)}`;
totalExpensesEl.textContent = `$${formatAmount(summary.total_expenses)}`;
netBalanceEl.textContent = `$${formatAmount(summary.net_balance)}`;

// AFTER
totalIncomeEl.textContent = `₪${formatAmount(summary.total_income)}`;
totalExpensesEl.textContent = `₪${formatAmount(summary.total_expenses)}`;
netBalanceEl.textContent = `₪${formatAmount(summary.net_balance)}`;
```

**D) Category breakdown in `renderSummary()` (1 occurrence)**

```javascript
// BEFORE
<span class="cat-total">$${formatAmount(cat.total)}</span>

// AFTER
<span class="cat-total">₪${formatAmount(cat.total)}</span>
```

**Total: 8 occurrences of `$` to replace with `₪` in `index.html`.**

#### `tests/test_ui_e2e.py`

The E2E tests currently check for numeric strings like `"3,000"`, `"500"`, `"2,500"`, `"1,000"`, and `"50"`. These assertions use `to_contain_text()` which matches substrings, so they do **not** include the `$` symbol explicitly. Therefore **no E2E test changes are needed for currency** -- the existing assertions will continue to pass because they match numeric substrings only.

However, if any future test or existing test explicitly asserts on the `$` character, it must be changed to `₪`. A grep for literal `$` in test assertion strings should be performed at implementation time to confirm.

**No changes needed in `tests/test_transactions.py`, `tests/test_summary.py`, or `tests/test_models.py`** -- these test API JSON responses which contain raw numbers, not formatted currency strings.

---

## 2. Recurring Transactions

### 2.1 Database Schema

#### New Table: `recurring_transactions`

```sql
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
);
```

Semantic constraints enforced at application level:
- If `frequency = 'weekly'`, then `day_of_week` must be provided (0=Monday .. 6=Sunday).
- If `frequency = 'monthly'`, then `day_of_month` must be provided (1-31).
- `start_date` defaults to today if not supplied by the client.
- `end_date` is nullable; `NULL` means the recurrence has no end.

#### Alter Existing Table: `transactions`

Add a nullable foreign key column:

```sql
ALTER TABLE transactions ADD COLUMN recurring_id INTEGER REFERENCES recurring_transactions(id);
```

Because SQLite does not support `ADD CONSTRAINT` after table creation, and the existing `CREATE TABLE IF NOT EXISTS` DDL is idempotent, the implementation approach is:

1. In `create_tables()`, after creating the `transactions` table, execute the `ALTER TABLE` inside a try/except block (catching the "duplicate column name" error for idempotency).
2. Add the `CREATE TABLE IF NOT EXISTS recurring_transactions` DDL.

#### Updated `app/database.py` -- `create_tables()` function

```python
def create_tables() -> None:
    """Create all tables if they don't exist."""
    conn = get_db()
    try:
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

        # New recurring_transactions table
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

        # Add recurring_id column to transactions (idempotent)
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
```

### 2.2 Pydantic Schemas

Add the following models to `app/schemas.py`:

```python
from typing import Literal, Optional
from pydantic import BaseModel, Field, field_validator, model_validator
from datetime import date as date_type


# --- Existing models remain unchanged (TransactionCreate, etc.) ---
# TransactionResponse gains an optional recurring_id field:

class TransactionResponse(BaseModel):
    """Schema for returning a transaction."""
    id: int
    type: str
    amount: float
    category: str
    description: str
    date: str
    created_at: str
    recurring_id: Optional[int] = None


# --- New Recurring Transaction Schemas ---

class RecurringTransactionCreate(BaseModel):
    """Schema for creating a new recurring transaction definition."""
    type: Literal["income", "expense"]
    amount: float = Field(..., gt=0, description="Recurring amount, must be > 0")
    category: str = Field(..., min_length=1, max_length=100)
    description: str = Field(default="", max_length=500)
    frequency: Literal["weekly", "monthly"]
    day_of_week: Optional[int] = Field(default=None, ge=0, le=6,
        description="0=Monday..6=Sunday, required when frequency='weekly'")
    day_of_month: Optional[int] = Field(default=None, ge=1, le=31,
        description="1-31, required when frequency='monthly'")
    start_date: Optional[str] = Field(default=None,
        description="YYYY-MM-DD, defaults to today if omitted")
    end_date: Optional[str] = Field(default=None,
        description="YYYY-MM-DD, null means no end date")

    @field_validator("start_date", "end_date")
    @classmethod
    def validate_date_format(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return v
        import re
        if not re.match(r"^\d{4}-\d{2}-\d{2}$", v):
            raise ValueError("Date must be in YYYY-MM-DD format")
        try:
            date_type.fromisoformat(v)
        except ValueError:
            raise ValueError("Invalid date value")
        return v

    @model_validator(mode="after")
    def check_frequency_fields(self) -> "RecurringTransactionCreate":
        """Ensure the correct day field is provided for the chosen frequency."""
        if self.frequency == "weekly" and self.day_of_week is None:
            raise ValueError("day_of_week is required when frequency is 'weekly'")
        if self.frequency == "monthly" and self.day_of_month is None:
            raise ValueError("day_of_month is required when frequency is 'monthly'")
        if self.frequency == "weekly" and self.day_of_month is not None:
            raise ValueError("day_of_month must not be set when frequency is 'weekly'")
        if self.frequency == "monthly" and self.day_of_week is not None:
            raise ValueError("day_of_week must not be set when frequency is 'monthly'")
        return self


class RecurringTransactionUpdate(BaseModel):
    """Schema for updating a recurring transaction definition. All fields optional."""
    type: Optional[Literal["income", "expense"]] = None
    amount: Optional[float] = Field(default=None, gt=0)
    category: Optional[str] = Field(default=None, min_length=1, max_length=100)
    description: Optional[str] = Field(default=None, max_length=500)
    frequency: Optional[Literal["weekly", "monthly"]] = None
    day_of_week: Optional[int] = Field(default=None, ge=0, le=6)
    day_of_month: Optional[int] = Field(default=None, ge=1, le=31)
    start_date: Optional[str] = Field(default=None)
    end_date: Optional[str] = Field(default=None)
    is_active: Optional[int] = Field(default=None, ge=0, le=1)

    @field_validator("start_date", "end_date")
    @classmethod
    def validate_date_format(cls, v: Optional[str]) -> Optional[str]:
        if v is None:
            return v
        import re
        if not re.match(r"^\d{4}-\d{2}-\d{2}$", v):
            raise ValueError("Date must be in YYYY-MM-DD format")
        try:
            date_type.fromisoformat(v)
        except ValueError:
            raise ValueError("Invalid date value")
        return v

    @model_validator(mode="after")
    def check_at_least_one_field(self) -> "RecurringTransactionUpdate":
        """Ensure at least one field is provided for update."""
        if all(
            getattr(self, field) is None
            for field in self.model_fields
        ):
            raise ValueError("At least one field must be provided")
        return self


class RecurringTransactionResponse(BaseModel):
    """Schema for returning a recurring transaction definition."""
    id: int
    type: str
    amount: float
    category: str
    description: str
    frequency: str
    day_of_week: Optional[int] = None
    day_of_month: Optional[int] = None
    start_date: str
    end_date: Optional[str] = None
    is_active: int
    created_at: str
```

### 2.3 API Endpoints

All endpoints live in a new file: `app/routers/recurring.py`

Router prefix: `/api/recurring`

#### POST `/api/recurring` -- Create Recurring Transaction

**Request Body:**
```json
{
    "type": "expense",
    "amount": 1200.00,
    "category": "Rent",
    "description": "Monthly apartment rent",
    "frequency": "monthly",
    "day_of_month": 1,
    "start_date": "2026-03-01",
    "end_date": "2026-12-31"
}
```

**Response (201 Created):**
```json
{
    "id": 1,
    "type": "expense",
    "amount": 1200.00,
    "category": "Rent",
    "description": "Monthly apartment rent",
    "frequency": "monthly",
    "day_of_week": null,
    "day_of_month": 1,
    "start_date": "2026-03-01",
    "end_date": "2026-12-31",
    "is_active": 1,
    "created_at": "2026-03-03 10:00:00"
}
```

**Logic:**
1. Validate via `RecurringTransactionCreate` schema.
2. If `start_date` is `None`, set to today's date (`date.today().isoformat()`).
3. INSERT into `recurring_transactions`.
4. Return the created row.

#### GET `/api/recurring` -- List All Recurring Definitions

**Query Parameters:** None (returns all, including inactive).

**Response (200 OK):**
```json
[
    {
        "id": 1,
        "type": "expense",
        "amount": 1200.00,
        "category": "Rent",
        "description": "Monthly apartment rent",
        "frequency": "monthly",
        "day_of_week": null,
        "day_of_month": 1,
        "start_date": "2026-03-01",
        "end_date": "2026-12-31",
        "is_active": 1,
        "created_at": "2026-03-03 10:00:00"
    }
]
```

**Logic:**
1. `SELECT * FROM recurring_transactions ORDER BY created_at DESC`
2. Return list of `RecurringTransactionResponse`.

#### GET `/api/recurring/{id}` -- Get Single Definition

**Response (200 OK):** Single `RecurringTransactionResponse` object.
**Response (404):** `{"detail": "Recurring transaction with id {id} not found"}`

#### PUT `/api/recurring/{id}` -- Update Definition

**Request Body:** Partial `RecurringTransactionUpdate` (at least one field).

**Response (200 OK):** Updated `RecurringTransactionResponse`.
**Response (404):** If ID does not exist.
**Response (422):** If validation fails.

**Logic:**
1. Verify record exists.
2. Build dynamic UPDATE from provided fields (same pattern as existing `update_transaction`).
3. Return updated row.

#### DELETE `/api/recurring/{id}` -- Delete Definition

**Behavior:** Hard delete. Also set `recurring_id = NULL` on any transactions that reference this recurring definition.

**Response (204 No Content):** Success.
**Response (404):** If ID does not exist.

**Logic:**
1. Verify record exists.
2. `UPDATE transactions SET recurring_id = NULL WHERE recurring_id = ?`
3. `DELETE FROM recurring_transactions WHERE id = ?`
4. Return 204.

#### POST `/api/recurring/{id}/generate` -- Generate Transactions

**Query Parameters:**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `start_date` | string (YYYY-MM-DD) | Yes | Start of generation window |
| `end_date` | string (YYYY-MM-DD) | Yes | End of generation window |

**Response (200 OK):**
```json
{
    "generated_count": 4,
    "transactions": [
        {
            "id": 10,
            "type": "expense",
            "amount": 1200.00,
            "category": "Rent",
            "description": "Monthly apartment rent",
            "date": "2026-03-01",
            "created_at": "2026-03-03 10:00:00",
            "recurring_id": 1
        }
    ]
}
```

**Response (404):** If recurring ID does not exist.
**Response (400):** If the recurring definition is inactive (`is_active = 0`).

### 2.4 Generation Logic

The generation endpoint materializes actual transaction records into the `transactions` table, linked via `recurring_id`.

```python
from datetime import date, timedelta

def generate_occurrences(
    frequency: str,
    day_of_week: int | None,
    day_of_month: int | None,
    rec_start: date,
    rec_end: date | None,
    gen_start: date,
    gen_end: date,
) -> list[date]:
    """
    Compute all occurrence dates for a recurring definition
    within the generation window [gen_start, gen_end].

    The effective window is:
        max(rec_start, gen_start) .. min(rec_end or gen_end, gen_end)
    """
    effective_start = max(rec_start, gen_start)
    effective_end = min(rec_end, gen_end) if rec_end else gen_end

    if effective_start > effective_end:
        return []

    dates = []

    if frequency == "weekly":
        # day_of_week: 0=Monday .. 6=Sunday (matches Python's date.weekday())
        current = effective_start
        # Advance to first matching weekday
        while current.weekday() != day_of_week:
            current += timedelta(days=1)
        while current <= effective_end:
            dates.append(current)
            current += timedelta(weeks=1)

    elif frequency == "monthly":
        import calendar
        # Iterate month by month
        year, month = effective_start.year, effective_start.month
        while True:
            # Clamp day_of_month to actual last day of this month
            last_day = calendar.monthrange(year, month)[1]
            actual_day = min(day_of_month, last_day)
            occurrence = date(year, month, actual_day)
            if occurrence > effective_end:
                break
            if occurrence >= effective_start:
                dates.append(occurrence)
            # Advance to next month
            if month == 12:
                year += 1
                month = 1
            else:
                month += 1

    return dates
```

**Duplicate Prevention:**

Before inserting each generated transaction, check:

```sql
SELECT COUNT(*) FROM transactions
WHERE recurring_id = ? AND date = ?
```

If a transaction already exists for that recurring definition on that date, skip it. This makes the generate endpoint idempotent.

**Full generate endpoint logic:**

```python
@router.post("/{recurring_id}/generate")
def generate_transactions(
    recurring_id: int,
    start_date: str = Query(..., description="YYYY-MM-DD"),
    end_date: str = Query(..., description="YYYY-MM-DD"),
):
    conn = get_db()
    try:
        # 1. Fetch recurring definition; 404 if not found
        row = conn.execute(
            "SELECT * FROM recurring_transactions WHERE id = ?",
            (recurring_id,)
        ).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail=...)

        rec = dict(row)

        # 2. Check is_active
        if not rec["is_active"]:
            raise HTTPException(status_code=400,
                detail="Cannot generate from inactive recurring transaction")

        # 3. Compute occurrence dates
        occurrences = generate_occurrences(
            frequency=rec["frequency"],
            day_of_week=rec["day_of_week"],
            day_of_month=rec["day_of_month"],
            rec_start=date.fromisoformat(rec["start_date"]),
            rec_end=date.fromisoformat(rec["end_date"]) if rec["end_date"] else None,
            gen_start=date.fromisoformat(start_date),
            gen_end=date.fromisoformat(end_date),
        )

        # 4. Insert transactions, skipping duplicates
        generated = []
        for occ_date in occurrences:
            existing = conn.execute(
                "SELECT COUNT(*) as cnt FROM transactions WHERE recurring_id = ? AND date = ?",
                (recurring_id, occ_date.isoformat())
            ).fetchone()
            if existing["cnt"] > 0:
                continue

            cursor = conn.execute(
                """INSERT INTO transactions
                   (type, amount, category, description, date, recurring_id)
                   VALUES (?, ?, ?, ?, ?, ?)""",
                (rec["type"], rec["amount"], rec["category"],
                 rec["description"], occ_date.isoformat(), recurring_id)
            )
            tx_row = conn.execute(
                "SELECT * FROM transactions WHERE id = ?",
                (cursor.lastrowid,)
            ).fetchone()
            generated.append(dict(tx_row))

        conn.commit()

        return {
            "generated_count": len(generated),
            "transactions": generated,
        }
    finally:
        conn.close()
```

### 2.5 Router Registration

#### `app/main.py` Changes

```python
from app.routers import transactions, summary, recurring

# ... inside existing code ...
app.include_router(transactions.router)
app.include_router(summary.router)
app.include_router(recurring.router)    # NEW
```

### 2.6 UI Changes

#### New Section in `app/static/index.html`

Add a new card section between the "Transaction List" card and the "Monthly Summary" card (or after the summary -- the exact position is flexible, but after the transaction list is recommended).

**HTML Structure:**

```html
<!-- RECURRING TRANSACTIONS -->
<div class="card" data-testid="recurring-section">
    <div class="card-title">Recurring Transactions</div>

    <!-- Add Recurring Form -->
    <form data-testid="recurring-form" class="form-grid" id="recurringForm">
        <div class="form-group">
            <label for="rec-type-select">Type</label>
            <select id="rec-type-select" data-testid="rec-type-select" required>
                <option value="income">Income</option>
                <option value="expense">Expense</option>
            </select>
        </div>
        <div class="form-group">
            <label for="rec-amount-input">Amount</label>
            <input type="number" id="rec-amount-input" data-testid="rec-amount-input"
                   placeholder="0.00" step="0.01" min="0.01" required>
        </div>
        <div class="form-group">
            <label for="rec-category-input">Category</label>
            <input type="text" id="rec-category-input" data-testid="rec-category-input"
                   placeholder="e.g. Rent, Salary" required maxlength="100">
        </div>
        <div class="form-group">
            <label for="rec-description-input">Description</label>
            <input type="text" id="rec-description-input" data-testid="rec-description-input"
                   placeholder="Optional note" maxlength="500">
        </div>
        <div class="form-group">
            <label for="rec-frequency-select">Frequency</label>
            <select id="rec-frequency-select" data-testid="rec-frequency-select" required>
                <option value="weekly">Weekly</option>
                <option value="monthly">Monthly</option>
            </select>
        </div>
        <div class="form-group" id="dayOfWeekGroup">
            <label for="rec-day-of-week">Day of Week</label>
            <select id="rec-day-of-week" data-testid="rec-day-of-week">
                <option value="0">Monday</option>
                <option value="1">Tuesday</option>
                <option value="2">Wednesday</option>
                <option value="3">Thursday</option>
                <option value="4">Friday</option>
                <option value="5">Saturday</option>
                <option value="6">Sunday</option>
            </select>
        </div>
        <div class="form-group" id="dayOfMonthGroup" style="display:none;">
            <label for="rec-day-of-month">Day of Month</label>
            <input type="number" id="rec-day-of-month" data-testid="rec-day-of-month"
                   min="1" max="31" placeholder="1-31">
        </div>
        <div class="form-group">
            <label for="rec-start-date">Start Date</label>
            <input type="date" id="rec-start-date" data-testid="rec-start-date">
        </div>
        <div class="form-group">
            <label for="rec-end-date">End Date (optional)</label>
            <input type="date" id="rec-end-date" data-testid="rec-end-date">
        </div>
        <button type="submit" class="submit-btn" data-testid="rec-submit-btn">
            Add Recurring Transaction
        </button>
    </form>

    <!-- Recurring Transaction List -->
    <div data-testid="recurring-list" id="recurringList" style="margin-top: 1rem;">
        <div class="loading">Loading recurring transactions...</div>
    </div>
</div>
```

**JavaScript additions:**

1. **Frequency toggle**: Show/hide `dayOfWeekGroup` vs `dayOfMonthGroup` based on selected frequency.
2. **`loadRecurring()`**: Fetch `GET /api/recurring` and render the list.
3. **`renderRecurring(items)`**: Render each recurring definition as a row with:
   - Type badge
   - Amount with `₪` prefix
   - Category + description
   - Frequency + day info
   - Active/inactive badge
   - Delete button (`data-testid="rec-delete-btn"`)
   - Generate button (`data-testid="rec-generate-btn"`)
4. **Form submit handler**: POST to `/api/recurring`, then reload.
5. **`deleteRecurring(id)`**: DELETE `/api/recurring/{id}`, then reload.
6. **`generateRecurring(id)`**: POST `/api/recurring/{id}/generate` with current month start/end as default, then reload transactions and summary.

**data-testid attributes for all new elements:**

| Element | data-testid |
|---------|------------|
| Recurring section container | `recurring-section` |
| Recurring form | `recurring-form` |
| Type select | `rec-type-select` |
| Amount input | `rec-amount-input` |
| Category input | `rec-category-input` |
| Description input | `rec-description-input` |
| Frequency select | `rec-frequency-select` |
| Day of week select | `rec-day-of-week` |
| Day of month input | `rec-day-of-month` |
| Start date input | `rec-start-date` |
| End date input | `rec-end-date` |
| Submit button | `rec-submit-btn` |
| Recurring list container | `recurring-list` |
| Each recurring row | `recurring-row` |
| Delete button (per row) | `rec-delete-btn` |
| Generate button (per row) | `rec-generate-btn` |

**Mobile responsiveness:**
- The form already uses the `.form-grid` class which collapses to single-column on mobile via the existing `@media (max-width: 480px)` rule.
- Recurring rows should use the same `.transaction-row` flexbox pattern for consistency.

### 2.7 Test Plan

#### New File: `tests/test_recurring.py`

**Test Classes and Methods:**

```python
class TestCreateRecurring:
    """POST /api/recurring"""

    def test_create_monthly_recurring(self, client):
        """Create a monthly recurring expense. Expect 201 with all fields returned."""

    def test_create_weekly_recurring(self, client):
        """Create a weekly recurring income. Expect 201."""

    def test_create_recurring_default_start_date(self, client):
        """Omitting start_date defaults to today."""

    def test_create_recurring_no_end_date(self, client):
        """Omitting end_date results in null (no end)."""

    def test_create_recurring_invalid_frequency(self, client):
        """Frequency other than 'weekly'/'monthly' returns 422."""

    def test_create_recurring_weekly_missing_day_of_week(self, client):
        """Weekly without day_of_week returns 422."""

    def test_create_recurring_monthly_missing_day_of_month(self, client):
        """Monthly without day_of_month returns 422."""

    def test_create_recurring_weekly_with_day_of_month(self, client):
        """Weekly with day_of_month set returns 422 (conflicting fields)."""

    def test_create_recurring_monthly_with_day_of_week(self, client):
        """Monthly with day_of_week set returns 422."""

    def test_create_recurring_negative_amount(self, client):
        """Negative amount returns 422."""

    def test_create_recurring_zero_amount(self, client):
        """Zero amount returns 422."""

    def test_create_recurring_empty_category(self, client):
        """Empty category returns 422."""

    def test_create_recurring_invalid_date_format(self, client):
        """Bad date format returns 422."""


class TestListRecurring:
    """GET /api/recurring"""

    def test_list_recurring_empty(self, client):
        """Empty DB returns empty list."""

    def test_list_recurring_multiple(self, client):
        """Creating multiple recurring defs returns all."""


class TestGetRecurringById:
    """GET /api/recurring/{id}"""

    def test_get_recurring_by_id(self, client):
        """Fetch existing recurring by ID returns 200."""

    def test_get_recurring_not_found(self, client):
        """Non-existent ID returns 404."""


class TestUpdateRecurring:
    """PUT /api/recurring/{id}"""

    def test_update_recurring_partial(self, client):
        """Partial update changes only provided fields."""

    def test_update_recurring_full(self, client):
        """Full update replaces all fields."""

    def test_update_recurring_not_found(self, client):
        """Update non-existent returns 404."""

    def test_update_recurring_no_fields(self, client):
        """Empty update body returns 422."""

    def test_update_recurring_deactivate(self, client):
        """Setting is_active=0 deactivates."""


class TestDeleteRecurring:
    """DELETE /api/recurring/{id}"""

    def test_delete_recurring(self, client):
        """Delete existing returns 204, confirm gone."""

    def test_delete_recurring_not_found(self, client):
        """Delete non-existent returns 404."""

    def test_delete_recurring_nullifies_transaction_references(self, client):
        """Deleting recurring sets recurring_id=NULL on linked transactions."""


class TestGenerateTransactions:
    """POST /api/recurring/{id}/generate"""

    def test_generate_monthly_transactions(self, client):
        """Generating monthly for a 3-month window creates 3 transactions."""

    def test_generate_weekly_transactions(self, client):
        """Generating weekly for a 4-week window creates ~4 transactions."""

    def test_generate_respects_start_date(self, client):
        """Occurrences before rec start_date are excluded."""

    def test_generate_respects_end_date(self, client):
        """Occurrences after rec end_date are excluded."""

    def test_generate_idempotent(self, client):
        """Calling generate twice does not create duplicates."""

    def test_generate_inactive_recurring(self, client):
        """Generating from inactive recurring returns 400."""

    def test_generate_not_found(self, client):
        """Generating for non-existent ID returns 404."""

    def test_generated_transactions_have_recurring_id(self, client):
        """Each generated transaction has recurring_id set."""

    def test_generate_monthly_day_clamping(self, client):
        """day_of_month=31 in February generates on Feb 28/29."""
```

#### New File: `tests/test_recurring_models.py`

```python
class TestRecurringTransactionCreate:
    def test_valid_monthly(self):
        """Valid monthly definition passes validation."""

    def test_valid_weekly(self):
        """Valid weekly definition passes validation."""

    def test_missing_day_of_week_for_weekly(self):
        """Raises ValidationError."""

    def test_missing_day_of_month_for_monthly(self):
        """Raises ValidationError."""

    def test_conflicting_day_fields(self):
        """weekly + day_of_month raises ValidationError."""

    def test_invalid_frequency(self):
        """Unknown frequency raises ValidationError."""

    def test_start_date_defaults_to_none(self):
        """start_date is None if not provided (backend sets to today)."""


class TestRecurringTransactionUpdate:
    def test_partial_update(self):
        """Single-field update is valid."""

    def test_empty_update_raises(self):
        """Empty body raises ValidationError."""


class TestRecurringTransactionResponse:
    def test_valid_response(self):
        """All fields accepted."""
```

#### Updates to `tests/conftest.py`

The `test_db` fixture must create the `recurring_transactions` table and add the `recurring_id` column to `transactions`:

```python
@pytest.fixture(scope="function")
def test_db():
    fd, db_path = tempfile.mkstemp(suffix=".db")
    os.close(fd)

    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")

    # transactions table (with recurring_id column)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS transactions (
            id            INTEGER PRIMARY KEY AUTOINCREMENT,
            type          TEXT    NOT NULL CHECK(type IN ('income', 'expense')),
            amount        REAL    NOT NULL CHECK(amount > 0),
            category      TEXT    NOT NULL,
            description   TEXT    DEFAULT '',
            date          TEXT    NOT NULL,
            created_at    TEXT    DEFAULT (datetime('now')),
            recurring_id  INTEGER REFERENCES recurring_transactions(id)
        )
    """)

    # recurring_transactions table
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
    conn.commit()

    yield conn, db_path

    conn.close()
    os.unlink(db_path)
```

The `client` fixture must also patch `app.routers.recurring.get_db`:

```python
@pytest.fixture(scope="function")
def client(test_db):
    _conn, db_path = test_db

    def _test_get_db():
        conn = sqlite3.connect(db_path)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA foreign_keys = ON")
        return conn

    with (
        patch("app.routers.transactions.get_db", _test_get_db),
        patch("app.routers.summary.get_db", _test_get_db),
        patch("app.routers.recurring.get_db", _test_get_db),
    ):
        with TestClient(app) as c:
            yield c
```

#### E2E Tests: `tests/test_ui_e2e.py` Additions

```python
class TestRecurringTransactionsUI:
    def test_recurring_section_visible(self, page):
        """The recurring transactions section is visible on page load."""

    def test_add_monthly_recurring(self, page):
        """Fill and submit recurring form, verify it appears in the list."""

    def test_add_weekly_recurring(self, page):
        """Fill weekly recurring and verify."""

    def test_frequency_toggle_shows_correct_field(self, page):
        """Switching frequency shows day_of_week or day_of_month."""

    def test_delete_recurring(self, page):
        """Delete a recurring definition from the UI list."""

    def test_generate_recurring(self, page):
        """Click generate and verify transactions appear in the list."""

    def test_recurring_amounts_show_shekel(self, page):
        """Recurring amounts display with shekel symbol."""
```

The E2E page fixture cleanup should also delete recurring transactions:

```python
@pytest.fixture
def page(browser):
    ctx = browser.new_context(viewport={"width": 1280, "height": 900})
    pg = ctx.new_page()
    with httpx.Client(base_url=BASE_URL, follow_redirects=True) as client:
        # Clean transactions
        txns = client.get("/api/transactions").json()
        for t in txns:
            client.delete(f"/api/transactions/{t['id']}")
        # Clean recurring
        recs = client.get("/api/recurring").json()
        for r in recs:
            client.delete(f"/api/recurring/{r['id']}")
    yield pg
    pg.close()
    ctx.close()
```

---

## 3. File Change Summary

### Files to Create

| File | Purpose |
|------|---------|
| `app/routers/recurring.py` | New router with CRUD + generate endpoints for recurring transactions |
| `tests/test_recurring.py` | API-level tests for all recurring endpoints |
| `tests/test_recurring_models.py` | Pydantic schema validation tests for recurring models |

### Files to Modify

| File | Changes |
|------|---------|
| `app/database.py` | Add `recurring_transactions` table DDL; add `recurring_id` column migration to `transactions` |
| `app/schemas.py` | Add `RecurringTransactionCreate`, `RecurringTransactionUpdate`, `RecurringTransactionResponse`; add `recurring_id` field to `TransactionResponse` |
| `app/main.py` | Import and register `recurring` router |
| `app/static/index.html` | Replace all `$` with `₪` (8 occurrences); add recurring transactions section (HTML + CSS + JS) |
| `tests/conftest.py` | Update `test_db` schema to include `recurring_transactions` table and `recurring_id` column; update `client` fixture to patch `recurring.get_db` |
| `tests/test_ui_e2e.py` | Add `TestRecurringTransactionsUI` class; update page fixture cleanup to also delete recurring records |

### Files That Require NO Changes

| File | Reason |
|------|--------|
| `app/routers/transactions.py` | No currency logic; `recurring_id` comes from DB and flows through unchanged |
| `app/routers/summary.py` | No currency logic; summary queries remain the same |
| `tests/test_transactions.py` | Tests raw API JSON, no currency symbols |
| `tests/test_summary.py` | Tests raw API JSON, no currency symbols |
| `tests/test_models.py` | Tests existing schemas; `recurring_id` is Optional with default None so existing tests still pass |

---

## 4. Implementation Order

The recommended order minimizes breakage and allows incremental testing:

| Step | Task | Depends On |
|------|------|------------|
| 1 | **Currency change in `index.html`** -- Replace all 8 `$` occurrences with `₪` | None |
| 2 | **Verify existing tests pass** -- Run `pytest tests/test_models.py tests/test_transactions.py tests/test_summary.py` | Step 1 |
| 3 | **Database schema** -- Update `app/database.py` with new table and column migration | None |
| 4 | **Pydantic schemas** -- Add recurring models to `app/schemas.py`; add `recurring_id` to `TransactionResponse` | Step 3 |
| 5 | **Test fixtures** -- Update `tests/conftest.py` with new table DDL and recurring router patch | Steps 3, 4 |
| 6 | **Recurring router** -- Create `app/routers/recurring.py` with all 6 endpoints and generation logic | Steps 3, 4 |
| 7 | **Register router** -- Update `app/main.py` to include the recurring router | Step 6 |
| 8 | **Schema tests** -- Create `tests/test_recurring_models.py` | Step 4 |
| 9 | **API tests** -- Create `tests/test_recurring.py` | Steps 5, 6, 7 |
| 10 | **Run all backend tests** -- Verify all pass | Steps 8, 9 |
| 11 | **UI recurring section** -- Add HTML, CSS, and JS to `index.html` | Steps 6, 7 |
| 12 | **E2E tests** -- Add recurring UI tests to `tests/test_ui_e2e.py`; update cleanup fixture | Steps 11 |
| 13 | **Full regression** -- Run entire test suite including E2E | All |
