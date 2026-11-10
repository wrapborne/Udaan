# Notes for coding agents

- Android app (Kotlin, Jetpack Compose, Material 3) backed by Firebase. Firebase Cloud Functions are in `functions/`.
- Compile: `./gradlew :app:compileDebugKotlin`. Unit tests: `./gradlew :app:testDebugUnitTest`.
  `gradle.properties` pins `org.gradle.java.home` to a Windows path; elsewhere, override it with
  `-Dorg.gradle.java.home=<your JDK>`.
- Judge speed with the `preview` build (`./gradlew :app:assemblePreview`): release settings, debug-signed. Debug
  builds of Compose are much slower than what users get.
- Performance rules: never read a whole Firestore collection per row or per item (build an index once per
  operation, see `PolicyNumberIndex` in `FirebaseApi`); convert, filter and sort lists on `Dispatchers.Default`;
  give `LazyColumn` items stable keys and don't nest long lists inside a single item.
- UI state: model step-by-step flows (load → review → submit → done) as sealed types with pure transitions, not as
  groups of booleans and nullables. Read `STATE_MACHINES.md` for the convention, the reference implementation, and the
  list of remaining implicit state machines.
- UI redesign rules (keep features, signatures and routes) are in `UI_REDESIGN_SPEC.md`.
