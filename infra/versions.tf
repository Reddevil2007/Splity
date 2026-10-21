terraform {
  required_version = ">= 1.10.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.10"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # State lives in an Azure Storage blob container so the pipeline and every
  # teammate share one copy. Its location is passed in at "terraform init" time.
  backend "azurerm" {}
}

provider "azurerm" {
  features {}

  # Resource providers are registered once by the setup script and the pipeline,
  # so Terraform does not try (and wait) to register every provider itself.
  resource_provider_registrations = "none"
}
