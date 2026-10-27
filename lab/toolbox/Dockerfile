# Рабочее место оператора: Ansible, ansible-lint, Terraform и клиент S3 (mc) в контейнере —
# на машине нужен только Docker. Репозиторий монтируется в /work (см. lab/compose.yaml).
ARG BASE_REGISTRY=mirror.gcr.io/library
ARG MC_IMAGE=mirror.gcr.io/pgsty/mc:RELEASE.2026-10-26T00-00-00Z

FROM ${MC_IMAGE} AS mc

FROM ${BASE_REGISTRY}/python:3.13-slim
ARG TERRAFORM_VERSION=1.16.5
# Зеркало релизов HashiCorp в Yandex Cloud: releases.hashicorp.com из РФ отвечает не всегда
ARG TERRAFORM_URL=https://hashicorp-releases.yandexcloud.net/terraform

RUN echo 'Acquire::Retries "10";' > /etc/apt/apt.conf.d/80-retries \
    && apt-get update \
    && apt-get install -y --no-install-recommends openssh-client curl unzip make git \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir --retries 10 "ansible-core==2.21.*" "ansible-lint==26.*"

RUN curl -fsSL --retry 5 "${TERRAFORM_URL}/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip" -o /tmp/terraform.zip \
    && unzip -q /tmp/terraform.zip terraform -d /usr/local/bin \
    && rm /tmp/terraform.zip \
    && terraform version

COPY --from=mc /usr/bin/mc /usr/local/bin/mc
# Провайдеры Terraform — через зеркало Yandex Cloud
COPY terraformrc /root/.terraformrc
COPY entrypoint.sh /usr/local/bin/toolbox-entrypoint
RUN chmod 0755 /usr/local/bin/toolbox-entrypoint

# ansible.cfg задаётся явно: в примонтированной Windows-папке (права 0777) Ansible
# не читает ansible.cfg из текущего каталога, считая его небезопасным
ENV ANSIBLE_CONFIG=/work/ansible/ansible.cfg \
    ANSIBLE_FORCE_COLOR=1 \
    MC_CONFIG_DIR=/tmp/.mc
WORKDIR /work/ansible
ENTRYPOINT ["toolbox-entrypoint"]
CMD ["bash"]
