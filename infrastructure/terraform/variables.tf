variable "project_name" {
  description = "Name of the project, used for resource naming"
  type        = string
  default     = "primecx"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "production"
}

variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "us-west-2"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "eks_cluster_version" {
  description = "Kubernetes version for the EKS cluster"
  type        = string
  default     = "1.28"
}

variable "eks_node_instance_type" {
  description = "EC2 instance type for EKS worker nodes"
  type        = string
  default     = "t3.medium"
}

variable "eks_desired_nodes" {
  description = "Desired number of worker nodes"
  type        = number
  default     = 2
}

variable "eks_min_nodes" {
  description = "Minimum number of worker nodes"
  type        = number
  default     = 1
}

variable "eks_max_nodes" {
  description = "Maximum number of worker nodes"
  type        = number
  default     = 4
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_name" {
  description = "Name of the PostgreSQL database"
  type        = string
  default     = "primecx"
}

variable "db_username" {
  description = "Master username for the RDS instance"
  type        = string
  default     = "primecx_admin"
}

variable "s3_bucket_name" {
  description = "Name of the S3 bucket for session recordings"
  type        = string
  default     = "primecx-recordings"
}

variable "domain_name" {
  description = "Domain name for the application"
  type        = string
  default     = "primecx.example.com"
}

variable "recording_retention_days" {
  description = "Days before recordings expire and are deleted from S3"
  type        = number
  default     = 365
}

variable "tags" {
  description = "Default tags applied to all resources"
  type        = map(string)
  default = {
    Project     = "primecx"
    Environment = "production"
    ManagedBy   = "terraform"
  }
}
