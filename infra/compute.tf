# EC2 (Elastic Compute Cloud) application server running Tomcat 10 with the SplitCloud WAR.

data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
}

resource "aws_instance" "app" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.app.id, aws_security_group.admin.id]
  iam_instance_profile   = local.instance_profile_name
  key_name               = var.ssh_key_name == "" ? null : var.ssh_key_name

  # First-boot script: installs Java + Tomcat and starts the pull-based deploy loop.
  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    region          = var.region
    tomcat_version  = var.tomcat_version
    artifact_bucket = aws_s3_bucket.artifacts.id
    war_key         = local.war_key
    db_host         = aws_db_instance.main.address
    db_port         = aws_db_instance.main.port
    db_name         = aws_db_instance.main.db_name
    db_user         = aws_db_instance.main.username
    db_password     = random_password.db.result
  })
  user_data_replace_on_change = true

  metadata_options {
    http_tokens = "required"
  }

  root_block_device {
    volume_size = 10
    volume_type = "gp3"
  }

  tags = {
    Name = "${var.project}-app"
  }

  lifecycle {
    ignore_changes = [ami]
  }

  depends_on = [aws_s3_object.war]
}

# A fixed public address. Learner Lab stops instances between sessions, and
# without an Elastic IP the public DNS name (and so CloudFront's origin) would change.
resource "aws_eip" "app" {
  domain   = "vpc"
  instance = aws_instance.app.id

  tags = {
    Name = "${var.project}-app-ip"
  }
}
