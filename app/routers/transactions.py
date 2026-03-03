"""CRUD endpoints for /api/transactions."""

from typing import Optional
from fastapi import APIRouter, HTTPException, Query, Response
from app.database import get_db
from app.schemas import TransactionCreate, TransactionUpdate, TransactionResponse

router = APIRouter(prefix="/api/transactions", tags=["transactions"])


@router.post("/", response_model=TransactionResponse, status_code=201)
def create_transaction(transaction: TransactionCreate) -> TransactionResponse:
    """Create a new transaction."""
    conn = get_db()
    try:
        cursor = conn.execute(
            """
            INSERT INTO transactions (type, amount, category, description, date)
            VALUES (?, ?, ?, ?, ?)
            """,
            (
                transaction.type,
                transaction.amount,
                transaction.category,
                transaction.description,
                transaction.date,
            ),
        )
        conn.commit()
        transaction_id = cursor.lastrowid

        row = conn.execute(
            "SELECT * FROM transactions WHERE id = ?", (transaction_id,)
        ).fetchone()

        return TransactionResponse(**dict(row))
    finally:
        conn.close()


@router.get("/", response_model=list[TransactionResponse])
def list_transactions(
    type: Optional[str] = Query(None, description="Filter by income or expense"),
    category: Optional[str] = Query(None, description="Filter by category"),
    date_from: Optional[str] = Query(None, description="Start date (YYYY-MM-DD)"),
    date_to: Optional[str] = Query(None, description="End date (YYYY-MM-DD)"),
) -> list[TransactionResponse]:
    """List all transactions with optional filters."""
    query = "SELECT * FROM transactions WHERE 1=1"
    params: list = []

    if type is not None:
        query += " AND type = ?"
        params.append(type)
    if category is not None:
        query += " AND category = ?"
        params.append(category)
    if date_from is not None:
        query += " AND date >= ?"
        params.append(date_from)
    if date_to is not None:
        query += " AND date <= ?"
        params.append(date_to)

    query += " ORDER BY date DESC, id DESC"

    conn = get_db()
    try:
        rows = conn.execute(query, params).fetchall()
        return [TransactionResponse(**dict(row)) for row in rows]
    finally:
        conn.close()


@router.get("/{transaction_id}", response_model=TransactionResponse)
def get_transaction(transaction_id: int) -> TransactionResponse:
    """Get a single transaction by ID."""
    conn = get_db()
    try:
        row = conn.execute(
            "SELECT * FROM transactions WHERE id = ?", (transaction_id,)
        ).fetchone()

        if row is None:
            raise HTTPException(
                status_code=404,
                detail=f"Transaction with id {transaction_id} not found",
            )

        return TransactionResponse(**dict(row))
    finally:
        conn.close()


@router.put("/{transaction_id}", response_model=TransactionResponse)
def update_transaction(
    transaction_id: int, transaction: TransactionUpdate
) -> TransactionResponse:
    """Update an existing transaction."""
    conn = get_db()
    try:
        # Check if transaction exists
        existing = conn.execute(
            "SELECT * FROM transactions WHERE id = ?", (transaction_id,)
        ).fetchone()

        if existing is None:
            raise HTTPException(
                status_code=404,
                detail=f"Transaction with id {transaction_id} not found",
            )

        # Build dynamic UPDATE query from provided fields
        update_fields = []
        update_values = []

        update_data = transaction.model_dump(exclude_unset=True)
        for field_name, value in update_data.items():
            if value is not None:
                update_fields.append(f"{field_name} = ?")
                update_values.append(value)

        if update_fields:
            update_values.append(transaction_id)
            conn.execute(
                f"UPDATE transactions SET {', '.join(update_fields)} WHERE id = ?",
                update_values,
            )
            conn.commit()

        # Return the updated transaction
        row = conn.execute(
            "SELECT * FROM transactions WHERE id = ?", (transaction_id,)
        ).fetchone()

        return TransactionResponse(**dict(row))
    finally:
        conn.close()


@router.delete("/{transaction_id}", status_code=204)
def delete_transaction(transaction_id: int) -> Response:
    """Delete a transaction."""
    conn = get_db()
    try:
        existing = conn.execute(
            "SELECT * FROM transactions WHERE id = ?", (transaction_id,)
        ).fetchone()

        if existing is None:
            raise HTTPException(
                status_code=404,
                detail=f"Transaction with id {transaction_id} not found",
            )

        conn.execute("DELETE FROM transactions WHERE id = ?", (transaction_id,))
        conn.commit()

        return Response(status_code=204)
    finally:
        conn.close()
