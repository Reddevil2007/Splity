variable "location" {
  description = "Azure region. Azure for Students only allows a few regions; the setup script picks an allowed one."
  type        = string
  default     = "centralindia"
}

variable "project" {
  description = "Name prefix for every resource."
  type        = string
  default     = "splitcloud"
}

variable "alert_email" {
  description = "Email address that receives alert notifications. Leave empty to skip."
  type        = string
  default     = ""
}

variable "app_service_sku" {
  description = "App Service plan size. B1 is the smallest tier that supports VNet integration and Always On."
  type        = string
  default     = "B1"
}

variable "db_sku" {
  description = "PostgreSQL Flexible Server compute size."
  type        = string
  default     = "B_Standard_B1ms"
}
