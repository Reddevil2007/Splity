# Observability: Log Analytics + Application Insights, an availability test that
# probes the API from three continents, and alerts that email the team.

resource "azurerm_log_analytics_workspace" "main" {
  name                = "${var.project}-logs"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
  tags                = local.tags
}

resource "azurerm_application_insights" "main" {
  name                = "${var.project}-insights"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  workspace_id        = azurerm_log_analytics_workspace.main.id
  application_type    = "java"
  tags                = local.tags
}

# App Service platform logs (HTTP requests, console output) into Log Analytics.
resource "azurerm_monitor_diagnostic_setting" "api" {
  name                       = "send-to-log-analytics"
  target_resource_id         = azurerm_linux_web_app.api.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id

  enabled_log {
    category = "AppServiceHTTPLogs"
  }

  enabled_log {
    category = "AppServiceConsoleLogs"
  }

  enabled_log {
    category = "AppServiceAppLogs"
  }
}

# Synthetic monitoring: every 5 minutes, three Azure regions call /api/health
# and check for HTTP 200 with "status":"UP" in the body.
resource "azurerm_application_insights_standard_web_test" "health" {
  name                    = "${var.project}-api-health"
  location                = azurerm_resource_group.main.location
  resource_group_name     = azurerm_resource_group.main.name
  application_insights_id = azurerm_application_insights.main.id
  description             = "Calls /api/health, which also checks the database"
  frequency               = 300
  timeout                 = 30
  enabled                 = true
  retry_enabled           = true
  geo_locations           = ["apac-sg-sin-azr", "emea-nl-ams-azr", "us-va-ash-azr"]

  request {
    url = "https://${azurerm_linux_web_app.api.default_hostname}/api/health"
  }

  validation_rules {
    expected_status_code = 200

    content {
      content_match = "\"status\":\"UP\""
    }
  }

  lifecycle {
    # Azure adds a hidden-link tag to every web test.
    ignore_changes = [tags]
  }
}

# ---------- notifications ----------

resource "azurerm_monitor_action_group" "team" {
  name                = "${var.project}-team"
  resource_group_name = azurerm_resource_group.main.name
  short_name          = "splitcloud"
  tags                = local.tags

  dynamic "email_receiver" {
    for_each = var.alert_email == "" ? [] : [var.alert_email]
    content {
      name                    = "team-email"
      email_address           = email_receiver.value
      use_common_alert_schema = true
    }
  }
}

# ---------- alerts ----------

resource "azurerm_monitor_metric_alert" "api_down" {
  name                = "${var.project}-api-down"
  resource_group_name = azurerm_resource_group.main.name
  scopes              = [azurerm_application_insights_standard_web_test.health.id, azurerm_application_insights.main.id]
  description         = "The API failed its health check from 2 or more regions"
  severity            = 1
  frequency           = "PT1M"
  window_size         = "PT5M"
  tags                = local.tags

  application_insights_web_test_location_availability_criteria {
    web_test_id           = azurerm_application_insights_standard_web_test.health.id
    component_id          = azurerm_application_insights.main.id
    failed_location_count = 2
  }

  action {
    action_group_id = azurerm_monitor_action_group.team.id
  }
}

resource "azurerm_monitor_metric_alert" "api_5xx" {
  name                = "${var.project}-api-server-errors"
  resource_group_name = azurerm_resource_group.main.name
  scopes              = [azurerm_linux_web_app.api.id]
  description         = "More than 5 HTTP 5xx responses from the API in 5 minutes"
  severity            = 2
  frequency           = "PT1M"
  window_size         = "PT5M"
  tags                = local.tags

  criteria {
    metric_namespace = "Microsoft.Web/sites"
    metric_name      = "Http5xx"
    aggregation      = "Total"
    operator         = "GreaterThan"
    threshold        = 5
  }

  action {
    action_group_id = azurerm_monitor_action_group.team.id
  }
}

resource "azurerm_monitor_metric_alert" "plan_cpu" {
  name                = "${var.project}-app-cpu-high"
  resource_group_name = azurerm_resource_group.main.name
  scopes              = [azurerm_service_plan.main.id]
  description         = "App Service plan CPU above 80% for 15 minutes"
  severity            = 3
  frequency           = "PT5M"
  window_size         = "PT15M"
  tags                = local.tags

  criteria {
    metric_namespace = "Microsoft.Web/serverfarms"
    metric_name      = "CpuPercentage"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.team.id
  }
}

resource "azurerm_monitor_metric_alert" "db_cpu" {
  name                = "${var.project}-db-cpu-high"
  resource_group_name = azurerm_resource_group.main.name
  scopes              = [azurerm_postgresql_flexible_server.main.id]
  description         = "Database CPU above 80% for 15 minutes"
  severity            = 3
  frequency           = "PT5M"
  window_size         = "PT15M"
  tags                = local.tags

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "cpu_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.team.id
  }
}

resource "azurerm_monitor_metric_alert" "db_storage" {
  name                = "${var.project}-db-storage-high"
  resource_group_name = azurerm_resource_group.main.name
  scopes              = [azurerm_postgresql_flexible_server.main.id]
  description         = "Database storage more than 80% used"
  severity            = 2
  frequency           = "PT15M"
  window_size         = "PT1H"
  tags                = local.tags

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "storage_percent"
    aggregation      = "Maximum"
    operator         = "GreaterThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.team.id
  }
}
