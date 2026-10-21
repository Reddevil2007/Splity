# Managed PostgreSQL on RDS (Relational Database Service), in the private subnets.

resource "random_password" "db" {
  length  = 24
  special = false
}

resource "aws_db_subnet_group" "main" {
  name_prefix = "${var.project}-db-"
  subnet_ids  = aws_subnet.private[*].id

  tags = {
    Name = "${var.project}-db-subnets"
  }
}

resource "aws_db_instance" "main" {
  identifier_prefix = "${var.project}-db-"

  engine         = "postgres"
  engine_version = "16"
  instance_class = var.db_instance_class

  allocated_storage = 20
  storage_type      = "gp2"

  db_name  = "splitcloud"
  username = "splitcloud"
  password = random_password.db.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.db.id]
  publicly_accessible    = false
  multi_az               = false

  # Demo settings: no backups to keep, quick teardown. A production database
  # would keep automated backups, a final snapshot and deletion protection.
  backup_retention_period = 0
  skip_final_snapshot     = true
  deletion_protection     = false
  apply_immediately       = true

  tags = {
    Name = "${var.project}-postgres"
  }
}
