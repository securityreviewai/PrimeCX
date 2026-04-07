output "eks_cluster_endpoint" {
  description = "EKS cluster API server endpoint"
  value       = aws_eks_cluster.main.endpoint
}

output "eks_cluster_name" {
  description = "EKS cluster name"
  value       = aws_eks_cluster.main.name
}

output "s3_bucket_name" {
  description = "Name of the S3 recordings bucket"
  value       = aws_s3_bucket.recordings.id
}

output "s3_bucket_arn" {
  description = "ARN of the S3 recordings bucket"
  value       = aws_s3_bucket.recordings.arn
}

output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.main.address
}

output "rds_port" {
  description = "RDS instance port"
  value       = aws_db_instance.main.port
}

output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = aws_subnet.private[*].id
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = aws_subnet.public[*].id
}

output "app_iam_role_arn" {
  description = "IAM role ARN for the backend service account (IRSA)"
  value       = aws_iam_role.app_backend.arn
}
