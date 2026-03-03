"""Playwright E2E tests for Budget Manager UI."""
import pytest
import httpx
from playwright.sync_api import sync_playwright, expect

BASE_URL = "http://localhost:8018"


@pytest.fixture(scope="module")
def browser():
    with sync_playwright() as p:
        b = p.chromium.launch(headless=True)
        yield b
        b.close()


@pytest.fixture
def page(browser):
    ctx = browser.new_context(viewport={"width": 1280, "height": 900})
    pg = ctx.new_page()
    # Clean DB before each test
    with httpx.Client(base_url=BASE_URL, follow_redirects=True) as client:
        txns = client.get("/api/transactions").json()
        for t in txns:
            client.delete(f"/api/transactions/{t['id']}")
    yield pg
    pg.close()
    ctx.close()


def _add_transaction(page, type_val, amount, category, description, date):
    """Helper to fill and submit the add transaction form."""
    page.select_option('[data-testid="type-select"]', type_val)
    page.fill('[data-testid="amount-input"]', str(amount))
    page.fill('[data-testid="category-input"]', category)
    page.fill('[data-testid="description-input"]', description)
    page.fill('[data-testid="date-input"]', date)
    page.click('[data-testid="submit-btn"]')
    page.wait_for_timeout(1000)  # Wait for API call and list refresh


class TestHomepage:
    def test_homepage_loads(self, page):
        page.goto(BASE_URL)
        expect(page.locator("h1, [data-testid='app-title']")).to_contain_text("Budget Manager")
        expect(page.locator('[data-testid="add-form"]')).to_be_visible()
        expect(page.locator('[data-testid="transaction-list"]')).to_be_visible()

    def test_mobile_responsive(self, page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(BASE_URL)
        expect(page.locator('[data-testid="add-form"]')).to_be_visible()
        # Check no horizontal overflow
        overflow = page.evaluate("document.documentElement.scrollWidth > document.documentElement.clientWidth")
        assert overflow is False, "Page has horizontal overflow on mobile"


class TestAddTransaction:
    def test_add_income_transaction(self, page):
        page.goto(BASE_URL)
        _add_transaction(page, "income", 1000, "Salary", "Test salary", "2026-03-01")
        rows = page.locator('[data-testid="transaction-row"]')
        expect(rows).to_have_count(1)
        expect(rows.first).to_contain_text("1,000")
        expect(rows.first).to_contain_text("Salary")

    def test_add_expense_transaction(self, page):
        page.goto(BASE_URL)
        _add_transaction(page, "expense", 50, "Food", "Lunch", "2026-03-01")
        rows = page.locator('[data-testid="transaction-row"]')
        expect(rows).to_have_count(1)
        expect(rows.first).to_contain_text("50")
        expect(rows.first).to_contain_text("Food")


class TestTransactionList:
    def test_transaction_list_displays(self, page):
        page.goto(BASE_URL)
        _add_transaction(page, "income", 2000, "Salary", "March pay", "2026-03-01")
        _add_transaction(page, "expense", 150, "Groceries", "Weekly shop", "2026-03-02")
        rows = page.locator('[data-testid="transaction-row"]')
        expect(rows).to_have_count(2)

    def test_delete_transaction(self, page):
        page.goto(BASE_URL)
        _add_transaction(page, "expense", 30, "Coffee", "Latte", "2026-03-01")
        rows = page.locator('[data-testid="transaction-row"]')
        expect(rows).to_have_count(1)
        page.click('[data-testid="delete-btn"]')
        page.wait_for_timeout(1000)
        rows = page.locator('[data-testid="transaction-row"]')
        expect(rows).to_have_count(0)


class TestMonthlySummary:
    def test_monthly_summary_displays(self, page):
        page.goto(BASE_URL)
        _add_transaction(page, "income", 3000, "Salary", "March pay", "2026-03-01")
        _add_transaction(page, "expense", 500, "Rent", "Monthly rent", "2026-03-02")
        page.wait_for_timeout(1500)
        summary = page.locator('[data-testid="summary-section"]')
        expect(summary).to_be_visible()
        expect(page.locator('[data-testid="total-income"]')).to_contain_text("3,000")
        expect(page.locator('[data-testid="total-expenses"]')).to_contain_text("500")
        expect(page.locator('[data-testid="net-balance"]')).to_contain_text("2,500")
