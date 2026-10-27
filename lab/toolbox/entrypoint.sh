#!/bin/sh
# Файлы из примонтированной Windows-папки видны с правами 0777: Ansible счёл бы
# .vault_pass исполняемым скриптом, а ssh отказался бы от ключа. Копируем их
# внутрь контейнера с правильными правами.
set -eu

if [ -f /work/.vault_pass ]; then
    install -m 0600 /work/.vault_pass /root/.vault_pass
    export ANSIBLE_VAULT_PASSWORD_FILE=/root/.vault_pass
fi

# SSH-ключи хоста (для настоящего сервера) — из ~/.ssh, смонтированного только на чтение
if [ -d /ssh-host ]; then
    mkdir -p /root/.ssh && chmod 0700 /root/.ssh
    for f in /ssh-host/id_* /ssh-host/known_hosts /ssh-host/config; do
        [ -f "$f" ] || continue
        install -m 0600 "$f" "/root/.ssh/$(basename "$f")"
    done
fi

exec "$@"
