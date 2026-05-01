# Local Microservices Test-Bench

This project provides a robust, containerized integration testing environment for a Spring Boot microservice (`pet-app`). It includes a full local infrastructure setup using Docker Compose to support databases, event streaming, search, and identity management.

## 🏗️ Architecture & Components

The environment is orchestrated using Docker Compose and includes the following services:

* **Spring Boot Application (`pet-app`)**: A clean DTO-based architecture microservice running on Java 21.
* **Oracle 23c Free**: Relational database for persistent storage.
* **Flyway**: Automatically executes SQL migrations against the Oracle database on startup.
* **Keycloak (24.x)**: Identity and Access Management (IAM) server, pre-configured with a `petapp-realm` for JWT-based authentication.
* **Kafka & Zookeeper (Confluent 5.5.12)**: Event streaming platform. A `kafka-init` container automatically creates required topics on startup.
* **Elasticsearch & Kibana (8.13.0)**: Search engine and visualization dashboard for advanced querying and logging.
* **Kafka-UI**: Web interface for managing and monitoring Kafka clusters.
* **Bruno CLI**: API client used for executing end-to-end contract and integration tests.

---

## 🚀 Getting Started

### 1. Start the Infrastructure
To start the entire environment (databases, brokers, identity provider, etc.), run from the root directory:
```bash
docker compose up -d
```
*Note: This will not start the Bruno test runner automatically, as it is assigned to a specific testing profile.*

### 2. Start the Spring Boot Application
Once the infrastructure is up and healthy (especially Oracle and Keycloak), start the Spring Boot application:
```bash
cd pet-app
mvn spring-boot:run
```
The API will be available at `http://localhost:8082`.

---

## 🧪 Running Tests

### Unit & Integration Tests (Spring Boot)
The Spring Boot application contains extensive unit tests (using Mockito) and integration tests (using MockMvc).
```bash
cd pet-app
mvn clean test
```

### End-to-End API Tests (Bruno)
Bruno is used to execute complete API scenarios. The tests are configured to automatically authenticate against Keycloak, retrieve JWT tokens, and inject them into the authenticated API requests.

To execute the Bruno test suite via Docker Compose:
```bash
docker compose --profile test up bruno
```
*This command uses the lightweight `bruno-cli` container to dynamically install Bruno, load the `local` environment, and run all `.bru` files in the `bruno/` directory.*

---

## 🌐 Web Interfaces

Once the infrastructure is running, you can access the following web interfaces to monitor and manage your services:

| Service | URL | Credentials / Notes |
|---------|-----|---------------------|
| **Spring Boot API** | [http://localhost:8082](http://localhost:8082) | Backend application |
| **Keycloak Admin Console** | [http://localhost:8080](http://localhost:8080) | User: `admin` <br> Pass: `admin` |
| **Kafka UI** | [http://localhost:8081](http://localhost:8081) | Topic & Broker management |
| **Kibana Dashboard** | [http://localhost:5601](http://localhost:5601) | Elasticsearch visualization |

### Pre-configured Keycloak Users
The `petapp-realm` is automatically imported into Keycloak on startup with the following test users:
* **Admin User**: `adminuser` / `adminpass` (Roles: `user`, `admin`)
* **Standard User**: `testuser` / `testpass` (Roles: `user`)
