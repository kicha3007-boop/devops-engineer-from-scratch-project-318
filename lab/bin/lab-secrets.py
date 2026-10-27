"""Секреты локального стенда (запускается в toolbox: make lab-secrets).

* .vault_pass — пароль Ansible Vault (если его ещё нет);
* ansible/inventories/lab/group_vars/all/vault.yml — случайные пароли и ключи, зашифрованные Vault;
* lab/minio.env — учётка администратора MinIO для контейнера p318-minio (из того же vault);
* lab/renderer.env — токен grafana-image-renderer (тот же, что у Grafana);
* /lab-ssh/id_ed25519 — SSH-ключ, которым Ansible ходит на «ВМ».

Существующие значения не перезаписываются: повторный запуск ничего не меняет,
недостающие ключи дописываются в vault.
"""

import os
import secrets
import subprocess
import sys

import yaml

ROOT = "/work"
VAULT_PASS = os.path.join(ROOT, ".vault_pass")
VAULT = os.path.join(ROOT, "ansible/inventories/lab/group_vars/all/vault.yml")
MINIO_ENV = os.path.join(ROOT, "lab/minio.env")
RENDERER_ENV = os.path.join(ROOT, "lab/renderer.env")
SSH_KEY = "/lab-ssh/id_ed25519"

DEFAULTS = {
    "vault_postgres_password": lambda: secrets.token_hex(16),
    "vault_s3_access_key": lambda: "bulletins-" + secrets.token_hex(6),
    "vault_s3_secret_key": lambda: secrets.token_hex(20),
    "vault_minio_root_user": lambda: "lab-admin",
    "vault_minio_root_password": lambda: secrets.token_hex(20),
    "vault_grafana_admin_password": lambda: secrets.token_urlsafe(18),
    "vault_smtp_password": lambda: secrets.token_urlsafe(18),
    "vault_grafana_renderer_token": lambda: secrets.token_hex(16),
}


def vault_cmd(*args):
    env = dict(os.environ, ANSIBLE_VAULT_PASSWORD_FILE="/root/.vault_pass")
    return subprocess.run(["ansible-vault", *args], check=True, env=env,
                          capture_output=True, text=True).stdout


def write_if_changed(path, content):
    current = open(path, encoding="utf-8").read() if os.path.exists(path) else None
    if current != content:
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(content)
        print("записан", os.path.relpath(path, ROOT))


def main():
    if not os.path.exists(VAULT_PASS) or os.path.getsize(VAULT_PASS) == 0:
        with open(VAULT_PASS, "w", encoding="utf-8") as f:
            f.write(secrets.token_urlsafe(32) + "\n")
        print("создан .vault_pass (пароль Ansible Vault, в git не попадает)")
    # Внутрь контейнера с правами 0600, иначе Ansible сочтёт файл исполняемым
    subprocess.run(["install", "-m", "0600", VAULT_PASS, "/root/.vault_pass"], check=True)

    data = yaml.safe_load(vault_cmd("view", VAULT)) if os.path.exists(VAULT) else {}
    missing = [k for k in DEFAULTS if k not in data]
    if missing:
        for key in missing:
            data[key] = DEFAULTS[key]()
        with open(VAULT, "w", encoding="utf-8", newline="\n") as f:
            f.write("---\n# Секреты локального стенда (сгенерированы make lab-secrets)\n")
            yaml.safe_dump(data, f, sort_keys=False)
        vault_cmd("encrypt", VAULT)
        print("зашифрован", os.path.relpath(VAULT, ROOT), "— добавлено:", ", ".join(missing))

    write_if_changed(MINIO_ENV, (
        "# Сгенерировано make lab-secrets из ansible/inventories/lab/group_vars/all/vault.yml\n"
        f"MINIO_ROOT_USER={data['vault_minio_root_user']}\n"
        f"MINIO_ROOT_PASSWORD={data['vault_minio_root_password']}\n"
    ))
    write_if_changed(RENDERER_ENV, (
        "# Сгенерировано make lab-secrets: токен, которым Grafana обращается к рендереру\n"
        f"AUTH_TOKEN={data['vault_grafana_renderer_token']}\n"
    ))

    if not os.path.exists(SSH_KEY):
        subprocess.run(["ssh-keygen", "-q", "-t", "ed25519", "-N", "", "-C", "p318-lab",
                        "-f", SSH_KEY], check=True)
        print("создан SSH-ключ стенда", SSH_KEY)
    return 0


if __name__ == "__main__":
    sys.exit(main())
