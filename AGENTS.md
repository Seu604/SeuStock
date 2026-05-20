# Repository Guidelines

## Project Structure & Module Organization

SeuStock is a Spring Boot 4 Java application using Thymeleaf, HTMX, MyBatis, PostgreSQL, and H2 for tests. Main Java code lives under `src/main/java/com/seu/seustock`, organized by role: `controller`, `service`, `mapper`, `model/dto`, `model/form`, and `configuration`. Thymeleaf pages and fragments are in `src/main/resources/templates`; MyBatis XML mappings are in `src/main/resources/mapper`. Database assets are split between `schema/`, `query/`, `docker/postgres/init/`, and `src/test/resources/schema-test.sql`. Tests live in `src/test/java/com/seu/seustock`.

## Build, Test, and Development Commands

- `./gradlew bootRun`: run the application locally.
- `docker compose up -d postgres`: start the local PostgreSQL database on host port `5433`.
- `./gradlew test`: run JUnit 5 tests, including mapper integration tests against H2.
- `./gradlew build`: compile, test, and package the application.
- `./gradlew clean`: remove generated build output.

Use the Gradle wrapper instead of a system Gradle install. The project targets Java 25 via the Gradle toolchain.

## Coding Style & Naming Conventions

Use 4-space indentation for Java and keep packages under `com.seu.seustock`. Follow the existing suffix conventions: `*Controller`, `*Service`, `*Mapper`, `*DTO`, and `*Form`. Keep MyBatis Java mapper interfaces and XML files paired by entity name, for example `StockMapper.java` and `StockMapper.xml`. Prefer constructor injection or established Spring patterns already present in the codebase. Keep Thymeleaf reusable markup in `templates/**/fragments/`.

## Testing Guidelines

Tests use JUnit Platform with Spring Boot test dependencies and MyBatis test support. Name mapper tests as `EntityMapperTest` and place them beside the existing tests in `src/test/java/com/seu/seustock/mapper`. When changing persistence behavior, update both the production schema/init SQL and `src/test/resources/schema-test.sql` as needed. Run `./gradlew test` before opening a PR.

## Commit & Pull Request Guidelines

Recent commit messages use imperative, descriptive sentences such as `Add persistence layer implementation...` or `Implement user authentication...`. Keep commits focused on one logical change and mention affected areas when useful.

Pull requests should include a brief summary, testing performed, and any database or configuration changes. For UI changes, include screenshots or short notes describing affected Thymeleaf pages/fragments. Link related issues when available and call out any required local setup changes.

## Security & Configuration Tips

Do not commit real credentials. Local PostgreSQL defaults are defined in `compose.yaml` for development only. Keep environment-specific settings in properties files or external configuration, and review changes to authentication, password handling, and database initialization carefully.
