# Объектное хранилище для картинок объявлений.
#   storage_admin — служебный аккаунт, которым Terraform создаёт бакет и его политику;
#   app           — аккаунт приложения: по политике бакета только чтение/запись объектов.

resource "yandex_iam_service_account" "storage_admin" {
  name        = "bulletins-318-storage-admin"
  description = "Terraform: управление бакетом картинок"
}

resource "yandex_resourcemanager_folder_iam_member" "storage_admin" {
  folder_id = var.folder_id
  role      = "storage.admin"
  member    = "serviceAccount:${yandex_iam_service_account.storage_admin.id}"
}

resource "yandex_iam_service_account_static_access_key" "storage_admin" {
  service_account_id = yandex_iam_service_account.storage_admin.id
  description        = "Ключ Terraform для управления бакетом"
}

resource "yandex_iam_service_account" "app" {
  name        = "bulletins-318-app"
  description = "Приложение «Доска объявлений»: доступ к своему бакету"
}

resource "yandex_iam_service_account_static_access_key" "app" {
  service_account_id = yandex_iam_service_account.app.id
  description        = "Ключ приложения (кладётся в Ansible Vault)"
}

resource "yandex_storage_bucket" "images" {
  bucket     = var.bucket_name
  access_key = yandex_iam_service_account_static_access_key.storage_admin.access_key
  secret_key = yandex_iam_service_account_static_access_key.storage_admin.secret_key

  # Приложению — чтение и запись объектов; всем — только чтение объектов
  # (картинки показываются в браузере через nginx /media/), листинг закрыт
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AppReadWrite"
        Effect    = "Allow"
        Principal = { CanonicalUser = yandex_iam_service_account.app.id }
        Action    = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
        Resource  = ["arn:aws:s3:::${var.bucket_name}/*"]
      },
      {
        Sid       = "AppList"
        Effect    = "Allow"
        Principal = { CanonicalUser = yandex_iam_service_account.app.id }
        Action    = ["s3:ListBucket"]
        Resource  = ["arn:aws:s3:::${var.bucket_name}"]
      },
      {
        Sid       = "PublicReadObjects"
        Effect    = "Allow"
        Principal = "*"
        Action    = ["s3:GetObject"]
        Resource  = ["arn:aws:s3:::${var.bucket_name}/*"]
      },
    ]
  })

  depends_on = [yandex_resourcemanager_folder_iam_member.storage_admin]
}
