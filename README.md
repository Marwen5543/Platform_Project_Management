# Plateforme de Gestion de Projet Tunisys

Ce projet, réalisé dans le cadre d'un projet de fin d'études, est une plateforme complète pour la gestion de projets et de ressources humaines. Il implémente une architecture microservices moderne, avec une infrastructure entièrement automatisée et une pipeline CI/CD sécurisée.

**Avis important :** Cette plateforme a été développée en tant que démonstrateur (proof-of-concept). Elle implémente les fonctionnalités essentielles pour illustrer une architecture technique avancée mais n'est pas une version destinée à la production.

---

## 🚀 Table des Matières
1. [Fonctionnalités](#fonctionnalités-️)
2. [Guide Utilisateur](#guide-utilisateur-)
3. [Architecture Technique](#architecture-technique-)
4. [Démarrage Rapide](#démarrage-rapide-)
5. [Tests](#tests-)

---

## Fonctionnalités 💡

### Spécifications Fonctionnelles

La plateforme s'articule autour des modules suivants :

#### Gestion des Identités et des Accès (IAM)
- **Processus d'Onboarding Contrôlé :**
    1.  **Inscription :** Un nouvel utilisateur crée un compte qui reste `inactif`.
    2.  **Activation (Super-Admin) :** Un Super-Admin doit activer le compte et lui assigner un rôle (`Employé`, `RH`, `Admin`).
    3.  **Affectation (Admin) :** Après avoir complété son profil, l'utilisateur est affecté à un projet par un Admin selon ses compétences.
- **Authentification Sécurisée :** Login/Logout via Keycloak (OAuth 2.0/OIDC).
- **Gestion des Rôles :** Le Super-Admin gère les permissions de manière centralisée.

#### Gestion des Documents RH
- Soumission de demandes de documents (fiche de paie, attestation de travail).
- Traitement des demandes par le service RH.
- Stockage et accès sécurisé aux documents générés.

#### Gestion des Congés
- Soumission de demandes de congé avec dates et motifs.
- Validation des demandes par le service RH.
- Historique des congés pour tous les utilisateurs.

#### Gestion de Projet et Collaboration
- Suivi de la progression des projets par les Admins.
- Affectation des employés aux projets.
- Calendrier partagé pour visualiser les tâches, deadlines et réunions.

### Cas d'Utilisation (Use Cases) par Rôle

- **Nouvel Utilisateur (Non activé) :** Peut s'inscrire et tenter de se connecter (verra un message d'attente).
- **Employé :** Peut soumettre des demandes (congés, documents), consulter ses projets et interagir avec le calendrier.
- **RH :** Gère les demandes de congés/documents et les dossiers des employés.
- **Admin :** Supervise les projets et affecte les employés aux équipes.
- **Super-Admin :** Gère les utilisateurs, les rôles, les projets globaux et la configuration du système.

---

## Guide Utilisateur 📖

### Processus de Première Connexion
1.  **Créez un compte.** Celui-ci sera en attente d'approbation.
2.  **Attendez l'activation** par un Super-Admin.
3.  **Connectez-vous et complétez votre profil.** Cette étape est obligatoire.
4.  **Attendez l'affectation à un projet** par un Admin.
5.  Vous avez maintenant un accès complet à la plateforme.

### Fonctionnalités par Rôle
- **Employé :** Consulte les projets sur son tableau de bord, soumet des demandes de congés/documents, et utilise le calendrier.
- **RH :** Approuve/rejette les demandes via des tableaux de bord dédiés.
- **Admin :** Gère les équipes projet et suit l'avancement des tâches.
- **Super-Admin :** Utilise les panneaux d'administration pour activer les utilisateurs, gérer les rôles et configurer la plateforme.

---

## Architecture Technique 🛠️

### Stack Technologique

| Composant | Technologie | Version (indicative) |
| :--- | :--- | :--- |
| **Frontend** | Angular | 16 |
| **Backend** | Java 17 / Spring Boot 3 | |
| | Spring Cloud Gateway | |
| **Base de Données** | PostgreSQL | 16 |
| **Sécurité (IAM)** | Keycloak | 24 |
| **Service Discovery** | Netflix Eureka | |
| **Infrastructure** | Docker, K3s (Kubernetes) | |
| | Vagrant / VirtualBox (Ubuntu 22.04) | |
| **IaC / Automation**| Ansible, Terraform | |
| **CI/CD & DevOps**| GitLab CI, Maven 3.9.9, SonarQube 9.9 LTS | |
| | Trivy 0.45.x, Nexus 3.x, Harbor 2.8.x, Cosign | |
| **Monitoring** | Prometheus 2.47.x, Grafana 10.x, Loki | |
| **Tests Automatisés**| Selenium 4.x, Gatling 3.9.x | |

### Architecture Applicative (Full-Stack)
- **Backend (Microservices) :** L'application suit un modèle microservices (User, Leave, Document) développé en **Java Spring Boot**. Un **API Gateway** (Spring Cloud Gateway) route les requêtes, **Eureka** gère la découverte de services, et **Keycloak** centralise la sécurité.
- **Frontend (SPA) :** L'interface est une Single-Page Application développée de zéro avec **Angular**. Elle est réactive, intuitive et intègre un calendrier interactif.

### Architecture d'Infrastructure (IaC)
L'environnement est 100% reproductible grâce à l'Infrastructure as Code.
1.  **`vagrant up`** provisionne une VM Ubuntu via **Vagrant**.
2.  **Ansible** configure la VM, installe Docker et déploie un cluster **K3s**.
3.  **Terraform** déploie les ressources de l'application (déploiements, services) sur le cluster K3s.

### Architecture CI/CD & Observability (DevSecOps)
La pipeline **GitLab CI** automatise le cycle de vie du logiciel :
1.  **Build & Unit Test :** Maven compile et teste le code.
2.  **Analyse & Scan :** **SonarQube** analyse le code, **Trivy** scanne les dépendances. L'artefact est poussé sur **Nexus**.
3.  **Build & Scan d'Image :** Une image Docker est construite puis scannée par **Trivy**.
4.  **Sign & Push :** L'image validée est signée avec **Cosign** puis poussée sur **Harbor**.
5.  **Deploy to Staging :** Déploiement automatique sur un environnement de test.
6.  **Integration & Performance Tests :** Des tests d'intégration et de performance (avec **Gatling**) sont exécutés.
7.  **Deploy to Production :** Déploiement manuel contrôlé en production.

L'**Observability** est assurée par la stack **Prometheus** (métriques), **Loki** (logs) et **Grafana** (visualisation).

---

## Démarrage Rapide ⚡

### Prérequis
- [VirtualBox](https://www.virtualbox.org/)
- [Vagrant](https://www.vagrantup.com/)
- Git

### Installation
1.  Clonez ce dépôt :
    ```bash
    git clone [URL_DU_DEPOT]
    cd [NOM_DU_DEPOT]
    ```
2.  Lancez le provisionnement de l'environnement :
    ```bash
    vagrant up
    ```
    Cette commande peut prendre plusieurs minutes. Elle va télécharger la VM, la configurer, installer K3s et déployer tous les outils et applications.

3.  Une fois terminé, la plateforme sera accessible aux adresses spécifiées dans le `Vagrantfile` (par exemple, `http://localhost:4200` pour le frontend).

---

## Tests 🧪

### Stratégie de Qualité
En complément de la validation technique dans la pipeline (tests unitaires, d'intégration), une stratégie de validation fonctionnelle est en place.

- **Tests End-to-End (E2E) avec Selenium :**
    - Une suite de tests complète développée avec **Selenium WebDriver** simule des parcours utilisateurs critiques.
    - Ces tests sont exécutés **indépendamment de la pipeline CI/CD** (de manière planifiée ou manuelle) sur l'environnement de Staging.
    - Leur objectif est de servir de campagne de **tests de non-régression** avant les mises en production majeures.
