#!/bin/sh
# Скриншоты страниц без авторизации — таргеты и алерты Prometheus, письма в Mailpit — headless
# Chromium из контейнера p318-renderer (он в сети стенда). Запускается на хосте: make lab-screenshots.
set -eu

OUT=${OUT:-assets}
COMPOSE=${COMPOSE:-docker compose -f lab/compose.yaml}

shot() { # адрес файл высота
    docker exec p318-renderer sh -c "cd /tmp && rm -f shot.png && timeout 90 chromium --headless=new \
        --no-sandbox --disable-gpu --hide-scrollbars --window-size=1600,$3 --virtual-time-budget=10000 \
        --screenshot=/tmp/shot.png '$1' >/dev/null 2>&1; test -s /tmp/shot.png"
    docker cp p318-renderer:/tmp/shot.png "$OUT/$2" >/dev/null
    echo "  $OUT/$2"
}

echo "Prometheus и Mailpit"
shot http://10.38.0.20:9090/targets prometheus-targets.png 1000
shot http://10.38.0.20:9090/alerts prometheus-alerts.png 1400
shot http://mailpit.p318.test:8025/ mailpit-inbox.png 800
for kind in FIRING RESOLVED; do
    id=$($COMPOSE run --rm -T toolbox python3 ../lab/bin/mailpit.py --id "$kind" 2>/dev/null | tr -d '\r' | tail -1 || true)
    file=$(echo "mailpit-$kind-email.png" | tr 'A-Z' 'a-z')
    [ -n "$id" ] && shot "http://mailpit.p318.test:8025/view/$id" "$file" 1300 || echo "  писем $kind нет"
done
