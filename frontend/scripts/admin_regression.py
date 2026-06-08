"""Admin console interaction regression.

Prerequisites:
- Start the frontend dev server at http://127.0.0.1:8010.
- Install Python Playwright and browser binaries in the local test environment.

This script mocks backend API responses so admin UI regression does not depend
on the current database state.
"""

from playwright.sync_api import expect, sync_playwright


BASE_URL = "http://127.0.0.1:8010"
TARGET_USER_ID = "player-1"


def api_response(path, method):
    if path == "/api/items":
        return []
    if path == "/api/auth/me":
        return {"userId": "admin-1", "username": "admin", "role": "ADMIN", "accessToken": "mock-admin-token"}
    if path == "/api/admin/tax-configs":
        if method == "GET":
            return [
                {"tradeType": "MARKET", "rateBasisPoints": 300, "updatedReason": "默认交易站税率"},
                {"tradeType": "PRIVATE", "rateBasisPoints": 500, "updatedReason": "默认私下交易税率"},
                {"tradeType": "BULK", "rateBasisPoints": 300, "updatedReason": "默认大宗税率"},
            ]
    if path.startswith("/api/admin/tax-configs/") and method == "PUT":
        trade_type = path.rsplit("/", 1)[-1]
        default_rate = 500 if trade_type == "PRIVATE" else 300
        return {"tradeType": trade_type, "rateBasisPoints": default_rate, "updatedReason": "回归测试"}
    if path == f"/api/admin/users/{TARGET_USER_ID}/bulk-tokens":
        return {
            "tokenCode": "BULK-ADMIN-001",
            "remainingUses": 1,
            "status": "ACTIVE",
            "expiresAt": "2026-06-09T12:00:00",
        }
    if path == f"/api/admin/users/{TARGET_USER_ID}/assets":
        return {
            "username": "farmer_001",
            "status": "ACTIVE",
            "balance": 10000,
            "lockedBalance": 0,
            "inventory": [
                {
                    "itemId": "inv-wheat",
                    "itemCode": "WHEAT",
                    "itemName": "小麦",
                    "itemType": "HARVEST",
                    "availableQuantity": 30,
                    "lockedQuantity": 0,
                }
            ],
        }
    if path == f"/api/admin/users/{TARGET_USER_ID}/trades":
        return [
            {
                "tradeId": "trade-1",
                "tradeSource": "MARKET",
                "side": "BUY",
                "itemCode": "WHEAT",
                "quantity": 10,
                "tradeAmount": 520,
                "taxAmount": 16,
                "status": "COMPLETED",
            }
        ]
    return {}


def main():
    console_errors = []
    api_calls = []

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 960})
        page.on("console", lambda msg: console_errors.append(msg.text) if msg.type == "error" else None)
        page.add_init_script("localStorage.setItem('farm_admin_token', 'mock-admin-token')")

        def handle_route(route):
            request = route.request
            url = request.url
            if "/api/" not in url:
                route.continue_()
                return
            path = "/" + url.split("/api/", 1)[1].split("?", 1)[0]
            path = "/api/" + path.lstrip("/")
            api_calls.append(f"{request.method} {path}")
            route.fulfill(json=api_response(path, request.method), status=200)

        page.route("**/api/**", handle_route)
        page.goto(BASE_URL)
        page.wait_for_load_state("networkidle")

        page.get_by_role("button", name="管理台").click()
        expect(page.get_by_text("管理经济参数、发放大宗令牌，并核对玩家资产与交易记录。")).to_be_visible()

        page.get_by_placeholder("要查询或发令牌的玩家 UUID").fill(TARGET_USER_ID)
        page.get_by_role("button", name="刷新控制台").click()
        page.wait_for_load_state("networkidle")
        expect(page.get_by_text("farmer_001")).to_be_visible()
        expect(page.get_by_text("WHEAT")).to_be_visible()

        page.get_by_role("button", name="读取税率").click()
        page.wait_for_load_state("networkidle")
        expect(page.get_by_text("3.00%")).to_be_visible()

        page.get_by_role("button", name="保存 交易站").click()
        page.wait_for_load_state("networkidle")

        page.get_by_role("button", name="发放令牌").click()
        page.wait_for_load_state("networkidle")
        expect(page.get_by_text("BULK-ADMIN-001")).to_be_visible()

        page.get_by_role("button", name="读取资产").click()
        page.get_by_role("button", name="读取交易").click()
        page.wait_for_load_state("networkidle")
        expect(page.get_by_text("COMPLETED")).to_be_visible()

        browser.close()

    if console_errors:
        raise AssertionError("Console errors found: " + " | ".join(console_errors))

    required_calls = {
        "GET /api/auth/me",
        "GET /api/admin/tax-configs",
        f"GET /api/admin/users/{TARGET_USER_ID}/assets",
        f"GET /api/admin/users/{TARGET_USER_ID}/trades",
        "PUT /api/admin/tax-configs/MARKET",
        f"POST /api/admin/users/{TARGET_USER_ID}/bulk-tokens",
    }
    missing = sorted(required_calls - set(api_calls))
    if missing:
        raise AssertionError("Missing expected API calls: " + ", ".join(missing))

    print("ADMIN_REGRESSION_OK")
    print("\n".join(api_calls))


if __name__ == "__main__":
    main()
