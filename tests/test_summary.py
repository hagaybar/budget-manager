"""Tests for the /api/summary/monthly endpoint."""

import pytest


# ---------------------------------------------------------------------------
# Helper
# ---------------------------------------------------------------------------

def _add_tx(client, *, type="income", amount=100.0, category="Salary",
            description="", date="2026-03-15"):
    """Convenience wrapper to create a transaction via the API."""
    resp = client.post("/api/transactions/", json={
        "type": type,
        "amount": amount,
        "category": category,
        "description": description,
        "date": date,
    })
    assert resp.status_code == 201, resp.text
    return resp.json()


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

class TestMonthlySummary:
    """GET /api/summary/monthly?year=...&month=..."""

    def test_monthly_summary_basic(self, client):
        """Income and expense in the same month are totalled correctly."""
        _add_tx(client, type="income", amount=3000.0, category="Salary",
                date="2026-03-01")
        _add_tx(client, type="expense", amount=200.0, category="Food",
                date="2026-03-05")

        resp = client.get("/api/summary/monthly",
                          params={"year": 2026, "month": 3})
        assert resp.status_code == 200
        data = resp.json()

        assert data["year"] == 2026
        assert data["month"] == 3
        assert data["total_income"] == 3000.0
        assert data["total_expenses"] == 200.0
        assert data["transaction_count"] == 2

    def test_monthly_summary_empty(self, client):
        """A month with no transactions returns zeros."""
        resp = client.get("/api/summary/monthly",
                          params={"year": 2026, "month": 1})
        assert resp.status_code == 200
        data = resp.json()

        assert data["total_income"] == 0.0
        assert data["total_expenses"] == 0.0
        assert data["net_balance"] == 0.0
        assert data["transaction_count"] == 0
        assert data["categories"] == []

    def test_monthly_summary_net_balance(self, client):
        """net_balance equals total_income minus total_expenses."""
        _add_tx(client, type="income", amount=5000.0, category="Salary",
                date="2026-03-01")
        _add_tx(client, type="expense", amount=1200.0, category="Rent",
                date="2026-03-01")
        _add_tx(client, type="expense", amount=300.0, category="Food",
                date="2026-03-10")

        resp = client.get("/api/summary/monthly",
                          params={"year": 2026, "month": 3})
        data = resp.json()

        assert data["total_income"] == 5000.0
        assert data["total_expenses"] == 1500.0
        assert data["net_balance"] == 3500.0

    def test_monthly_summary_category_breakdown(self, client):
        """Each category has an accurate total and count."""
        _add_tx(client, type="expense", amount=50.0, category="Food",
                date="2026-03-01")
        _add_tx(client, type="expense", amount=30.0, category="Food",
                date="2026-03-02")
        _add_tx(client, type="expense", amount=100.0, category="Transport",
                date="2026-03-03")

        resp = client.get("/api/summary/monthly",
                          params={"year": 2026, "month": 3})
        data = resp.json()

        cats = {c["category"]: c for c in data["categories"]}
        assert "Food" in cats
        assert cats["Food"]["total"] == 80.0
        assert cats["Food"]["count"] == 2
        assert "Transport" in cats
        assert cats["Transport"]["total"] == 100.0
        assert cats["Transport"]["count"] == 1

    def test_monthly_summary_different_months(self, client):
        """Only transactions in the queried month appear in the summary."""
        _add_tx(client, type="income", amount=1000.0, category="Salary",
                date="2026-02-15")
        _add_tx(client, type="income", amount=2000.0, category="Salary",
                date="2026-03-15")
        _add_tx(client, type="expense", amount=500.0, category="Rent",
                date="2026-04-01")

        # Query March only
        resp = client.get("/api/summary/monthly",
                          params={"year": 2026, "month": 3})
        data = resp.json()

        assert data["total_income"] == 2000.0
        assert data["total_expenses"] == 0.0
        assert data["transaction_count"] == 1

    def test_monthly_summary_multiple_categories(self, client):
        """Multiple income and expense categories are reported separately."""
        _add_tx(client, type="income", amount=5000.0, category="Salary",
                date="2026-03-01")
        _add_tx(client, type="income", amount=200.0, category="Freelance",
                date="2026-03-05")
        _add_tx(client, type="expense", amount=1200.0, category="Rent",
                date="2026-03-01")
        _add_tx(client, type="expense", amount=80.0, category="Food",
                date="2026-03-10")
        _add_tx(client, type="expense", amount=45.0, category="Food",
                date="2026-03-12")

        resp = client.get("/api/summary/monthly",
                          params={"year": 2026, "month": 3})
        data = resp.json()

        assert data["total_income"] == 5200.0
        assert data["total_expenses"] == 1325.0
        assert data["net_balance"] == 3875.0
        assert data["transaction_count"] == 5

        cats = {c["category"]: c for c in data["categories"]}
        # The implementation keys by "type:category" so same category
        # under different types may appear once per type.  All our
        # Food entries are expenses and all Salary are income, so each
        # category name appears once.
        assert cats["Salary"]["total"] == 5000.0
        assert cats["Salary"]["count"] == 1
        assert cats["Freelance"]["total"] == 200.0
        assert cats["Freelance"]["count"] == 1
        assert cats["Rent"]["total"] == 1200.0
        assert cats["Rent"]["count"] == 1
        assert cats["Food"]["total"] == 125.0
        assert cats["Food"]["count"] == 2

    def test_monthly_summary_missing_params(self, client):
        """Omitting required year/month params returns 422."""
        resp = client.get("/api/summary/monthly")
        assert resp.status_code == 422

    def test_monthly_summary_invalid_month(self, client):
        """Month outside 1-12 returns 422."""
        resp = client.get("/api/summary/monthly",
                          params={"year": 2026, "month": 13})
        assert resp.status_code == 422
