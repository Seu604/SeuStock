# Repository Guidelines

## Project Structure & Module Organization

SeuStock is a Java 25 Spring Boot 4 application using Thymeleaf, HTMX, MyBatis, Flyway, PostgreSQL, Redis, MinIO, and H2 for tests. Main Java code lives under `src/main/java/com/seu/seustock`: `controller` handles web requests, `service` contains business logic, `mapper` contains MyBatis interfaces, `configuration` holds framework setup, and `model` contains DTOs, forms, enums, and pagination types. Resource files are in `src/main/resources`: `templates` for Thymeleaf pages/fragments, `static` for JavaScript and images, `mapper` for MyBatis XML, and `db/migration` for Flyway SQL. Tests mirror the package structure under `src/test/java`; test configuration and schema are in `src/test/resources`.

## Build, Test, and Development Commands

- `./gradlew test` runs the JUnit 5 test suite with H2 and `schema-test.sql`.
- `./gradlew bootRun --args='--spring.profiles.active=local'` starts the app locally at `http://localhost:8080`.
- `docker compose up -d` starts PostgreSQL on `5433`, MinIO on `9000/9001`, and Redis on `6379`.
- `./gradlew build` compiles, tests, and packages the application.

Use the Gradle wrapper rather than a system Gradle install.

## Coding Style & Naming Conventions

Use 4-space indentation for Java and keep package names under `com.seu.seustock`. Follow existing Spring naming: `*Controller`, `*Service`, `*Mapper`, `*DTO`, and `*Form`. Keep Thymeleaf reusable fragments under feature-specific `templates/<feature>/fragments/` directories. Add new MyBatis XML statements beside existing mapper XML files and keep interface method names aligned with SQL statement ids. Store user-facing text in `messages.properties` and `messages_en.properties` instead of hardcoding template labels.

## Testing Guidelines

Tests use JUnit 5, Spring Boot Test, Spring Security Test, and MyBatis Test. Name test classes with the `*Test` suffix, or `*IntegrationTest` when exercising broader request/database flows. When persistence changes, update both Flyway migrations and `src/test/resources/schema-test.sql`, then add or adjust mapper tests. Controller changes should cover validation, security, response fragments, and error handling where applicable.

## Commit & Pull Request Guidelines

Recent commits use concise imperative summaries, often with a scope and detail, for example: `Add comprehensive controller test suite` or `Internationalize stock-related messages`. Keep commits focused and mention tests or configuration updates when relevant. Pull requests should include a short behavior summary, linked issues if any, test commands run, and screenshots or screen recordings for visible UI changes.

## Security & Configuration Tips

Do not commit production secrets. Local defaults live in `application-local.properties`; production values should come from environment variables such as `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `MINIO_*`, `REDIS_*`, and `OLLAMA_BASE_URL`. Keep uploaded/generated files out of source unless they are intentional test or sample assets.
