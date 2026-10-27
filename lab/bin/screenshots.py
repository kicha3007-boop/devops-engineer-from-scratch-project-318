"""Скриншоты для assets/ (make lab-screenshots). Запускается в toolbox внутри сети p318-lab.

Дашборды и список алертов Grafana — через API рендеринга Grafana (/render/...), который
вызывает сервис grafana-image-renderer (контейнер p318-renderer). Пароль Grafana берётся из
Ansible Vault стенда. Страницы без авторизации (Prometheus, Mailpit) снимает browser-shots.sh.
"""

import base64
import os
import subprocess
import sys
import urllib.parse
import urllib.request

import yaml

GRAFANA = "http://10.38.0.20:3000"
OUT = "/work/assets"
VAULT = "inventories/lab/group_vars/all/vault.yml"
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))

# (файл, uid дашборда, высота): ширина у всех 1600
DASHBOARDS = [
    ("grafana-app.png", "bulletins-app", 2200),
    ("grafana-node.png", "bulletins-node", 1500),
    ("grafana-nginx.png", "bulletins-nginx", 1200),
    ("grafana-logs.png", "bulletins-logs", 2600),
    ("grafana-status.png", "bulletins-status", 1300),
]


def vault():
    out = subprocess.run(["ansible-vault", "view", VAULT], check=True, capture_output=True, text=True).stdout
    return yaml.safe_load(out)


def fetch(url, headers, path):
    req = urllib.request.Request(url, headers=headers)
    with OPENER.open(req, timeout=180) as resp:
        body = resp.read()
    if not body.startswith(b"\x89PNG"):
        raise RuntimeError(f"{url}: не PNG ({body[:200]!r})")
    with open(path, "wb") as f:
        f.write(body)
    print(f"  {os.path.relpath(path, '/work')}  ({len(body) // 1024} КБ)")


def grafana(path, query, out, auth):
    url = f"{GRAFANA}/render/{path}?" + urllib.parse.urlencode(query)
    fetch(url, {"Authorization": "Basic " + auth}, os.path.join(OUT, out))


def main():
    os.makedirs(OUT, exist_ok=True)
    secrets = vault()
    auth = base64.b64encode(f"admin:{secrets['vault_grafana_admin_password']}".encode()).decode()
    window = sys.argv[1] if len(sys.argv) > 1 else "now-30m"

    print("Дашборды Grafana")
    for out, uid, height in DASHBOARDS:
        grafana(f"d/{uid}", {"orgId": 1, "from": window, "to": "now", "width": 1600, "height": height,
                             "kiosk": "true", "timeout": 90, "tz": "Europe/Moscow"}, out, auth)

    print("Алерты Grafana")
    grafana("alerting/list", {"orgId": 1, "width": 1600, "height": 1300, "timeout": 60,
                              "view": "list", "search": "state:firing"}, "grafana-alerts-firing.png", auth)
    grafana("alerting/list", {"orgId": 1, "width": 1600, "height": 1500, "timeout": 60},
            "grafana-alert-rules.png", auth)

    return 0


if __name__ == "__main__":
    sys.exit(main())
