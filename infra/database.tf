# Azure Database for PostgreSQL Flexible Server: managed PostgreSQL 16,
# injected into the private database subnet with public network access disabled.

resource "random_password" "db" {
  length  = 24
  special = false
}

resource "azurerm_postgresql_flexible_server" "main" {
  name                = "${var.project}-db-${local.suffix}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = local.tags

  version  = "16"
  sku_name = var.db_sku

  storage_mb   = 32768
  storage_tier = "P4"

  administrator_login    = "splitcloud"
  administrator_password = random_password.db.result

  delegated_subnet_id           = azurerm_subnet.db.id
  private_dns_zone_id           = azurerm_private_dns_zone.postgres.id
  public_network_access_enabled = false

  backup_retention_days        = 7
  geo_redundant_backup_enabled = false

  lifecycle {
    # Azure picks an availability zone on create; don't fight it afterwards.
    ignore_changes = [zone]
  }

  depends_on = [
    azurerm_private_dns_zone_virtual_network_link.postgres,
    azurerm_subnet_network_security_group_association.db,
  ]
}

resource "azurerm_postgresql_flexible_server_database" "app" {
  name      = "splitcloud"
  server_id = azurerm_postgresql_flexible_server.main.id
  charset   = "UTF8"
  collation = "en_US.utf8"
}
