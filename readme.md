# 🐾 Project Huellitas - Sistema de Gestión Veterinaria

[🇪🇸 Versión en Español](readme.md) | [🇺🇸 English Version](README_EN.md)

[![Java](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18%2F19-61DAFB.svg?logo=react)](https://es.react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6.svg?logo=typescript)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-7-646CFF.svg?logo=vite)](https://vitejs.dev/)
[![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![MariaDB](https://img.shields.io/badge/MariaDB-10+-003545.svg?logo=mariadb)](https://mariadb.org/)
[![Security](https://img.shields.io/badge/Security-JWT%20%7C%20AES--256-red.svg)]()
[![License](https://img.shields.io/badge/Licencia-MIT-green)](LICENSE)

> **Plataforma integral para clínicas veterinarias**: Solución fullstack para la gestión de pacientes, citas, tratamientos, recetas, control de sucursales y auditoría de seguridad en tiempo real con clientes Web y Móvil.

---

## 📑 Tabla de Contenidos

- [Características Principales](#-características-principales)
- [Arquitectura y Estructura del Proyecto](#-arquitectura-y-estructura-del-proyecto)
- [Tecnologías Utilizadas](#-tecnologías-utilizadas)
- [Seguridad y Criptografía](#-seguridad-y-criptografía)
- [Bitácora Administrativa (Auditoría)](#-bitácora-administrativa-auditoría)
- [Roles de Usuario](#-roles-de-usuario)
- [Requisitos Previos](#-requisitos-previos)
- [Configuración y Variables de Entorno](#-configuración-y-variables-de-entorno)
- [Instalación y Ejecución](#-instalación-y-ejecución)
  - [1. Base de Datos (MariaDB)](#1-base-de-datos-mariadb)
  - [2. Backend (Spring Boot)](#2-backend-spring-boot)
  - [3. Frontend Web (React + Vite)](#3-frontend-web-react--vite)
  - [4. Aplicación Móvil (Android)](#4-aplicación-móvil-android)
  - [5. Instalación Automatizada con Scripts](#5-instalación-automatizada-con-scripts)
  - [6. Despliegue en Kubernetes con HTTPS](#6-despliegue-en-kubernetes-con-https)

---

## ✨ Características Principales

- 🐶 **Gestión de Pacientes (Mascotas):** Expedientes clínicos, especies, razas, control de dueños y seguimiento personalizado.
- 📅 **Control Integral de Citas:** Programación, cambio de estados (Pendiente, Programada, Completada, Cancelada) y consultas filtradas por cliente o mascota.
- 🩺 **Consultas y Tratamientos Médicos:** Registro de diagnósticos, control de recetas, medicamentos e historial médico.
- 📜 **Bitácora Administrativa y Auditoría:** Registro automático de inicios de sesión y operaciones de modificación (`POST`, `PUT`, `DELETE`, `PATCH`) con IP, usuario, rol y trazabilidad completa.
- 📊 **Panel Administrativo Dinámico:** Dashboard con métricas clave, gestión de usuarios, personal de salud, explorador multi-tabla CRUD y filtros en tiempo real.
- 📱 **Aplicación Móvil Nativa (Android):** Diseñada con Jetpack Compose y adaptada a la vista de cada rol.
- 🏢 **Gestión Multi-Sucursal y Geolocalización:** Control de sucursales, gestores, direcciones, colonias, ciudades y estados.

---

## 🏛 Arquitectura y Estructura del Proyecto

El repositorio está organizado como un monorepo modular:

```text
Project-Huellitas-Veterinary/
├── devops-proyect/         # Backend: API REST en Spring Boot 3.5 (Java 21)
│   ├── src/main/java/      # Módulos: bitacora, citas, consultaMedica, mascotas, users, etc.
│   ├── src/main/resources/ # application.properties y recursos del servidor
│   └── pom.xml             # Dependencias Maven
│
├── devops-frontend/        # Frontend Web: SPA en React, TypeScript y Vite
│   ├── src/                # Vistas por rol, componentes, servicios de API y mappers
│   ├── package.json        # Dependencias de Node y scripts npm
│   └── vite.config.ts      # Configuración de compilación y servidor de desarrollo
│
├── devops-app-mobil/       # Aplicación Móvil: Android nativo con Kotlin y Jetpack Compose
│   ├── app/src/main/       # Actividades, vistas por rol, modelos y llamadas de red
│   └── build.gradle.kts    # Configuración de Gradle
│
├── scripts/                # Scripts Fish para instalación, respaldo y automatización
│   ├── instalarelproyecto.fish
│   ├── backup.fish
│   └── desintalar.fish
│
└── readme.md               # Documentación general del repositorio
```

---

## 🚀 Tecnologías Utilizadas

| Capa | Tecnologías | Descripción |
| :--- | :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.5, Spring Data JPA, Hibernate | API REST modular, servicios genéricos e interceptores. |
| **Base de Datos** | MariaDB / MySQL | Almacenamiento relacional con dialecto MariaDB. |
| **Frontend Web** | React 18/19, TypeScript, Vite, Lucide Icons | Interfaz moderna, reactiva, accesible y responsive. |
| **Móvil Android** | Kotlin, Jetpack Compose, Material 3 | Aplicación nativa con navegación diferenciada por roles. |
| **Seguridad** | Spring Security, OAuth2 Resource Server, Nimbus JWT, Argon2id, AES-GCM, Cloudflare Turnstile | Protección de endpoints, autenticación sin estado, hashes de contraseñas, CAPTCHA y cifrado simétrico. |
| **Testing** | JUnit Jupiter, Mockito, Spring Boot Test | Pruebas unitarias de servicios, controladores e interceptores. |

---

## 🔐 Seguridad y Criptografía

El sistema implementa medidas de seguridad sólidas:

1. **Autenticación Stateless con JWT:** Los tokens de acceso se emiten tras el login (`POST /usuario/login`), firmados criptográficamente mediante algoritmo HMAC-SHA256 (`HmacSHA256`) con tiempo de expiración de 15 minutos.
2. **Cifrado Simétrico AES-256-GCM:** Soporte para cifrado y descifrado de datos sensibles mediante `AES/GCM/NoPadding` con vector de inicialización (IV) de 12 bytes y tags de autenticación de 128 bits.
3. **Control de Acceso Basado en Roles (RBAC):** Restricciones automáticas en `SecurityConfig`:
   - Endpoint `/bitacora/**` restringido exclusivamente a roles `Admin` y `Administrador`, y es de solo lectura.
   - Endpoints de administración de `/usuario` y las operaciones CRUD genéricas restringidas exclusivamente a `Admin` y `Administrador`.
   - Gestores y veterinarios usan endpoints propios (`/gestor/**`, `/veterinario/**`) con validación de sucursal; los clientes solo acceden a sus propios recursos.
4. **Transporte HTTPS:** TLS y las cabeceras de seguridad se gestionan en el proxy inverso y en nginx, que además sobrescribe `X-Forwarded-For` y `X-Real-IP` para no confiar en cabeceras enviadas por el cliente.
5. **Protección de Contraseñas:** Hash Argon2id con salt aleatorio; las claves nuevas requieren 12 caracteres, mayúscula, minúscula, número y símbolo. Los hashes BCrypt existentes se migran progresivamente en el inicio de sesión.
6. **Protección anti-bots:** Cloudflare Turnstile se valida en el backend antes de consultar credenciales. La API comprueba el éxito, un hostname de la lista permitida y la acción `login`; en producción el CAPTCHA es obligatorio y falla de forma cerrada si Cloudflare no responde.
7. **Límite de intentos por cuenta:** El login y la verificación MFA se bloquean temporalmente tras 5 fallos (configurable) y responden `429` con `Retry-After`. En despliegues con varias réplicas se debe usar un almacén compartido (por ejemplo Redis).
8. **Errores seguros:** `GlobalExceptionHandler` registra el detalle técnico solo en el servidor y devuelve mensajes genéricos; Spring oculta mensajes, trazas y errores de validación en las respuestas.
9. **Cabeceras de seguridad:** CSP, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy` y HSTS se aplican en nginx y en la API.
10. **Integridad de la auditoría:** La bitácora es `@Immutable` y la base de datos rechaza `UPDATE` y `DELETE` mediante disparadores append-only (`scripts/hardening.sql`).
11. **Respaldos protegidos:** Los volcados se cifran con AES-256 (GPG), se acompañan de una suma SHA-256 y el archivo en claro se elimina al terminar.
12. **Cadena de suministro:** Dependabot, `npm audit`, SBOM CycloneDX en CI, imágenes inmutables por SHA y despliegues con `kubectl set image`. El flujo `Security Scan` ejecuta Trivy sobre dependencias, secretos y configuración en cada PR, push a `main` y semanalmente.
13. **Configuración endurecida (A05):** sin credenciales por defecto, secretos obligatorios por entorno, `ddl-auto=validate` en producción, Actuator limitado a salud, errores sin detalles, CORS explícito, contenedores sin privilegios y cabeceras `Cache-Control: no-store` en la API.
14. **Registro y monitoreo (A09):** eventos de seguridad `security_event=...` en stdout para login, bloqueos y MFA; bitácora persistente con redacción de secretos y saneamiento de saltos de línea; sondas de salud de Kubernetes y auditoría de solo lectura.
15. **SSRF (A10):** la única petición saliente del backend es a la URL fija `https://challenges.cloudflare.com/turnstile/v0/siteverify`; no se aceptan URL, host ni ruta controlados por el usuario.

---

## 📜 Bitácora Administrativa (Auditoría)

El módulo de **Bitácora** garantiza la trazabilidad y rendición de cuentas en el sistema:

* **Registro Automático de Accesos:** Monitorea en `UsuarioController` los eventos `LOGIN_EXITOSO` y `LOGIN_FALLIDO` (tanto por credenciales erróneas como por campos incompletos), registrando la IP de origen.
* **Interceptor de Acciones (`BitacoraInterceptor`):** Registra automáticamente toda operación mutante (`POST`, `PUT`, `DELETE`, `PATCH`) en cualquier controlador o recurso del sistema.
* **Datos Auditados por Evento:**
  - `ID`: Identificador autoincremental único.
  - `Fecha y Hora`: Timestamp oficial del servidor.
  - `Usuario`: Correo de la cuenta autenticada (o *Anónimo*).
  - `Rol`: Rol verificado del usuario extraído del JWT.
  - `Acción`: Nombre estandarizado de la operación (ej. `CREAR_CITA`, `CANCELAR_CITA`, `ELIMINAR_MASCOTA`).
  - `Módulo`: Sección del sistema (`Citas`, `Mascotas`, `Usuarios`, `Tratamientos`, etc.).
  - `Detalle`: Método HTTP, URI y código de resultado (`exitoso` o error con código HTTP y mensaje).
  - `IP`: Dirección IP remota (con detección de proxies vía `X-Forwarded-For`).
* **Transacciones Independientes (`REQUIRES_NEW`):** Los logs se confirman en su propia transacción, asegurando que los eventos de error o excepciones queden guardados aunque el controlador principal haga rollback.
* **Solo lectura:** El controlador no expone `POST`, `PUT` ni `DELETE`. La entidad es inmutable y los disparadores `bitacora_append_only_update` / `bitacora_append_only_delete` rechazan cualquier modificación o borrado directo en la base de datos. El script `scripts/hardening.sql` los reinstala tras una restauración.

---

## 👥 Roles de Usuario

El sistema adapta sus vistas y permisos según el tipo de cuenta:

| Rol | ID | Alcance |
| :--- | :---: | :--- |
| **Administrador** | `1` | Control total del sistema: gestión de usuarios, personal, mascotas, citas, tratamientos, visor de bitácora y mantenimiento de tablas. |
| **Dueño de Mascota** | `2` | Portal de clientes: registro y consulta de sus propias mascotas, solicitud y cancelación de citas médicas y consulta de recetas/tratamientos activos. |
| **Gestor / Personal** | `3` | Administración operativa: control de agenda de citas, consulta de mascotas y asignación de tratamientos veterinarios. |

---

## 💻 Requisitos Previos

- **Java Development Kit (JDK) 21** o superior.
- **Node.js** (versión 20+ o 24 LTS) y gestor **npm**.
- **MariaDB** o **MySQL** (versión 10.5+).
- **Android Studio** con SDK instalado (si se desea compilar la app móvil).
- Shell **bash** o **fish**.

---

## 🔑 Configuración y Variables de Entorno

El backend requiere claves criptográficas seguras de 64 caracteres hexadecimales (256 bits).

Crea un archivo `.env` en la raíz de `devops-proyect/` (o exporta las variables de entorno en el sistema):

```dotenv
# Claves criptográficas en formato HEX (64 caracteres = 256 bits)
JWT_SECRET=tu_clave_secreta_hex_de_64_caracteres_para_firmar_tokens_jwt_aqui
AES_SECRET=tu_clave_secreta_hex_de_64_caracteres_para_cifrado_aes_gcm_aqui
HMAC_SECRET=tu_clave_secreta_hex_de_64_caracteres_para_validacion_hmac_aqui
SPRING_DATASOURCE_USERNAME=huellitas_app
SPRING_DATASOURCE_PASSWORD=usa_una_contrasena_larga_y_unica
# Solo necesario cuando se habilita Turnstile en el backend
TURNSTILE_ENABLED=true
TURNSTILE_SECRET=secreto_privado_de_cloudflare
TURNSTILE_EXPECTED_HOSTNAMES=app.example.com,dev-server.tail223158.ts.net
```

> **Tip:** Puedes generar claves válidas desde la terminal con OpenSSL:
> ```bash
> openssl rand -hex 32
> ```

---

## 📦 Instalación y Ejecución

### 1. Base de Datos (MariaDB)

Crea la base de datos y un usuario de aplicación con una contraseña fuerte. No uses credenciales predeterminadas ni publiques estas credenciales:

```sql
CREATE DATABASE IF NOT EXISTS huellitas_db;
CREATE USER IF NOT EXISTS 'huellitas_app'@'localhost' IDENTIFIED BY '<contraseña-fuerte-generada>';
GRANT SELECT, INSERT, UPDATE, DELETE ON huellitas_db.* TO 'huellitas_app'@'localhost';
FLUSH PRIVILEGES;
```

`SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` son obligatorias; la aplicación no incluye credenciales por defecto. `APP_CORS_ALLOWED_ORIGINS` permite configurar los orígenes para desarrollo; en producción, el frontend y la API se sirven desde el mismo origen a través del proxy inverso.

Registra todos los hostnames desde los que se servirá el login en la configuración del widget de Cloudflare Turnstile. Configura la **clave del sitio** como `VITE_TURNSTILE_SITE_KEY` al construir el frontend (por ejemplo, en un archivo `.env` local, no versionado) y guarda la **clave secreta** únicamente en el entorno del backend. En Kubernetes, añade `TURNSTILE_SECRET` y `TURNSTILE_EXPECTED_HOSTNAMES` al Secret `huellitas-secrets`; esta última es una lista separada por comas que debe coincidir con los hostnames registrados. En desarrollo local el widget es opcional; en el build de producción es obligatorio.

---

### 2. Backend (Spring Boot)

Entra a la carpeta del backend y levanta la aplicación con el perfil local (permite crear el esquema en desarrollo):

```bash
cd devops-proyect

# Compilar y ejecutar pruebas
./mvnw test-compile

# Iniciar servidor Spring Boot (puerto 8080 por defecto)
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

El perfil `prod` usa `spring.jpa.hibernate.ddl-auto=validate` y exige todas las variables de entorno. Los cambios de esquema deben aplicarse de forma controlada antes de desplegar; la aplicación no modifica tablas en producción.

---

### 3. Frontend Web (React + Vite)

Entra a la carpeta del frontend, instala dependencias e inicia el servidor de desarrollo:

```bash
cd devops-frontend

# Instalar dependencias
npm install

# Publica la clave del sitio de Turnstile al proceso de Vite
export VITE_TURNSTILE_SITE_KEY=clave-publica-de-cloudflare

# Iniciar servidor de desarrollo (puerto 5173 por defecto)
npm run dev
```

Abre tu navegador en `http://localhost:5173`.

---

### 4. Aplicación Móvil (Android)

Para compilar la aplicación móvil desde la línea de comandos:

```bash
cd devops-app-mobil

# Compilar APK de depuración
./gradlew :app:assembleDebug
```

El archivo APK generado estará disponible en:
`app/build/outputs/apk/debug/app-debug.apk`.

---

### 5. Instalación Automatizada con Scripts

En sistemas basados en Arch Linux, puedes usar los scripts incluidos en `scripts/`:

```bash
# Otorgar permisos de ejecución
chmod +x scripts/*.fish

# Ejecutar el instalador automático completo
./scripts/instalarelproyecto.fish
```

Para detener los servicios en segundo plano:

```bash
pkill -f "spring-boot" 2>/dev/null
pkill -f "node" 2>/dev/null
pkill -f "npm" 2>/dev/null
```

#### Respaldos cifrados

```bash
# Crear un respaldo cifrado (AES-256) con suma SHA-256
./scripts/backup.fish

# Restaurar el respaldo más reciente o uno concreto
./scripts/restore.fish --latest
./scripts/restore.fish ~/backups/huellitas_2026-01-01_12-00-00.sql.gpg
```

La clave de respaldo se guarda en `~/.config/huellitas/backup.key` con permisos `600`. Consérvela fuera del servidor: sin ella los respaldos no se pueden restaurar. El script de restauración verifica la suma SHA-256 antes de importar y reaplica los disparadores append-only de la bitácora.

---

### 6. Despliegue en Kubernetes detrás de un proxy inverso

El proxy inverso externo termina TLS y agrega las cabeceras de seguridad. El frontend del clúster escucha HTTP en el NodePort `30081`; configura el proxy para enviarle allí las solicitudes del sitio. El backend permanece como servicio interno y recibe las llamadas API mediante el frontend. Antes de desplegar, configura la variable de repositorio de GitHub Actions `VITE_TURNSTILE_SITE_KEY` y agrega `TURNSTILE_SECRET` y `TURNSTILE_EXPECTED_HOSTNAMES` al Secret `huellitas-secrets`. Esta lista debe contener todos y solo los hostnames registrados en Turnstile, separados por comas y sin protocolo ni rutas.

---

## 📄 Licencia

Este proyecto se encuentra bajo la licencia **MIT**. Consulta el archivo `LICENSE` para más detalles.
