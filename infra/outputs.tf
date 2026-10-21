output "resource_group" {
  description = "Resource group holding every SplitCloud resource."
  value       = azurerm_resource_group.main.name
}

output "app_url" {
  description = "Open this in a browser: the React app on the storage static website."
  value       = trimsuffix(azurerm_storage_account.web.primary_web_endpoint, "/")
}

output "api_url" {
  description = "Base URL of the Spring Boot API on App Service."
  value       = "https://${azurerm_linux_web_app.api.default_hostname}"
}

output "web_app_name" {
  description = "App Service name, used by the pipeline to deploy the WAR."
  value       = azurerm_linux_web_app.api.name
}

output "storage_account_name" {
  description = "Storage account whose $web container holds the frontend."
  value       = azurerm_storage_account.web.name
}

output "database_host" {
  description = "Private hostname of the PostgreSQL server (resolves only inside the VNet)."
  value       = azurerm_postgresql_flexible_server.main.fqdn
}

output "application_insights_name" {
  description = "Application Insights resource with the availability test."
  value       = azurerm_application_insights.main.name
}
