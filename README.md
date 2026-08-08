# CareerPilot

An AI-powered job portal built as a Spring Boot microservice platform with a
React (Vite) front end. Job seekers search and apply for jobs, employers post
jobs and screen applicants, and a platform administrator manages users,
employers, subscriptions and the AI features. Google Gemini powers resume
analysis, cover-letter generation, job recommendations and candidate screening.

This guide assumes you have never seen the project before. Follow it top to
bottom and it will run.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Prerequisites](#3-prerequisites)
4. [Get the Code](#4-get-the-code)
5. [External Services (API Keys and Credentials)](#5-external-services-api-keys-and-credentials)
6. [Environment Variables](#6-environment-variables)
7. [Database Setup](#7-database-setup)
8. [Configuration Files](#8-configuration-files)
9. [How to Run](#9-how-to-run)
10. [Project URLs](#10-project-urls)
11. [Login Credentials](#11-login-credentials)
12. [Verification / Smoke Test](#12-verification--smoke-test)
13. [Troubleshooting](#13-troubleshooting)
14. [Project Structure](#14-project-structure)

---

## 1. Project Overview

CareerPilot has three user roles:

| Role | What they can do |
|---|---|
| **Job Seeker** | Search and browse jobs, save jobs, upload a resume, apply, track applications, use AI resume analysis / cover letters / job recommendations |
| **Employer** | Register a company (needs admin approval), post and publish jobs, view applicants, download resumes, change application status, use AI candidate screening |
| **Admin** | Approve or revoke employers, manage users, monitor jobs and applications, manage subscriptions, turn AI features on and off |

The backend is split into eight Spring Boot applications: two infrastructure
services (Config Server, Eureka), one API Gateway, and six business services
(auth, user, job, application, notification, ai). The browser only ever talks
to the API Gateway on port 8080.

---

## 2. Technology Stack

**Backend**

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.x |
| Microservices | Spring Cloud (Config Server, Eureka, Gateway, OpenFeign) |
| Security | Spring Security + JWT (JJWT) |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL 8 |
| API docs | springdoc-openapi 2.6 (Swagger UI) |
| Build | Maven (multi-module) |
| Email | Spring Mail (Gmail SMTP) |
| Payments | Razorpay (Premium subscriptions) |
| AI | Google Gemini API |

**Frontend**

| Component | Technology |
|---|---|
| Framework | React 19 |
| Build tool | Vite 8 |
| Routing | React Router 7 |
| HTTP | Axios |
| UI | Bootstrap 5 + Bootstrap Icons |
| Notifications | react-toastify |
| Linting | oxlint |

---

## 3. Prerequisites

Install these before you start.

| Requirement | Version | How to check | Download |
|---|---|---|---|
| **JDK** | **21** (required — the build sets `<release>21</release>`) | `java -version` must print `21.x` | <https://adoptium.net/temurin/releases/?version=21> |
| **Maven** | **3.9+** | `mvn -v` | <https://maven.apache.org/download.cgi> (or use your IDE's bundled Maven) |
| **Node.js** | **20.19+ or 22.12+** (required by Vite 8) | `node -v` | <https://nodejs.org/en/download> |
| **npm** | **10+** (ships with Node) | `npm -v` | — |
| **MySQL** | **8.0+**, running on `localhost:3306` | `mysql --version` | <https://dev.mysql.com/downloads/mysql/> |
| **Git** | any recent version | `git --version` | <https://git-scm.com/downloads> |

> **Git is mandatory**, not optional. The Spring Cloud Config Server reads
> configuration out of a **git repository**. If `careerpilot-config-repo` is not
> a git repo with at least one commit, Config Server fails to start and every
> other service fails with it.

**Recommended IDEs**

| Part of the project | Recommended IDE |
|---|---|
| Backend (Java / Spring Boot) | IntelliJ IDEA (Community is enough) or Spring Tool Suite (STS) 4 / Eclipse |
| Frontend (React) | Visual Studio Code |

- In **IntelliJ**: `File → Open` → select `careerpilot-backend/pom.xml` → *Open as Project*. Set `File → Project Structure → SDK` to **JDK 21**.
- In **STS/Eclipse**: `File → Import → Maven → Existing Maven Projects` → select `careerpilot-backend`.
- In **VS Code**: open the `client` folder. Suggested extensions: *ESLint*, *Prettier*, *ES7+ React snippets*.

Docker is **not** required.

---

## 4. Get the Code

Unzip the project. You should end up with this layout:

```
CareerPilot/
├── careerpilot-backend/        # Maven multi-module backend
├── careerpilot-config-repo/    # Configuration served by the Config Server
├── client/                     # React frontend
├── scripts/                    # Smoke tests
├── setup-config-repo.sh        # Config repo initialiser (Linux/macOS)
├── setup-config-repo.bat       # Config repo initialiser (Windows)
└── README.md
```

All paths in this guide are relative to the `CareerPilot/` folder.

---

## 5. External Services (API Keys and Credentials)

### 5.1 Google Gemini API Key (required for AI features)

1. Go to **<https://aistudio.google.com/apikey>** and sign in with a Google account.
2. Click **Create API key** and pick (or create) a Google Cloud project.
3. Copy the key. Current keys look like `AQ.Ab8...`; older keys look like `AIza...`. Both work.
4. Set it as the `GEMINI_API_KEY` environment variable (see [section 6](#6-environment-variables)).

Docs: <https://ai.google.dev/gemini-api/docs/api-key>

> **Never paste the key into `careerpilot-config-repo/ai-service.yml`.** That
> file is committed to git. The key is read from the environment only.

Verify the key without opening the app:

```bash
curl -s https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent \
  -H "x-goog-api-key: $GEMINI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"contents":[{"parts":[{"text":"say hi"}]}]}'
```

A JSON response containing `candidates` means the key works.

If you skip this step the app still runs — only the AI screens return a clear
"AI is not configured" message.

### 5.2 Gmail App Password (required to actually send email)

CareerPilot emails users on registration, on password reset (OTP), and when an
application's status changes. Gmail will not accept your normal password, so
you need a 16-character **App Password**.

1. Turn on **2-Step Verification** for the Google account: <https://myaccount.google.com/signinoptions/two-step-verification>
2. Open **<https://myaccount.google.com/apppasswords>**
3. Enter a name such as `CareerPilot` and click **Create**
4. Copy the 16-character password that appears (for example `abcd efgh ijkl mnop`) — **remove the spaces** when you use it
5. Set the SMTP environment variables:

```
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your.address@gmail.com
SMTP_PASSWORD=abcdefghijklmnop      # the App Password, no spaces
SMTP_FROM=your.address@gmail.com
```

Docs: <https://support.google.com/accounts/answer/185833>

> **Email is optional for local development.** If `SMTP_HOST` and
> `SMTP_USERNAME` are left blank, `notification-service` prints the email to the
> console instead of sending it. That is enough to read a password-reset OTP
> while developing.

### 5.3 Razorpay Keys (optional — Premium checkout)

Premium subscriptions use Razorpay. Test-mode placeholders are configured by
default; the Premium page works end-to-end only with real test keys.

1. Sign up at <https://dashboard.razorpay.com/signup>
2. Stay in **Test Mode**
3. Go to **Account & Settings → API Keys → Generate Test Key**
4. Set `RAZORPAY_KEY_ID` (starts with `rzp_test_`) and `RAZORPAY_KEY_SECRET`

Docs: <https://razorpay.com/docs/payments/dashboard/account-settings/api-keys/>

### 5.4 JWT Secret (required for anything beyond a local demo)

A development fallback secret exists so the platform runs out of the box, but
you should set your own. It must be **at least 32 characters**.

```bash
# Linux / macOS - generate one
openssl rand -base64 48
```

```powershell
# Windows PowerShell - generate one
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Max 256 }))
```

Set the result as `JWT_SECRET`.

---

## 6. Environment Variables

Every backend service must be started from a shell where these are set.

### 6.1 Full list

| Variable | Required? | Default | Used by | Purpose |
|---|---|---|---|---|
| `DB_HOST` | No | `localhost` | all data services | MySQL host |
| `DB_PORT` | No | `3306` | all data services | MySQL port |
| `DB_USERNAME` | **Yes** (if not `root`) | `root` | all data services | MySQL user |
| `DB_PASSWORD` | **Yes** (if not `root`) | `root` | all data services | MySQL password |
| `JWT_SECRET` | **Recommended** | dev placeholder | all services | HMAC key that signs/validates JWTs — must be identical across every service |
| `JWT_EXPIRY_MINUTES` | No | `60` | auth-service | Access-token lifetime |
| `GEMINI_API_KEY` | **Yes, for AI** | *(empty)* | ai-service | Google Gemini key |
| `GEMINI_MODEL` | No | `gemini-3.5-flash` | ai-service | Gemini model name |
| `GEMINI_BASE_URL` | No | `https://generativelanguage.googleapis.com/v1beta` | ai-service | Gemini endpoint |
| `GEMINI_TEMPERATURE` | No | `0.4` | ai-service | Model creativity |
| `SMTP_HOST` | No | *(empty → console mode)* | notification-service | `smtp.gmail.com` |
| `SMTP_PORT` | No | `587` | notification-service | SMTP port |
| `SMTP_USERNAME` | No | *(empty)* | notification-service | Gmail address |
| `SMTP_PASSWORD` | No | *(empty)* | notification-service | **Gmail App Password** (not your login password) |
| `SMTP_FROM` | No | `noreply@careerpilot.com` | notification-service | From address |
| `ADMIN_EMAIL` | No | `admin@careerpilot.com` | auth-service | Seeded admin login |
| `ADMIN_PASSWORD` | No | `CareerPilot@123` | auth-service | Seeded admin password |
| `ADMIN_FIRST_NAME` | No | `Platform` | auth-service | Admin first name |
| `ADMIN_LAST_NAME` | No | `Administrator` | auth-service | Admin last name |
| `ADMIN_RESET_PASSWORD` | No | `false` | auth-service | Set `true` once to reset a forgotten admin password |
| `RAZORPAY_KEY_ID` | No | `rzp_test_placeholder` | auth-service | Razorpay key id |
| `RAZORPAY_KEY_SECRET` | No | `placeholder_secret` | auth-service | Razorpay secret |
| `RAZORPAY_WEBHOOK_SECRET` | No | *(empty)* | auth-service | Razorpay webhook signature secret |
| `RAZORPAY_PREMIUM_AMOUNT_PAISE` | No | `49900` (₹499) | auth-service | Premium price |
| `RAZORPAY_PREMIUM_DURATION_DAYS` | No | `30` | auth-service | Premium duration |
| `UPLOAD_DIR` | No | `./uploads` | user-service | Where resume PDFs are stored |
| `EUREKA_URI` | No | `http://localhost:8761/eureka/` | all services | Service registry URL |
| `CONFIG_REPO_URI` | No | auto-discovered local folder | config-server | Git URI of the config repo |
| `CONFIG_REPO_USERNAME` / `CONFIG_REPO_PASSWORD` | No | *(empty)* | config-server | Credentials for a private config repo |
| `VITE_API_BASE_URL` | No | `http://localhost:8080/api` | client | Gateway base URL (set in `client/.env`) |

### 6.2 Setting them — Linux / macOS

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=replace_me_with_a_private_random_string_at_least_32_chars
export GEMINI_API_KEY=AQ.your_key_here

# Optional - real email delivery
export SMTP_HOST=smtp.gmail.com
export SMTP_PORT=587
export SMTP_USERNAME=your.address@gmail.com
export SMTP_PASSWORD=your16charapppassword
export SMTP_FROM=your.address@gmail.com
```

Put these in `~/.bashrc` or `~/.zshrc` so every new terminal has them — you will
be opening several.

### 6.3 Setting them — Windows PowerShell

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="replace_me_with_a_private_random_string_at_least_32_chars"
$env:GEMINI_API_KEY="AQ.your_key_here"

$env:SMTP_HOST="smtp.gmail.com"
$env:SMTP_PORT="587"
$env:SMTP_USERNAME="your.address@gmail.com"
$env:SMTP_PASSWORD="your16charapppassword"
$env:SMTP_FROM="your.address@gmail.com"
```

`$env:` variables only live in the current PowerShell window. To make them
permanent use `setx JWT_SECRET "..."` and open a new terminal, or set them under
*System Properties → Environment Variables*.

### 6.4 Setting them in an IDE

- **IntelliJ IDEA**: `Run → Edit Configurations…` → select the service →
  *Environment variables* → paste `DB_PASSWORD=...;JWT_SECRET=...;GEMINI_API_KEY=...`.
  Do this for each of the eight run configurations, or define one and copy it.
- **STS / Eclipse**: `Run Configurations… → Spring Boot App → Environment tab → Add`.

---

## 7. Database Setup

### 7.1 You do not create the schema by hand

Every service creates its own database and tables on first start.
`createDatabaseIfNotExist=true` in the JDBC URL creates the database, and
Hibernate's `ddl-auto: update` creates and evolves the tables. **There is no
SQL script to import.**

You only need MySQL running and correct credentials.

### 7.2 Databases that will be created

| Database | Owning service |
|---|---|
| `careerpilot_auth` | auth-service |
| `careerpilot_user` | user-service |
| `careerpilot_job` | job-service |
| `careerpilot_application` | application-service |
| `careerpilot_notification` | notification-service |
| `careerpilot_ai` | ai-service |

### 7.3 Confirm MySQL is reachable

```bash
mysql -u root -p -e "SELECT VERSION();"
```

### 7.4 Optional — use a dedicated MySQL user instead of root

```sql
CREATE USER 'careerpilot'@'localhost' IDENTIFIED BY 'StrongPassword123!';
GRANT ALL PRIVILEGES ON `careerpilot\_%`.* TO 'careerpilot'@'localhost';
GRANT CREATE ON *.* TO 'careerpilot'@'localhost';
FLUSH PRIVILEGES;
```

Then set `DB_USERNAME=careerpilot` and `DB_PASSWORD=StrongPassword123!`.

### 7.5 Starting over

```sql
DROP DATABASE careerpilot_auth;
DROP DATABASE careerpilot_user;
DROP DATABASE careerpilot_job;
DROP DATABASE careerpilot_application;
DROP DATABASE careerpilot_notification;
DROP DATABASE careerpilot_ai;
```

They are recreated on the next start, and the admin account is re-seeded.

---

## 8. Configuration Files

You normally edit **none** of these — everything above is driven by environment
variables. This section tells you what each file is for, in case you need to.

| File | What it controls | When you would edit it |
|---|---|---|
| `careerpilot-config-repo/application.yml` | Shared defaults: JWT secret/expiry, Eureka URL | Rarely; prefer `JWT_SECRET` |
| `careerpilot-config-repo/auth-service.yml` | Port 8081, auth DB, Razorpay, seeded admin account | Change admin defaults, Razorpay pricing |
| `careerpilot-config-repo/user-service.yml` | Port 8082, user DB, 10 MB upload limit, resume upload dir | Raise the resume size limit |
| `careerpilot-config-repo/job-service.yml` | Port 8083, job DB | Rarely |
| `careerpilot-config-repo/application-service.yml` | Port 8084, application DB, Feign timeouts | Rarely |
| `careerpilot-config-repo/notification-service.yml` | Port 8085, notification DB, **SMTP settings** | Prefer `SMTP_*` env vars |
| `careerpilot-config-repo/ai-service.yml` | Port 8086, AI DB, **Gemini model/base URL** | Change the Gemini model |
| `careerpilot-config-repo/api-gateway.yml` | Port 8080, **CORS allowed origins**, all route definitions | Add a frontend origin/port |
| `careerpilot-config-repo/eureka-server.yml` | Port 8761 | Rarely |
| `careerpilot-backend/config-server/src/main/resources/application.yml` | Port 8888, where the config git repo lives | Point at a remote config repo |
| `client/.env` | `VITE_API_BASE_URL` | Only if the gateway is not on 8080 |

> **Critical:** the Config Server serves **committed** git content. After editing
> anything under `careerpilot-config-repo/`, re-run `setup-config-repo.sh` /
> `setup-config-repo.bat` and restart the affected service. Otherwise your change
> is silently ignored.

### `client/.env`

```
VITE_API_BASE_URL=http://localhost:8080/api
```

The file already exists. If it is missing, copy `client/.env.example` to
`client/.env`. Vite only reads `.env` at startup — restart `npm run dev` after
changing it.

---

## 9. How to Run

### Step 0 — Start MySQL

```bash
# Linux
sudo systemctl start mysql

# macOS (Homebrew)
brew services start mysql
```

On Windows, start the **MySQL80** service from *Services*, or use MySQL Workbench.

### Step 1 — Initialise the config repository (once)

From the `CareerPilot/` folder:

```bash
# Linux / macOS
chmod +x setup-config-repo.sh
./setup-config-repo.sh
```

```bat
REM Windows
setup-config-repo.bat
```

You should see `Initialised config repo on branch 'main'.` followed by a commit
hash. Re-run this any time you edit a file under `careerpilot-config-repo/`.

### Step 2 — Build the backend

```bash
cd careerpilot-backend
mvn clean install -DskipTests
```

`mvn clean install` (not `package`) is required the first time: the `common`
module has to be installed into your local Maven repository before the other
modules can resolve it.

Expected ending:

```
[INFO] BUILD SUCCESS
```

Run the tests too if you want: `mvn clean install`.

### Step 3 — Start the backend services, in order

**Order matters.** Config Server → Eureka → Gateway → business services. Each
command runs in **its own terminal** and stays in the foreground. Wait for the
`Started ...Application` line before moving on to the next one.

```bash
cd careerpilot-backend

# 1. Config Server (8888) - wait for "Started ConfigServerApplication"
mvn -pl config-server spring-boot:run

# 2. Eureka Server (8761) - wait for "Started EurekaServerApplication"
mvn -pl eureka-server spring-boot:run

# 3. API Gateway (8080)
mvn -pl api-gateway spring-boot:run

# 4. Business services - these six can start in any order, in parallel
mvn -pl auth-service         spring-boot:run   # 8081
mvn -pl user-service         spring-boot:run   # 8082
mvn -pl job-service          spring-boot:run   # 8083
mvn -pl application-service  spring-boot:run   # 8084
mvn -pl notification-service spring-boot:run   # 8085
mvn -pl ai-service           spring-boot:run   # 8086
```

That is **eight** terminals. Alternatively run the packaged jars after
`mvn clean install`:

```bash
java -jar config-server/target/config-server-1.0.0.jar
java -jar eureka-server/target/eureka-server-1.0.0.jar
java -jar api-gateway/target/api-gateway-1.0.0.jar
java -jar auth-service/target/auth-service-1.0.0.jar
java -jar user-service/target/user-service-1.0.0.jar
java -jar job-service/target/job-service-1.0.0.jar
java -jar application-service/target/application-service-1.0.0.jar
java -jar notification-service/target/notification-service-1.0.0.jar
java -jar ai-service/target/ai-service-1.0.0.jar
```

From an IDE, run each `*Application` main class in the same order.

**Checkpoint:** open <http://localhost:8761>. All seven registrable services
(gateway, auth, user, job, application, notification, ai) must appear under
*Instances currently registered with Eureka*. If one is missing, the frontend
will fail on the features that need it.

### Step 4 — Start the frontend

In a ninth terminal:

```bash
cd client
npm install
npm run dev
```

Open **<http://localhost:5173>**.

Other frontend commands:

```bash
npm run build     # production build into client/dist
npm run preview   # serve the production build on 4173
npm run lint      # oxlint
```

### Complete run order (summary)

1. MySQL
2. `setup-config-repo.sh` / `.bat` — first time only
3. Config Server — 8888
4. Eureka Server — 8761
5. API Gateway — 8080
6. auth, user, job, application, notification, ai — 8081–8086
7. Frontend — `npm run dev`, 5173

---

## 10. Project URLs

| Service | URL |
|---|---|
| **Frontend (React)** | <http://localhost:5173> |
| **API Gateway** | <http://localhost:8080> |
| **Eureka dashboard** | <http://localhost:8761> |
| **Config Server** | <http://localhost:8888> |
| auth-service | <http://localhost:8081> |
| user-service | <http://localhost:8082> |
| job-service | <http://localhost:8083> |
| application-service | <http://localhost:8084> |
| notification-service | <http://localhost:8085> |
| ai-service | <http://localhost:8086> |

**Swagger UI** — per service, on its own port (deliberately *not* routed through
the gateway):

| Service | Swagger UI |
|---|---|
| auth-service | <http://localhost:8081/swagger-ui.html> |
| user-service | <http://localhost:8082/swagger-ui.html> |
| job-service | <http://localhost:8083/swagger-ui.html> |
| application-service | <http://localhost:8084/swagger-ui.html> |
| ai-service | <http://localhost:8086/swagger-ui.html> |

To call a secured endpoint in Swagger: `POST /api/auth/login`, copy the
`accessToken` from the response, click **Authorize**, paste it.

**Useful checks**

| What | URL |
|---|---|
| Config Server health | <http://localhost:8888/actuator/health> |
| Gateway health | <http://localhost:8080/actuator/health> |
| Gateway route list | <http://localhost:8080/actuator/gateway/routes> |
| Config served to auth-service | <http://localhost:8888/auth-service/default> |

---

## 11. Login Credentials

### Administrator — created automatically

There is **no admin registration screen**. `auth-service` seeds the account on
every startup.

```
Email:    admin@careerpilot.com
Password: CareerPilot@123
```

Log in at <http://localhost:5173/login>; you land on `/admin/dashboard`.

Change it with `ADMIN_EMAIL` / `ADMIN_PASSWORD` **before the first start**. If
you change the password from inside the app and forget it, set
`ADMIN_RESET_PASSWORD=true`, restart `auth-service` once, then unset it.

### Job Seeker and Employer — you create them

No job-seeker or employer accounts are pre-seeded. Register them yourself at
<http://localhost:5173/register> and pick the role on the form.
`POST /api/auth/register` only accepts `JobSeeker` and `Employer`.

**To get a working employer end-to-end:**

1. Register an **Employer** account and log in.
2. Go to **Company Profile** and register the company.
3. Log in as the **admin**, open **Employers**, and click **Approve**.
4. Log back in as the employer — you can now create *and publish* jobs.
   (An unapproved employer can create a job but not publish it. This is intended.)

**To exercise the full flow:**

5. Register a **Job Seeker**, upload a resume under **Resume**, then apply to the published job.
6. As the employer, open **Manage Jobs → view** to see the applicant, download the resume, and change the application status.

**AI features** additionally require an active Premium subscription (or the
admin turning *Require Premium* off under **Admin → AI**).

---

## 12. Verification / Smoke Test

A script exercises login for all three roles, the admin endpoints, role
isolation, and the Gemini configuration.

```bash
# Linux / macOS  (needs curl and jq)
chmod +x scripts/smoke-test.sh
./scripts/smoke-test.sh
```

```powershell
# Windows
powershell -ExecutionPolicy Bypass -File scripts\smoke-test.ps1
```

Run it after everything in section 9 is up.

---

## 13. Troubleshooting

### Startup and configuration

| Symptom | Cause | Fix |
|---|---|---|
| `Could not locate PropertySource` / service exits immediately | Config Server is not running, or the config repo has no commits | Start Config Server first (8888); run `setup-config-repo.sh` / `.bat` |
| A config change has no effect | Your edits were never committed to the config repo | Re-run `setup-config-repo.sh` / `.bat`, then restart the service |
| `Invalid config server` / `repo not found` | Config repo folder not found | Run the setup script from the `CareerPilot/` folder, or set `CONFIG_REPO_URI` |
| `No instances available for <service>` | The service is not registered yet | Check <http://localhost:8761>; start the missing service; wait ~30 s after startup |
| Services start in the wrong order and fail | Order matters | Config Server → Eureka → Gateway → business services |

### Port already in use

`Web server failed to start. Port 8081 was already in use.`

```bash
# Linux / macOS - find and kill
lsof -i :8081
kill -9 <PID>
```

```powershell
# Windows - find and kill
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

Ports used: **3306** (MySQL), **4173** (vite preview), **5173** (frontend),
**8080** (gateway), **8081–8086** (services), **8761** (Eureka), **8888** (Config Server).

To move a service instead of killing the process, change `server.port` in that
service's file under `careerpilot-config-repo/`, re-run the setup script, and
restart.

### Database

| Symptom | Cause | Fix |
|---|---|---|
| `Access denied for user 'root'@'localhost'` | Wrong DB credentials | Set `DB_USERNAME` / `DB_PASSWORD` and restart the service from that same shell |
| `Communications link failure` / `Connection refused` | MySQL not running or wrong port | Start MySQL; check `DB_HOST` / `DB_PORT`; `mysql -u root -p -e "SELECT 1"` |
| `Unknown database 'careerpilot_auth'` | The user cannot create databases | Grant `CREATE`, or create the six databases manually (see 7.2) |
| `Public Key Retrieval is not allowed` | MySQL 8 auth plugin | Already handled by `allowPublicKeyRetrieval=true` in the JDBC URL — check you have not overridden the URL |
| `Table ... doesn't exist` after a schema change | Stale tables | Drop the six databases (7.5) and restart |

### JWT and authentication

| Symptom | Cause | Fix |
|---|---|---|
| 401 on every API call right after logging in | Services are using **different** `JWT_SECRET` values | Export the same `JWT_SECRET` in **every** terminal, restart all services |
| Logged out after an hour | Token expiry | Normal — the client refreshes automatically; raise `JWT_EXPIRY_MINUTES` if you want longer |
| `io.jsonwebtoken.security.WeakKeyException` | `JWT_SECRET` shorter than 32 characters | Use a longer secret |
| 403 "You do not have permission" | Correct behaviour — wrong role for that endpoint | Log in as the right role |
| Employer cannot publish a job | The company is not approved yet | Approve it as admin under **Employers** |

### CORS

| Symptom | Fix |
|---|---|
| `blocked by CORS policy` in the browser console | Vite fell back to 5174/5175 because 5173 was taken. Free port 5173, **or** add your port to `globalcors.allowedOrigins` in `careerpilot-config-repo/api-gateway.yml`, re-run the setup script, and restart the gateway |
| Requests go to port 5000 / a wrong host | Fix `VITE_API_BASE_URL` in `client/.env` and restart `npm run dev` |

### Gemini AI

| Symptom | Cause | Fix |
|---|---|---|
| "AI is not configured correctly" | `GEMINI_API_KEY` unset or invalid | Export the key, restart `ai-service`, verify with the curl in 5.1 |
| HTTP 401 `ACCESS_TOKEN_TYPE_UNSUPPORTED` | The key was sent as `?key=` instead of the `x-goog-api-key` header | The app already sends the header — if you are testing by hand, use the header form |
| "model that no longer exists" / 404 | `GEMINI_MODEL` points at a retired model | Unset `GEMINI_MODEL` (defaults to `gemini-3.5-flash`) or set a current one from <https://ai.google.dev/gemini-api/docs/models> |
| HTTP 429 | Free-tier rate limit | Wait a minute, or enable billing on the Google Cloud project |
| "requires an active Premium subscription" | Working as designed | Buy Premium, or turn off *Require Premium* under **Admin → AI** |
| Candidate screening returns fewer results than applicants | Applicants without a resume, and withdrawn applications, are skipped | Expected behaviour |

### Gmail SMTP

| Symptom | Cause | Fix |
|---|---|---|
| `535-5.7.8 Username and Password not accepted` | Using your Google password instead of an App Password | Generate an App Password (5.2) and use it as `SMTP_PASSWORD`, with no spaces |
| `Must issue a STARTTLS command first` | Wrong port | Use `SMTP_PORT=587` |
| No email arrives, but no error either | SMTP is unconfigured | Expected — the email is printed to the `notification-service` console. Set `SMTP_HOST` / `SMTP_USERNAME` / `SMTP_PASSWORD` to send for real |
| App Passwords option is missing in Google | 2-Step Verification is off | Enable 2-Step Verification first |
| Connection times out | Corporate network or ISP blocks port 587 | Try another network |

### Maven build

| Symptom | Cause | Fix |
|---|---|---|
| `release version 21 not supported` / `invalid target release` | Wrong JDK | Install JDK 21 and point `JAVA_HOME` at it; `mvn -v` must report 21 |
| `Could not resolve dependencies for ... common` | `common` not installed locally | Run `mvn clean install -DskipTests` from `careerpilot-backend`, not `mvn package` |
| Build hangs downloading dependencies | First build fetches the whole Spring dependency tree | Let it finish; on a bad connection retry — Maven resumes |
| `Could not transfer artifact ... Connection timed out` | No internet / behind a proxy | Configure a proxy in `~/.m2/settings.xml` |
| Corrupted jar / `Checksum validation failed` | Bad partial download | Delete the offending folder under `~/.m2/repository` and rebuild |
| `mvn: command not found` | Maven not on PATH | Install Maven, or use `./mvnw` / the IDE's bundled Maven |

### npm / frontend

| Symptom | Cause | Fix |
|---|---|---|
| `npm install` fails with `EACCES` | Permission problem | Do not use `sudo npm`; fix npm's folder permissions, or use nvm |
| `Unsupported engine` / Vite refuses to start | Node too old | Install Node 20.19+ or 22.12+ |
| `ERESOLVE unable to resolve dependency tree` | Peer-dependency conflict | `rm -rf node_modules package-lock.json && npm install` |
| `EADDRINUSE :::5173` | Port taken | Kill the process, or `npm run dev -- --port 5174` **and** add that origin to the gateway CORS list |
| Blank white page, console shows chunk errors | Stale Vite cache | `rm -rf node_modules/.vite` and restart |
| `Network Error` on every request | Backend is not up, or the gateway is not on 8080 | Check <http://localhost:8080/actuator/health> and `client/.env` |

### General

| Symptom | Fix |
|---|---|
| An action succeeds but the UI shows an error | Check the service's console — every unhandled 500 is now logged with a stack trace by `GlobalExceptionHandler` |
| Admin login fails | The password was changed. Set `ADMIN_RESET_PASSWORD=true`, restart `auth-service` once, unset it |
| Resume upload fails | Files must be PDF and under 10 MB (`spring.servlet.multipart` in `user-service.yml`) |
| Resumes disappear after a restart | They are on local disk under `UPLOAD_DIR` (`./uploads`), relative to the service's working directory — start the service from the same folder each time |

---

## 14. Project Structure

```
CareerPilot/
├── careerpilot-backend/
│   ├── pom.xml                     # Parent POM - Java 21, Spring Boot / Spring Cloud versions
│   ├── common/                     # Shared: ApiResponse, GlobalExceptionHandler, JWT filter, OpenAPI config
│   ├── config-server/              # 8888 - serves careerpilot-config-repo over HTTP
│   ├── eureka-server/              # 8761 - service registry
│   ├── api-gateway/                # 8080 - the only port the browser talks to
│   ├── auth-service/               # 8081 - auth, users, admin, dashboards, subscriptions
│   ├── user-service/               # 8082 - profiles, companies, resumes
│   ├── job-service/                # 8083 - jobs, bookmarks
│   ├── application-service/        # 8084 - applications, status, employer resume access
│   ├── notification-service/       # 8085 - email (internal only, no gateway route)
│   └── ai-service/                 # 8086 - Gemini-backed AI features
├── careerpilot-config-repo/        # Per-service YAML, served by config-server (must be a git repo)
├── client/                         # React 19 + Vite frontend
│   ├── .env                        # VITE_API_BASE_URL
│   └── src/
│       ├── api/                    # axios instance + endpoint wrappers
│       ├── components/             # Navbar, Footer, guards, shared AI panels
│       ├── context/                # AuthContext
│       └── pages/                  # public / jobseeker / employer / admin screens
├── scripts/                        # smoke-test.sh, smoke-test.ps1
├── setup-config-repo.sh
└── setup-config-repo.bat
```

**Response envelope.** Every JSON endpoint returns the same shape, which is what
the React client parses:

```json
{ "success": true, "message": "Job published", "data": null }
```

`data` is `null` for commands that have nothing to return.
