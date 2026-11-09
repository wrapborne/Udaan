# State machines in LIC Udaan

An implicit state machine is a set of separate flags and nullable fields (`isLoading`, `error`, `result`, ...) that
together stand for one state. They let the code represent combinations that can't happen, such as "parsing and
applying at the same time", and every reader has to rebuild the real state in their head. This file lists every one
in the app, what was made explicit, and the convention for new code.

## Convention

When a screen's state is a sequence of steps (load → show → submit → done), model it as a `sealed interface` with one
subtype per step and pure transition functions that return `null` when an event isn't allowed in the current state.

- Keep the sealed type free of Android imports so it can be unit tested on the JVM (`./gradlew :app:testDebugUnitTest`).
- In the ViewModel, apply transitions atomically (`compareAndSet`) and ignore events the machine rejects. Don't rely on
  the UI disabling buttons.
- If screens already read flags such as `isLoading` or `error`, keep them as **derived getters** on the UI state so
  call sites don't change.
- Values that only make sense together (a parsed file and its duplicates list) go in one data class, not two nullables
  read with `!!`.

Reference implementation: `ui/viewmodel/PremiumPdfImportPhase.kt` and its test,
`app/src/test/.../ui/viewmodel/PremiumPdfImportPhaseTest.kt`.

## Made explicit

| Where | Before | After | Bug it removed |
|---|---|---|---|
| `PremiumPdfImportViewModel` | `isParsing`, `isApplying`, `parseResult`, `applyResult`, `error` | `PremiumPdfImportPhase`: `Idle → Parsing → Reviewing → Applying → Applied`, plus `ParseFailed` / `ApplyFailed` (retryable) | After a successful import, the Apply button stayed enabled, so the same PDF could be written to Firestore twice. A successful response with no body left the screen showing nothing. |
| `AdminViewModel` proposal upload | `pendingUpload: List<Policy>?` + `duplicatePolicies: List<String>?`, read with `!!` | `pendingUpload: PendingUpload<Policy>?` | The duplicates dialog stayed open and tappable during the upload, so a double tap uploaded twice. An exception during the upload crashed the app instead of showing an error. |
| `AdminViewModel` premium summary upload | `pendingSummaryUpload` + `duplicateSummaries`, read with `!!` | `pendingSummaryUpload: PendingUpload<PremiumSummary>?` | Same as above. |
| `UlipViewModel.UlipUiState` (calculator) | `projectionResult`, `finalMaturityValue`, `finalNetYield` | `projection: Projection?`, which always has at least one year | Calculating with a blank or zero term (plan 867, or any plan once a sum assured is set) crashed the app while formatting a null maturity value. |
| `NonMedicalCheckerViewModel.UiState` | `resultCategory`, `resultSaLimit` | `result: Result?` | None; the pair is now one value. |
| Agent, Admin and Superadmin dashboards | `selectedTabIndex: Int` in `remember`, `when (index) { 0 -> … }` | `AgentTab` / `AdminTab` / `SuperadminTab` enums in `rememberSaveable` | The selected tab reset to the first tab after opening something (e.g. a draft) and pressing Back, and on screen rotation. |
| `AdminViewModel` proposals error | `error` never cleared after its toast | `onProposalsErrorShown()` clears it | A second identical error (e.g. two network failures in a row) showed no toast. |

## Already explicit (good examples)

`LoginViewModel.LoginUiState`, `RegistrationViewModel.RegistrationState`, `ForgotPasswordViewModel.ForgotPasswordState`,
`ReviewViewModel.ReviewUiState`, `AdminViewModel.LeadsUiState` / `AnalyticsUiState` / `AgentListUiState`,
`SuperadminViewModel.UserListUiState`, `DocumentOperationProgress`, `UploadResult`, `UploadProgress`,
`ExtractionResult`, `PdfExtractionResult`, `NavScraper.ScrapeResult`, `LicPremiumPdfParseResult`.

## Still implicit (deliberately left as is)

These were not converted, either because the flag combination is valid or because changing them would mean touching
large screens with no test coverage for little gain. Convert them when you next work on the screen.

| Where | Fields | Notes / suggested shape |
|---|---|---|
| `AgentViewModel.AgentUiState`, `AdminViewModel.ProposalsUiState`, `PremiumSummaryUiState` | `isLoading`, `error` + data | Loading while still showing the previous data is intended (refresh), so a single sealed type would lose it. Fine as is. `PremiumSummaryUiState.error` is also shown inline, so it isn't cleared after its toast. |
| `GmailImportStatusViewModel.UiState` | `isLoading`, `items`, `error` | Same pattern as above; fine. |
| `UlipPlanViewModel.UlipPlanUiState` | `isLoading`, `error`, `planNumbers` | Small `Loading / Loaded / Failed` candidate. |
| `GraphicsViewModel.GraphicsUiState` | `isLoading`, `isUploading`, `error`, `selectedImageUri` | Two independent machines (fetch and upload) share one `error`, which is never cleared, so a repeated error doesn't toast again. Template and footer uploads also share `selectedImageUri`. The management screen is switched off (`isFeatureEnabled = false`), so this was left until it's turned on. |
| `GraphicsEditorViewModel.EditorUiState` | `isLoading`, `isUploading`, `error`, `user` | Same split as `GraphicsUiState`. |
| `DataCollectionViewModel.UiState` | `isSaving`, `error`, `isNewDraft`, `isReadOnly`, `currentStepIndex` / `totalSteps` | Wizard plus save status. `isReadOnly` and `isNewDraft` are a mode (`New`, `EditDraft`, `ReadOnly`). |
| `UlipViewModel.UlipUiState` (NAV list) | `isFetchingAllNavs`, `navListError`, `cacheInitialized`, `filtersInitialized` | Fetch lifecycle spread over four flags. |
| `AdminDashboardScreen` (29), `AgentDashboardScreen` (23), `UploadDocumentDialog` (11), `RegistrationScreen` (10) | `remember { mutableStateOf(...) }` dialog/expansion flags | Mostly independent toggles. Several "show dialog X" flags that can't be on at the same time are candidates for one `ActiveDialog?` value. |
