# Dragons vs Ravens

A Spring Boot Kotlin web app with a TypeScript frontend for a simple board game prototype.

## Requirements

- Java 21+
- Gradle 8+ or a generated Gradle wrapper

## Run

```bash
./gradlew bootRun
```

Then open `http://localhost:8080`.

## Notes

- The frontend logic lives in `src/main/frontend/app.ts`.
- `src/main/resources/static/app.js` is a checked-in browser-ready copy so the UI can run without an extra frontend build step.
