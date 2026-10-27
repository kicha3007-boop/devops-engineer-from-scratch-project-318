# В Git Bash на Windows не даём MSYS переписывать пути вида /work/... в аргументах docker
export MSYS_NO_PATHCONV := 1

# ============================================================================
# Инфраструктура: Ansible и Terraform
# ============================================================================
# Инструменты запускаются в контейнере toolbox (lab/toolbox): на машине нужен только Docker.
# Команды выполняются из каталога ansible/ (там ansible.cfg, inventories, playbooks, roles).
# Если Ansible и Terraform стоят локально: make deploy TOOLBOX='cd ansible &&'

ENV ?= production
INVENTORY = inventories/$(ENV)/hosts.ini
VAULT_FILE = inventories/$(ENV)/group_vars/all/vault.yml
COMPOSE = docker compose -f lab/compose.yaml
TOOLBOX ?= $(COMPOSE) run --rm toolbox
PLAYBOOK = $(TOOLBOX) ansible-playbook -i $(INVENTORY)

# make deploy-app TAGS=app TAG=<sha> — только приложение нужной сборки (откат — так же)
TAGS ?=
TAG ?=
PULL ?=
ANSIBLE_ARGS = $(if $(TAGS),--tags $(TAGS)) $(if $(TAG),-e app_image_tag=$(TAG)) $(if $(PULL),-e app_image_pull=$(PULL))

toolbox:
	$(COMPOSE) build --no-cache toolbox

# Сторонние роли и коллекции из requirements.yml (с повтором: Galaxy иногда обрывает загрузку)
install:
	for i in 1 2 3 4 5; do $(TOOLBOX) sh -c 'ansible-galaxy role install -r ../requirements.yml -p /root/.ansible/roles && ansible-galaxy collection install -r ../requirements.yml' && exit 0; sleep 10; done; exit 1

setup: toolbox install

# Обе ВМ целиком (идемпотентно: повторный запуск ничего не меняет)
deploy:
	$(PLAYBOOK) playbooks/site.yml $(ANSIBLE_ARGS)

# Только ВМ приложения / только ВМ мониторинга
deploy-app:
	$(PLAYBOOK) playbooks/app.yml $(ANSIBLE_ARGS)

deploy-monitoring:
	$(PLAYBOOK) playbooks/monitoring.yml $(ANSIBLE_ARGS)

# Дашборды, datasource'ы и правила алертинга Grafana из репозитория (одной командой)
dashboards:
	$(PLAYBOOK) playbooks/monitoring.yml --tags grafana

# Конфиг и правила Prometheus (таргеты из group_vars), с проверкой promtool
prometheus-config:
	$(PLAYBOOK) playbooks/monitoring.yml --tags prometheus

# Сухой прогон: что изменится на ВМ
deploy-check:
	$(PLAYBOOK) playbooks/site.yml --check --diff $(ANSIBLE_ARGS)

ping:
	$(TOOLBOX) ansible -i $(INVENTORY) all -m ansible.builtin.ping

# Линтеры: ansible-lint (профиль production), terraform fmt, синтаксис JSON дашбордов
lint:
	$(TOOLBOX) sh -c 'ansible-lint && terraform -chdir=../terraform fmt -check -recursive && for f in roles/grafana/files/dashboards/*.json; do python3 -m json.tool "$$f" > /dev/null || exit 1; done && echo "lint: ok"'

# Smoke-тесты на ВМ: ping, сервисы, эндпоинты, promtool, все таргеты up, Loki, Grafana
test: ping
	$(PLAYBOOK) playbooks/test.yml

# Быстрая проверка снаружи curl-запросами (адреса — переменные, для стенда заданы ниже)
APP_URL ?=
PROMETHEUS_URL ?=
GRAFANA_URL ?=
smoke:
	$(TOOLBOX) env APP_URL=$(APP_URL) PROMETHEUS_URL=$(PROMETHEUS_URL) GRAFANA_URL=$(GRAFANA_URL) sh ../scripts/smoke.sh

# Тестовая строка лога: nginx -> Promtail -> Loki -> запрос через Grafana
logs-e2e:
	$(PLAYBOOK) playbooks/logs-e2e.yml

# Задёргать алерт: SCENARIO=errors (поток 5xx) или SCENARIO=app-down (остановка приложения)
SCENARIO ?= errors
DURATION ?= 180
alert-test:
	$(PLAYBOOK) playbooks/alert-test.yml -e alert_test_scenario=$(SCENARIO) -e alert_test_duration=$(DURATION)

vault-encrypt:
	$(TOOLBOX) ansible-vault encrypt $(VAULT_FILE)

vault-edit:
	$(TOOLBOX) ansible-vault edit $(VAULT_FILE)

vault-view:
	$(TOOLBOX) ansible-vault view $(VAULT_FILE)

TF = $(TOOLBOX) terraform -chdir=../terraform

tf-init:
	$(TF) init

tf-validate: tf-init
	$(TF) validate

tf-plan:
	$(TF) plan

tf-apply:
	$(TF) apply

tf-output:
	$(TF) output

tf-destroy:
	$(TF) destroy

# ============================================================================
# Локальный стенд вместо облака: make lab-up && make lab-deploy && make lab-test
# ============================================================================

LAB_TOOLBOX = $(COMPOSE) run --rm toolbox
LAB_SSH = ssh -q -i /lab-ssh/id_ed25519 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null
LAB_URLS = APP_URL=http://10.38.0.10 PROMETHEUS_URL=http://10.38.0.20:9090 GRAFANA_URL=http://10.38.0.20:3000

# Собрать образы стенда, сгенерировать секреты, поднять две «ВМ», MinIO, Mailpit и рендерер
lab-up:
	$(COMPOSE) build --no-cache app toolbox
	$(LAB_TOOLBOX) python3 ../lab/bin/lab-secrets.py
	$(COMPOSE) up -d --no-build app monitoring minio mailpit renderer
	$(MAKE) install
	$(LAB_TOOLBOX) sh -c 'for vm in p318-app p318-monitoring; do ok=; for i in $$(seq 1 60); do $(LAB_SSH) -o ConnectTimeout=2 ubuntu@$$vm true && ok=1 && break; sleep 2; done; [ -n "$$ok" ] || { echo "«ВМ» $$vm не отвечает по SSH"; exit 1; }; done'
	$(MAKE) lab-s3

lab-secrets:
	$(LAB_TOOLBOX) python3 ../lab/bin/lab-secrets.py

# Бакет, политики и ключ приложения в MinIO (локальный аналог terraform/storage.tf)
lab-s3:
	$(LAB_TOOLBOX) ansible-playbook -i inventories/lab/hosts.ini playbooks/lab-s3.yml

lab-deploy:
	$(MAKE) deploy ENV=lab

lab-deploy-check:
	$(MAKE) deploy-check ENV=lab

lab-test:
	$(MAKE) test ENV=lab

lab-smoke:
	$(MAKE) smoke ENV=lab $(LAB_URLS)

lab-logs-e2e:
	$(MAKE) logs-e2e ENV=lab

lab-alert-test:
	$(MAKE) alert-test ENV=lab

# Письма алертов в Mailpit (то же видно в браузере: http://localhost:38025)
lab-mail:
	$(LAB_TOOLBOX) python3 ../lab/bin/mailpit.py

# Скриншоты дашбордов, таргетов Prometheus, алертов и писем в assets/
lab-screenshots:
	$(LAB_TOOLBOX) python3 ../lab/bin/screenshots.py
	sh lab/bin/browser-shots.sh

# Собрать образ из текущего кода и задеплоить его на стенд без публикации в реестр
lab-image:
	docker build --no-cache -t $(IMAGE):local .
	docker save $(IMAGE):local | docker exec -i p318-app docker load
	$(MAKE) deploy-app ENV=lab TAGS=app TAG=local PULL=false

lab-ssh-app:
	$(LAB_TOOLBOX) $(LAB_SSH) -t ubuntu@p318-app

lab-ssh-monitoring:
	$(LAB_TOOLBOX) $(LAB_SSH) -t ubuntu@p318-monitoring

# Перезагрузка обеих «ВМ»: всё должно подняться само (systemd + restart policy)
lab-reboot:
	docker restart p318-app p318-monitoring

lab-down:
	$(COMPOSE) --profile tools down -v --remove-orphans

# ============================================================================
# Приложение (Gradle, нужен JDK 27) и Docker-образ
# ============================================================================

IMAGE ?= ghcr.io/kicha3007-boop/devops-engineer-from-scratch-project-318
IMAGE_TAG ?= local

app-build:
	./gradlew build

app-run:
	./gradlew bootRun

app-test:
	./gradlew test

app-lint:
	./gradlew spotlessCheck

app-lint-fix:
	./gradlew spotlessApply

docker-build:
	docker build --no-cache -t $(IMAGE):$(IMAGE_TAG) .

# Тесты приложения внутри сборки (стадия test в Dockerfile)
docker-test:
	docker build --no-cache --target test -t $(IMAGE):test .

# Приложение на http://localhost:38081 с H2 в памяти (профиль dev)
docker-start:
	docker run --rm --name p318-app-dev -p 127.0.0.1:38081:8080 -e SPRING_PROFILES_ACTIVE=dev $(IMAGE):$(IMAGE_TAG)

.PHONY: toolbox install setup deploy deploy-app deploy-monitoring dashboards prometheus-config \
	deploy-check ping lint test smoke logs-e2e alert-test vault-encrypt vault-edit vault-view \
	tf-init tf-validate tf-plan tf-apply tf-output tf-destroy \
	lab-up lab-secrets lab-s3 lab-deploy lab-deploy-check lab-test lab-smoke lab-logs-e2e \
	lab-alert-test lab-mail lab-screenshots lab-image lab-ssh-app lab-ssh-monitoring lab-reboot lab-down \
	app-build app-run app-test app-lint app-lint-fix docker-build docker-test docker-start
