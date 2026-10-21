# Security groups are stateful firewalls attached to each resource.

# AWS-managed list of the IP ranges CloudFront uses to reach origins.
data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

# AWS-managed list of the IP ranges used by browser-based EC2 Instance Connect.
data "aws_ec2_managed_prefix_list" "instance_connect" {
  name = "com.amazonaws.${var.region}.ec2-instance-connect"
}

resource "aws_security_group" "app" {
  name_prefix = "${var.project}-app-"
  description = "Tomcat on 8080, reachable only from CloudFront"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Tomcat from CloudFront edge locations only"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.cloudfront.id]
  }

  egress {
    description = "Outbound for package installs, S3 and RDS"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "${var.project}-app-sg"
  }
}

# Kept separate from the app group because the CloudFront prefix list already
# uses most of one security group's rule quota.
resource "aws_security_group" "admin" {
  name_prefix = "${var.project}-admin-"
  description = "SSH from the AWS console's EC2 Instance Connect only"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "SSH via EC2 Instance Connect"
    from_port       = 22
    to_port         = 22
    protocol        = "tcp"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.instance_connect.id]
  }

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "${var.project}-admin-sg"
  }
}

resource "aws_security_group" "db" {
  name_prefix = "${var.project}-db-"
  description = "PostgreSQL, reachable only from the application tier"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL from the app security group only"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "${var.project}-db-sg"
  }
}
