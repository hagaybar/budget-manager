"""Tests for the /api/transactions CRUD endpoints."""

import pytest


# ---------------------------------------------------------------------------
# Helper
# ---------------------------------------------------------------------------

def _make_transaction(client, **overrides):
    """POST a valid transaction and return the JSON response."""
    payload = {
        "type": "income",
        "amount": 1000.0,
        "category": "Salary",
        "description": "Monthly salary",
        "date": "2026-03-01",
    }
    payload.update(overrides)
    resp = client.post("/api/transactions/", json=payload)
    return resp


# ---------------------------------------------------------------------------
# CREATE
# ---------------------------------------------------------------------------

class TestCreateTransaction:
    """POST /api/transactions/"""

    def test_create_income_transaction(self, client):
        """Creating an income transaction returns 201 with full response."""
        resp = _make_transaction(client, type="income", amount=5000.0,
                                  category="Salary", description="March pay",
                                  date="2026-03-15")
        assert resp.status_code == 201
        data = resp.json()
        assert data["type"] == "income"
        assert data["amount"] == 5000.0
        assert data["category"] == "Salary"
        assert data["description"] == "March pay"
        assert data["date"] == "2026-03-15"
        assert "id" in data
        assert "created_at" in data

    def test_create_expense_transaction(self, client):
        """Creating an expense transaction returns 201."""
        resp = _make_transaction(client, type="expense", amount=42.50,
                                  category="Food", description="Groceries",
                                  date="2026-03-02")
        assert resp.status_code == 201
        data = resp.json()
        assert data["type"] == "expense"
        assert data["amount"] == 42.50
        assert data["category"] == "Food"

    def test_create_transaction_invalid_type(self, client):
        """An invalid type value should be rejected with 422."""
        resp = _make_transaction(client, type="other")
        assert resp.status_code == 422

    def test_create_transaction_negative_amount(self, client):
        """A negative amount should be rejected with 422."""
        resp = _make_transaction(client, amount=-100)
        assert resp.status_code == 422

    def test_create_transaction_zero_amount(self, client):
        """A zero amount should be rejected with 422 (amount must be > 0)."""
        resp = _make_transaction(client, amount=0)
        assert resp.status_code == 422

    def test_create_transaction_missing_fields(self, client):
        """An empty body should be rejected with 422."""
        resp = client.post("/api/transactions/", json={})
        assert resp.status_code == 422

    def test_create_transaction_invalid_date_format(self, client):
        """A malformed date should be rejected with 422."""
        resp = _make_transaction(client, date="03-01-2026")
        assert resp.status_code == 422

    def test_create_transaction_invalid_date_value(self, client):
        """A syntactically-correct but impossible date should be rejected."""
        resp = _make_transaction(client, date="2026-02-30")
        assert resp.status_code == 422

    def test_create_transaction_empty_category(self, client):
        """An empty category string should be rejected (min_length=1)."""
        resp = _make_transaction(client, category="")
        assert resp.status_code == 422


# ---------------------------------------------------------------------------
# READ (list / detail)
# ---------------------------------------------------------------------------

class TestListTransactions:
    """GET /api/transactions/"""

    def test_get_all_transactions(self, client):
        """Creating 3 transactions then listing returns all 3."""
        for i in range(3):
            _make_transaction(client, amount=float(100 + i),
                              date=f"2026-03-0{i + 1}")

        resp = client.get("/api/transactions/")
        assert resp.status_code == 200
        data = resp.json()
        assert len(data) == 3

    def test_get_all_transactions_empty(self, client):
        """Listing with no data returns an empty list, not an error."""
        resp = client.get("/api/transactions/")
        assert resp.status_code == 200
        assert resp.json() == []

    def test_filter_by_type(self, client):
        """Filtering by type returns only matching transactions."""
        _make_transaction(client, type="income", amount=100, date="2026-03-01")
        _make_transaction(client, type="expense", amount=50,
                          category="Food", date="2026-03-02")
        _make_transaction(client, type="income", amount=200, date="2026-03-03")

        resp = client.get("/api/transactions/", params={"type": "income"})
        assert resp.status_code == 200
        data = resp.json()
        assert len(data) == 2
        assert all(t["type"] == "income" for t in data)

    def test_filter_by_category(self, client):
        """Filtering by category returns only matching transactions."""
        _make_transaction(client, category="Food", date="2026-03-01")
        _make_transaction(client, category="Transport", date="2026-03-02")
        _make_transaction(client, category="Food", date="2026-03-03")

        resp = client.get("/api/transactions/", params={"category": "Food"})
        assert resp.status_code == 200
        data = resp.json()
        assert len(data) == 2
        assert all(t["category"] == "Food" for t in data)

    def test_filter_by_date_range(self, client):
        """Filtering by date_from and date_to returns transactions in range."""
        _make_transaction(client, date="2026-01-15")
        _make_transaction(client, date="2026-03-10")
        _make_transaction(client, date="2026-06-20")

        resp = client.get("/api/transactions/",
                          params={"date_from": "2026-03-01",
                                  "date_to": "2026-03-31"})
        assert resp.status_code == 200
        data = resp.json()
        assert len(data) == 1
        assert data[0]["date"] == "2026-03-10"


class TestGetTransactionById:
    """GET /api/transactions/{transaction_id}"""

    def test_get_transaction_by_id(self, client):
        """Fetching a transaction by ID returns the correct record."""
        create_resp = _make_transaction(client)
        assert create_resp.status_code == 201
        tx_id = create_resp.json()["id"]

        resp = client.get(f"/api/transactions/{tx_id}")
        assert resp.status_code == 200
        data = resp.json()
        assert data["id"] == tx_id
        assert data["type"] == "income"

    def test_get_transaction_not_found(self, client):
        """Requesting a non-existent ID returns 404."""
        resp = client.get("/api/transactions/99999")
        assert resp.status_code == 404
        assert "not found" in resp.json()["detail"].lower()


# ---------------------------------------------------------------------------
# UPDATE
# ---------------------------------------------------------------------------

class TestUpdateTransaction:
    """PUT /api/transactions/{transaction_id}"""

    def test_update_transaction(self, client):
        """Full update replaces the specified fields."""
        create_resp = _make_transaction(client, type="income", amount=100,
                                        category="Salary", date="2026-03-01")
        tx_id = create_resp.json()["id"]

        update_payload = {
            "type": "expense",
            "amount": 250.0,
            "category": "Travel",
            "description": "Flight tickets",
            "date": "2026-03-05",
        }
        resp = client.put(f"/api/transactions/{tx_id}", json=update_payload)
        assert resp.status_code == 200
        data = resp.json()
        assert data["id"] == tx_id
        assert data["type"] == "expense"
        assert data["amount"] == 250.0
        assert data["category"] == "Travel"
        assert data["description"] == "Flight tickets"
        assert data["date"] == "2026-03-05"

    def test_update_transaction_partial(self, client):
        """Providing only some fields updates only those fields."""
        create_resp = _make_transaction(client, type="income", amount=500,
                                        category="Salary",
                                        description="Old desc",
                                        date="2026-03-01")
        tx_id = create_resp.json()["id"]

        resp = client.put(f"/api/transactions/{tx_id}",
                          json={"amount": 750.0})
        assert resp.status_code == 200
        data = resp.json()
        # Updated field
        assert data["amount"] == 750.0
        # Unchanged fields
        assert data["type"] == "income"
        assert data["category"] == "Salary"
        assert data["description"] == "Old desc"
        assert data["date"] == "2026-03-01"

    def test_update_transaction_not_found(self, client):
        """Updating a non-existent transaction returns 404."""
        resp = client.put("/api/transactions/99999",
                          json={"amount": 100.0})
        assert resp.status_code == 404

    def test_update_transaction_no_fields(self, client):
        """Sending an empty body for update should be rejected (422)."""
        create_resp = _make_transaction(client)
        tx_id = create_resp.json()["id"]

        resp = client.put(f"/api/transactions/{tx_id}", json={})
        assert resp.status_code == 422


# ---------------------------------------------------------------------------
# DELETE
# ---------------------------------------------------------------------------

class TestDeleteTransaction:
    """DELETE /api/transactions/{transaction_id}"""

    def test_delete_transaction(self, client):
        """Deleting an existing transaction returns 204 and it vanishes."""
        create_resp = _make_transaction(client)
        tx_id = create_resp.json()["id"]

        del_resp = client.delete(f"/api/transactions/{tx_id}")
        assert del_resp.status_code == 204

        # Confirm it is gone
        get_resp = client.get(f"/api/transactions/{tx_id}")
        assert get_resp.status_code == 404

    def test_delete_transaction_not_found(self, client):
        """Deleting a non-existent transaction returns 404."""
        resp = client.delete("/api/transactions/99999")
        assert resp.status_code == 404
