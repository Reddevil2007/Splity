# Two Lambda functions on EventBridge schedules:
#   dues-reminder -> daily at 09:00 IST, emails a digest of pending dues via SNS
#   healthcheck   -> every minute, calls /api/health; failures trip a CloudWatch alarm

locals {
  app_url = "https://${aws_cloudfront_distribution.main.domain_name}"
}

# ---------- dues reminder ----------

data "archive_file" "reminder" {
  type        = "zip"
  source_file = "${path.module}/lambda/reminder.py"
  output_path = "${path.module}/build/reminder.zip"
}

resource "aws_cloudwatch_log_group" "reminder" {
  name              = "/aws/lambda/${var.project}-dues-reminder"
  retention_in_days = 7
}

resource "aws_lambda_function" "reminder" {
  function_name    = "${var.project}-dues-reminder"
  description      = "Emails a digest of everyone who still owes money"
  role             = local.lambda_role_arn
  runtime          = "python3.12"
  handler          = "reminder.handler"
  filename         = data.archive_file.reminder.output_path
  source_code_hash = data.archive_file.reminder.output_base64sha256
  timeout          = 30
  memory_size      = 128

  environment {
    variables = {
      APP_URL   = local.app_url
      TOPIC_ARN = aws_sns_topic.alerts.arn
    }
  }

  depends_on = [aws_cloudwatch_log_group.reminder]
}

resource "aws_cloudwatch_event_rule" "reminder" {
  name                = "${var.project}-daily-reminder"
  description         = "Every day at 03:30 UTC (09:00 IST)"
  schedule_expression = "cron(30 3 * * ? *)"
}

resource "aws_cloudwatch_event_target" "reminder" {
  rule = aws_cloudwatch_event_rule.reminder.name
  arn  = aws_lambda_function.reminder.arn
}

resource "aws_lambda_permission" "reminder" {
  statement_id  = "AllowEventBridgeInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.reminder.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.reminder.arn
}

# ---------- health check ----------

data "archive_file" "healthcheck" {
  type        = "zip"
  source_file = "${path.module}/lambda/healthcheck.py"
  output_path = "${path.module}/build/healthcheck.zip"
}

resource "aws_cloudwatch_log_group" "healthcheck" {
  name              = "/aws/lambda/${var.project}-healthcheck"
  retention_in_days = 7
}

resource "aws_lambda_function" "healthcheck" {
  function_name    = "${var.project}-healthcheck"
  description      = "Calls /api/health through CloudFront every minute"
  role             = local.lambda_role_arn
  runtime          = "python3.12"
  handler          = "healthcheck.handler"
  filename         = data.archive_file.healthcheck.output_path
  source_code_hash = data.archive_file.healthcheck.output_base64sha256
  timeout          = 15
  memory_size      = 128

  environment {
    variables = {
      HEALTH_URL = "${local.app_url}/api/health"
    }
  }

  depends_on = [aws_cloudwatch_log_group.healthcheck]
}

# Scheduled invocations are asynchronous and retried twice by default;
# a health probe should report each failure once, not three times.
resource "aws_lambda_function_event_invoke_config" "healthcheck" {
  function_name          = aws_lambda_function.healthcheck.function_name
  maximum_retry_attempts = 0
}

resource "aws_cloudwatch_event_rule" "healthcheck" {
  name                = "${var.project}-healthcheck"
  description         = "Probe the API every minute"
  schedule_expression = "rate(1 minute)"
}

resource "aws_cloudwatch_event_target" "healthcheck" {
  rule = aws_cloudwatch_event_rule.healthcheck.name
  arn  = aws_lambda_function.healthcheck.arn
}

resource "aws_lambda_permission" "healthcheck" {
  statement_id  = "AllowEventBridgeInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.healthcheck.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.healthcheck.arn
}
