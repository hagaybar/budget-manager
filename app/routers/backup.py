"""Backup and restore endpoints for /api/backup."""

import json
import os
from datetime import datetime
from fastapi import APIRouter, HTTPException, UploadFile, File
from fastapi.responses import JSONResponse
from app.database import get_db

router = APIRouter(prefix="/api/backup", tags=["backup"])

BACKUP_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "backups")


@router.post("/save")
def save_backup():
    """Save all data (transactions + recurring) to a timestamped backup file."""
    conn = get_db()
    try:
        transactions = [dict(row) for row in conn.execute(
            "SELECT * FROM transactions ORDER BY id"
        ).fetchall()]
        recurring = [dict(row) for row in conn.execute(
            "SELECT * FROM recurring_transactions ORDER BY id"
        ).fetchall()]
    finally:
        conn.close()

    backup = {
        "created_at": datetime.now().isoformat(),
        "transactions": transactions,
        "recurring_transactions": recurring,
    }

    os.makedirs(BACKUP_DIR, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"backup_{timestamp}.json"
    filepath = os.path.join(BACKUP_DIR, filename)

    with open(filepath, "w") as f:
        json.dump(backup, f, indent=2)

    return {
        "message": f"Backup saved: {filename}",
        "filename": filename,
        "transactions_count": len(transactions),
        "recurring_count": len(recurring),
    }


@router.get("/list")
def list_backups():
    """List available backup files."""
    if not os.path.exists(BACKUP_DIR):
        return {"backups": []}

    backups = []
    for f in sorted(os.listdir(BACKUP_DIR), reverse=True):
        if f.endswith(".json"):
            filepath = os.path.join(BACKUP_DIR, f)
            try:
                with open(filepath) as fh:
                    data = json.load(fh)
                backups.append({
                    "filename": f,
                    "created_at": data.get("created_at", ""),
                    "transactions_count": len(data.get("transactions", [])),
                    "recurring_count": len(data.get("recurring_transactions", [])),
                })
            except (json.JSONDecodeError, KeyError):
                continue

    return {"backups": backups}


@router.post("/restore/{filename}")
def restore_backup(filename: str):
    """Restore data from a backup file. Replaces all current data."""
    filepath = os.path.join(BACKUP_DIR, filename)

    if not os.path.exists(filepath):
        raise HTTPException(status_code=404, detail=f"Backup file not found: {filename}")

    with open(filepath) as f:
        backup = json.load(f)

    transactions = backup.get("transactions", [])
    recurring = backup.get("recurring_transactions", [])

    conn = get_db()
    try:
        # Clear existing data (transactions first due to FK)
        conn.execute("DELETE FROM transactions")
        conn.execute("DELETE FROM recurring_transactions")

        # Restore recurring first (for FK references)
        for rec in recurring:
            conn.execute(
                """INSERT INTO recurring_transactions
                   (id, type, amount, category, description, frequency,
                    day_of_week, day_of_month, start_date, end_date, is_active, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (rec["id"], rec["type"], rec["amount"], rec["category"],
                 rec.get("description", ""), rec["frequency"],
                 rec.get("day_of_week"), rec.get("day_of_month"),
                 rec["start_date"], rec.get("end_date"),
                 rec.get("is_active", 1), rec.get("created_at", "")),
            )

        # Restore transactions
        for txn in transactions:
            conn.execute(
                """INSERT INTO transactions
                   (id, type, amount, category, description, date, created_at, recurring_id)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                (txn["id"], txn["type"], txn["amount"], txn["category"],
                 txn.get("description", ""), txn["date"],
                 txn.get("created_at", ""), txn.get("recurring_id")),
            )

        conn.commit()
    finally:
        conn.close()

    return {
        "message": f"Restored from {filename}",
        "transactions_count": len(transactions),
        "recurring_count": len(recurring),
    }


@router.get("/export")
def export_data():
    """Export all current data as JSON (for download)."""
    conn = get_db()
    try:
        transactions = [dict(row) for row in conn.execute(
            "SELECT * FROM transactions ORDER BY id"
        ).fetchall()]
        recurring = [dict(row) for row in conn.execute(
            "SELECT * FROM recurring_transactions ORDER BY id"
        ).fetchall()]
    finally:
        conn.close()

    return {
        "created_at": datetime.now().isoformat(),
        "transactions": transactions,
        "recurring_transactions": recurring,
    }


@router.post("/import")
def import_data(data: dict):
    """Import data from JSON payload. Replaces all current data."""
    transactions = data.get("transactions", [])
    recurring = data.get("recurring_transactions", [])

    conn = get_db()
    try:
        conn.execute("DELETE FROM transactions")
        conn.execute("DELETE FROM recurring_transactions")

        for rec in recurring:
            conn.execute(
                """INSERT INTO recurring_transactions
                   (id, type, amount, category, description, frequency,
                    day_of_week, day_of_month, start_date, end_date, is_active, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (rec["id"], rec["type"], rec["amount"], rec["category"],
                 rec.get("description", ""), rec["frequency"],
                 rec.get("day_of_week"), rec.get("day_of_month"),
                 rec["start_date"], rec.get("end_date"),
                 rec.get("is_active", 1), rec.get("created_at", "")),
            )

        for txn in transactions:
            conn.execute(
                """INSERT INTO transactions
                   (id, type, amount, category, description, date, created_at, recurring_id)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                (txn["id"], txn["type"], txn["amount"], txn["category"],
                 txn.get("description", ""), txn["date"],
                 txn.get("created_at", ""), txn.get("recurring_id")),
            )

        conn.commit()
    finally:
        conn.close()

    return {
        "message": "Data imported successfully",
        "transactions_count": len(transactions),
        "recurring_count": len(recurring),
    }
