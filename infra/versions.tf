terraform {
  required_version = ">= 1.10.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.70"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.6"
    }
  }

  # State lives in an S3 bucket so the pipeline and every teammate share one copy.
  # The bucket name is passed in at "terraform init" time by the deploy workflow.
  backend "s3" {}
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project   = "SplitCloud"
      ManagedBy = "Terraform"
    }
  }
}
