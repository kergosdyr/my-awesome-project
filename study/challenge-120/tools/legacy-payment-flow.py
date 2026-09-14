#!/usr/bin/env python3
"""실제 HTTP로 PG receipt를 받아 알림을 전송한다. 업무 정답을 포함하지 않는다."""
import json
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:18086"
def call(method, path, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data=data, method=method,
                                 headers={"Content-Type": "application/json"})
    try:
        response = urllib.request.urlopen(req)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        raw = response.read().decode()
        value = json.loads(raw) if raw else None
        print(method, path, response.status, value)
        return response.status, value

call("PUT", "/lab/pg/mode/PROCESSING")
call("POST", "/payments", {"reservationId": 1, "amount": 1000})
_, receipt = call("POST", "/lab/pg/1/complete")
call("GET", "/payments/1")
call("POST", "/payments/notifications", receipt)
call("POST", "/payments/notifications", receipt)
call("GET", "/payments/1")
