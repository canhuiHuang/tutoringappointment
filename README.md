# Tutoring appointments - Milestone 4

Java 21, Spring Boot, React + TypeScript, PostgreSQL, and hand-written JDBC SQL.

Includes session login with BCrypt, role checks, filtered/paginated slots, booking, cancellation,
appointment history, provider availability management, validation, global errors, and concurrency protection.
The database uses STUDENT for customers and TUTOR for providers.

Milestone 3 adds an HTTP notification gateway and mock service, commit-then-notify,
timeouts/retries/fallback, `/health`, `/metrics`, Docker Compose, and GitHub Actions tests.

Milestone 4 adds a Gemini/Spring AI booking assistant, confirmed writes, scheduled AI
reminders, and policy Q&A using SimpleVectorStore. AI is off until you supply your key.
For local development, add these settings to the root `.env` and restart the backend:

```properties
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
AI_ENABLED=true
AI_REMINDERS_ENABLED=false
```

Open **Assistant** after logging in as a customer. Set `AI_REMINDERS_ENABLED=true`
to enable the scheduled reminders. Keep the API key out of Git and the frontend.

## Run with Docker (frontend + backend + PostgreSQL)

Install Docker Desktop with Linux containers and start it. From the repository root:

```powershell
$env:DB_PASSWORD = "choose-a-local-database-password"
docker compose up --build -d
docker compose logs -f app
```

Open **http://localhost:8062**. The app image includes the React build, so Node.js, Java,
and a separate local PostgreSQL installation are not needed for this option. Compose
creates `tutoringdb`, waits for the database to become ready, and starts the app.
The mock notification controller runs in the same app but is called through HTTP.

```powershell
Invoke-RestMethod http://localhost:8062/health
Invoke-RestMethod http://localhost:8062/metrics
docker compose down
```

The named `postgres-data` volume preserves the database after `down`. Keep the same
database password when restarting an existing volume. Stop any local backend using
port 8062 before starting Compose. The commands below are the alternative development setup.

## Prerequisites

- JDK 21.
- Node.js 24 and npm for the frontend.
- PostgreSQL installed and running (the local setup uses port 5432).
- Git to clone the repository.

Maven does not need to be installed separately; the repository includes the Maven Wrapper.
The commands below use Windows PowerShell.

## Run locally

### 1. Clone the repository

```powershell
git clone https://github.com/canhuiHuang/tutoringappointment.git
cd tutoringappointment
```

### 2. Create and configure the database

Start PostgreSQL. In pgAdmin, open the Query Tool on the existing `postgres` database,
or connect to it with psql. Run this statement once, outside a transaction:

```sql
CREATE DATABASE tutoringdb;
```

The database must exist before Spring Boot starts. `schema.sql` creates tables inside
it; it cannot create the database that Spring needs to connect to.

Edit `src/main/resources/application.properties` to match your PostgreSQL installation:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/tutoringdb}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:root}
```

Set environment variables for your PostgreSQL password and change the username,
host, or port if needed. The local defaults currently use username `postgres`
and password `root`; these must match your own installation. Use a database account
with permission to create tables and insert/update data in `tutoringdb`.

Alternatively, override the credentials for the current PowerShell terminal:

```powershell
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "YOUR_POSTGRES_PASSWORD"
```

These environment variables configure the connection when you launch the backend from
that terminal. To use another database URL, set `$env:DB_URL` as well.

### 3. Start the backend

From the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

Spring Boot runs schema.sql and then seed.sql automatically.
The SQL inserts missing accounts, provider profiles, services, and initial slots without changing existing passwords or bookings.
The backend runs at http://localhost:8062.

### 4. Start the frontend

Open a second terminal at the repository root:

```powershell
cd frontend
npm ci
npm run dev -- --port 8061
```

Open Vite's URL, normally http://localhost:8061. Spring Boot serves JSON on port 8062.
Keep both terminals running.

## Sample accounts

All seeded passwords are 1234. seed.sql stores only BCrypt hashes.

| Username       | Role / service                          |
| -------------- | --------------------------------------- |
| john@sjsu.edu  | Customer                                |
| alice@sjsu.edu | Provider / Algebra, 60 minutes          |
| bob@sjsu.edu   | Provider / Java Programming, 90 minutes |
| emma@sjsu.edu  | Provider / Physics, 60 minutes          |

## Test

Run backend tests from the repository root:

```powershell
.\mvnw.cmd test
```

Tests use a separate H2 database and do not require a running backend or PostgreSQL.
To build the backend and check the frontend:

```powershell
.\mvnw.cmd package
cd frontend
npm run build
npm run lint
```

Tests use H2 in PostgreSQL mode. `BookingServiceTests` covers booking rules, owner-only cancellation,
and validation. `BookingIntegrationTests` runs two concurrent booking attempts and asserts exactly one succeeds.
Run only the unit tests with `.\mvnw.cmd test "-Dtest=BookingServiceTests"`.
Run only the concurrency test with `.\mvnw.cmd test "-Dtest=BookingIntegrationTests"`.
Results are in target/surefire-reports.

Milestone 3 tests also cover booking/cancellation during notification downtime, rollback
without notification, committed changes visible before notification, retry payload stability,
timeout fallback, persistent duplicate suppression, and health responses.

## Notifications and observability

`BookingService` publishes a `NotificationRequest` inside its database transaction.
`BookingNotifications` handles it with `@TransactionalEventListener(AFTER_COMMIT)`.
Only after commit does `NotificationGateway` POST JSON to `/mock/notifications`.
There is no real email/SMS delivery. The DTO includes `id`, `recipient`, `type`, and `message`.
The mock stores it in the `notifications` table. Retries reuse the ID, and the primary key
prevents duplicate storage, including after restart.

| Setting | Default | Purpose |
| --- | --- | --- |
| `NOTIFICATION_URL` | `http://localhost:8062/mock/notifications` | HTTP destination; port follows `server.port` unless overridden |
| `notification.timeout-ms` | 500 | Connect and read timeout, each in milliseconds |
| `notification.attempts` | 3 | Maximum total attempts, not three extra retries |
| `notification.retry-delay-ms` | 100 | Delay between retryable failures |

Connection failures, timeouts, HTTP 429, and 5xx responses are retried. Other HTTP 4xx
responses are not retried. Exhaustion logs an ERROR and increments `notificationsFailed`;
booking/cancellation remains successful. Delivery is synchronous after commit, so retries
can delay the HTTP response but do not hold the booking row lock.

- `GET /health`: executes `SELECT 1`; returns HTTP 200 with `status=UP,database=UP`,
  or HTTP 503 with both `DOWN` if database access fails. It does not require notifications
  to be up for the scheduler to be healthy.
- `GET /metrics`: JSON counters for `bookingsCommitted`, `cancellationsCommitted`,
  `notificationsDelivered`, and `notificationsFailed`. Counters are per process since startup;
  they reset on restart and are not historical database totals.
- The notification counters above cover booking/cancellation confirmations. AI reminder
  outcomes use the manual-run response, reminder logs, and the `ai_reminders` table.
- INFO logs show committed operations and accepted/delivered notification IDs/types.
  WARN logs show failed attempts. ERROR logs show fallback or failed database health checks.
  Notification logs omit passwords, recipient addresses, and message bodies.

### Demonstrate notification failure and recovery

For local development, stop the backend and start it from a terminal with:

```powershell
$env:NOTIFICATION_URL = "http://127.0.0.1:1/mock/notifications"
.\mvnw.cmd spring-boot:run
```

Book or cancel through React. The operation still succeeds; logs show three failed attempts
and fallback, and `/metrics` shows a failed notification. Stop the backend, then restore
the normal destination and restart:

```powershell
Remove-Item Env:NOTIFICATION_URL
.\mvnw.cmd spring-boot:run
```

New notifications can now be delivered. Missed notifications are **not automatically queued
or replayed**. An operator can resubmit the original DTO to the restored mock with the same
operation ID, such as `appointment-12-BOOKED`; duplicate requests remain safe. A crash between
commit and notification can lose a notification. A durable outbox would address that, but is
outside this minimal implementation.

### CI

`.github/workflows/ci.yml` builds React, includes it in the application, and runs Maven
`verify` (including all tests) on every push and pull request. Inspect the GitHub Actions
run after pushing; local test success does not establish a completed remote CI run.

## API

| Method   | Endpoint                               | Purpose                                                                     |
| -------- | -------------------------------------- | --------------------------------------------------------------------------- |
| GET      | /api/auth/session                      | Current account, or null if logged out                                      |
| POST     | /api/auth/login                        | JSON username/password; establishes a server session                        |
| POST     | /api/auth/logout                       | Ends the session                                                            |
| GET      | /api/services                          | Services                                                                    |
| GET      | /slots                                 | Available slots; providerId, serviceId, date, exclusive endDate, page, size |
| GET/POST | /api/customer/appointments             | List own appointments / book with slotId                                    |
| POST     | /api/customer/appointments/{id}/cancel | Owner-only cancellation                                                     |
| GET      | /api/provider/appointments             | Bookings with the provider                                                  |
| POST     | /api/provider/slots                    | Create using serviceId, startTime, endTime                                  |
| DELETE   | /api/provider/slots/{id}               | Remove an available slot owned by the provider                              |
| GET | /api/customer/ai/status | Whether AI is enabled |
| POST | /api/customer/ai/chat | Tool-calling assistant; JSON message |
| POST | /api/customer/ai/confirm | Confirm the pending action; JSON confirmationId |
| DELETE | /api/customer/ai/chat | Clear conversation and pending action |
| POST | /api/customer/ai/ask | Policy-only RAG question; JSON message |
| POST | /api/provider/ai/reminders/run | Run AI reminders for the logged-in tutor |

React sends the session cookie automatically through Vite's /api and /slots proxy.
The UI navigates one Monday-to-Sunday week at a time and loads all matching slots in that week.
The API retains SQL LIMIT/OFFSET internally; the UI has no page-size field.
Errors return status/message: 400 input, 401 no login, 403 wrong role/owner, 404 missing resource, 409 conflict.

Booking uses READ_COMMITTED and SELECT ... FOR UPDATE. A unique active_slot_id prevents duplicate active bookings.
Cancellation clears that reference while preserving slot_id for history.
Removed slots keep their database rows because historical appointments reference them.
No automatic booking retries; refresh and explicitly try another slot.
Past bookings become COMPLETED when appointments are viewed.

## Submission

- Milestone 4 report: [docs/CMPE172_Milestone_4.pdf](docs/CMPE172_Milestone_4.pdf)
- Milestone 4 video: **[ADD YOUR VIDEO LINK HERE]**.
- Include the report, repository link, and video link in the submission.

Previous milestone 2 report: docs/CMPE172_Milestone_2_Report.pdf

Previous milestone 2 video: https://drive.google.com/file/d/1I2d9oYg6jLG8ODiSDpyP3o9HqlT7AYmF
