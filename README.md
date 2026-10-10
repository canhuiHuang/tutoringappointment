# Tutoring appointments - Milestone 2

Java 21, Spring Boot, React + TypeScript, PostgreSQL, and hand-written JDBC SQL.

Includes session login with BCrypt, role checks, filtered/paginated slots, booking, cancellation,
appointment history, provider availability management, validation, global errors, and concurrency protection.
The database uses STUDENT for customers and TUTOR for providers.

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
spring.datasource.username=postgres
spring.datasource.password=YOUR_POSTGRES_PASSWORD
```

Replace `YOUR_POSTGRES_PASSWORD` with your PostgreSQL password and change the username,
host, or port if needed. The checked-in local defaults currently use username `postgres`
and password `root`; these must match your own installation. Use a database account
with permission to create tables and insert/update data in `tutoringdb`.

Alternatively, override the credentials for the current PowerShell terminal:

```powershell
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "YOUR_POSTGRES_PASSWORD"
```

These environment variables override the properties when you launch the backend from
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

#### Report: docs/CMPE172_Milestone_2_Report.pdf

#### Video link: https://drive.google.com/file/d/1I2d9oYg6jLG8ODiSDpyP3o9HqlT7AYmF
