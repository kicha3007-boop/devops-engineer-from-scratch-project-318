# Необязательно: если домен делегирован на ns1/ns2.yandexcloud.net, A-запись создаётся здесь.
# Иначе A-запись на адрес из output app_public_ip заводится у регистратора (README → «Домен и DNS»).

resource "yandex_dns_zone" "main" {
  count  = var.dns_zone == "" ? 0 : 1
  name   = "bulletins-318-zone"
  zone   = "${var.dns_zone}."
  public = true
}

resource "yandex_dns_recordset" "app" {
  count   = var.dns_zone == "" ? 0 : 1
  zone_id = yandex_dns_zone.main[0].id
  name    = "${var.app_subdomain}.${var.dns_zone}."
  type    = "A"
  ttl     = 300
  data    = [yandex_vpc_address.vm["app"].external_ipv4_address[0].address]
}
