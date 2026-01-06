# 🚀 Tunisys Project Management Platform

*Microservices • DevSecOps • Cloud-Native • Monitoring*

## 📌 Overview

The Tunisys Project Management Platform is a full-stack microservices-based application developed as a Proof of Concept (PoC) within a Final Year Engineering Project.

The platform provides project management and human resources management features, while showcasing a modern enterprise-grade architecture, including:

- Microservices architecture
- Secure IAM with OAuth2 / OIDC
- Infrastructure as Code (IaC)
- CI/CD with DevSecOps practices
- Kubernetes-based deployment
- Full observability and monitoring

> **⚠️ Important Notice**
> This project is a demonstrator (PoC). It implements core business and technical concepts but is not intended for production use without further hardening, scalability tuning, and operational validation.

## 📚 Table of Contents

- [Functional Features](#-functional-features)
- [User Roles & Use Cases](#-user-roles--use-cases)
- [User Guide](#-user-guide)
- [Technical Architecture](#️-technical-architecture)
  - [CI/CD & DevSecOps Pipeline](#-cicd--devsecops-pipeline)
  - [Monitoring & Observability](#-monitoring--observability)
- [Tests & Quality Strategy](#-tests--quality-strategy)
- [Quick Start](#-quick-start)

---

## 💡 Functional Features

### 🔐 Identity & Access Management (IAM)

A secure and controlled onboarding workflow is implemented:

1.  **User Registration**
    - User creates an account.
    - Account remains `inactive` with limited permissions.
2.  **Activation & Role Assignment (Super-Admin)**
    - Manual activation by Super-Admin.
    - Role assignment: `Employee`, `RH`, `Admin`.
3.  **Profile Completion & Project Assignment (Admin)**
    - User completes profile (skills, specialization).
    - Admin assigns user to relevant projects.

**Security Features:**
- Authentication & authorization via **Keycloak**.
- **OAuth 2.0 / OpenID Connect** protocols.
- Centralized role and permission management.

### 📄 HR Document Management

- Employees submit document requests (salary slips, work certificates, etc.).
- RH department processes requests.
- Secure storage and controlled access to generated documents.

### 🏖️ Leave Management

- Leave request submission (dates, type, reason).
- RH validation workflow.
- Leave history tracking for employees and administrators.

### 📊 Project Management

- Project creation and tracking.
- Employee assignment to projects.
- Global project progress monitoring.
- Shared calendar (tasks, meetings, deadlines).

---

## 👥 User Roles & Use Cases

| Role | Main Responsibilities |
| :--- | :--- | :--- |
| **Unactivated User** | Register, attempt login (pending activation). |
| **Employee** | Submit leave & document requests, view projects. |
| **RH** | Manage leave & document requests, employee records. |
| **Admin** | Manage projects and team assignments. |
| **Super-Admin** | Manage users, roles, and system configuration. |

---

## 📖 User Guide

### First Login Workflow

1.  **Register** an account.
2.  **Wait** for Super-Admin activation.
3.  **Complete** personal profile.
4.  **Get assigned** to a project.
5.  **Access** platform features based on assigned role.

### Navigation

- Role-based dashboards.
- Centralized access to features.
- Clear and intuitive UI.

---

## 🛠️ Technical Architecture

### 🔧 Technology Stack

| Layer | Technology | Version |
| :--- | :--- | :--- |
| **Frontend** | Angular | `16` |
| **Backend** | Java / Spring Boot | `Java 17` / `Spring Boot 3` |
| **API Gateway** | Spring Cloud Gateway | — |
| **Security** | Keycloak | `24` |
| **Service Discovery**| Netflix Eureka | — |
| **Database** | PostgreSQL | `16` |
| **Containers** | Docker | — |
| **Orchestration** | Kubernetes (K3s) | — |
| **Infrastructure** | Vagrant / VirtualBox | `Ubuntu 22.04` |
| **IaC** | Ansible, Terraform | — |
| **CI/CD** | GitLab CI, Maven | `3.9.9` |
| **Security & Quality**| SonarQube, Trivy, Cosign | — |
| **Registry** | Nexus, Harbor | — |
| **Monitoring** | Prometheus, Grafana, Loki | — |
| **Testing** | Selenium, Gatling | — |

### 🧩 Application Architecture

- **Backend (Microservices):**
  - An ecosystem of independent Spring Boot microservices:
    1.  **`user-management-service`**: Handles all user-related operations, profiles, and authentication logic.
    2.  **`leave-management-service`**: Manages the entire lifecycle of leave requests.
    3.  **`document-management-service`**: Manages HR document requests and generation.
  - **API Gateway** for routing and cross-cutting concerns.
  - **Eureka** for dynamic service discovery.
  - Centralized security via **Keycloak**.
- **Frontend:**
  - **Angular SPA** built from scratch.
  - REST communication via API Gateway.
  - Interactive calendar and dashboards.
  - Responsive and role-aware UI.

### 🏗️ Infrastructure Architecture (IaC)

- Fully reproducible environment.
- **Vagrant** provisions Ubuntu VM.
- **Ansible** installs Docker & K3s.
- **Terraform** deploys Kubernetes resources.
- Dedicated Kubernetes namespace: `tunisys`.

---

## 🔁 CI/CD & DevSecOps Pipeline

The **GitLab CI** pipeline automates the entire lifecycle:

1.  **Build & Unit Tests** (Maven)
2.  **Static Analysis & Dependency Scan**
    - SonarQube (SAST)
    - Trivy (CVEs)
3.  **Artifact Repository** -> Nexus
4.  **Docker Image Build & Scan** (Trivy)
5.  **Image Signing** (Cosign)
6.  **Private Registry** -> Harbor
7.  **Deploy to Staging**
8.  **Integration & Performance Tests** (Gatling)
9.  **Production Deployment** (Manual / optional trigger)

---

## 📊 Monitoring & Observability

- **Prometheus:** Metrics collection.
- **Grafana:** Dashboards & visualization.
- **Loki:** Centralized logging.

This provides:
- Service health monitoring.
- Performance insights.
- Infrastructure observability.

---

## 🧪 Tests & Quality Strategy

### Automated Testing
- Unit & integration tests in CI pipeline.
- Performance testing with Gatling.

### End-to-End (E2E) Testing
- **Selenium WebDriver**.
- Executed on Staging environment.
- Validates complete user workflows.
- Used for non-regression testing.
- Detailed reports analyzed before production releases.

---

## ⚡ Quick Start

### Prerequisites

- [VirtualBox](https://www.virtualbox.org/)
- [Vagrant](https://www.vagrantup.com/)
- [Git](https://git-scm.com/)

### Installation

1. Clone the repository:
   ```bash
   git clone <REPOSITORY_URL>
   cd <REPOSITORY_NAME>
Launch the automated provisioning:
code
Bash
vagrant up
⏳ Provisioning may take several minutes. This single command will provision the VM, install a K3s cluster, and deploy the entire application stack.
What's Running?
Once the vagrant up process is complete, the entire platform is deployed. This includes:
- Frontend Application:
- angular-frontend accessible at http://localhost:4200
- Backend Microservices:
- user-management-service
- leave-management-service
- document-management-service
- Core Platform Services:
- api-gateway, eureka-server, keycloak, postgresql, and the full observability stack.
- You can access the various service UIs (Grafana, Keycloak, etc.) via the ports forwarded in the Vagrantfile.
