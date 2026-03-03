"""Tests for the /api/recurring CRUD and generate endpoints."""

import pytest


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_recurring(client, **overrides):
    """POST a valid recurring transaction definition and return the response."""
    payload = {
        "type": "income",
        "amount": 5000.0,
        "category": "Salary",
        "description": "Monthly salary",
        "frequency": "monthly",
        "day_of_month": 1,
        "start_date": "2026-01-01",
    }
    payload.update(overrides)
    resp = client.post("/api/recurring", json=payload)
    return resp


def _make_weekly_recurring(client, **overrides):
    """POST a valid weekly recurring definition and return the response."""
    payload = {
        "type": "income",
        "amount": 5000.0,
        "category": "Salary",
        "description": "Weekly salary",
        "frequency": "weekly",
        "day_of_week": 4,
        "start_date": "2026-01-01",
    }
    payload.update(overrides)
    resp = client.post("/api/recurring", json=payload)
    return resp


# ---------------------------------------------------------------------------
# CREATE
# ---------------------------------------------------------------------------

class TestCreateRecurring:
    """POST /api/recurring"""

    def test_create_weekly_recurring(self, client):
        """Create a weekly income recurring with day_of_week=4 (Friday). Expect 201."""
        resp = _make_weekly_recurring(
            client,
            type="income",
            amount=5000.0,
            category="Salary",
            frequency="weekly",
            day_of_week=4,
        )
        assert resp.status_code == 201
        data = resp.json()
        assert data["type"] == "income"
        assert data["amount"] == 5000.0
        assert data["category"] == "Salary"
        assert data["frequency"] == "weekly"
        assert data["day_of_week"] == 4
        assert data["day_of_month"] is None
        assert "id" in data
        assert "created_at" in data
        assert data["is_active"] == 1

    def test_create_monthly_recurring(self, client):
        """Create a monthly expense recurring with day_of_month=1. Expect 201."""
        resp = _make_recurring(
            client,
            type="expense",
            amount=1200.0,
            category="Rent",
            description="Monthly apartment rent",
            frequency="monthly",
            day_of_month=1,
        )
        assert resp.status_code == 201
        data = resp.json()
        assert data["type"] == "expense"
        assert data["amount"] == 1200.0
        assert data["category"] == "Rent"
        assert data["frequency"] == "monthly"
        assert data["day_of_month"] == 1
        assert data["day_of_week"] is None

    def test_create_recurring_no_end_date(self, client):
        """Create without end_date -- verify end_date is null in response."""
        resp = _make_recurring(client)
        assert resp.status_code == 201
        data = resp.json()
        assert data["end_date"] is None

    def test_create_recurring_with_end_date(self, client):
        """Create with end_date='2026-12-31' -- verify it appears in response."""
        resp = _make_recurring(client, end_date="2026-12-31")
        assert resp.status_code == 201
        data = resp.json()
        assert data["end_date"] == "2026-12-31"

    def test_create_recurring_with_start_date(self, client):
        """Create with start_date='2026-01-01' -- verify it appears in response."""
        resp = _make_recurring(client, start_date="2026-01-01")
        assert resp.status_code == 201
        data = resp.json()
        assert data["start_date"] == "2026-01-01"


# ---------------------------------------------------------------------------
# LIST
# ---------------------------------------------------------------------------

class TestListRecurring:
    """GET /api/recurring"""

    def test_list_recurring(self, client):
        """Create 2 recurring definitions, GET /api/recurring returns list of 2."""
        resp1 = _make_recurring(client, category="Salary")
        assert resp1.status_code == 201
        resp2 = _make_recurring(client, category="Rent", type="expense", amount=1200.0)
        assert resp2.status_code == 201

        resp = client.get("/api/recurring")
        assert resp.status_code == 200
        data = resp.json()
        assert isinstance(data, list)
        assert len(data) == 2


# ---------------------------------------------------------------------------
# GET BY ID
# ---------------------------------------------------------------------------

class TestGetRecurringById:
    """GET /api/recurring/{id}"""

    def test_get_recurring_by_id(self, client):
        """Create one, GET by id, verify fields match."""
        create_resp = _make_recurring(client, category="Insurance", amount=300.0)
        assert create_resp.status_code == 201
        rec_id = create_resp.json()["id"]

        resp = client.get(f"/api/recurring/{rec_id}")
        assert resp.status_code == 200
        data = resp.json()
        assert data["id"] == rec_id
        assert data["category"] == "Insurance"
        assert data["amount"] == 300.0

    def test_recurring_not_found(self, client):
        """GET /api/recurring/9999 returns 404."""
        resp = client.get("/api/recurring/9999")
        assert resp.status_code == 404


# ---------------------------------------------------------------------------
# UPDATE
# ---------------------------------------------------------------------------

class TestUpdateRecurring:
    """PUT /api/recurring/{id}"""

    def test_update_recurring(self, client):
        """Create, PUT with new amount, verify updated value."""
        create_resp = _make_recurring(client, amount=1000.0)
        assert create_resp.status_code == 201
        rec_id = create_resp.json()["id"]

        resp = client.put(f"/api/recurring/{rec_id}", json={"amount": 2000.0})
        assert resp.status_code == 200
        data = resp.json()
        assert data["amount"] == 2000.0
        # Other fields unchanged
        assert data["category"] == "Salary"


# ---------------------------------------------------------------------------
# DELETE
# ---------------------------------------------------------------------------

class TestDeleteRecurring:
    """DELETE /api/recurring/{id}"""

    def test_delete_recurring(self, client):
        """Create, DELETE, expect 204. Subsequent GET should 404."""
        create_resp = _make_recurring(client)
        assert create_resp.status_code == 201
        rec_id = create_resp.json()["id"]

        del_resp = client.delete(f"/api/recurring/{rec_id}")
        assert del_resp.status_code == 204

        get_resp = client.get(f"/api/recurring/{rec_id}")
        assert get_resp.status_code == 404


# ---------------------------------------------------------------------------
# GENERATE TRANSACTIONS
# ---------------------------------------------------------------------------

class TestGenerateTransactions:
    """POST /api/recurring/{id}/generate"""

    def test_generate_transactions_from_weekly(self, client):
        """Create weekly Friday recurring (start 2026-03-01, end 2026-03-31),
        generate for March 2026 -- expect 4 Fridays (Mar 6, 13, 20, 27)."""
        create_resp = _make_weekly_recurring(
            client,
            type="income",
            amount=5000.0,
            category="Salary",
            frequency="weekly",
            day_of_week=4,  # Friday (0=Mon, 4=Fri)
            start_date="2026-03-01",
            end_date="2026-03-31",
        )
        assert create_resp.status_code == 201
        rec_id = create_resp.json()["id"]

        gen_resp = client.post(
            f"/api/recurring/{rec_id}/generate",
            params={"start_date": "2026-03-01", "end_date": "2026-03-31"},
        )
        assert gen_resp.status_code == 200
        data = gen_resp.json()
        assert data["generated_count"] == 4

        # Verify exact Friday dates in March 2026
        dates = sorted([tx["date"] for tx in data["transactions"]])
        assert dates == ["2026-03-06", "2026-03-13", "2026-03-20", "2026-03-27"]

    def test_generate_transactions_from_monthly(self, client):
        """Create monthly day=15 (start 2026-01-01, end 2026-03-31),
        generate for Jan-Mar -- verify 3 transactions (Jan 15, Feb 15, Mar 15)."""
        create_resp = _make_recurring(
            client,
            type="expense",
            amount=1200.0,
            category="Rent",
            frequency="monthly",
            day_of_month=15,
            start_date="2026-01-01",
            end_date="2026-03-31",
        )
        assert create_resp.status_code == 201
        rec_id = create_resp.json()["id"]

        gen_resp = client.post(
            f"/api/recurring/{rec_id}/generate",
            params={"start_date": "2026-01-01", "end_date": "2026-03-31"},
        )
        assert gen_resp.status_code == 200
        data = gen_resp.json()
        assert data["generated_count"] == 3

        dates = sorted([tx["date"] for tx in data["transactions"]])
        assert dates == ["2026-01-15", "2026-02-15", "2026-03-15"]

    def test_generate_respects_end_date(self, client):
        """Create with end_date=2026-02-28, generate for Jan-Mar.
        Verify only Jan and Feb transactions are created (not March)."""
        create_resp = _make_recurring(
            client,
            type="expense",
            amount=500.0,
            category="Subscription",
            frequency="monthly",
            day_of_month=15,
            start_date="2026-01-01",
            end_date="2026-02-28",
        )
        assert create_resp.status_code == 201
        rec_id = create_resp.json()["id"]

        gen_resp = client.post(
            f"/api/recurring/{rec_id}/generate",
            params={"start_date": "2026-01-01", "end_date": "2026-03-31"},
        )
        assert gen_resp.status_code == 200
        data = gen_resp.json()
        assert data["generated_count"] == 2

        dates = sorted([tx["date"] for tx in data["transactions"]])
        assert dates == ["2026-01-15", "2026-02-15"]

    def test_generate_respects_start_date(self, client):
        """Create with start_date=2026-02-01, generate for Jan-Mar.
        Verify only Feb and Mar transactions (not January)."""
        create_resp = _make_recurring(
            client,
            type="expense",
            amount=800.0,
            category="Insurance",
            frequency="monthly",
            day_of_month=15,
            start_date="2026-02-01",
            end_date="2026-03-31",
        )
        assert create_resp.status_code == 201
        rec_id = create_resp.json()["id"]

        gen_resp = client.post(
            f"/api/recurring/{rec_id}/generate",
            params={"start_date": "2026-01-01", "end_date": "2026-03-31"},
        )
        assert gen_resp.status_code == 200
        data = gen_resp.json()
        assert data["generated_count"] == 2

        dates = sorted([tx["date"] for tx in data["transactions"]])
        assert dates == ["2026-02-15", "2026-03-15"]

    def test_generate_idempotent(self, client):
        """Generate twice for the same range -- verify no duplicate transactions."""
        create_resp = _make_recurring(
            client,
            type="expense",
            amount=100.0,
            category="Gym",
            frequency="monthly",
            day_of_month=1,
            start_date="2026-01-01",
            end_date="2026-03-31",
        )
        assert create_resp.status_code == 201
        rec_id = create_resp.json()["id"]

        params = {"start_date": "2026-01-01", "end_date": "2026-03-31"}

        # First generation
        gen1 = client.post(f"/api/recurring/{rec_id}/generate", params=params)
        assert gen1.status_code == 200
        assert gen1.json()["generated_count"] == 3

        # Second generation -- same range, should produce 0 new
        gen2 = client.post(f"/api/recurring/{rec_id}/generate", params=params)
        assert gen2.status_code == 200
        assert gen2.json()["generated_count"] == 0

        # Verify total transactions via GET /api/transactions
        all_tx = client.get("/api/transactions/").json()
        # Only the 3 from the first generation exist
        recurring_txs = [tx for tx in all_tx if tx.get("recurring_id") == rec_id]
        assert len(recurring_txs) == 3

    def test_generated_transactions_have_recurring_id(self, client):
        """After generate, GET /api/transactions -- verify recurring_id field is set."""
        create_resp = _make_recurring(
            client,
            type="income",
            amount=200.0,
            category="Freelance",
            frequency="monthly",
            day_of_month=10,
            start_date="2026-03-01",
            end_date="2026-03-31",
        )
        assert create_resp.status_code == 201
        rec_id = create_resp.json()["id"]

        gen_resp = client.post(
            f"/api/recurring/{rec_id}/generate",
            params={"start_date": "2026-03-01", "end_date": "2026-03-31"},
        )
        assert gen_resp.status_code == 200
        assert gen_resp.json()["generated_count"] == 1

        # Fetch all transactions and verify recurring_id
        all_tx = client.get("/api/transactions/").json()
        assert len(all_tx) >= 1
        for tx in all_tx:
            assert "recurring_id" in tx
        # The generated transaction should have the correct recurring_id
        gen_tx = [tx for tx in all_tx if tx.get("recurring_id") == rec_id]
        assert len(gen_tx) == 1
        assert gen_tx[0]["recurring_id"] == rec_id


# ---------------------------------------------------------------------------
# VALIDATION ERRORS
# ---------------------------------------------------------------------------

class TestRecurringValidation:
    """Validation error scenarios for POST /api/recurring."""

    def test_invalid_frequency(self, client):
        """POST with frequency='daily' -- expect 422."""
        resp = client.post("/api/recurring", json={
            "type": "income",
            "amount": 100.0,
            "category": "Test",
            "frequency": "daily",
            "day_of_week": 0,
            "start_date": "2026-01-01",
        })
        assert resp.status_code == 422

    def test_invalid_day_of_week(self, client):
        """POST weekly with day_of_week=7 -- expect 422 (valid range 0-6)."""
        resp = client.post("/api/recurring", json={
            "type": "income",
            "amount": 100.0,
            "category": "Test",
            "frequency": "weekly",
            "day_of_week": 7,
            "start_date": "2026-01-01",
        })
        assert resp.status_code == 422

    def test_invalid_day_of_month(self, client):
        """POST monthly with day_of_month=32 -- expect 422 (valid range 1-31)."""
        resp = client.post("/api/recurring", json={
            "type": "expense",
            "amount": 100.0,
            "category": "Test",
            "frequency": "monthly",
            "day_of_month": 32,
            "start_date": "2026-01-01",
        })
        assert resp.status_code == 422

    def test_weekly_requires_day_of_week(self, client):
        """POST weekly without day_of_week -- expect 422."""
        resp = client.post("/api/recurring", json={
            "type": "income",
            "amount": 100.0,
            "category": "Test",
            "frequency": "weekly",
            "start_date": "2026-01-01",
        })
        assert resp.status_code == 422

    def test_monthly_requires_day_of_month(self, client):
        """POST monthly without day_of_month -- expect 422."""
        resp = client.post("/api/recurring", json={
            "type": "expense",
            "amount": 100.0,
            "category": "Test",
            "frequency": "monthly",
            "start_date": "2026-01-01",
        })
        assert resp.status_code == 422
