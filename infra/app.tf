# Azure App Service: a managed Linux host running Tomcat 10.1 on Java 17.
# The pipeline deploys the Spring Boot WAR into it as the ROOT application.

resource "azurerm_service_plan" "main" {
  name                = "${var.project}-plan"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  os_type             = "Linux"
  sku_name            = var.app_service_sku
  tags                = local.tags
}

resource "azurerm_linux_web_app" "api" {
  name                = "${var.project}-api-${local.suffix}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  service_plan_id     = azurerm_service_plan.main.id
  https_only          = true
  tags                = local.tags

  # Outbound traffic (including the database connection) goes through the VNet.
  virtual_network_subnet_id = azurerm_subnet.app.id

  site_config {
    always_on              = true
    ftps_state             = "Disabled"
    minimum_tls_version    = "1.2"
    vnet_route_all_enabled = true

    # App Service pings this path and restarts the app if it keeps failing.
    health_check_path                 = "/api/health"
    health_check_eviction_time_in_min = 5

    application_stack {
      java_server         = "TOMCAT"
      java_server_version = "10.1"
      java_version        = "17"
    }

    # The React frontend is served from a different origin (the storage static
    # website), so the API allows that one origin through CORS
    # (Cross-Origin Resource Sharing), handled by the platform.
    cors {
      allowed_origins = [trimsuffix(azurerm_storage_account.web.primary_web_endpoint, "/")]
    }
  }

  app_settings = {
    DB_URL                                     = "jdbc:postgresql://${azurerm_postgresql_flexible_server.main.fqdn}:5432/${azurerm_postgresql_flexible_server_database.app.name}?sslmode=require"
    DB_USER                                    = azurerm_postgresql_flexible_server.main.administrator_login
    DB_PASSWORD                                = random_password.db.result
    APPLICATIONINSIGHTS_CONNECTION_STRING      = azurerm_application_insights.main.connection_string
    ApplicationInsightsAgent_EXTENSION_VERSION = "~3"
  }

  logs {
    detailed_error_messages = true
    failed_request_tracing  = true

    application_logs {
      file_system_level = "Information"
    }

    http_logs {
      file_system {
        retention_in_days = 3
        retention_in_mb   = 35
      }
    }
  }

  depends_on = [azurerm_postgresql_flexible_server_database.app]
}
