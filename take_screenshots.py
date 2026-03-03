import asyncio
from playwright.async_api import async_playwright

async def main():
    import os
    os.makedirs("screenshots", exist_ok=True)

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)

        # Desktop viewport
        ctx = await browser.new_context(viewport={"width": 1280, "height": 900})
        page = await ctx.new_page()
        await page.goto("http://localhost:8018")
        await page.wait_for_timeout(1000)

        # Add transactions via the form
        for type_val, amount, category, desc, date in [
            ("income", "3500", "Salary", "March salary", "2026-03-01"),
            ("expense", "1200", "Rent", "Monthly rent", "2026-03-02"),
            ("expense", "85", "Groceries", "Weekly groceries", "2026-03-03"),
        ]:
            await page.select_option('[data-testid="type-select"]', type_val)
            await page.fill('[data-testid="amount-input"]', amount)
            await page.fill('[data-testid="category-input"]', category)
            await page.fill('[data-testid="description-input"]', desc)
            await page.fill('[data-testid="date-input"]', date)
            await page.click('[data-testid="submit-btn"]')
            await page.wait_for_timeout(800)

        await page.wait_for_timeout(1000)
        await page.screenshot(path="screenshots/ui_desktop.png", full_page=True)
        print("Saved: screenshots/ui_desktop.png")

        # Mobile viewport
        await page.set_viewport_size({"width": 375, "height": 667})
        await page.wait_for_timeout(500)
        await page.screenshot(path="screenshots/ui_mobile.png", full_page=True)
        print("Saved: screenshots/ui_mobile.png")

        # Summary section focus
        await page.set_viewport_size({"width": 1280, "height": 900})
        summary = page.locator('[data-testid="summary-section"]')
        await summary.scroll_into_view_if_needed()
        await page.wait_for_timeout(500)
        await page.screenshot(path="screenshots/ui_summary.png", full_page=True)
        print("Saved: screenshots/ui_summary.png")

        await browser.close()

asyncio.run(main())
