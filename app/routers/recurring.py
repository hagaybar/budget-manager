"""CRUD + generate endpoints for /api/recurring."""

import calendar
from datetime import date, timedelta
from typing import Optional

from fastapi import APIRouter, HTTPException, Query, Response
from app.database import get_db
from app.schemas import (
    RecurringTransactionCreate,
    RecurringTransactionUpdate,
    RecurringTransactionResponse,
    TransactionResponse,
)

router = APIRouter(prefix="/api/recurring", tags=["recurring"])


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _generate_occurrences(
    frequency: str,
    day_of_week: Optional[int],
    day_of_month: Optional[int],
    rec_start: date,
    rec_end: Optional[date],
    gen_start: date,
    gen_end: date,
) -> list[date]:
    """Compute all occurrence dates within the effective window."""
    effective_start = max(rec_start, gen_start)
    effective_end = min(rec_end, gen_end) if rec_end else gen_end

    if effective_start > effective_end:
        return []

    dates: list[date] = []

    if frequency == "weekly":
        current = effective_start
        # Advance to first matching weekday
        while current.weekday() != day_of_week:
            current += timedelta(days=1)
        while current <= effective_end:
            dates.append(current)
            current += timedelta(weeks=1)

    elif frequency == "monthly":
        year, month = effective_start.year, effective_start.month
        while True:
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


# ---------------------------------------------------------------------------
# POST /api/recurring  --  Create
# ---------------------------------------------------------------------------

@router.post("/", response_model=RecurringTransactionResponse, status_code=201)
def create_recurring(recurring: RecurringTransactionCreate) -> RecurringTransactionResponse:
    """Create a new recurring transaction definition."""
    conn = get_db()
    try:
        start_date = recurring.start_date or date.today().isoformat()

        cursor = conn.execute(
            """
            INSERT INTO recurring_transactions
                (type, amount, category, description, frequency,
                 day_of_week, day_of_month, start_date, end_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                recurring.type,
                recurring.amount,
                recurring.category,
                recurring.description,
                recurring.frequency,
                recurring.day_of_week,
                recurring.day_of_month,
                start_date,
                recurring.end_date,
            ),
        )
        conn.commit()
        rec_id = cursor.lastrowid

        row = conn.execute(
            "SELECT * FROM recurring_transactions WHERE id = ?", (rec_id,)
        ).fetchone()

        return RecurringTransactionResponse(**dict(row))
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# GET /api/recurring  --  List all
# ---------------------------------------------------------------------------

@router.get("/", response_model=list[RecurringTransactionResponse])
def list_recurring() -> list[RecurringTransactionResponse]:
    """List all recurring transaction definitions."""
    conn = get_db()
    try:
        rows = conn.execute(
            "SELECT * FROM recurring_transactions ORDER BY created_at DESC"
        ).fetchall()
        return [RecurringTransactionResponse(**dict(row)) for row in rows]
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# GET /api/recurring/{id}  --  Get one
# ---------------------------------------------------------------------------

@router.get("/{recurring_id}", response_model=RecurringTransactionResponse)
def get_recurring(recurring_id: int) -> RecurringTransactionResponse:
    """Get a single recurring transaction definition by ID."""
    conn = get_db()
    try:
        row = conn.execute(
            "SELECT * FROM recurring_transactions WHERE id = ?", (recurring_id,)
        ).fetchone()

        if row is None:
            raise HTTPException(
                status_code=404,
                detail=f"Recurring transaction with id {recurring_id} not found",
            )

        return RecurringTransactionResponse(**dict(row))
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# PUT /api/recurring/{id}  --  Update
# ---------------------------------------------------------------------------

@router.put("/{recurring_id}", response_model=RecurringTransactionResponse)
def update_recurring(
    recurring_id: int, recurring: RecurringTransactionUpdate
) -> RecurringTransactionResponse:
    """Update an existing recurring transaction definition."""
    conn = get_db()
    try:
        existing = conn.execute(
            "SELECT * FROM recurring_transactions WHERE id = ?", (recurring_id,)
        ).fetchone()

        if existing is None:
            raise HTTPException(
                status_code=404,
                detail=f"Recurring transaction with id {recurring_id} not found",
            )

        # Build dynamic UPDATE from provided fields
        update_fields = []
        update_values = []

        update_data = recurring.model_dump(exclude_unset=True)
        for field_name, value in update_data.items():
            if value is not None:
                update_fields.append(f"{field_name} = ?")
                update_values.append(value)

        if update_fields:
            update_values.append(recurring_id)
            conn.execute(
                f"UPDATE recurring_transactions SET {', '.join(update_fields)} WHERE id = ?",
                update_values,
            )
            conn.commit()

        row = conn.execute(
            "SELECT * FROM recurring_transactions WHERE id = ?", (recurring_id,)
        ).fetchone()

        return RecurringTransactionResponse(**dict(row))
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# DELETE /api/recurring/{id}  --  Delete
# ---------------------------------------------------------------------------

@router.delete("/{recurring_id}", status_code=204)
def delete_recurring(recurring_id: int) -> Response:
    """Delete a recurring transaction definition. Nullifies recurring_id on linked transactions."""
    conn = get_db()
    try:
        existing = conn.execute(
            "SELECT * FROM recurring_transactions WHERE id = ?", (recurring_id,)
        ).fetchone()

        if existing is None:
            raise HTTPException(
                status_code=404,
                detail=f"Recurring transaction with id {recurring_id} not found",
            )

        # Nullify references in transactions
        conn.execute(
            "UPDATE transactions SET recurring_id = NULL WHERE recurring_id = ?",
            (recurring_id,),
        )
        # Delete the recurring definition
        conn.execute(
            "DELETE FROM recurring_transactions WHERE id = ?", (recurring_id,)
        )
        conn.commit()

        return Response(status_code=204)
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# POST /api/recurring/{id}/generate  --  Generate transactions
# ---------------------------------------------------------------------------

@router.post("/{recurring_id}/generate")
def generate_transactions(
    recurring_id: int,
    start_date: str = Query(..., description="YYYY-MM-DD"),
    end_date: str = Query(..., description="YYYY-MM-DD"),
):
    """Generate transactions from a recurring definition for a date range."""
    conn = get_db()
    try:
        # 1. Fetch recurring definition
        row = conn.execute(
            "SELECT * FROM recurring_transactions WHERE id = ?", (recurring_id,)
        ).fetchone()

        if row is None:
            raise HTTPException(
                status_code=404,
                detail=f"Recurring transaction with id {recurring_id} not found",
            )

        rec = dict(row)

        # 2. Check is_active
        if not rec["is_active"]:
            raise HTTPException(
                status_code=400,
                detail="Cannot generate from inactive recurring transaction",
            )

        # 3. Compute occurrence dates
        occurrences = _generate_occurrences(
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
                (recurring_id, occ_date.isoformat()),
            ).fetchone()

            if existing["cnt"] > 0:
                continue

            cursor = conn.execute(
                """INSERT INTO transactions
                   (type, amount, category, description, date, recurring_id)
                   VALUES (?, ?, ?, ?, ?, ?)""",
                (
                    rec["type"],
                    rec["amount"],
                    rec["category"],
                    rec["description"],
                    occ_date.isoformat(),
                    recurring_id,
                ),
            )

            tx_row = conn.execute(
                "SELECT * FROM transactions WHERE id = ?", (cursor.lastrowid,)
            ).fetchone()
            generated.append(dict(tx_row))

        conn.commit()

        return {
            "generated_count": len(generated),
            "transactions": generated,
        }
    finally:
        conn.close()
