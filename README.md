# PrimeCX — Customer Experience Platform

PrimeCX is a customer experience platform where support executives assist users via live sessions. Desktop recordings of support sessions are captured and stored in AWS S3 for quality assurance and training. The platform provides role-based dashboards, ticket management, and session recording workflows, all secured with Okta OIDC single sign-on.

## Architecture

| Layer | Technology | Details |
|-------|-----------|---------|
| Backend | Java 17, Spring Boot 3.2 | REST API, OAuth2/OIDC, JPA, S3 integration |
| Frontend | React 18 | SPA with Okta auth, served via nginx in production |
| Database | PostgreSQL 15 | Users, tickets, sessions, recording metadata |
| Storage | AWS S3 | Session recording files |
| Auth | Okta OIDC | SSO with role-based access control |
| Infrastructure | AWS EKS, Terraform | Kubernetes on AWS with ALB Ingress |

## Project Structure

```
PrimeCX/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/primecx/
│       ├── PrimeCxApplication.java
│       ├── config/          # Security, S3, CORS configuration
│       ├── controller/      # REST controllers
│       ├── dto/             # Request/response objects
│       ├── exception/       # Global error handling
│       ├── model/           # JPA entities and enums
│       ├── repository/      # Spring Data repositories
│       └── service/         # Business logic
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── public/
│   └── src/
│       ├── config.js
│       ├── index.js
│       └── services/api.js
├── infrastructure/
│   ├── kubernetes/
│   │   ├── namespace.yaml
│   │   ├── configmap.yaml
│   │   ├── secrets.yaml
│   │   ├── service-account.yaml
│   │   ├── backend-deployment.yaml
│   │   ├── backend-service.yaml
│   │   ├── frontend-deployment.yaml
│   │   ├── frontend-service.yaml
│   │   ├── ingress.yaml
│   │   └── hpa.yaml
│   └── terraform/
│       ├── main.tf
│       ├── variables.tf
│       ├── outputs.tf
│       └── vpc.tf
├── Dockerfile              # Backend multi-stage build
├── docker-compose.yml
└── README.md
```

## Prerequisites

- Java 17 (Eclipse Temurin recommended)
- Node.js 18
- Docker and Docker Compose
- Terraform >= 1.5
- kubectl
- AWS CLI v2 (configured with appropriate credentials)
- Okta developer account

## Local Development

Start the full stack locally with Docker Compose:

```bash
docker-compose up --build
```

This starts:

| Service | URL | Purpose |
|---------|-----|---------|
| Backend | http://localhost:8080 | Spring Boot API |
| Frontend | http://localhost:3000 | React app (nginx) |
| PostgreSQL | localhost:5432 | Database |
| LocalStack | localhost:4566 | S3 emulation |

To run the backend outside Docker during development:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

To run the frontend dev server:

```bash
cd frontend
npm install
npm start
```

The React dev server proxies `/api` requests to `http://localhost:8080` (configured in `package.json`).

## Configuration

### Okta

1. Create an Okta developer account at https://developer.okta.com
2. Register a new Web application with:
   - Sign-in redirect URI: `http://localhost:8080/login/oauth2/code/okta`
   - Sign-out redirect URI: `http://localhost:8080`
3. Note the **Client ID**, **Client Secret**, and **Issuer URI**
4. Set these values in your environment or update `infrastructure/kubernetes/secrets.yaml` (base64-encoded) for production

### AWS Credentials

For local development, LocalStack handles S3. For production:

1. The backend uses IRSA (IAM Roles for Service Accounts) on EKS
2. Update the role ARN in `infrastructure/kubernetes/service-account.yaml`
3. The IAM role needs `s3:PutObject`, `s3:GetObject`, and `s3:ListBucket` on the recordings bucket

### Database

- Local: Docker Compose provisions PostgreSQL automatically
- Production: Terraform creates an RDS PostgreSQL instance; the connection URL is injected via the ConfigMap

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/auth/me` | Current authenticated user |
| POST | `/api/auth/logout` | Invalidate session |
| GET | `/api/users` | List all users (admin) |
| GET | `/api/users/assignable-executives` | Active support executives for ticket assignment (admin / manager) |
| PUT | `/api/users/{id}/role` | Update a user's role |
| GET | `/api/tickets` | List support tickets |
| GET | `/api/tickets/tags` | Distinct tags on tickets visible to the caller (role-scoped) |
| GET | `/api/tickets/my/summary` | Compact open / in-progress / resolved / closed counts for visible tickets |
| GET | `/api/tickets/recent` | Paginated recently updated tickets visible to the caller (`updatedAt` desc) |
| GET | `/api/tickets/sla/at-risk` | OPEN/IN_PROGRESS tickets with `slaRespondBy` in the next `withinHours` (1–168, default 24); visibility-scoped |
| GET | `/api/tickets/sla/breached` | OPEN/IN_PROGRESS tickets past first-response SLA; visibility-scoped |
| POST | `/api/tickets` | Create a new ticket |
| POST | `/api/tickets/{id}/reopen` | Reopen a resolved or closed ticket (same visibility as ticket); refreshes first-response SLA from now |
| PUT | `/api/tickets/{id}` | Update a ticket |
| POST | `/api/tickets/{id}/release` | Release assignee; ticket returns to OPEN / unassigned queue (executive self-release or admin / manager) |
| GET | `/api/tickets/{id}/sessions` | Support sessions for a ticket (same visibility as the ticket) |
| GET | `/api/tickets/{id}/recordings` | Session recordings for a ticket, newest first (same visibility as the ticket); use GET `/api/recordings/{id}` for download URLs |
| GET | `/api/tickets/{id}/timeline` | Merged chronological activity + messages (internal notes only for staff; same ticket visibility rules) |
| GET | `/api/tickets/{id}/transcript` | Plain-text export of ticket header + timeline (`text/plain`; same visibility as timeline) |
| GET | `/api/saved-replies` | List saved replies / macros (support roles) |
| GET | `/api/saved-replies/search` | Search saved replies by title or body (`q`, optional `limit` max 50; support roles) |
| GET | `/api/sessions` | List support sessions |
| POST | `/api/sessions` | Start a new support session |
| POST | `/api/sessions/{id}/end` | End a session with notes |
| POST | `/api/recordings/upload-url` | Get a presigned S3 upload URL |
| POST | `/api/recordings/confirm` | Confirm a completed upload |
| GET | `/api/recordings/{id}` | Get recording metadata |
| GET | `/api/recordings/session/{id}` | List recordings for a session |
| GET | `/api/admin/dashboard` | Dashboard statistics |
| GET | `/api/admin/activity/recent` | Paginated recent ticket activity across all tickets (admin / manager) |
| GET | `/api/admin/reports/executive-workload` | Per–support-executive counts: OPEN+IN_PROGRESS assigned tickets and ACTIVE sessions |
| GET | `/api/admin/reports/satisfaction` | Global CSAT summary: averages, per-star counts, tickets with written feedback |
| GET | `/api/admin/reports/ticket-volume` | Daily ticket creation counts for charting (`days` 1–366, default 30); zero-filled per calendar day |
| GET | `/api/admin/reports/tickets-by-category` | Tickets created in the window, counted per category enum (`days` default 30); every category listed (zero if none) |
| GET | `/api/admin/reports/resolution-time` | RESOLVED/CLOSED tickets updated in the window: count + avg hours from creation to last update (`days` default 90) |
| GET | `/actuator/health` | Health check (public) |

## Deployment

### 1. Provision AWS Infrastructure

```bash
cd infrastructure/terraform
terraform init
terraform plan -out=plan.tfplan
terraform apply plan.tfplan
```

This creates the VPC, EKS cluster, RDS instance, S3 bucket, and IAM roles.

### 2. Configure kubectl

```bash
aws eks update-kubeconfig --name primecx-cluster --region us-west-2
```

### 3. Build and Push Container Images

```bash
# Backend
docker build -t <ECR_REPO>/primecx-backend:latest -f Dockerfile .
docker push <ECR_REPO>/primecx-backend:latest

# Frontend
docker build -t <ECR_REPO>/primecx-frontend:latest frontend/
docker push <ECR_REPO>/primecx-frontend:latest
```

### 4. Deploy to Kubernetes

```bash
kubectl apply -f infrastructure/kubernetes/namespace.yaml
kubectl apply -f infrastructure/kubernetes/configmap.yaml
kubectl apply -f infrastructure/kubernetes/secrets.yaml
kubectl apply -f infrastructure/kubernetes/service-account.yaml
kubectl apply -f infrastructure/kubernetes/backend-deployment.yaml
kubectl apply -f infrastructure/kubernetes/backend-service.yaml
kubectl apply -f infrastructure/kubernetes/frontend-deployment.yaml
kubectl apply -f infrastructure/kubernetes/frontend-service.yaml
kubectl apply -f infrastructure/kubernetes/ingress.yaml
kubectl apply -f infrastructure/kubernetes/hpa.yaml
```

Verify the deployment:

```bash
kubectl get pods -n primecx
kubectl get ingress -n primecx
```

## Roles and Permissions

| Role | Key Capabilities |
|------|-----------------|
| `USER` | Submit support tickets, view own tickets and sessions |
| `SUPPORT_EXECUTIVE` | Handle tickets, start/end support sessions, upload recordings |
| `SUPPORT_ADMIN` | Manage users and roles, access admin endpoints, view all data |
| `SUPPORT_MANAGER` | View dashboards and analytics, access manager endpoints, oversee operations |

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA, AWS SDK v2
- **Frontend:** React 18, React Router 6, Axios, Okta React SDK
- **Database:** PostgreSQL 15
- **Auth:** Okta OIDC (OAuth2 + JWT)
- **Storage:** AWS S3 (presigned URLs for uploads)
- **Containerization:** Docker (multi-stage builds), Docker Compose
- **Orchestration:** Kubernetes on AWS EKS
- **Infrastructure:** Terraform, ALB Ingress Controller
- **API Docs:** SpringDoc OpenAPI (Swagger UI at `/swagger-ui/`)
