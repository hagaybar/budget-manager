"""Playwright script to verify Budget Manager UI and take screenshots."""

import os
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:8018"
SCREENSHOTS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "screenshots")

os.makedirs(SCREENSHOTS_DIR, exist_ok=True)


def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)

        # Desktop viewport
        context = browser.new_context(viewport={"width": 1280, "height": 900})
        page = context.new_page()
        page.goto(BASE_URL)
        page.wait_for_load_state("networkidle")

        # -------------------------------------------------------
        # (b) Add transaction: income, 5000, Salary, March salary, 2026-03-01
        # -------------------------------------------------------
        page.locator('[data-testid="type-select"]').select_option("income")
        page.locator('[data-testid="amount-input"]').fill("5000")
        page.locator('[data-testid="category-input"]').fill("Salary")
        page.locator('[data-testid="description-input"]').fill("March salary")
        page.locator('[data-testid="date-input"]').fill("2026-03-01")
        page.locator('[data-testid="submit-btn"]').click()

        # Wait for the transaction to appear in the list
        page.wait_for_timeout(1000)
        page.wait_for_load_state("networkidle")

        # -------------------------------------------------------
        # (c) Add transaction: expense, 1200, Rent, Monthly rent, 2026-03-02
        # -------------------------------------------------------
        page.locator('[data-testid="type-select"]').select_option("expense")
        page.locator('[data-testid="amount-input"]').fill("1200")
        page.locator('[data-testid="category-input"]').fill("Rent")
        page.locator('[data-testid="description-input"]').fill("Monthly rent")
        page.locator('[data-testid="date-input"]').fill("2026-03-02")
        page.locator('[data-testid="submit-btn"]').click()

        # (d) Wait for list to refresh
        page.wait_for_timeout(1500)
        page.wait_for_load_state("networkidle")

        # (e) Take screenshot -> screenshots/currency_desktop.png
        page.screenshot(
            path=os.path.join(SCREENSHOTS_DIR, "currency_desktop.png"),
            full_page=True,
        )
        print("Screenshot saved: screenshots/currency_desktop.png")

        # -------------------------------------------------------
        # (f) Scroll down to the recurring section
        # -------------------------------------------------------
        page.locator('[data-testid="recurring-section"]').scroll_into_view_if_needed()
        page.wait_for_timeout(500)

        # -------------------------------------------------------
        # (g) Fill recurring form #1: income, 5000, Salary, weekly, Friday (value 4), start 2026-03-01
        # -------------------------------------------------------
        page.locator('[data-testid="rec-type-select"]').select_option("income")
        page.locator('[data-testid="rec-amount-input"]').fill("5000")
        page.locator('[data-testid="rec-category-input"]').fill("Salary")
        page.locator('[data-testid="rec-frequency-select"]').select_option("weekly")
        page.wait_for_timeout(300)
        page.locator('[data-testid="rec-day-of-week"]').select_option("4")
        page.locator('[data-testid="rec-start-date"]').fill("2026-03-01")

        # (h) Submit the recurring form
        page.locator('[data-testid="rec-submit-btn"]').click()
        page.wait_for_timeout(1000)
        page.wait_for_load_state("networkidle")

        # -------------------------------------------------------
        # (i) Fill recurring form #2: expense, 1200, Rent, monthly, day_of_month=1, start 2026-03-01
        # -------------------------------------------------------
        page.locator('[data-testid="rec-type-select"]').select_option("expense")
        page.locator('[data-testid="rec-amount-input"]').fill("1200")
        page.locator('[data-testid="rec-category-input"]').fill("Rent")
        page.locator('[data-testid="rec-frequency-select"]').select_option("monthly")
        page.wait_for_timeout(300)
        page.locator('[data-testid="rec-day-of-month"]').fill("1")
        page.locator('[data-testid="rec-start-date"]').fill("2026-03-01")

        # (j) Submit the recurring form
        page.locator('[data-testid="rec-submit-btn"]').click()

        # (k) Wait for the list to refresh
        page.wait_for_timeout(1500)
        page.wait_for_load_state("networkidle")

        # (l) Take screenshot -> screenshots/recurring_desktop.png
        page.locator('[data-testid="recurring-section"]').scroll_into_view_if_needed()
        page.wait_for_timeout(500)
        page.screenshot(
            path=os.path.join(SCREENSHOTS_DIR, "recurring_desktop.png"),
            full_page=True,
        )
        print("Screenshot saved: screenshots/recurring_desktop.png")

        # -------------------------------------------------------
        # (m) Switch to mobile viewport (375x667)
        # -------------------------------------------------------
        page.set_viewport_size({"width": 375, "height": 667})
        page.wait_for_timeout(500)

        # (n) Take screenshot -> screenshots/recurring_mobile.png
        page.screenshot(
            path=os.path.join(SCREENSHOTS_DIR, "recurring_mobile.png"),
            full_page=True,
        )
        print("Screenshot saved: screenshots/recurring_mobile.png")

        # Cleanup
        context.close()
        browser.close()
        print("All screenshots captured successfully.")


if __name__ == "__main__":
    run()
