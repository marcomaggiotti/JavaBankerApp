#!/usr/bin/env python3
"""Send random transactions to the banker-platform REST API (stdlib only).

    python3 scripts/load_generator.py --url http://localhost:18080 --count 200 --accounts 5
"""
import argparse
import json
import random
import time
import urllib.error
import urllib.request


def post(url: str, payload: dict) -> int:
    req = urllib.request.Request(
        f"{url}/api/transactions",
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return resp.status
    except urllib.error.HTTPError as e:
        return e.code


def wait_until_ready(url: str, timeout_s: int = 180) -> None:
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(f"{url}/actuator/health/readiness", timeout=5) as resp:
                if resp.status == 200:
                    return
        except (urllib.error.URLError, OSError):
            pass
        print("waiting for the app to be ready...")
        time.sleep(3)
    raise SystemExit(f"app at {url} not ready after {timeout_s}s")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://localhost:18080", help="18080 = docker compose default")
    parser.add_argument("--count", type=int, default=100)
    parser.add_argument("--accounts", type=int, default=5)
    parser.add_argument("--seed", type=int, default=None)
    parser.add_argument("--wait", action="store_true", help="wait for the app readiness endpoint first")
    args = parser.parse_args()
    if args.wait:
        wait_until_ready(args.url)

    rnd = random.Random(args.seed)
    stats: dict[int, int] = {}
    for _ in range(args.count):
        payload = {
            "accountId": f"acc-{rnd.randint(1, args.accounts)}",
            # bias towards deposits so balances grow; bursts of withdrawals trigger fraud alerts
            "type": rnd.choices(["DEPOSIT", "WITHDRAWAL"], weights=[6, 4])[0],
            "amount": round(rnd.uniform(1, 500), 2),
            "currency": "EUR",
        }
        status = post(args.url, payload)
        stats[status] = stats.get(status, 0) + 1

    print("HTTP status counts:", stats)
    time.sleep(3)  # balances are eventually consistent: give the stream a moment to process
    for i in range(1, args.accounts + 1):
        try:
            with urllib.request.urlopen(f"{args.url}/api/accounts/acc-{i}/balance", timeout=5) as resp:
                print(json.loads(resp.read()))
        except urllib.error.HTTPError as e:
            print(f"acc-{i}: HTTP {e.code}")


if __name__ == "__main__":
    main()
