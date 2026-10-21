# IAM (Identity and Access Management).
#
# AWS Academy Learner Lab accounts cannot create IAM roles. They provide a
# pre-made role called LabRole and an instance profile called LabInstanceProfile,
# so in lab_mode we reference those. In a normal AWS account (lab_mode = false)
# we create least-privilege roles instead.

data "aws_caller_identity" "current" {}

locals {
  instance_profile_name = var.lab_mode ? "LabInstanceProfile" : aws_iam_instance_profile.app[0].name
  lambda_role_arn       = var.lab_mode ? "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/LabRole" : aws_iam_role.lambda[0].arn
}

# ---------- EC2 role (normal accounts only) ----------

resource "aws_iam_role" "app" {
  count = var.lab_mode ? 0 : 1

  name_prefix = "${var.project}-ec2-"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "app_read_artifacts" {
  count = var.lab_mode ? 0 : 1

  name = "read-release-artifacts"
  role = aws_iam_role.app[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:GetObject"]
      Resource = "${aws_s3_bucket.artifacts.arn}/*"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "app_ssm" {
  count = var.lab_mode ? 0 : 1

  role       = aws_iam_role.app[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "app" {
  count = var.lab_mode ? 0 : 1

  name_prefix = "${var.project}-ec2-"
  role        = aws_iam_role.app[0].name
}

# ---------- Lambda role (normal accounts only) ----------

resource "aws_iam_role" "lambda" {
  count = var.lab_mode ? 0 : 1

  name_prefix = "${var.project}-lambda-"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_logs" {
  count = var.lab_mode ? 0 : 1

  role       = aws_iam_role.lambda[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "lambda_publish" {
  count = var.lab_mode ? 0 : 1

  name = "publish-alerts"
  role = aws_iam_role.lambda[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["sns:Publish"]
      Resource = aws_sns_topic.alerts.arn
    }]
  })
}
