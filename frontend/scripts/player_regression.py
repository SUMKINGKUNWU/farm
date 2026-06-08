"""Player workspace interaction regression.

Prerequisites:
- Start the frontend dev server at http://127.0.0.1:8010.
- Install Python Playwright and browser binaries in the local test environment.

This script mocks backend API responses so player UI regression does not depend
on the current database state.
"""

from playwright.sync_api import expect, sync_playwright


BASE_URL = "http://127.0.0.1:8010"


def api_response(path, method):
    if path == "/api/items":
        return [
            {"code": "WHEAT_SEED", "name": "小麦种子", "itemType": "SEED", "growSeconds": 600},
            {"code": "CHICKEN", "name": "鸡苗", "itemType": "ANIMAL", "growSeconds": 900},
            {"code": "WHEAT", "name": "小麦", "itemType": "HARVEST", "growSeconds": 0},
            {"code": "EGG", "name": "鸡蛋", "itemType": "HARVEST", "growSeconds": 0},
        ]
    if path == "/api/auth/me":
        return {"userId": "player-1", "username": "farmer_001", "nickname": "麦田玩家", "accessToken": "mock-token"}
    if path == "/api/me/summary":
        return {
            "balance": 10000,
            "lockedBalance": 0,
            "farmSlots": 2,
            "ranchSlots": 1,
            "nextFarmExpandCost": 1000,
            "nextRanchExpandCost": 1000,
            "tradePasswordSet": False,
        }
    if path == "/api/me/farm/plots":
        return {
            "maxSlots": 16,
            "slots": [
                {"id": "farm-slot-1", "slotIndex": 1, "status": "IDLE"},
                {"id": "farm-slot-2", "slotIndex": 2, "status": "IDLE"},
            ],
        }
    if path == "/api/me/ranch/slots":
        return {
            "maxSlots": 16,
            "slots": [
                {"id": "ranch-slot-1", "slotIndex": 1, "status": "IDLE"},
            ],
        }
    if path == "/api/me/inventory":
        return [
            {
                "itemId": "inv-wheat-seed",
                "itemCode": "WHEAT_SEED",
                "itemName": "小麦种子",
                "itemType": "SEED",
                "availableQuantity": 10,
                "lockedQuantity": 0,
            },
            {
                "itemId": "inv-wheat",
                "itemCode": "WHEAT",
                "itemName": "小麦",
                "itemType": "HARVEST",
                "availableQuantity": 20,
                "lockedQuantity": 0,
            },
        ]
    if path == "/api/me/growth":
        return []
    if path == "/api/me/bulk-tokens":
        return [{"tokenCode": "BULK-001", "remainingUses": 1, "status": "ACTIVE"}]
    if path == "/api/me/private-trades":
        return []
    if path == "/api/me/market/items/WHEAT/quote":
        return {
            "itemCode": "WHEAT",
            "currentPrice": 52,
            "basePrice": 50,
            "volume24h": 120,
            "tradeCount24h": 8,
        }
    if method in {"POST", "PUT", "PATCH", "DELETE"}:
        return {}
    return {}


def main():
    console_errors = []
    api_calls = []

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 960})
        page.on("console", lambda msg: console_errors.append(msg.text) if msg.type == "error" else None)
        page.add_init_script("localStorage.setItem('farm_player_token', 'mock-token')")

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

        expect(page.get_by_text("农田与牧场")).to_be_visible()
        expect(page.get_by_text("金币")).to_be_visible()

        page.get_by_role("button", name="仓商店").click()
        expect(page.get_by_text("农场商店")).to_be_visible()
        page.get_by_role("button", name="查看商品浮层").click()
        expect(page.get_by_text("物品信息")).to_be_visible()
        page.keyboard.press("Escape")
        expect(page.get_by_text("物品信息")).not_to_be_visible()

        page.get_by_role("button", name="市交易站").click()
        expect(page.get_by_text("交易站买卖")).to_be_visible()
        page.get_by_role("button", name="刷新行情").click()
        expect(page.get_by_text("52")).to_be_visible()
        page.get_by_role("button", name="交易确认浮层").click()
        expect(page.get_by_text("交易确认")).to_be_visible()
        page.keyboard.press("Escape")
        expect(page.get_by_text("交易确认")).not_to_be_visible()

        page.get_by_role("button", name="约私下").click()
        expect(page.get_by_text("私下交易")).to_be_visible()
        page.get_by_role("button", name="+").click()
        expect(page.get_by_text("设置交易密码")).to_be_visible()
        page.locator(".float-password input").fill("123456")
        page.locator(".float-password button").click()
        page.wait_for_load_state("networkidle")

        browser.close()

    if console_errors:
        raise AssertionError("Console errors found: " + " | ".join(console_errors))

    required_calls = {
        "GET /api/items",
        "GET /api/auth/me",
        "GET /api/me/summary",
        "GET /api/me/market/items/WHEAT/quote",
        "POST /api/me/trade-password",
    }
    missing = sorted(required_calls - set(api_calls))
    if missing:
        raise AssertionError("Missing expected API calls: " + ", ".join(missing))

    print("PLAYER_REGRESSION_OK")
    print("\n".join(api_calls))


if __name__ == "__main__":
    main()
