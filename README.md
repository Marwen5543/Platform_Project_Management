# 🚀 Resilient Microservices Monitoring Platform (DevSecOps PFE)

![Project Status](https://img.shields.io/badge/Status-Completed-success)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.0-green)
![Angular](https://img.shields.io/badge/Angular-16+-red)
![Kubernetes](https://img.shields.io/badge/Kubernetes-K3s-blue)
![GitLab CI](https://img.shields.io/badge/GitLab_CI-Pipeline-orange)

## 📖 Introduction

This repository hosts the source code and infrastructure configurations for my **End-of-Studies Project (PFE)**, completed at **Tunisys** to obtain the National Engineering Diploma from **ESPRIT**.

The goal was to architect a **Resilient Microservices Platform** supported by a fully automated **DevSecOps Software Factory**. The project bridges the gap between software development and operations, ensuring rapid, secure, and reliable deployments using modern cloud-native technologies.

---

## 🏗️ Global Architecture

The system is built on a **Microservices Architecture** utilizing the **Spring Cloud Ecosystem**, containerized with **Docker**, and orchestrated via **Kubernetes (K3s)**.

### 🧩 The Logical Stack
*   **Ingress/Load Balancer:** Nginx
*   **API Gateway:** Spring Cloud Gateway
*   **Service Discovery:** Netflix Eureka
*   **Security:** Keycloak (OIDC/OAuth2)
*   **Microservices:** User Service, Leave Service, Document Service
*   **Databases:** PostgreSQL (Containerized per service)

*(Recommended: Add your 'Logical Architecture Diagram' image here)*

---

## 🛠️ Technology Stack

| Category | Technologies Used |
| :--- | :--- |
| **Backend** | Java 17, Spring Boot 3, Spring Cloud (Eureka, Gateway, OpenFeign, Config Server) |
| **Frontend** | Angular 16+, TypeScript, Bootstrap, RxJS |
| **Database** | PostgreSQL, H2 (Testing) |
| **Security** | Keycloak (IAM), OIDC, JWT |
| **Containerization** | Docker, Docker Compose |
| **Orchestration** | Kubernetes (K3s), Helm |
| **IaC** | Terraform, Vagrant, Ansible |
| **CI/CD** | GitLab CI, Maven, Nexus, Harbor, ArgoCD (Optional) |
| **Quality & Security** | SonarQube, Trivy, Cosign |
| **Testing** | JUnit, Mockito, Selenium, Gatling |
| **Observability** | Prometheus, Grafana, AlertManager, Loki |

---

## ⚙️ The DevSecOps Software Factory

The core of this project is the **Automated Pipeline** that ensures code moves from development to production securely.

### 1. Infrastructure as Code (IaC)
To solve the "It works on my machine" problem, the infrastructure is provisioned automatically:
*   **Vagrant:** Provisioning local Virtual Machines.
*   **Ansible:** Configuration management (installing Docker, K3s, Java, etc.).
*   **Terraform:** Managing cloud resources (simulated).

### 2. The CI/CD Pipeline (GitLab CI)
Every commit triggers a strict pipeline defined in `.gitlab-ci.yml`:

1.  **Build:** Compiles code using Maven and pushes artifacts to **Nexus Repository Manager**.
2.  **Quality Gate:** Runs static code analysis via **SonarQube**. Fails if technical debt or bugs exceed thresholds.
3.  **Security Scan:** Scans Docker images for vulnerabilities using **Trivy**.
4.  **Signing:** Signs the Docker image using **Cosign** to ensure integrity.
5.  **Registry:** Pushes the secure image to **Harbor**.
6.  **Deploy:** Automatically updates the **Kubernetes (K3s)** cluster.
7.  **Notify:** Sends an **Email Notification** regarding the pipeline status (Success/Failure).

---

## 🛡️ Testing Strategy (The Pyramid)

I implemented a comprehensive testing strategy to guarantee zero-downtime releases:

*   **Unit Testing:** (JUnit, Mockito) Testing individual business logic methods.
*   **Integration Testing:** Verifying interaction between Microservices and PostgreSQL.
*   **E2E (Regression) Testing:** using **Selenium**. Scripts act as a "Robot User" to verify critical workflows (Login, Leave Request, Document Generation).
*   **Performance Testing:** using **Gatling**. Simulating high traffic to verify the Horizontal Pod Autoscaling (HPA).

---

## 👁️ Observability & Monitoring

The platform includes a centralized monitoring stack:
*   **Prometheus:** Scrapes metrics from Spring Boot Actuator and Kubernetes nodes.
*   **Grafana:** Visualizes CPU/RAM usage, Request Latency, and JVM stats.
*   **AlertManager:** Triggers alerts when services go down.

---

## 🚀 How to Run (Local Dev)

### Prerequisites
*   Docker & Docker Compose
*   Java 17 JDK
*   Node.js 18+

### Steps
1.  **Clone the repository**
    ```bash
    git clone https://github.com/Marwen5543/Tunisys-PFE.git
    cd Tunisys-PFE
    ```

2.  **Start Infrastructure (Keycloak, DBs, Discovery, Gateway)**
    ```bash
    cd docker
    docker-compose up -d
    ```

3.  **Start Microservices**
    ```bash
    # Run each service in a separate terminal
    cd user-service && mvn spring-boot:run
    cd leave-service && mvn spring-boot:run
    cd document-service && mvn spring-boot:run
    ```

4.  **Start Frontend**
    ```bash
    cd frontend
    npm install
    ng serve
    ```

5.  **Access the Application**
    *   Frontend: `http://localhost:4200`
    *   Keycloak Console: `http://localhost:8080`
    *   Grafana: `http://localhost:3000`

---

## 👨‍💻 About the Author

I am **Marwen Mamlouk**, a Software Engineer graduating from **ESPRIT** with distinction. Passionate about bridging the gap between Development and Operations, I specialize in **Full Stack Development** (Spring Boot/Angular) and **DevSecOps Automation**.

I engineered this project to demonstrate a production-ready approach to **Microservices Architecture**, focusing on resilience, security, and automation. I am currently open to new opportunities where I can apply my skills in building scalable software factories.

*   📫 **Contact:** [LinkedIn](https://www.linkedin.com/in/marwen-mamlouk-223077273/) | [Email](mailto:marwen.mamlouk@esprit.tn)

---
*© 2025 Tunisys PFE Project - Marwen Mamlouk*
