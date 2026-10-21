# Everything for SplitCloud lives in one resource group, so it can be inspected
# (and deleted) as a unit.

resource "random_string" "suffix" {
  length  = 6
  special = false
  upper   = false
}

locals {
  suffix = random_string.suffix.result
  tags = {
    project   = "SplitCloud"
    managedBy = "Terraform"
  }
}

resource "azurerm_resource_group" "main" {
  name     = "${var.project}-rg"
  location = var.location
  tags     = local.tags
}
