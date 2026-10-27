# Значения для Ansible Vault (ansible/inventories/production/group_vars/all/vault.yml)

output "app_public_ip" {
  description = "vault_app_public_ip; A-запись домена приложения"
  value       = yandex_vpc_address.vm["app"].external_ipv4_address[0].address
}

output "app_private_ip" {
  description = "vault_app_private_ip — по нему Prometheus опрашивает экспортеры"
  value       = yandex_compute_instance.vm["app"].network_interface[0].ip_address
}

output "monitoring_public_ip" {
  description = "vault_monitoring_public_ip — Grafana :3000, Prometheus :9090"
  value       = yandex_vpc_address.vm["monitoring"].external_ipv4_address[0].address
}

output "monitoring_private_ip" {
  description = "vault_monitoring_private_ip — сюда Promtail отправляет логи (Loki :3100)"
  value       = yandex_compute_instance.vm["monitoring"].network_interface[0].ip_address
}

output "db_host" {
  description = "vault_db_host: FQDN мастера кластера PostgreSQL"
  value       = "c-${yandex_mdb_postgresql_cluster.main.id}.rw.mdb.yandexcloud.net"
}

output "bucket_name" {
  description = "Имя бакета: app_s3_bucket в group_vars окружения"
  value       = yandex_storage_bucket.images.bucket
}

output "app_access_key" {
  description = "Ключ доступа приложения к бакету: vault_s3_access_key"
  value       = yandex_iam_service_account_static_access_key.app.access_key
}

output "app_secret_key" {
  description = "Секретный ключ приложения: vault_s3_secret_key (terraform output -raw app_secret_key)"
  value       = yandex_iam_service_account_static_access_key.app.secret_key
  sensitive   = true
}

output "app_fqdn" {
  description = "Имя приложения, если DNS-зона управляется здесь"
  value       = var.dns_zone == "" ? null : trimsuffix(yandex_dns_recordset.app[0].name, ".")
}
