# Сеть и группы безопасности. Снаружи открыто только нужное:
#   app        — SSH (админам), HTTP (всем); метрики 9090/9100/9113 — только ВМ мониторинга;
#   monitoring — SSH, Grafana 3000, Prometheus 9090 (админам); Loki 3100 — только ВМ приложения;
#   db         — PostgreSQL 6432 — только ВМ приложения.

resource "yandex_vpc_network" "main" {
  name = "bulletins"
}

resource "yandex_vpc_subnet" "main" {
  name           = "bulletins-${var.zone}"
  zone           = var.zone
  network_id     = yandex_vpc_network.main.id
  v4_cidr_blocks = [var.subnet_cidr]
}

resource "yandex_vpc_security_group" "app" {
  name       = "bulletins-app"
  network_id = yandex_vpc_network.main.id

  ingress {
    description    = "SSH для администраторов"
    protocol       = "TCP"
    port           = 22
    v4_cidr_blocks = var.admin_cidrs
  }

  ingress {
    description    = "HTTP приложения"
    protocol       = "TCP"
    port           = 80
    v4_cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description    = "Исходящие: реестр образов, Object Storage, apt, Loki"
    protocol       = "ANY"
    v4_cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "yandex_vpc_security_group" "monitoring" {
  name       = "bulletins-monitoring"
  network_id = yandex_vpc_network.main.id

  dynamic "ingress" {
    for_each = { ssh = 22, grafana = 3000, prometheus = 9090 }
    content {
      description    = ingress.key
      protocol       = "TCP"
      port           = ingress.value
      v4_cidr_blocks = var.admin_cidrs
    }
  }

  egress {
    description    = "Исходящие: опрос экспортеров, SMTP, Docker Hub, apt"
    protocol       = "ANY"
    v4_cidr_blocks = ["0.0.0.0/0"]
  }
}

# Перекрёстные правила — отдельными ресурсами, иначе группы ссылались бы друг на друга по кругу
resource "yandex_vpc_security_group_rule" "app_metrics_from_monitoring" {
  for_each = { actuator = 9090, node_exporter = 9100, nginx_exporter = 9113 }

  security_group_binding = yandex_vpc_security_group.app.id
  direction              = "ingress"
  description            = "${each.key}: только с ВМ мониторинга"
  protocol               = "TCP"
  port                   = each.value
  security_group_id      = yandex_vpc_security_group.monitoring.id
}

resource "yandex_vpc_security_group_rule" "loki_from_app" {
  security_group_binding = yandex_vpc_security_group.monitoring.id
  direction              = "ingress"
  description            = "Loki push: только с ВМ приложения (Promtail)"
  protocol               = "TCP"
  port                   = 3100
  security_group_id      = yandex_vpc_security_group.app.id
}

resource "yandex_vpc_security_group" "db" {
  name       = "bulletins-db"
  network_id = yandex_vpc_network.main.id

  ingress {
    description       = "PostgreSQL (Odyssey): только с ВМ приложения"
    protocol          = "TCP"
    port              = 6432
    security_group_id = yandex_vpc_security_group.app.id
  }
}
