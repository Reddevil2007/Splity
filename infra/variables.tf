variable "region" {
  description = "AWS region. AWS Academy Learner Lab only allows us-east-1 and us-west-2."
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Name prefix for every resource."
  type        = string
  default     = "splitcloud"
}

variable "lab_mode" {
  description = "true for AWS Academy Learner Lab accounts, which cannot create IAM roles and must use the provided LabRole / LabInstanceProfile."
  type        = bool
  default     = true
}

variable "alert_email" {
  description = "Email address subscribed to alarm and reminder notifications. Leave empty to skip."
  type        = string
  default     = ""
}

variable "instance_type" {
  description = "EC2 instance type for the Tomcat application server."
  type        = string
  default     = "t3.micro"
}

variable "db_instance_class" {
  description = "RDS instance class for PostgreSQL."
  type        = string
  default     = "db.t3.micro"
}

variable "war_path" {
  description = "Path to the WAR built by Maven."
  type        = string
  default     = "../backend/target/splitcloud.war"
}

variable "tomcat_version" {
  description = "Apache Tomcat 10.1 release installed on the EC2 instance."
  type        = string
  default     = "10.1.31"
}

variable "ssh_key_name" {
  description = "Optional EC2 key pair name for SSH (in Learner Lab this is usually vockey). Browser-based EC2 Instance Connect works without it."
  type        = string
  default     = ""
}

variable "create_budget" {
  description = "Create an AWS Budgets cost alert. Learner Lab accounts usually cannot, so it is off by default."
  type        = bool
  default     = false
}

variable "budget_limit_usd" {
  description = "Monthly budget in USD used when create_budget is true."
  type        = number
  default     = 20
}
