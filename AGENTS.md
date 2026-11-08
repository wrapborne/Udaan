# Notes for coding agents

- Android app (Kotlin, Jetpack Compose, Material 3) backed by Firebase. Firebase Cloud Functions are in `functions/`.
- Compile: `./gradlew :app:compileDebugKotlin`. Unit tests: `./gradlew :app:testDebugUnitTest`.
  `gradle.properties` pins `org.gradle.java.home` to a Windows path; elsewhere, override it with
  `-Dorg.gradle.java.home=<your JDK>`.
- UI state: model step-by-step flows (load → review → submit → done) as sealed types with pure transitions, not as
  groups of booleans and nullables. Read `STATE_MACHINES.md` for the convention, the reference implementation, and the
  list of remaining implicit state machines.
- UI redesign rules (keep features, signatures and routes) are in `UI_REDESIGN_SPEC.md`.
