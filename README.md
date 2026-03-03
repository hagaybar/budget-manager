# Budget Manager API

A RESTful API for managing personal budgets -- track income and expenses with monthly summaries.

## Tech Stack

- **Python 3.12**
- **FastAPI** -- high-performance async web framework
- **SQLite** -- lightweight embedded database (zero configuration)
- **Pydantic v2** -- data validation and serialization

## Setup

```bash
# Clone the repository
git clone <repo-url>
cd budget_manager

# Create and activate a virtual environment
python -m venv venv
source venv/bin/activate   # Linux / macOS
# venv\Scripts\activate    # Windows

# Install dependencies
pip install -r requirements.txt

# Run the server
uvicorn app.main:app --reload
```

The API will be available at `http://127.0.0.1:8000`.

## API Endpoints

### Create a Transaction

```
POST /api/transactions
```

```bash
curl -X POST http://127.0.0.1:8000/api/transactions/ \
  -H "Content-Type: application/json" \
  -d '{
    "type": "expense",
    "amount": 42.50,
    "category": "Food",
    "description": "Groceries",
    "date": "2026-03-03"
  }'
```

**Response** (201 Created):

```json
{
  "id": 1,
  "type": "expense",
  "amount": 42.5,
  "category": "Food",
  "description": "Groceries",
  "date": "2026-03-03",
  "created_at": "2026-03-03 12:00:00"
}
```

### List Transactions

```
GET /api/transactions
```

Supports optional query filters: `type`, `category`, `date_from`, `date_to`.

```bash
# List all transactions
curl http://127.0.0.1:8000/api/transactions/

# Filter by type
curl "http://127.0.0.1:8000/api/transactions/?type=income"

# Filter by date range
curl "http://127.0.0.1:8000/api/transactions/?date_from=2026-03-01&date_to=2026-03-31"

# Filter by category
curl "http://127.0.0.1:8000/api/transactions/?category=Food"
```

**Response** (200 OK):

```json
[
  {
    "id": 1,
    "type": "expense",
    "amount": 42.5,
    "category": "Food",
    "description": "Groceries",
    "date": "2026-03-03",
    "created_at": "2026-03-03 12:00:00"
  }
]
```

### Get a Single Transaction

```
GET /api/transactions/{id}
```

```bash
curl http://127.0.0.1:8000/api/transactions/1
```

**Response** (200 OK):

```json
{
  "id": 1,
  "type": "expense",
  "amount": 42.5,
  "category": "Food",
  "description": "Groceries",
  "date": "2026-03-03",
  "created_at": "2026-03-03 12:00:00"
}
```

### Update a Transaction

```
PUT /api/transactions/{id}
```

All fields are optional; only provided fields are updated.

```bash
curl -X PUT http://127.0.0.1:8000/api/transactions/1 \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 55.00,
    "description": "Groceries + snacks"
  }'
```

**Response** (200 OK):

```json
{
  "id": 1,
  "type": "expense",
  "amount": 55.0,
  "category": "Food",
  "description": "Groceries + snacks",
  "date": "2026-03-03",
  "created_at": "2026-03-03 12:00:00"
}
```

### Delete a Transaction

```
DELETE /api/transactions/{id}
```

```bash
curl -X DELETE http://127.0.0.1:8000/api/transactions/1
```

**Response**: 204 No Content

### Monthly Summary

```
GET /api/summary/monthly?year=2026&month=3
```

Returns total income, total expenses, net balance, transaction count, and a per-category breakdown.

```bash
curl "http://127.0.0.1:8000/api/summary/monthly?year=2026&month=3"
```

**Response** (200 OK):

```json
{
  "year": 2026,
  "month": 3,
  "total_income": 5000.0,
  "total_expenses": 1325.0,
  "net_balance": 3675.0,
  "transaction_count": 4,
  "categories": [
    { "category": "Food", "total": 125.0, "count": 2 },
    { "category": "Rent", "total": 1200.0, "count": 1 },
    { "category": "Salary", "total": 5000.0, "count": 1 }
  ]
}
```

## Running Tests

```bash
pytest tests/ -v --cov=app
```

For a full coverage report with missing lines:

```bash
python -m pytest tests/ -v --cov=app --cov-report=term-missing
```

## Project Structure

```
budget_manager/
├── app/
│   ├── __init__.py
│   ├── database.py          # SQLite connection and table creation
│   ├── main.py              # FastAPI application entry point
│   ├── models.py            # Internal enums and constants
│   ├── schemas.py           # Pydantic v2 request/response models
│   └── routers/
│       ├── __init__.py
│       ├── transactions.py  # CRUD endpoints for /api/transactions
│       └── summary.py       # Monthly summary endpoint /api/summary
├── tests/
│   ├── __init__.py
│   ├── conftest.py          # Shared fixtures (temp DB, test client)
│   ├── test_models.py       # Pydantic schema unit tests
│   ├── test_summary.py      # Monthly summary endpoint tests
│   └── test_transactions.py # Transaction CRUD endpoint tests
├── requirements.txt
└── README.md
```

## API Documentation (Swagger)

FastAPI auto-generates interactive API documentation. Once the server is running, visit:

- **Swagger UI**: [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)
- **ReDoc**: [http://127.0.0.1:8000/redoc](http://127.0.0.1:8000/redoc)
