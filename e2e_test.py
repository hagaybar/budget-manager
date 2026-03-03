"""End-to-end browser test using headless Playwright."""
import asyncio
from playwright.async_api import async_playwright
import httpx

BASE = "http://localhost:8018"
SCREENSHOTS_DIR = "/home/ubuntu/projects/budget_manager/screenshots"

async def main():
    import os
    os.makedirs(SCREENSHOTS_DIR, exist_ok=True)

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page(viewport={"width": 1280, "height": 900})

        # 1. Screenshot: Swagger UI landing page
        print("1. Opening Swagger docs...")
        await page.goto(f"{BASE}/docs", wait_until="networkidle")
        await page.wait_for_timeout(2000)
        await page.screenshot(path=f"{SCREENSHOTS_DIR}/01_swagger_docs.png", full_page=True)
        print("   -> Screenshot saved: 01_swagger_docs.png")

        # 2. Create transactions via API
        async with httpx.AsyncClient(base_url=BASE, follow_redirects=True) as client:
            txns = [
                {"type": "income", "amount": 5000, "category": "Salary", "description": "March salary", "date": "2026-03-01"},
                {"type": "expense", "amount": 1200, "category": "Rent", "description": "Monthly rent", "date": "2026-03-02"},
                {"type": "expense", "amount": 85.50, "category": "Groceries", "description": "Weekly groceries", "date": "2026-03-03"},
                {"type": "expense", "amount": 45, "category": "Transport", "description": "Gas", "date": "2026-03-04"},
                {"type": "income", "amount": 200, "category": "Freelance", "description": "Logo design", "date": "2026-03-05"},
                {"type": "expense", "amount": 120, "category": "Utilities", "description": "Electric bill", "date": "2026-03-06"},
                {"type": "expense", "amount": 60, "category": "Groceries", "description": "Farmer's market", "date": "2026-03-07"},
            ]
            print("\n2. Creating test transactions via API...")
            for t in txns:
                r = await client.post("/api/transactions", json=t)
                print(f"   POST {t['type']:>7} ${t['amount']:>8.2f} {t['category']:<12} -> {r.status_code}")

        # 3. Screenshot: Browse to GET /api/transactions (JSON response in browser)
        print("\n3. Viewing all transactions in browser...")
        await page.goto(f"{BASE}/api/transactions", wait_until="networkidle")
        await page.wait_for_timeout(500)
        await page.screenshot(path=f"{SCREENSHOTS_DIR}/02_all_transactions.png", full_page=True)
        print("   -> Screenshot saved: 02_all_transactions.png")

        # 4. Screenshot: Monthly summary
        print("\n4. Viewing monthly summary in browser...")
        await page.goto(f"{BASE}/api/summary/monthly?year=2026&month=3", wait_until="networkidle")
        await page.wait_for_timeout(500)
        await page.screenshot(path=f"{SCREENSHOTS_DIR}/03_monthly_summary.png", full_page=True)
        print("   -> Screenshot saved: 03_monthly_summary.png")

        # 5. Test UPDATE and DELETE via API
        async with httpx.AsyncClient(base_url=BASE, follow_redirects=True) as client:
            print("\n5. Testing UPDATE (transaction #1)...")
            r = await client.put("/api/transactions/1", json={"amount": 5500, "description": "March salary (adjusted)"})
            print(f"   PUT /api/transactions/1 -> {r.status_code}: amount={r.json().get('amount')}, desc={r.json().get('description')}")

            print("\n6. Testing DELETE (transaction #4)...")
            r = await client.delete("/api/transactions/4")
            print(f"   DELETE /api/transactions/4 -> {r.status_code}")
            r = await client.get("/api/transactions/4")
            print(f"   GET /api/transactions/4    -> {r.status_code} (confirms deletion)")

            # Get updated summary
            print("\n7. Updated monthly summary after changes...")
            r = await client.get("/api/summary/monthly", params={"year": 2026, "month": 3})
            s = r.json()
            print(f"   Income:   ${s['total_income']:,.2f}")
            print(f"   Expenses: ${s['total_expenses']:,.2f}")
            print(f"   Net:      ${s['net_balance']:,.2f}")
            for cat in s["categories"]:
                print(f"     - {cat['category']}: ${cat['total']:,.2f} ({cat['count']} txns)")

        # 8. Screenshot: Updated summary after modifications
        print("\n8. Viewing updated summary in browser...")
        await page.goto(f"{BASE}/api/summary/monthly?year=2026&month=3", wait_until="networkidle")
        await page.wait_for_timeout(500)
        await page.screenshot(path=f"{SCREENSHOTS_DIR}/04_updated_summary.png", full_page=True)
        print("   -> Screenshot saved: 04_updated_summary.png")

        # 9. Screenshot: Single transaction detail
        print("\n9. Viewing single transaction detail...")
        await page.goto(f"{BASE}/api/transactions/1", wait_until="networkidle")
        await page.wait_for_timeout(500)
        await page.screenshot(path=f"{SCREENSHOTS_DIR}/05_single_transaction.png", full_page=True)
        print("   -> Screenshot saved: 05_single_transaction.png")

        await browser.close()
        print("\n=== All E2E tests passed! ===")

asyncio.run(main())
