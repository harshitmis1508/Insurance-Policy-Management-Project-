# 🛡️ Insurance Policy & Claim Management System

A full-featured, role-based **Insurance Policy and Claim Management System** built with **Spring Boot**. It handles the complete insurance lifecycle — user onboarding with OTP verification, product/plan catalog management, policy purchase & issuance, premium payments, claim submission, agent review, risk assessment, admin decisioning, and claim settlement — all secured with JWT-based authentication and role-based access control.

---

## 📌 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [User Roles](#-user-roles)
- [Project Workflow](#-project-workflow)
- [API Endpoints](#-api-endpoints)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [Default Admin Account](#-default-admin-account)
- [API Documentation (Swagger)](#-api-documentation-swagger)
- [Project Structure](#-project-structure)
- [Testing the APIs](#-testing-the-apis)
- [Contributors](#-contributors)
- [License](#-license)

---

## 📖 Overview

This project simulates a real-world insurance company backend where:

- **Customers** can register, get verified via OTP, build a KYC profile, browse insurance plans, purchase policies, pay premiums, and file claims.
- **Agents** review and investigate claims assigned to them and recommend a decision.
- **Admins** manage products/plans, onboard agents, issue policies, make the final claim decision, and process settlements.

Every sensitive action is tracked through an **audit log**, and claims go through a **risk assessment** step before a final decision is made — mirroring how real insurance workflows operate.

---

## ✨ Features

- 🔐 **JWT Authentication** with email/mobile OTP-based registration verification (via Email/SMTP and Twilio SMS)
- 👥 **Role-Based Access Control** — `ADMIN`, `AGENT`, `CUSTOMER`
- 📦 **Product & Plan Management** — Health, Motor, Life, Travel insurance products with configurable plans
- 📄 **Policy Lifecycle** — purchase, admin-issue, activation on payment, cancellation, expiry
- 💳 **Premium Payments** — customer self-pay or admin/agent-recorded offline payments
- 📝 **Claims Workflow** — submission → document upload → agent review → risk assessment → admin decision → settlement
- 📁 **Document Uploads** — Cloudinary-backed storage for claim-supporting documents
- ⚖️ **Automated Risk Assessment** for claims before final approval
- 💰 **Claim Settlement Tracking** — initiate → mark as paid
- 🧾 **Audit Logging** of key actions across the system, filterable by actor/entity type
- 📚 **Interactive API Docs** via Swagger / OpenAPI
- 📃 **Pagination & Sorting** on all list endpoints
- ⚠️ **Centralized Exception Handling** with clean, consistent error responses

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.6 |
| Security | Spring Security + JWT (`jjwt` 0.12.6) |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| Object Mapping | ModelMapper |
| File Storage | Cloudinary |
| Email | Spring Mail (SMTP) |
| SMS/OTP | Twilio Verify API |
| API Docs | springdoc-openapi (Swagger UI) |
| Build Tool | Maven |
| Boilerplate Reduction | Lombok |

---

## 🏗 Architecture

The project follows a clean, layered Spring Boot architecture:

```
Controller  →  Service  →  Repository  →  Database
     ↓            ↓
   DTOs      Business Rules,
 (Request/    Validation,
  Response)   Audit Logging
```

- **`controller/`** — REST endpoints, request validation, role authorization (`@PreAuthorize`)
- **`service/`** — business logic, orchestration between repositories
- **`repository/`** — Spring Data JPA repositories
- **`entity/`** — JPA entities mapped to database tables
- **`dto/request` & `dto/response`** — clean API contracts, decoupled from entities
- **`security/`** — JWT filter, token utility, custom user details
- **`exception/`** — global exception handler with consistent `ApiResponse` error format
- **`config/`** — Security, Swagger, Cloudinary, Twilio configuration + admin data seeding

---

## 👥 User Roles

| Role | Capabilities |
|---|---|
| **CUSTOMER** | Register/login, manage own profile, browse plans, purchase policies, pay premiums, submit & track claims, upload claim documents |
| **AGENT** | View assigned claims, review & recommend claim decisions, run risk assessments |
| **ADMIN** | Manage products/plans, onboard agents, manage users, issue policies, assign claims, make final claim decisions, process settlements, view audit logs |

---

## 🔄 Project Workflow

```
1. Customer registers  →  OTP sent (Email/SMS)  →  Account verified
2. Customer logs in    →  Creates KYC profile
3. Admin creates Product → creates Plan under that Product
4. Customer purchases a Policy (status: PENDING_PAYMENT)
5. Customer/Admin records Payment  →  Policy becomes ACTIVE
6. Customer submits a Claim with supporting documents
7. Admin assigns the Claim to an Agent
8. Agent reviews the Claim and recommends APPROVE/REJECT
9. System runs a Risk Assessment on the Claim
10. Admin makes the final Decision (APPROVED/REJECTED)
11. If approved, Admin initiates a Settlement → marks it PAID
12. Every step is recorded in the Audit Log
```

---

## 🔌 API Endpoints

Base URL: `http://localhost:8080`

<details>
<summary><b>🔑 Auth — <code>/api/auth</code></b></summary>

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/verify-otp` | Public |
| POST | `/api/auth/resend-otp` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/forgot-password` | Public |
| POST | `/api/auth/reset-password` | Public |

</details>

<details>
<summary><b>👤 Users — <code>/api/users</code></b></summary>

| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/users` | ADMIN |
| GET | `/api/users/{userId}` | ADMIN |
| POST | `/api/users/agent` | ADMIN |
| PATCH | `/api/users/{userId}/status` | ADMIN |

</details>

<details>
<summary><b>🙋 Customers — <code>/api/customers</code></b></summary>

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/customers/profile` | CUSTOMER |
| PUT | `/api/customers/profile` | CUSTOMER |
| GET | `/api/customers/profile` | CUSTOMER |
| GET | `/api/customers/{customerId}` | ADMIN, AGENT |
| GET | `/api/customers` | ADMIN, AGENT |

</details>

<details>
<summary><b>📦 Products — <code>/api/products</code></b></summary>

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/products` | ADMIN |
| PUT | `/api/products/{id}` | ADMIN |
| PATCH | `/api/products/{id}/deactivate` | ADMIN |
| GET | `/api/products/active` | Public |
| GET | `/api/products` | ADMIN |

</details>

<details>
<summary><b>📋 Plans — <code>/api/plans</code></b></summary>

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/plans` | ADMIN |
| PUT | `/api/plans/{id}` | ADMIN |
| PATCH | `/api/plans/{id}/deactivate` | ADMIN |
| GET | `/api/plans/active` | Public |
| GET | `/api/plans/product/{productId}` | Public |

</details>

<details>
<summary><b>📄 Policies — <code>/api/policies</code></b></summary>

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/policies/purchase` | CUSTOMER |
| POST | `/api/policies/issue` | ADMIN, AGENT |
| GET | `/api/policies/my` | CUSTOMER |
| GET | `/api/policies` | ADMIN, AGENT |
| GET | `/api/policies/customer/{customerId}` | ADMIN, AGENT |
| GET | `/api/policies/{policyId}` | ADMIN, AGENT, CUSTOMER |
| PATCH | `/api/policies/{policyId}/cancel` | ADMIN, AGENT |

</details>

<details>
<summary><b>💳 Payments — <code>/api/payments</code></b></summary>

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/payments` | CUSTOMER |
| POST | `/api/payments/admin` | ADMIN, AGENT |
| GET | `/api/payments/my/{policyId}` | CUSTOMER |
| GET | `/api/payments` | ADMIN, AGENT |
| GET | `/api/payments/policy/{policyId}` | ADMIN, AGENT |

</details>

<details>
<summary><b>📝 Claims — <code>/api/claims</code></b></summary>

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/claims` | CUSTOMER |
| GET | `/api/claims/my` | CUSTOMER |
| GET | `/api/claims` | ADMIN, AGENT |
| GET | `/api/claims/assigned-to-me` | AGENT |
| GET | `/api/claims/{claimId}` | ADMIN, AGENT, CUSTOMER |
| PATCH | `/api/claims/{claimId}/review` | AGENT |
| PATCH | `/api/claims/{claimId}/assign/{agentId}` | ADMIN |
| GET | `/api/claims/{claimId}/risk-assessment` | ADMIN, AGENT |
| PATCH | `/api/claims/{claimId}/decide` | ADMIN |
| GET | `/api/claims/{claimId}/history` | ADMIN, AGENT, CUSTOMER |

</details>

<details>
<summary><b>📁 Claim Documents — <code>/api/claims/{claimId}/documents</code></b></summary>

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/claims/{claimId}/documents` | CUSTOMER, ADMIN, AGENT |
| GET | `/api/claims/{claimId}/documents` | CUSTOMER, ADMIN, AGENT |
| DELETE | `/api/claims/documents/{documentId}` | CUSTOMER, ADMIN, AGENT |

</details>

<details>
<summary><b>💰 Settlements — <code>/api/settlements</code></b></summary>

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/settlements/claim/{claimId}` | ADMIN |
| PATCH | `/api/settlements/{settlementId}/paid` | ADMIN |
| GET | `/api/settlements/{settlementId}` | ADMIN, AGENT |
| GET | `/api/settlements/claim/{claimId}` | ADMIN, AGENT |

</details>

<details>
<summary><b>🧾 Audit Logs — <code>/api/audit-logs</code></b></summary>

| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/audit-logs` | ADMIN |

</details>

All responses follow a consistent envelope:

```json
{
  "success": true,
  "message": "Descriptive message",
  "data": { },
  "timestamp": "2026-07-02T10:15:30"
}
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+
- (Optional) Cloudinary account — for claim document uploads
- (Optional) Gmail/SMTP credentials — for Email OTP
- (Optional) Twilio account — for SMS OTP

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/Insurance-Policy-Management-Project.git
cd Insurance-Policy-Management-Project
```

### 2. Create the database

```sql
CREATE DATABASE insurance_db;
```

### 3. Configure environment variables

Set the variables listed below (via your OS environment, an `.env` loader, or your IDE run configuration). The app has sensible local defaults for the database connection.

### 4. Run the application

```bash
./mvnw spring-boot:run
```

The API will start on **`http://localhost:8080`**.

### 5. (Optional) Build a JAR

```bash
./mvnw clean package
java -jar target/09_Insurance_Project-0.0.1-SNAPSHOT.jar
```

---

## 🔧 Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/insurance_db` |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | *(empty)* |
| `JWT_SECRET` | Secret key used to sign JWTs | pre-set dev value |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name | — |
| `CLOUDINARY_API_KEY` | Cloudinary API key | — |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret | — |
| `MAIL_USERNAME` | SMTP username (e.g. Gmail address) | — |
| `MAIL_PASSWORD` | SMTP app password | — |
| `TWILIO_ACCOUNT_SID` | Twilio account SID | — |
| `TWILIO_AUTH_TOKEN` | Twilio auth token | — |
| `TWILIO_VERIFY_SERVICE_SID` | Twilio Verify service SID | — |
| `TWILIO_ENABLED` | Enable/disable Twilio phone OTP | `true` |

> 💡 If Cloudinary/SMTP/Twilio credentials aren't configured, the corresponding features (document upload, Email OTP, Phone OTP) will not work — the rest of the system remains fully functional.

---

## 🔑 Default Admin Account

On first startup, a default admin is auto-seeded so you can log in immediately:

```
Email:    admin@gmail.com
Password: password123
```

Use this account to create products, plans, and agent accounts.

---

## 📚 API Documentation (Swagger)

Once the app is running, explore and test every endpoint interactively at:

```
http://localhost:8080/swagger-ui.html
```

Login via `/api/auth/login`, copy the JWT `token` from the response, click **Authorize** in Swagger UI, and paste it in as `Bearer <token>`.

---

## 📂 Project Structure

```
src/main/java/com/harshit/monocept
├── config/          # Security, Swagger, Cloudinary, Twilio config, DB seeding
├── controller/       # REST controllers
├── dto/
│   ├── request/       # Incoming request payloads
│   └── response/       # Outgoing response payloads
├── entity/          # JPA entities
├── enums/            # Domain enums (Role, ClaimStatus, PolicyStatus, ...)
├── exception/         # Custom exceptions + global handler
├── repository/         # Spring Data JPA repositories
├── security/          # JWT filter, JWT util, user details service
├── service/            # Business logic
├── util/              # Pagination & phone number helpers
└── Application.java    # Entry point
```

---

## 🧪 Testing the APIs

This project ships with a ready-to-use **Postman collection** (auth flow, admin setup, policy purchase, payments, claims, settlements, and audit logs, in the correct sequence with auto-captured tokens/IDs). Import it and the accompanying environment file into Postman to test every endpoint end-to-end without manually managing JWTs.

You can also test through Swagger UI (see above) or directly from a connected frontend application.

---

## 👨‍💻 Contributors

This is a **group project**, built and maintained by:

- **Harshit Mishra**
- **Harsh Agarwal**

Contributions, issues, and feature requests are welcome — feel free to check the [issues page](../../issues) or open a pull request.

---

## 📄 License

This project is available for educational and portfolio purposes. Add a license of your choice (e.g. MIT) if you plan to open-source it publicly.

---

<p align="center">Made with ☕ and Spring Boot by <b>Harshit Mishra</b> & <b>Harsh Agarwal</b></p>
