#!/bin/sh
# Быстрая проверка снаружи (make smoke / make lab-smoke): curl к приложению, Prometheus и Grafana.
#   APP_URL=http://<app>  PROMETHEUS_URL=http://<monitoring>:9090  GRAFANA_URL=http://<monitoring>:3000
# Нужны curl и python3 (есть в toolbox).
set -u

: "${APP_URL:?задайте APP_URL, например make smoke APP_URL=http://bulletins.example.ru}"
: "${PROMETHEUS_URL:?задайте PROMETHEUS_URL}"
: "${GRAFANA_URL:?задайте GRAFANA_URL}"

failed=0
ok()   { printf '  [ok]   %s\n' "$*"; }
fail() { printf '  [FAIL] %s\n' "$*"; failed=1; }
code() { curl -s -o /dev/null --max-time 10 -w '%{http_code}' "$1"; }

echo "Приложение: $APP_URL"
c=$(code "$APP_URL/");                              [ "$c" = 200 ] && ok "главная страница: $c" || fail "главная страница: $c"
c=$(code "$APP_URL/api/bulletins?page=1&perPage=1"); [ "$c" = 200 ] && ok "REST /api/bulletins: $c" || fail "REST /api/bulletins: $c"
c=$(code "$APP_URL/actuator/prometheus");           [ "$c" = 404 ] && ok "actuator на публичном порту закрыт: $c" || fail "actuator на публичном порту: $c"

host=$(echo "$APP_URL" | sed -E 's#^[a-z]+://([^/:]+).*#\1#')
if curl -s -o /dev/null --max-time 5 "http://$host:9090/actuator/prometheus"; then
    c=$(code "http://$host:9090/actuator/prometheus")
    [ "$c" = 403 ] && ok "management-порт 9090 отвечает 403 не-мониторингу" || fail "management-порт 9090 доступен отсюда: $c"
else
    ok "management-порт 9090 закрыт для этого адреса (firewall)"
fi

echo "Prometheus: $PROMETHEUS_URL"
c=$(code "$PROMETHEUS_URL/-/healthy"); [ "$c" = 200 ] && ok "/-/healthy: $c" || fail "/-/healthy: $c"
targets=$(curl -s --max-time 10 "$PROMETHEUS_URL/api/v1/targets?state=active")
summary=$(printf '%s' "$targets" | python3 -c '
import json, sys
t = json.load(sys.stdin)["data"]["activeTargets"]
down = [x["scrapeUrl"] for x in t if x["health"] != "up"]
for x in t:
    print("         %-4s %-14s %s" % (x["health"], x["labels"]["job"], x["scrapeUrl"]))
print("DOWN" if down or not t else "UP", len(t))
' 2>/dev/null)
printf '%s\n' "$summary" | sed '$d'
case "$(printf '%s\n' "$summary" | tail -1)" in
    UP*) ok "все таргеты up: $(printf '%s\n' "$summary" | tail -1 | cut -d' ' -f2) шт." ;;
    *)   fail "есть таргеты не в состоянии up" ;;
esac
alerts=$(curl -s --max-time 10 "$PROMETHEUS_URL/api/v1/rules?type=alert" | python3 -c '
import json, sys
g = json.load(sys.stdin)["data"]["groups"]
print(sum(len(x["rules"]) for x in g))' 2>/dev/null)
[ -n "$alerts" ] && [ "$alerts" -gt 0 ] && ok "правил алертинга Prometheus: $alerts" || fail "правила алертинга не загружены"

echo "Grafana: $GRAFANA_URL"
health=$(curl -s --max-time 10 "$GRAFANA_URL/api/health")
printf '%s' "$health" | python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("database") != "ok")' 2>/dev/null \
    && ok "/api/health: $(printf '%s' "$health" | tr -d '\n ')" || fail "/api/health: $health"

if [ "$failed" -ne 0 ]; then
    echo "Есть ошибки"
    exit 1
fi
echo "Smoke-тесты пройдены"
