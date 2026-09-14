#!/usr/bin/env python3
"""실제 주문과 PG receipt를 사용해 Day6 승인 알림 계약을 관찰한다."""
import json
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:18086"


def call(method, path, body=None):
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(
        BASE + path, data=data, method=method,
        headers={"Content-Type": "application/json"},
    )
    try:
        response = urllib.request.urlopen(request, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        raw = response.read().decode()
        value = json.loads(raw) if raw else None
        print(method, path, response.status, value)
        return response.status, value


def main():
    code, order = call("POST", "/api/orders", {"optionId": 101, "quantity": 1})
    if code != 201:
        raise SystemExit("주문 생성 실패: 재고를 확인하거나 서버를 재시작하세요.")
    order_id = order["id"]
    call("PUT", "/dev/pg/mode/PROCESSING")
    try:
        code, _ = call("POST", f"/api/orders/{order_id}/payments")
        if code != 202:
            raise SystemExit("승인 대기 결제를 만들지 못했습니다.")
        code, receipt = call("POST", f"/dev/pg/{order_id}/complete")
        if code != 200:
            raise SystemExit("PG 승인 실패")
        call("GET", f"/api/orders/{order_id}")
        call("POST", "/api/payments/notifications", receipt)
        call("POST", "/api/payments/notifications", receipt)
        call("GET", f"/api/orders/{order_id}")
    finally:
        call("PUT", "/dev/pg/mode/NORMAL")


if __name__ == "__main__":
    main()
