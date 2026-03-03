"""Monthly summary endpoint for /api/summary."""

import calendar
from collections import defaultdict
from fastapi import APIRouter, HTTPException, Query
from app.database import get_db
from app.schemas import MonthlySummary, CategoryBreakdown

router = APIRouter(prefix="/api/summary", tags=["summary"])


@router.get("/monthly", response_model=MonthlySummary)
def get_monthly_summary(
    year: int = Query(..., description="Year (e.g. 2026)"),
    month: int = Query(..., ge=1, le=12, description="Month (1-12)"),
) -> MonthlySummary:
    """Get income/expense summary for a given month."""
    # Compute start and end dates
    start_date = f"{year:04d}-{month:02d}-01"
    last_day = calendar.monthrange(year, month)[1]
    end_date = f"{year:04d}-{month:02d}-{last_day:02d}"

    conn = get_db()
    try:
        rows = conn.execute(
            """
            SELECT type, amount, category
            FROM transactions
            WHERE date >= ? AND date <= ?
            """,
            (start_date, end_date),
        ).fetchall()

        total_income = 0.0
        total_expenses = 0.0
        category_totals: dict[str, dict[str, float | int]] = defaultdict(
            lambda: {"total": 0.0, "count": 0}
        )

        for row in rows:
            row_dict = dict(row)
            cat = row_dict["category"]
            amount = row_dict["amount"]
            tx_type = row_dict["type"]

            if tx_type == "income":
                total_income += amount
            else:
                total_expenses += amount

            # Build a combined category key that includes type
            key = f"{tx_type}:{cat}"
            category_totals[key]["total"] += amount
            category_totals[key]["count"] += 1

        # Build category breakdowns (combined income + expense categories)
        categories = []
        for key, data in sorted(category_totals.items()):
            # Strip the type prefix for the category name
            cat_name = key.split(":", 1)[1]
            categories.append(
                CategoryBreakdown(
                    category=cat_name,
                    total=round(data["total"], 2),
                    count=int(data["count"]),
                )
            )

        # Sort alphabetically by category name
        categories.sort(key=lambda c: c.category)

        net_balance = round(total_income - total_expenses, 2)

        return MonthlySummary(
            year=year,
            month=month,
            total_income=round(total_income, 2),
            total_expenses=round(total_expenses, 2),
            net_balance=net_balance,
            transaction_count=len(rows),
            categories=categories,
        )
    finally:
        conn.close()
