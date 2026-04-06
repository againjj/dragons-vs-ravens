# Dragons vs Ravens

A Spring Boot + Kotlin web app that serves a TypeScript board game prototype.

## What This Repo Contains

- A minimal Spring Boot backend that serves the app
- A browser-based frontend for the game UI
- A separate frontend game-logic module with tests

## Requirements

- Java 21 installed and available to the Gradle build
- No separate Gradle installation is required because the Gradle wrapper is included
- No separate Node installation is required because Gradle downloads the frontend toolchain
- Internet access the first time you run the app so Gradle can download its distribution, frontend toolchain, and project dependencies

## Run The App

```bash
./gradlew bootRun
```

Then open [http://localhost:8080](http://localhost:8080).

## Run Tests

```bash
./gradlew test
```

This runs:

- the frontend game-logic tests
- the Spring Boot test suite

## Project Structure

- `src/main/frontend/game.ts`
  - game rules, state transitions, and board helper logic
- `src/main/frontend/app.ts`
  - DOM wiring, rendering, browser events, and fullscreen behavior
- `src/main/resources/static/styles.css`
  - layout and styling
- `docs/code-summary.md`
  - architecture and codebase summary for future changes
- `docs/codex-rules.md`
  - project-specific rules for AI-assisted work

## AI Session Prompt

Use this at the start of a new AI coding session:

```text
Read docs/code-summary.md and docs/codex-rules.md before making changes. Follow those instructions unless I say otherwise.
```

## Notes

- The frontend is compiled by TypeScript into `build/generated/frontend`.
- Spring Boot serves the generated frontend assets as static resources.
- If you change architecture, workflow, or gameplay in a meaningful way, update `docs/code-summary.md`.
