# EventForge

A **game-agnostic event/story engine** with a web-based scenario editor, a visual graph editor, and a full REST API — built with Spring Boot, Thymeleaf, and PostgreSQL.

EventForge lets you author branching, interactive-fiction-style scenarios (events, conditions, player choices, weighted random transitions, cooldowns) without touching code, then play them through a browser or drive them entirely over REST.

---

## ✨ Features

- **Game-agnostic engine** (`engine` package) — knows nothing about "zombies" or "shelters"; any domain can be modeled as a set of numeric state variables.
- **Conditions** — comparisons (`>`, `>=`, `<`, `<=`, `==`, `!=`) combined with `AND` / `OR` / `NOT`, fully polymorphic and JSON-serializable.
- **Actions** — modify or set a state variable (`ModifyResourceAction`, `SetResourceAction`).
- **Player choices** — each with its own condition, actions, and next-event transition.
- **Weighted random transitions** (`nextEventPool`) — pick the next event randomly, weighted by candidate, restricted to whichever candidates are currently eligible.
- **Cooldowns** — an event can be excluded from random pools for N ticks after it last fired.
- **Visual graph editor** (`/scenarios/{id}/graph`) — drag nodes, connect events, edit them inline, all backed by a JSON REST API.
- **Full REST API** for scenario/event CRUD and gameplay, independent of the MVC/Thymeleaf UI.
- **Cookie-based "My Games"** — resume in-progress sessions without a real login system.
- **PostgreSQL persistence** with JSONB columns for conditions/actions/pools (via Jackson polymorphic (de)serialization).

---

## 🏗️ Architecture

```
com.fizzycoyotestudio.eventforge
├── engine/            Game-agnostic core: Event, Choice, Condition, GameAction,
│                       GameState, EventEngine, EventRegistry, GameSession, etc.
├── web/                MVC controllers (dashboard, scenario/event builders,
│                       gameplay) + REST controllers + DTOs + form mappers/validators
├── persistence/        JPA entities, repositories, and services that translate
│                       between the engine's domain model and the database
└── game/zombieshelter/ A concrete example scenario built on top of the engine,
                        used for demos and the DataLoader
```

**Design principles worth noting:**

- The `engine` package has zero dependencies on Spring, JPA, or any specific game — it could be reused as a standalone library.
- Two separate exception handlers exist on purpose: `GlobalExceptionHandler` (`@RestControllerAdvice`) returns JSON errors for the REST API, while `MvcExceptionHandler` (`@ControllerAdvice`) renders an `error.html` page for the browser-facing MVC flow.
- Conditions/Actions/WeightedTransitions are persisted as JSONB via a single consolidated Jackson mapper (`EventForgeJsonMapper`), reused by every persistence-layer class that needs it.

This means you can swap the persistence layer (e.g. from JPA to MongoDB) without touching a single line of engine code — the engine doesn't even know what a `@Entity` or a `@Transactional` is.

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven (or just use the included Maven Wrapper — no local Maven install needed)
- Docker (for PostgreSQL) — or a local PostgreSQL instance

### 1. Start PostgreSQL

```bash
docker run -d --name eventforge-db \
  -p 5433:5432 \
  -e POSTGRES_DB=eventforge \
  -e POSTGRES_USER=eventforge \
  -e POSTGRES_PASSWORD=eventforge \
  postgres
```

> The app expects Postgres on **port 5433** (see `application-local.properties`), not the default 5432 — this avoids clashing with a Postgres instance you might already have running locally.

### 2. Create your local config

`application-local.properties` is **gitignored** (it's meant to hold your own local DB credentials), so it won't exist yet after cloning. Create it yourself:

```bash
# from the project root
touch src/main/resources/application-local.properties
```

and paste in:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/eventforge
spring.datasource.username=eventforge
spring.datasource.password=eventforge
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.show-sql=false
```

> Adjust the URL/credentials if your Docker container (step 1) uses different values.

### 3. Run the application

**Windows (PowerShell / cmd):**
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

**Linux / macOS:**
```bash
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

The `local` profile loads a demo "Zombie Shelter" scenario automatically on first run (see `DataLoader`), so there's data to look at immediately.

### 4. Open the app

- Dashboard: [http://localhost:8080/dashboard](http://localhost:8080/dashboard)
- Quick play (demo scenario): [http://localhost:8080/game/zombie-shelter](http://localhost:8080/game/zombie-shelter)

---

## 🧪 Running Tests

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

Tests run against the `test` profile (see `application-test.properties`), which uses `ddl-auto=create-drop` against the same Postgres instance/port as `local` — make sure Postgres is running (step 1 above) before running tests.

Coverage includes:
- Engine logic: conditions, actions, weighted transitions, cooldowns, eligibility checks
- Form mappers and validators (event builder, scenario builder)
- DTO mapping (domain ↔ REST)
- Controllers via `MockMvc`, plus a full end-to-end integration test hitting a real database

---

## 📚 API Documentation (Javadoc)

Generate it locally:

```bash
# Windows
.\mvnw.cmd javadoc:javadoc "-Dencoding=UTF-8" "-Ddocencoding=UTF-8" "-Dcharset=UTF-8"

# Linux / macOS
./mvnw javadoc:javadoc -Dencoding=UTF-8 -Ddocencoding=UTF-8 -Dcharset=UTF-8
```

Output lands in `target/reports/apidocs/index.html` — open it in a browser.

---

## 🔌 REST API Overview

| Method | Endpoint                                          | Description                          |
|--------|----------------------------------------------------|---------------------------------------|
| POST   | `/api/scenarios`                                  | Create a scenario with its events    |
| GET    | `/api/scenarios/{id}`                             | Fetch a scenario                     |
| GET    | `/api/scenarios/{scenarioId}/events/{eventId}`    | Fetch a single event                 |
| POST   | `/api/scenarios/{scenarioId}/events`              | Create a new event                   |
| PUT    | `/api/scenarios/{scenarioId}/events/{eventId}`    | Replace an event                     |
| DELETE | `/api/scenarios/{scenarioId}/events/{eventId}`    | Delete an event                      |
| POST   | `/api/game/start`                                 | Start a game session                 |
| GET    | `/api/game/{id}`                                  | Get session state                    |
| POST   | `/api/game/{id}/event`                            | Trigger the current event            |
| POST   | `/api/game/{id}/choice`                           | Resolve a player choice              |

---

## 🔮 Possible Next Steps

- Replace the cookie-based player identity with real authentication
- Import/export scenarios as standalone JSON files
- Undo/redo in the graph editor
- ranching history & player path tracking — persist each session's full sequence of triggered events/choices (not just the current tick, as today), then visualize on `scenario-graph.html` which nodes/edges a given playthrough actually visited, and inspect the game state at any past point. A full "rewind and replay from here with different choices" is a further step beyond that — it needs either seeded/recorded RNG rolls (since `nextEventPool` picks are currently non-deterministic) or a branching session model instead of a linear history, so it's left as an open design question rather than solved outright.
---

## 📄 License

MIT (or update this to whatever you choose).
