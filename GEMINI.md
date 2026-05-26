# GEMINI.md - SeuStock Project Instructions

This file provides context and instructions for the **SeuStock** project, a web-based inventory management system.

## Project Overview
SeuStock is a personal or small-team inventory management application designed to track items across hierarchical locations: **Spaces → Shelves → Boxes**. It features a modern SSR-based UI with HTMX for interactivity, AI-powered image analysis for item identification, and QR code generation for physical location tracking.

### Core Tech Stack
- **Language:** Java 25
- **Framework:** Spring Boot 4.0.6
- **UI:** Thymeleaf (SSR), HTMX 2 (Partial Updates), Tailwind CSS
- **Persistence:** MyBatis, Flyway (Migration), PostgreSQL (Prod/Local), H2 (Test)
- **Session/Cache:** Redis (Spring Session)
- **Object Storage:** MinIO (Local fallback available)
- **AI/ML:** Spring AI (Ollama/Gemma), YOLO (HTTP API)
- **Tools:** Gradle, Docker Compose, ZXing (QR)

---

## Architecture & Design Patterns

### 1. Rendering Strategy
- **Standard SSR:** Full-page navigations and initial loads use standard Thymeleaf SSR.
- **HTMX Partial Updates:** Used for modals, inline row editing (Items/Spaces/Shelves/Boxes), delete confirmations, and toast notifications.
- **Fragments:** HTMX responses are located in `templates/<entity>/fragments/`. Controllers return these specific fragments when an HTMX header is present or for specific HTMX-targeted routes.

### 2. Identity & Security
- **Dual IDs:** Every table uses a `serial` `id` for internal FK joins and a `UUID` `external_id` for all external references (URLs, DTOs). **Never** expose internal `id`s.
- **Ownership Pattern:** All user-owned data access must be validated. Fetch the entity by UUID → fetch owning user → compare against `Principal.getName()` → throw `SecurityException` if they don't match.
- **Authentication:** Spring Security manages login/register. Routes are protected by default. Controllers use `Principal principal` to identify the user.

### 3. Inventory Model
- **Items:** The "Catalog" entries. No location or quantity data.
- **Stocks:** The physical unit. **One row = One physical unit**. No quantity column; counts are derived.
- **VerifiedLocation:** `StockService` uses `resolveVerifiedLocation()` to validate the full Space → Shelf → Box hierarchy in one atomic step.
- **Transactions:** Every stock change (IN, OUT, MOVE, ADJUST) must record an entry in `stock_transactions` within the same `@Transactional` block.

---

## Development Conventions

### MyBatis & Database
- **Naming:** `snake_case` in DB maps to `camelCase` in DTOs via `map-underscore-to-camel-case=true`.
- **Mappers:** SQL in `src/main/resources/mapper/`, interfaces in `com.seu.seustock.mapper`.
- **Type Handlers:** `UUIDTypeHandler` is used for UUID mapping.
- **Migrations:** Use Flyway (`src/main/resources/db/migration`). Do **not** use `init.sql` for schema changes.

### AI Image Analysis
- **Pipeline:** Resize (1024px) → YOLO (Optional Detect) → Gemma (Ollama/Spring AI) → ImageAnalysisDTO.
- **Interface:** `ImageAnalysisService` is the entry point.
- **UI:** Triggered via `image-upload.js` `onImageReady` callback in modals.

### Image Storage
- **Primary:** `MinioImageStorageService` (MinIO).
- **Fallback:** `LocalImageStorageService` (Local disk).
- **Deduplication:** Managed via `contentHash` (SHA-256).

---

## Key Commands

### Environment
```bash
# Start infrastructure (PostgreSQL, MinIO, Redis)
docker compose up -d

# Stop infrastructure
docker compose down
```

### Build & Run
```bash
# Run application with 'local' profile
./gradlew bootRun --args='--spring.profiles.active=local'

# Build project
./gradlew build
```

### Testing
```bash
# Run all tests (uses H2)
./gradlew test

# Run specific tests
./gradlew test --tests "com.seu.seustock.mapper.*"
./gradlew test --tests "com.seu.seustock.service.*"
```

---

## Project Structure
- `src/main/java/com/seu/seustock/configuration`: App, Security, MinIO, and MVC config.
- `src/main/java/com/seu/seustock/controller`: Thymeleaf/HTMX controllers.
- `src/main/java/com/seu/seustock/mapper`: MyBatis interfaces.
- `src/main/java/com/seu/seustock/model`: DTOs, Enums, Forms, and Pagination models.
- `src/main/java/com/seu/seustock/service`: Core logic and AI pipelines.
- `src/main/resources/db/migration`: Flyway SQL files.
- `src/main/resources/mapper`: MyBatis XML files.
- `src/main/resources/templates`: Thymeleaf templates and fragments.
- `src/test/resources/schema-test.sql`: H2-compatible schema for tests.

---

## Development Workflow
1. **Infrastructure:** Ensure Docker is running and `docker compose up -d` is executed.
2. **Schema Changes:** Add a new SQL file to `db/migration` and update `src/test/resources/schema-test.sql` for H2 compatibility.
3. **Logic:** Implement mappers (interface + XML), then service (with ownership checks), then controllers.
4. **UI:** Create/Update Thymeleaf templates. Use HTMX for interactive elements like modals or inline edits.
5. **Test:** Write Mapper tests (slice tests) and Service tests (mock tests). Use `./gradlew test` for verification.
