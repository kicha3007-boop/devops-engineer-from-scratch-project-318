terraform {
  required_version = ">= 1.6"

  required_providers {
    yandex = {
      source  = "yandex-cloud/yandex"
      version = "~> 0.239"
    }
  }
}

# Авторизация — из окружения: YC_TOKEN (или YC_SERVICE_ACCOUNT_KEY_FILE), YC_CLOUD_ID.
# Провайдер скачивается через зеркало Yandex Cloud, см. lab/toolbox/terraformrc.
provider "yandex" {
  folder_id = var.folder_id
  zone      = var.zone
}
