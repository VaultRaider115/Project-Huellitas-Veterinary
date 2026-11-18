# 🐾 Project Huellitas - Veterinary Management System

[🇪🇸 Versión en Español](readme.md) | [🇺🇸 English Version](README_EN.md)

[![Java](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18%2F19-61DAFB.svg?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6.svg?logo=typescript)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-7-646CFF.svg?logo=vite)](https://vitejs.dev/)
[![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![MariaDB](https://img.shields.io/badge/MariaDB-10+-003545.svg?logo=mariadb)](https://mariadb.org/)
[![Security](https://img.shields.io/badge/Security-JWT%20%7C%20AES--256-red.svg)]()
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

> **Comprehensive Veterinary Clinic Platform**: Full-stack solution for managing patients, appointments, medical treatments, prescriptions, clinic branches, and real-time security auditing with Web and Mobile clients.

---

## 📑 Table of Contents

- [Key Features](#-key-features)
- [Architecture & Project Structure](#-architecture--project-structure)
- [Technologies Used](#-technologies-used)
- [Security & Cryptography](#-security--cryptography)
- [Administrative Audit Log (Bitácora)](#-administrative-audit-log-bitácora)
- [User Roles](#-user-roles)
- [Prerequisites](#-prerequisites)
- [Configuration & Environment Variables](#-configuration--environment-variables)
- [Installation & Execution](#-installation--execution)
  - [1. Database Setup (MariaDB)](#1-database-setup-mariadb)
  - [2. Backend (Spring Boot)](#2-backend-spring-boot)
  - [3. Web Frontend (React + Vite)](#3-web-frontend-react--vite)
  - [4. Mobile Application (Android)](#4-mobile-application-android)
  - [5. Automated Setup with Scripts](#5-automated-setup-with-scripts)
  - [6. Kubernetes Deployment with HTTPS](#6-kubernetes-deployment-with-https)

---

## ✨ Key Features

- 🐶 **Patient Management (Pets):** Medical records, species, breeds, pet owners, and personalized tracking.
- 📅 **Comprehensive Appointment Scheduling:** Booking workflow, status transitions (Pending, Scheduled, Completed, Cancelled), cancellation flow, and filtered lookups by customer or pet.
- 🩺 **Medical Consultations & Treatments:** Diagnosis tracking, prescription management, medication registry, and clinical history.
- 📜 **Administrative Audit Logging:** Automatic auditing of login attempts and state-modifying operations (`POST`, `PUT`, `DELETE`, `PATCH`) capturing client IP, timestamp, user, role, and full operation traceability.
- 📊 **Dynamic Administrator Dashboard:** Real-time metrics, user management, clinical staff directory, full multi-table CRUD explorer, and live search filters.
- 📱 **Native Mobile Application (Android):** Built with Jetpack Compose, featuring dedicated interfaces tailored to each user role.
- 🏢 **Multi-Branch Management & Geolocation:** Branch oversight, branch managers, addresses, neighborhoods, cities, and states.

---

## 🏛 Architecture & Project Structure

The repository is organized as a modular monorepo:

```text
Project-Huellitas-Veterinary/
├── devops-proyect/         # Backend: REST API with Spring Boot 3.5 (Java 21)
│   ├── src/main/java/      # Modules: bitacora, citas, consultaMedica, mascotas, users, etc.
│   ├── src/main/resources/ # application.properties and server resources
│   └── pom.xml             # Maven dependencies & build configuration
│
├── devops-frontend/        # Web Frontend: SPA built with React, TypeScript & Vite
│   ├── src/                # Role-based pages, UI components, API client & mappers
│   ├── package.json        # Node dependencies & npm scripts
│   └── vite.config.ts      # Build pipeline & dev server configuration
│
├── devops-app-mobil/       # Mobile App: Native Android built with Kotlin & Jetpack Compose
│   ├── app/src/main/       # Activities, role screens, data models & network layer
│   └── build.gradle.kts    # Gradle build configuration
│
├── scripts/                # Fish scripts for installation, backups & automation
│   ├── instalarelproyecto.fish
│   ├── backup.fish
│   └── desintalar.fish
│
├── readme.md               # Main documentation (Spanish)
└── README_EN.md            # English documentation
```

---

## 🚀 Technologies Used

| Layer | Technologies | Description |
| :--- | :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.5, Spring Data JPA, Hibernate | Modular REST API, generic services, and MVC interceptors. |
| **Database** | MariaDB / MySQL | Relational data persistence with MariaDB dialect. |
| **Web Frontend** | React 18/19, TypeScript, Vite, Lucide Icons | Responsive, accessible, and reactive single-page interface. |
| **Android Mobile** | Kotlin, Jetpack Compose, Material 3 | Native application with role-tailored navigation. |
| **Security** | Spring Security, OAuth2 Resource Server, Nimbus JWT, Argon2id, AES-GCM, Cloudflare Turnstile | Protected endpoints, stateless authentication, password hashing, CAPTCHA, and symmetric encryption. |
| **Testing** | JUnit Jupiter, Mockito, Spring Boot Test | Unit testing for interceptors, services, and controllers. |

---

## 🔐 Security & Cryptography

The application enforces robust security standards:

1. **Stateless JWT Authentication:** Access tokens are issued upon successful authentication (`POST /usuario/login`), cryptographically signed using HMAC-SHA256 (`HmacSHA256`) with a 15-minute expiration window.
2. **AES-256-GCM Symmetric Encryption:** End-to-end data encryption for sensitive fields using `AES/GCM/NoPadding` with a 12-byte initialization vector (IV) and 128-bit authentication tags.
3. **Role-Based Access Control (RBAC):** Configured in `SecurityConfig`:
   - Endpoint `/bitacora/**` is restricted strictly to `Admin` and `Administrador` authorities and is read-only.
   - User administration endpoints and generic CRUD operations are restricted strictly to `Admin` and `Administrador`.
   - Managers and veterinarians use their own scoped endpoints (`/gestor/**`, `/veterinario/**`); clients can only access their own resources.
4. **HTTPS:** TLS and security headers are handled at the reverse proxy and nginx, which overwrites `X-Forwarded-For` and `X-Real-IP` so client-supplied headers are never trusted.
5. **Password protection:** Argon2id hashing with a random salt; new passwords require 12 characters, uppercase, lowercase, number, and symbol. Legacy BCrypt hashes are migrated progressively at login.
6. **Anti-bot protection:** Cloudflare Turnstile is validated by the backend before checking credentials. The API verifies success, an allowed hostname, and the `login` action; in production CAPTCHA is mandatory and fails closed.
7. **Per-account attempt limit:** Login and MFA verification are temporarily locked after 5 failures (configurable) and return `429` with `Retry-After`. Multi-replica deployments should use a shared store such as Redis.
8. **Safe errors:** Internal details are logged only on the server; clients receive generic messages and Spring hides messages, stack traces, and validation details.
9. **Security headers:** CSP, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`, and HSTS are applied by nginx and the API.
10. **Audit integrity:** The audit log is `@Immutable`, and the database rejects `UPDATE` and `DELETE` through append-only triggers (`scripts/hardening.sql`).
11. **Protected backups:** Dumps are AES-256 encrypted (GPG), paired with a SHA-256 checksum, and the plaintext dump is deleted afterwards.
12. **Supply chain:** Dependabot, `npm audit`, CycloneDX SBOMs in CI, immutable SHA image tags, and `kubectl set image` deployments. The `Security Scan` workflow runs Trivy on dependencies, secrets, and configuration for every PR, every push to `main`, and weekly.
13. **Hardened configuration (A05):** no default credentials, secrets required per environment, `ddl-auto=validate` in production, Actuator limited to health, errors without details, explicit CORS, non-privileged containers, and `Cache-Control: no-store` on API responses.
14. **Logging and monitoring (A09):** `security_event=...` records on stdout for login, lockout, and MFA events; persistent audit log with secret redaction and newline sanitization; Kubernetes health probes and read-only audit access.
15. **SSRF (A10):** the only outbound backend request targets the fixed URL `https://challenges.cloudflare.com/turnstile/v0/siteverify`; no user-controlled URL, host, or path is accepted.

---

## 📜 Administrative Audit Log (Bitácora)

The **Audit Log** system provides full traceability, compliance, and incident investigation:

* **Automatic Access Monitoring:** Logs `LOGIN_EXITOSO` (successful logins) and `LOGIN_FALLIDO` (failed logins due to invalid credentials or missing payload fields) directly from `UsuarioController`, recording the client's IP address.
* **Global Action Interceptor (`BitacoraInterceptor`):** Automatically intercepts and logs every state-modifying action (`POST`, `PUT`, `DELETE`, `PATCH`) across all controllers in the application.
* **Recorded Audit Fields per Event:**
  - `ID`: Unique auto-incremented event identifier.
  - `Date & Time`: Official server timestamp.
  - `User`: Authenticated account email (or *Anónimo* if unauthenticated).
  - `Role`: Verified role claim extracted from the JWT token.
  - `Action`: Standardized action code (e.g., `CREAR_CITA`, `CANCELAR_CITA`, `ELIMINAR_MASCOTA`).
  - `Module`: System component (`Citas`, `Mascotas`, `Usuarios`, `Tratamientos`, etc.).
  - `Detail`: HTTP method, target URI, and outcome description (HTTP status code and error messages if applicable).
  - `IP`: Remote client IP address (with `X-Forwarded-For` proxy detection).
* **Independent Transactions (`REQUIRES_NEW`):** Audit entries are committed in their own transaction scope, guaranteeing that failed requests or rolled-back transactions are still permanently recorded.

---

## 👥 User Roles

The platform dynamically adjusts navigation and permissions according to the user's role:

| Role | ID | Scope |
| :--- | :---: | :--- |
| **Administrator** | `1` | Full administrative control: user and staff management, pets, appointments, treatments, audit log viewer, and multi-table maintenance. |
| **Pet Owner / Client** | `2` | Customer portal: register and view personal pets, request and cancel appointments, and consult active treatments/prescriptions. |
| **Branch Manager / Staff** | `3` | Operational management: appointment schedule tracking, patient consultations, and medical treatment assignments. |

---

## 💻 Prerequisites

- **Java Development Kit (JDK) 21** or higher.
- **Node.js** (v20+ or v24 LTS recommended) and **npm** package manager.
- **MariaDB** or **MySQL** (v10.5+).
- **Android Studio** with Android SDK (if building the mobile application).
- **bash** or **fish** shell.

---

## 🔑 Configuration & Environment Variables

The backend requires 64-character hexadecimal cryptographic keys (256 bits).

Create a `.env` file in the root directory of `devops-proyect/` (or configure them as environment variables):

```dotenv
# Hex-encoded cryptographic keys (64 characters = 256 bits)
JWT_SECRET=your_64_character_hex_secret_key_for_signing_jwt_tokens_here
AES_SECRET=your_64_character_hex_secret_key_for_aes_gcm_encryption_here
HMAC_SECRET=your_64_character_hex_secret_key_for_hmac_validation_here
SPRING_DATASOURCE_USERNAME=huellitas_app
SPRING_DATASOURCE_PASSWORD=use_a_long_unique_password
# Only required when Turnstile is enabled in the backend
TURNSTILE_ENABLED=true
TURNSTILE_SECRET=private_cloudflare_secret
TURNSTILE_EXPECTED_HOSTNAMES=app.example.com,dev-server.tail223158.ts.net
```

> **Tip:** You can generate secure 64-character hex keys using OpenSSL:
> ```bash
> openssl rand -hex 32
> ```

---

## 📦 Installation & Execution

### 1. Database Setup (MariaDB)

Create the database and a dedicated application user with a strong password. Do not use default credentials or commit these credentials:

```sql
CREATE DATABASE IF NOT EXISTS huellitas_db;
CREATE USER IF NOT EXISTS 'huellitas_app'@'localhost' IDENTIFIED BY '<generated-strong-password>';
GRANT SELECT, INSERT, UPDATE, DELETE ON huellitas_db.* TO 'huellitas_app'@'localhost';
FLUSH PRIVILEGES;
```

`SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` are required; the application does not ship with default database credentials. `APP_CORS_ALLOWED_ORIGINS` configures development origins; in production, the frontend and API are served from the same origin through the reverse proxy.

Register all hostnames that will serve the login page in the Cloudflare Turnstile widget settings. Set the **site key** as `VITE_TURNSTILE_SITE_KEY` when building the frontend (for example in a local, untracked `.env` file) and keep the **secret key** only in the backend environment. In Kubernetes, add `TURNSTILE_SECRET` and `TURNSTILE_EXPECTED_HOSTNAMES` to the `huellitas-secrets` Secret; the latter is a comma-separated list that must match the registered hostnames. The widget is optional in local development and mandatory in production builds.

---

### 2. Backend (Spring Boot)

Navigate to the backend directory and launch the application:

```bash
cd devops-proyect

# Compile classes and run tests
./mvnw test-compile

# Start the Spring Boot server (port 8080 by default)
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

The `prod` profile uses `spring.jpa.hibernate.ddl-auto=validate` and requires all environment variables. Schema changes must be applied in a controlled migration before deployment; the application does not alter tables in production.

---

### 3. Web Frontend (React + Vite)

Navigate to the frontend directory, install dependencies, and start the development server:

```bash
cd devops-frontend

# Install dependencies
npm install

# Expose the Turnstile site key to Vite
export VITE_TURNSTILE_SITE_KEY=your-public-cloudflare-site-key

# Start development server (port 5173 by default)
npm run dev
```

Open your browser at `http://localhost:5173`.

---

### 4. Mobile Application (Android)

To build the Android application via the command line:

```bash
cd devops-app-mobil

# Build debug APK
./gradlew :app:assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`.

---

### 5. Automated Setup with Scripts

On Arch Linux-based environments, you can run the automation scripts located in `scripts/`:

```bash
# Grant execution permissions
chmod +x scripts/*.fish

# Run full automated installer
./scripts/instalarelproyecto.fish
```

To stop background background services:

```bash
pkill -f "spring-boot" 2>/dev/null
pkill -f "node" 2>/dev/null
pkill -f "npm" 2>/dev/null
```

---

### 6. Kubernetes Deployment Behind a Reverse Proxy

The external reverse proxy terminates TLS and adds the security headers. The cluster frontend listens on HTTP at NodePort `30081`; configure the proxy to forward site requests there. The backend remains an internal cluster service and API requests reach it through the frontend. Before deployment, configure the GitHub Actions repository variable `VITE_TURNSTILE_SITE_KEY` and add `TURNSTILE_SECRET` and `TURNSTILE_EXPECTED_HOSTNAMES` to the `huellitas-secrets` Secret. This list must contain all and only the hostnames registered with Turnstile, comma-separated without protocol or paths.

---

## 📄 License

This project is licensed under the **MIT License**. See the `LICENSE` file for details.
