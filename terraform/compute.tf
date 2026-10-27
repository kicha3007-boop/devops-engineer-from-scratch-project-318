# Две ВМ на Ubuntu 24.04 со статическими публичными адресами и фиксированными внутренними:
#   app        — приложение, nginx, Node Exporter, nginx-prometheus-exporter, Promtail;
#   monitoring — Prometheus, Grafana, Loki, Node Exporter.
# Внутренние адреса прописываются в Ansible Vault (private_ip хостов инвентаря).

locals {
  vms = {
    app = {
      cores          = 2
      memory         = 4
      disk           = 20
      private_ip     = cidrhost(var.subnet_cidr, 10)
      security_group = yandex_vpc_security_group.app.id
    }
    monitoring = {
      cores          = 2
      memory         = 4
      disk           = 30
      private_ip     = cidrhost(var.subnet_cidr, 20)
      security_group = yandex_vpc_security_group.monitoring.id
    }
  }
}

data "yandex_compute_image" "ubuntu" {
  family = "ubuntu-2404-lts"
}

resource "yandex_vpc_address" "vm" {
  for_each = local.vms
  name     = "bulletins-${each.key}"

  external_ipv4_address {
    zone_id = var.zone
  }
}

resource "yandex_compute_instance" "vm" {
  for_each    = local.vms
  name        = "bulletins-${each.key}"
  hostname    = "${each.key}-1"
  platform_id = "standard-v3"

  resources {
    cores         = each.value.cores
    memory        = each.value.memory
    core_fraction = 50
  }

  boot_disk {
    initialize_params {
      image_id = data.yandex_compute_image.ubuntu.id
      size     = each.value.disk
      type     = "network-ssd"
    }
  }

  network_interface {
    subnet_id          = yandex_vpc_subnet.main.id
    ip_address         = each.value.private_ip
    nat                = true
    nat_ip_address     = yandex_vpc_address.vm[each.key].external_ipv4_address[0].address
    security_group_ids = [each.value.security_group]
  }

  metadata = {
    user-data = <<-EOT
      #cloud-config
      users:
        - name: ${var.vm_user}
          groups: sudo
          shell: /bin/bash
          sudo: "ALL=(ALL) NOPASSWD:ALL"
          ssh_authorized_keys:
            - ${var.ssh_public_key}
    EOT
  }
}
