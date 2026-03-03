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
        # Clean recurring transactions (guarded in case endpoint not yet live)
        try:
            recs = client.get("/api/recurring").json()
            for r in recs:
                client.delete(f"/api/recurring/{r['id']}")
        except Exception:
            pass
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


def _add_recurring(page, type_val, amount, category, description, frequency,
                   day_value, start_date, end_date=None):
    """Helper to fill and submit the recurring transaction form.

    Parameters
    ----------
    type_val : str
        "income" or "expense".
    amount : float | int
        Recurring amount.
    category : str
        Category text.
    description : str
        Description text.
    frequency : str
        "weekly" or "monthly".
    day_value : int | str
        For weekly: the day_of_week value (0=Monday..6=Sunday) to select.
        For monthly: the day_of_month integer to fill.
    start_date : str
        YYYY-MM-DD format start date.
    end_date : str | None
        YYYY-MM-DD format end date, or None to leave blank.
    """
    page.select_option('[data-testid="rec-type-select"]', type_val)
    page.fill('[data-testid="rec-amount-input"]', str(amount))
    page.fill('[data-testid="rec-category-input"]', category)
    page.fill('[data-testid="rec-description-input"]', description)
    page.select_option('[data-testid="rec-frequency-select"]', frequency)

    if frequency == "weekly":
        page.select_option('[data-testid="rec-day-of-week"]', str(day_value))
    else:
        page.fill('[data-testid="rec-day-of-month"]', str(day_value))

    page.fill('[data-testid="rec-start-date"]', start_date)

    if end_date:
        page.fill('[data-testid="rec-end-date"]', end_date)

    page.click('[data-testid="rec-submit-btn"]')
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


# ---------------------------------------------------------------------------
# Currency Display Tests
# ---------------------------------------------------------------------------

class TestCurrencyDisplay:
    """Verify that the UI uses the shekel symbol (not dollar)."""

    def test_currency_displays_shekel(self, page):
        """Add a transaction and verify the shekel symbol appears in the UI."""
        page.goto(BASE_URL)
        _add_transaction(page, "income", 1000, "Salary", "Test salary", "2026-03-01")
        page.wait_for_timeout(1500)

        # The transaction row should show the shekel symbol
        row = page.locator('[data-testid="transaction-row"]').first
        row_text = row.inner_text()
        assert "\u20aa" in row_text, (
            f"Expected shekel symbol in transaction row, got: {row_text}"
        )
        assert "$" not in row_text, (
            f"Dollar sign should not appear in transaction row, got: {row_text}"
        )


# ---------------------------------------------------------------------------
# Recurring Transactions UI Tests
# ---------------------------------------------------------------------------

class TestRecurringUI:
    """E2E tests for the recurring transactions UI section."""

    def test_recurring_section_visible(self, page):
        """The recurring transactions section is visible on page load."""
        page.goto(BASE_URL)
        section = page.locator('[data-testid="recurring-section"]')
        expect(section).to_be_visible()

    def test_add_weekly_recurring_via_ui(self, page):
        """Fill the recurring form (weekly, Friday, 5000, Salary) and submit.
        Verify a row appears in the recurring list."""
        page.goto(BASE_URL)
        _add_recurring(
            page,
            type_val="income",
            amount=5000,
            category="Salary",
            description="Weekly pay",
            frequency="weekly",
            day_value=4,  # Friday
            start_date="2026-03-01",
        )
        rows = page.locator('[data-testid="recurring-row"]')
        expect(rows).to_have_count(1)
        expect(rows.first).to_contain_text("Salary")
        expect(rows.first).to_contain_text("5,000")

    def test_delete_recurring_via_ui(self, page):
        """Add a recurring definition, click delete, verify it is removed."""
        page.goto(BASE_URL)
        _add_recurring(
            page,
            type_val="expense",
            amount=1200,
            category="Rent",
            description="Monthly rent",
            frequency="monthly",
            day_value=1,
            start_date="2026-03-01",
        )
        rows = page.locator('[data-testid="recurring-row"]')
        expect(rows).to_have_count(1)

        # Click the delete button on the recurring row
        page.click('[data-testid="rec-delete-btn"]')
        page.wait_for_timeout(1000)

        rows = page.locator('[data-testid="recurring-row"]')
        expect(rows).to_have_count(0)


# ---------------------------------------------------------------------------
# Collapsible Card Sections (Mobile) Tests
# ---------------------------------------------------------------------------

class TestCollapsibleMobile:
    """E2E tests for collapsible card sections on mobile."""

    def test_sections_collapsible_on_mobile(self, page):
        """On mobile, clicking a card title collapses/expands its content."""
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(BASE_URL)
        page.wait_for_timeout(500)

        # The "Add Transaction" card body should start visible
        add_body = page.locator('[data-testid="card-body-add-form"]')
        expect(add_body).to_be_visible()

        # Click the card title to collapse
        page.locator('.card-title').first.click()
        page.wait_for_timeout(500)

        # After collapse, the card body should be hidden
        expect(add_body).not_to_be_visible()

        # Click again to expand
        page.locator('.card-title').first.click()
        page.wait_for_timeout(500)

        # Should be visible again
        expect(add_body).to_be_visible()

    def test_sections_expanded_on_desktop(self, page):
        """On desktop, all sections are always expanded, collapse toggles hidden."""
        page.set_viewport_size({"width": 1280, "height": 900})
        page.goto(BASE_URL)
        page.wait_for_timeout(500)

        # All card bodies should be visible
        for section in ["add-form", "transactions", "summary", "recurring"]:
            body = page.locator(f'[data-testid="card-body-{section}"]')
            expect(body).to_be_visible()

        # Collapse toggles (chevrons) should be hidden on desktop
        toggles = page.locator('[data-testid="collapse-toggle"]')
        if toggles.count() > 0:
            expect(toggles.first).not_to_be_visible()

    def test_collapse_does_not_delete_data(self, page):
        """Collapsing and expanding a section preserves its data."""
        # First add a transaction at desktop size
        page.set_viewport_size({"width": 1280, "height": 900})
        page.goto(BASE_URL)
        _add_transaction(page, "income", 999, "TestCat", "Collapse test", "2026-03-01")
        rows = page.locator('[data-testid="transaction-row"]')
        expect(rows).to_have_count(1)

        # Switch to mobile
        page.set_viewport_size({"width": 375, "height": 667})
        page.wait_for_timeout(500)

        # Get the second card (Transactions), click its title to collapse
        cards = page.locator('.card')
        cards.nth(1).locator('.card-title').click()
        page.wait_for_timeout(500)

        # Body should be hidden
        tx_body = page.locator('[data-testid="card-body-transactions"]')
        expect(tx_body).not_to_be_visible()

        # Expand again
        cards.nth(1).locator('.card-title').click()
        page.wait_for_timeout(500)

        # Body visible and data still there
        expect(tx_body).to_be_visible()
        rows = page.locator('[data-testid="transaction-row"]')
        expect(rows).to_have_count(1)
        expect(rows.first).to_contain_text("999")
