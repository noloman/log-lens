# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LogLens is a cloud-native log ingestion and analysis backend written in Kotlin + Spring Boot. It uses a multi-module Gradle build with two Spring Boot applications and a shared library.

## Build Commands

```bash
# Build everything
./gradlew build

# Build a specific module
./gradlew :apps:api:build
./gradlew :apps:worker:build

# Run tests
./gradlew test                    # all modules
./gradlew :apps:api:test          # single module

# Run a single test class
./gradlew :apps:api:test --tests "me.manulorenzo.api.SomeTest"

# Boot JARs
./gradlew :apps:api:bootJar
./gradlew :apps:worker:bootJar

# Run applications
./gradlew :apps:api:bootRun       # port 8080
./gradlew :apps:worker:bootRun    # port 8081

# Local dependencies (Postgres)
docker compose -f deploy/docker/docker-compose.local.yml up
```

## Architecture

### Module Structure

- **`apps/api`** — Spring Boot REST API (port 8080). Handles log ingestion and search. Package root: `me.manulorenzo.api`
- **`apps/worker`** — Spring Boot worker service (port 8081). Handles async log processing. Package root: `me.manulorenzo.worker`
- **`libs/common`** — Shared library (plain Kotlin, no Spring Boot plugin). Both apps depend on this via `project(":libs:common")`

### Build System

- **`build-logic/`** — Contains the `loglens.kotlin-jvm` convention plugin that configures Kotlin JVM (Java 17 toolchain) and JUnit Platform for all modules. This is the single source for shared build configuration.
- **`gradle/libs.versions.toml`** — Version catalog for all dependencies. All dependency versions are managed here.
- Root `build.gradle.kts` declares plugins with `apply false`; each module applies what it needs.
- Gradle build cache and configuration cache are enabled (`gradle.properties`).

### API Module Patterns

- `error/GlobalExceptionHandler` — `@RestControllerAdvice` for centralized exception handling
- `observability/CorrelationIdFilter` — Servlet filter that propagates `X-Request-Id` header via SLF4J MDC. Registered in `configuration/FilterConfig` for `/api/*` paths.
- `-Xjsr305=strict` is enabled for null-safety interop with Spring annotations

### Key Conventions

- Kotlin with Spring Boot's `kotlin-spring` plugin (open classes for Spring proxying)
- YAML configuration files (`application.yml`), not `.properties`
- Spring dependency management plugin handles transitive version alignment