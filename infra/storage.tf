# S3 (Simple Storage Service) buckets.
#   frontend  -> the React build, served only through CloudFront
#   artifacts -> release WAR files that the EC2 instance pulls and deploys

resource "aws_s3_bucket" "frontend" {
  bucket_prefix = "${var.project}-web-"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Only this CloudFront distribution may read the frontend bucket (Origin Access Control).
resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "AllowCloudFrontRead"
      Effect    = "Allow"
      Principal = { Service = "cloudfront.amazonaws.com" }
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.frontend.arn}/*"
      Condition = {
        StringEquals = { "AWS:SourceArn" = aws_cloudfront_distribution.main.arn }
      }
    }]
  })

  depends_on = [aws_s3_bucket_public_access_block.frontend]
}

resource "aws_s3_bucket" "artifacts" {
  bucket_prefix = "${var.project}-artifacts-"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  versioning_configuration {
    status = "Enabled"
  }
}

locals {
  war_key = "releases/splitcloud.war"
}

# Uploading a new WAR (new etag) is the release. The instance polls for it.
resource "aws_s3_object" "war" {
  bucket = aws_s3_bucket.artifacts.id
  key    = local.war_key
  source = var.war_path
  etag   = filemd5(var.war_path)

  depends_on = [aws_s3_bucket_versioning.artifacts]
}
