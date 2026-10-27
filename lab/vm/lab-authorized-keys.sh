#!/bin/sh
# Кладёт публичный ключ стенда пользователю ubuntu (аналог cloud-init в облаке)
set -eu
install -d -m 0700 -o ubuntu -g ubuntu /home/ubuntu/.ssh
install -m 0600 -o ubuntu -g ubuntu /lab-ssh/id_ed25519.pub /home/ubuntu/.ssh/authorized_keys
