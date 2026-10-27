# Managed Service for PostgreSQL: кластер из одного хоста во внутренней сети.
# Подключение только с ВМ приложения (группа безопасности db) и только по TLS (порт 6432).

resource "yandex_mdb_postgresql_cluster" "main" {
  name               = "bulletins"
  environment        = "PRODUCTION"
  network_id         = yandex_vpc_network.main.id
  security_group_ids = [yandex_vpc_security_group.db.id]

  config {
    version = "17"
    resources {
      resource_preset_id = "s2.micro"
      disk_type_id       = "network-ssd"
      disk_size          = 10
    }
  }

  host {
    zone      = var.zone
    subnet_id = yandex_vpc_subnet.main.id
  }
}

resource "yandex_mdb_postgresql_user" "bulletins" {
  cluster_id = yandex_mdb_postgresql_cluster.main.id
  name       = "bulletins"
  password   = var.db_password
}

resource "yandex_mdb_postgresql_database" "bulletins" {
  cluster_id = yandex_mdb_postgresql_cluster.main.id
  name       = "bulletins"
  owner      = yandex_mdb_postgresql_user.bulletins.name
}
