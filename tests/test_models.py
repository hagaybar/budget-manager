"""Tests for Pydantic schemas defined in app/schemas.py."""

import pytest
from pydantic import ValidationError

from app.schemas import (
    CategoryBreakdown,
    MonthlySummary,
    TransactionCreate,
    TransactionResponse,
    TransactionUpdate,
)


# ---------------------------------------------------------------------------
# TransactionCreate
# ---------------------------------------------------------------------------

class TestTransactionCreate:

    def test_transaction_create_valid(self):
        """A fully-valid payload is accepted."""
        tx = TransactionCreate(
            type="income",
            amount=1500.0,
            category="Salary",
            description="March pay",
            date="2026-03-01",
        )
        assert tx.type == "income"
        assert tx.amount == 1500.0
        assert tx.category == "Salary"
        assert tx.description == "March pay"
        assert tx.date == "2026-03-01"

    def test_transaction_create_default_description(self):
        """description defaults to empty string when omitted."""
        tx = TransactionCreate(
            type="expense",
            amount=50.0,
            category="Food",
            date="2026-03-10",
        )
        assert tx.description == ""

    def test_transaction_create_invalid_type(self):
        """A type value other than 'income'/'expense' raises ValidationError."""
        with pytest.raises(ValidationError) as exc_info:
            TransactionCreate(
                type="refund",
                amount=100.0,
                category="Misc",
                date="2026-03-01",
            )
        errors = exc_info.value.errors()
        # The error should relate to the 'type' field
        assert any(e["loc"] == ("type",) for e in errors)

    def test_transaction_create_negative_amount(self):
        """A negative amount raises ValidationError (gt=0)."""
        with pytest.raises(ValidationError) as exc_info:
            TransactionCreate(
                type="expense",
                amount=-10.0,
                category="Food",
                date="2026-03-01",
            )
        errors = exc_info.value.errors()
        assert any("amount" in str(e["loc"]) for e in errors)

    def test_transaction_create_zero_amount(self):
        """Zero amount raises ValidationError (gt=0, not gte)."""
        with pytest.raises(ValidationError):
            TransactionCreate(
                type="income",
                amount=0,
                category="Salary",
                date="2026-03-01",
            )

    def test_transaction_create_invalid_date_format(self):
        """A date not matching YYYY-MM-DD raises ValidationError."""
        with pytest.raises(ValidationError):
            TransactionCreate(
                type="income",
                amount=100.0,
                category="Salary",
                date="01-03-2026",
            )

    def test_transaction_create_invalid_date_value(self):
        """A date that matches the format but is not real raises ValidationError."""
        with pytest.raises(ValidationError):
            TransactionCreate(
                type="income",
                amount=100.0,
                category="Salary",
                date="2026-02-30",
            )

    def test_transaction_create_empty_category(self):
        """An empty category string raises ValidationError (min_length=1)."""
        with pytest.raises(ValidationError):
            TransactionCreate(
                type="income",
                amount=100.0,
                category="",
                date="2026-03-01",
            )


# ---------------------------------------------------------------------------
# TransactionUpdate
# ---------------------------------------------------------------------------

class TestTransactionUpdate:

    def test_transaction_update_partial(self):
        """Providing only some fields is valid."""
        tx = TransactionUpdate(amount=999.0)
        assert tx.amount == 999.0
        assert tx.type is None
        assert tx.category is None
        assert tx.description is None
        assert tx.date is None

    def test_transaction_update_full(self):
        """All fields can be provided at once."""
        tx = TransactionUpdate(
            type="expense",
            amount=200.0,
            category="Travel",
            description="Flight",
            date="2026-04-01",
        )
        assert tx.type == "expense"
        assert tx.amount == 200.0

    def test_transaction_update_empty_raises(self):
        """An empty update (no fields) raises ValidationError."""
        with pytest.raises(ValidationError):
            TransactionUpdate()

    def test_transaction_update_invalid_type(self):
        """An invalid type in update raises ValidationError."""
        with pytest.raises(ValidationError):
            TransactionUpdate(type="refund")

    def test_transaction_update_negative_amount(self):
        """Negative amount in update raises ValidationError."""
        with pytest.raises(ValidationError):
            TransactionUpdate(amount=-50.0)

    def test_transaction_update_invalid_date(self):
        """Invalid date format in update raises ValidationError."""
        with pytest.raises(ValidationError):
            TransactionUpdate(date="not-a-date")


# ---------------------------------------------------------------------------
# TransactionResponse
# ---------------------------------------------------------------------------

class TestTransactionResponse:

    def test_transaction_response_model(self):
        """TransactionResponse accepts all required fields."""
        tx = TransactionResponse(
            id=1,
            type="income",
            amount=500.0,
            category="Salary",
            description="Pay",
            date="2026-03-01",
            created_at="2026-03-01 12:00:00",
        )
        assert tx.id == 1
        assert tx.type == "income"
        assert tx.amount == 500.0
        assert tx.created_at == "2026-03-01 12:00:00"

    def test_transaction_response_missing_id(self):
        """Omitting 'id' raises ValidationError."""
        with pytest.raises(ValidationError):
            TransactionResponse(
                type="income",
                amount=500.0,
                category="Salary",
                description="Pay",
                date="2026-03-01",
                created_at="2026-03-01 12:00:00",
            )


# ---------------------------------------------------------------------------
# CategoryBreakdown & MonthlySummary
# ---------------------------------------------------------------------------

class TestCategoryBreakdown:

    def test_category_breakdown_model(self):
        """CategoryBreakdown accepts valid data."""
        cb = CategoryBreakdown(category="Food", total=125.50, count=3)
        assert cb.category == "Food"
        assert cb.total == 125.50
        assert cb.count == 3

    def test_category_breakdown_missing_field(self):
        """Omitting required fields raises ValidationError."""
        with pytest.raises(ValidationError):
            CategoryBreakdown(category="Food")


class TestMonthlySummary:

    def test_monthly_summary_model(self):
        """MonthlySummary with nested categories is valid."""
        summary = MonthlySummary(
            year=2026,
            month=3,
            total_income=5000.0,
            total_expenses=1500.0,
            net_balance=3500.0,
            transaction_count=4,
            categories=[
                CategoryBreakdown(category="Salary", total=5000.0, count=1),
                CategoryBreakdown(category="Food", total=1500.0, count=3),
            ],
        )
        assert summary.year == 2026
        assert summary.month == 3
        assert summary.net_balance == 3500.0
        assert len(summary.categories) == 2

    def test_monthly_summary_empty_categories(self):
        """MonthlySummary with an empty categories list is valid."""
        summary = MonthlySummary(
            year=2026,
            month=1,
            total_income=0.0,
            total_expenses=0.0,
            net_balance=0.0,
            transaction_count=0,
            categories=[],
        )
        assert summary.categories == []
        assert summary.transaction_count == 0
