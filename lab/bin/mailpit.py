"""Письма алертов в Mailpit (make lab-mail). Запускается в toolbox внутри сети p318-lab.

Печатает список писем; код возврата 1, если ни одного письма о сработавшем алерте (FIRING) нет.
То же самое видно в браузере: http://localhost:38025
"""

import json
import sys
import urllib.request

BASE = "http://mailpit.p318.test:8025"
# Стенд — внутренняя сеть: прокси хоста сюда не нужен
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def get(path):
    with OPENER.open(BASE + path, timeout=10) as resp:
        return json.load(resp)


def main():
    data = get("/api/v1/messages?limit=50")
    messages = data.get("messages") or []
    # --id FIRING — только ID последнего письма с этим словом в теме (для скриншота)
    if len(sys.argv) == 3 and sys.argv[1] == "--id":
        found = [m for m in messages if sys.argv[2] in m.get("Subject", "")]
        if found:
            print(found[0]["ID"])
        return 0 if found else 1
    print(f"Писем в Mailpit: {data.get('total', 0)}")
    for m in messages:
        to = ", ".join(t.get("Address", "") for t in m.get("To") or [])
        print(f"  {m['Created'][:19]}  {to:<18} {m['Subject']}")
    firing = [m for m in messages if "FIRING" in m.get("Subject", "")]
    if not firing:
        print("Писем о сработавших алертах (FIRING) нет")
        return 1
    latest = firing[0]
    text = get(f"/api/v1/message/{latest['ID']}").get("Text", "")
    print("\nПоследнее письмо FIRING:", latest["Subject"])
    print("\n".join(line for line in text.splitlines()[:25] if line.strip()))
    return 0


if __name__ == "__main__":
    sys.exit(main())
