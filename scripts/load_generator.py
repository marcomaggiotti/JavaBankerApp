#!/usr/bin/env python3
"""Send random transactions to the banker-platform REST API (stdlib only).

    python3 scripts/load_generator.py --url http://localhost:8080 --count 200 --accounts 5
"""
import argparse
import json
import random
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


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://localhost:8080")
    parser.add_argument("--count", type=int, default=100)
    parser.add_argument("--accounts", type=int, default=5)
    parser.add_argument("--seed", type=int, default=None)
    args = parser.parse_args()

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
    for i in range(1, args.accounts + 1):
        try:
            with urllib.request.urlopen(f"{args.url}/api/accounts/acc-{i}/balance", timeout=5) as resp:
                print(json.loads(resp.read()))
        except urllib.error.HTTPError as e:
            print(f"acc-{i}: HTTP {e.code}")


if __name__ == "__main__":
    main()
