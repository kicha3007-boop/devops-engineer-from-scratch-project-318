variable "folder_id" {
  description = "ID каталога Yandex Cloud"
  type        = string
}

variable "zone" {
  description = "Зона доступности"
  type        = string
  default     = "ru-central1-a"
}

variable "subnet_cidr" {
  description = "Внутренняя сеть ВМ и кластера БД"
  type        = string
  default     = "10.10.0.0/24"
}

variable "vm_user" {
  description = "Пользователь на ВМ, под которым заходит Ansible"
  type        = string
  default     = "ubuntu"
}

variable "ssh_public_key" {
  description = "Публичный SSH-ключ для пользователя ВМ (содержимое ~/.ssh/id_ed25519.pub)"
  type        = string
}

variable "admin_cidrs" {
  description = "Откуда разрешены SSH, Grafana (3000) и Prometheus (9090): адреса администраторов"
  type        = list(string)
}

variable "db_password" {
  description = "Пароль пользователя БД (тот же — vault_postgres_password в Ansible Vault); TF_VAR_db_password"
  type        = string
  sensitive   = true
}

variable "bucket_name" {
  description = "Имя бакета для картинок объявлений (уникально во всём Object Storage)"
  type        = string
  default     = "bulletins-318-kicha3007-boop"
}

variable "dns_zone" {
  description = "Домен, делегированный на NS Yandex Cloud (пусто — A-запись заводится у регистратора)"
  type        = string
  default     = ""
}

variable "app_subdomain" {
  description = "Имя приложения внутри dns_zone (bulletins -> bulletins.<dns_zone>)"
  type        = string
  default     = "bulletins"
}
