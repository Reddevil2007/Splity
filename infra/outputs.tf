output "app_url" {
  description = "Open this in a browser: the SplitCloud app through CloudFront."
  value       = local.app_url
}

output "cloudfront_distribution_id" {
  description = "Used by the pipeline to invalidate cached index.html after a deploy."
  value       = aws_cloudfront_distribution.main.id
}

output "frontend_bucket" {
  description = "S3 bucket holding the React build."
  value       = aws_s3_bucket.frontend.id
}

output "artifacts_bucket" {
  description = "S3 bucket holding release WAR files."
  value       = aws_s3_bucket.artifacts.id
}

output "app_server_public_ip" {
  description = "Elastic IP of the EC2 application server."
  value       = aws_eip.app.public_ip
}

output "app_server_instance_id" {
  description = "EC2 instance ID, for the console or EC2 Instance Connect."
  value       = aws_instance.app.id
}

output "database_endpoint" {
  description = "RDS endpoint, only reachable from inside the VPC."
  value       = aws_db_instance.main.address
}

output "alerts_topic_arn" {
  description = "SNS topic for alarms and reminders."
  value       = aws_sns_topic.alerts.arn
}

output "dashboard_url" {
  description = "CloudWatch dashboard."
  value       = "https://${var.region}.console.aws.amazon.com/cloudwatch/home?region=${var.region}#dashboards:name=${aws_cloudwatch_dashboard.main.dashboard_name}"
}
