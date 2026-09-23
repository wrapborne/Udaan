# Premium PDF Parser Work Notes

## Current Status

Parser v1 and the first import/review/apply flow have been added for LIC premium due list PDFs and agency commission bill PDFs.

New files:

- `app/src/main/java/com/viplove/licadvisornative/model/PremiumPdfImportModels.kt`
- `app/src/main/java/com/viplove/licadvisornative/model/PremiumPdfImportApplyModels.kt`
- `app/src/main/java/com/viplove/licadvisornative/util/LicPremiumPdfParser.kt`
- `app/src/main/java/com/viplove/licadvisornative/ui/viewmodel/PremiumPdfImportViewModel.kt`
- `app/src/main/java/com/viplove/licadvisornative/ui/screens/PremiumPdfImportScreen.kt`

Updated files:

- `app/src/main/java/com/viplove/licadvisornative/network/FirebaseApi.kt`
- `app/src/main/java/com/viplove/licadvisornative/network/TokenManager.kt`
- `app/src/main/java/com/viplove/licadvisornative/ui/screens/AgentDashboardScreen.kt`
- `app/src/main/java/com/viplove/licadvisornative/ui/screens/AdminDashboardScreen.kt`
- `app/src/main/java/com/viplove/licadvisornative/ui/screens/SuperadminDashboardScreen.kt`
- `app/src/main/java/com/viplove/licadvisornative/ui/viewmodel/AgentViewModel.kt`
- `app/src/main/java/com/viplove/licadvisornative/ui/viewmodel/AdminViewModel.kt`
- `app/src/main/java/com/viplove/licadvisornative/ui/viewmodel/SuperadminViewModel.kt`
- `firestore.rules`
- `gradle.properties`

The app now has a `PDF Import` tab in both the agent dashboard and the DO/admin dashboard.

The flow is:

1. Select a PDF.
2. Parse it locally.
3. Show a review screen.
4. Confirm the import.
5. Apply due/payment records to Firestore and update matching policy status/payment fields.

## Sample PDFs Used

Premium due list:

- `C:\Users\vip13\Downloads\Premdue-202609-0189927C.pdf`

Commission bills:

- `C:\Users\vip13\Downloads\CM-27C-20260901-0189927C.pdf`
- `C:\Users\vip13\Downloads\CM-27C-20260802-0189927C.pdf`

These PDFs are text-based PDFs, not scanned images, so OCR is not required.

## Confirmed Business Rules

- `FY` means first year premium.
- `ST` means second year or later renewal premium.
- Policy number is enough for matching because it is unique.
- A due item should be tracked by policy number plus due month/FUP.
- A payment item should be tracked by policy number plus due date.
- The same policy can appear more than once in one commission bill for different due dates.
- If a policy appears in the commission bill with positive premium, that month due is paid.
- If a commission row has negative premium or negative commission, it is a cooling-off/reversal row.
- Red rows in the premium due list mean the policy is lapsed.
- If a lapsed policy later appears as paid in a commission bill, it should move back to paid/active.
- Agents should import only their own PDFs.
- Admin/DO should be able to import PDFs for any advisor.

## Parser Output

Premium due list parser returns:

- branch code
- agent name
- agent code
- report month
- policy rows
- warnings

Each premium due row includes:

- serial number
- policy number
- policyholder name
- date of commencement
- plan/term
- mode
- FUP month
- premium year type: first year, renewal, or unknown
- lapsed flag
- installment premium
- due count
- GST
- total premium
- estimated commission
- due key: `policyNumber|fupMonth`

Commission bill parser returns:

- branch code
- agent name
- agent code
- report month
- batch
- processed date
- commission rows
- warnings

Each commission row includes:

- serial number
- policyholder name
- policy number
- plan/term
- due date
- risk date
- CBO
- adjustment date
- premium
- commission
- status: paid or cooling-off/reversal
- payment key: `policyNumber|dueDate`

## Implementation Notes

The parser reads compressed PDF content streams directly. This is needed because normal text extraction loses row styling, and row styling is required for lapsed policy detection.

The premium due parser maps table cells by their X positions in the LIC PDF layout.

The commission bill parser also maps table cells by X positions and ignores summary/payment rows by requiring policy-like row data.

Lapsed detection checks for red row text or red row fill in the premium due row. The sample due PDF marks at least one lapsed row with dark red text rather than a red background, so both signals are supported. Cooling-off detection checks for negative premium or commission in commission bill rows.

## Verification

Kotlin compile was run successfully:

```powershell
./gradlew.bat :app:compileDebugKotlin
```

Result:

```text
BUILD SUCCESSFUL
```

Debug APK build was run successfully:

```powershell
./gradlew.bat :app:assembleDebug
```

Result:

```text
BUILD SUCCESSFUL
```

APK output:

- `app/build/outputs/apk/debug/app-debug.apk`

Latest APK build after logout crash fix:

- Built `app/build/outputs/apk/debug/app-debug.apk` successfully on 22-09-2026 at 15:55:54.
- Built `app/build/outputs/apk/debug/app-debug.apk` successfully on 22-09-2026 at 16:09:06 after commission import diagnostics and rules updates.
- Built `app/build/outputs/apk/debug/app-debug.apk` successfully on 22-09-2026 at 16:22:24 after replacing logout navigation with an activity restart flow.
- Built `app/build/outputs/apk/debug/app-debug.apk` successfully on 23-09-2026 at 14:06:59 after simplifying logout further.
- Built `app/build/outputs/apk/debug/app-debug.apk` successfully on 23-09-2026 after adding an in-app crash reporter.
- Built `app/build/outputs/apk/debug/app-debug.apk` successfully on 23-09-2026 after changing crash recovery to block splash/session loading until the crash is copied, cleared, or the session is cleared.

Logout crash fix:

- `TokenManager.clearAll()` now signs out Firebase immediately before clearing cached preferences.
- Agent, DO/admin, and superadmin logout methods now clear the local session immediately before doing network logout work.
- Logout now restarts `MainActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` after clearing the session, so it no longer depends on Compose Navigation to leave dashboard screens.
- Dashboard logout no longer calls the network/API logout endpoint after local Firebase sign-out. Firebase local `signOut()` plus cached-session clearing is treated as the logout, avoiding async teardown races.
- The earlier unsafe `popUpTo(0)` route was removed.
- This addresses the crash pattern where logout could leave the app in a half-authenticated state, then reopening the app tried to resume the wrong session/dashboard.

Crash capture update:

- Added `CrashReporter`, installed from `MyApplication`, to store the latest uncaught crash stack trace locally before Android closes the app.
- `AppNavigator` now shows a `Last crash detected` dialog on next app open when a crash was captured.
- The dialog has `Copy` and `Clear` actions so the real phone crash can be shared without adb/logcat.
- Local emulator smoke test: installed and launched the debug APK on `LIC_Stable_API_36_1`; startup stayed alive and no AndroidRuntime crash was recorded. The remaining crash appears to be specific to the logged-in/logout flow and should be diagnosed from the captured phone stack trace.
- The crash recovery UI now blocks the normal navigation host while a saved crash is present. This prevents `SplashScreen`/session loading from immediately re-crashing before the crash can be copied.
- The crash recovery UI includes `Clear session and open login`, which signs out Firebase, clears cached session data, clears the saved crash, and then lets the app continue to the login route.

Commission import permission/debug update:

- Firestore rules now treat raw DO/admin role variants such as `DO`, `do`, `Development Officer`, and `development_officer` as admin-like.
- DO/admin can write and re-write premium due/payment import records without being blocked by exact advisor/admin metadata on existing import docs.
- Commission bill import now returns step-specific errors, for example whether it failed while saving payment history, finding a policy, updating paid status, or clearing a due item.
- The PDF import screen now displays the server error message instead of replacing it with a generic `Could not apply import`.

Gradle build stability note:

- APK packaging initially failed on this Windows machine from JVM/native memory pressure during dex merging.
- `gradle.properties` was adjusted to use one Gradle worker and a 1536 MB daemon heap with reduced JVM compiler pressure.

Latest sample sanity check before export:

- `Premdue-202609-0189927C.pdf` auto-detected as premium due list.
- Due parser read agent code `LIC0189927C`, stored import agent code as `0189927C`, and found 32 due rows.
- Due parser found 9 FY rows, 22 ST rows, 1 unknown/blank premium-year flag row, and 1 red-text lapsed row in the sample.
- `CM-27C-20260901-0189927C.pdf` auto-detected as commission bill and found 10 paid rows.
- `CM-27C-20260802-0189927C.pdf` auto-detected as commission bill and found 19 rows: 18 paid rows and 1 cooling-off/reversal row.

## Import Review Screen

The review screen supports both PDF types:

- Premium due list review shows due row count, lapsed count, FY count, renewal/ST count, warnings, and sample rows.
- Commission bill review shows paid row count, cooling-off/reversal count, warnings, and sample rows.

The screen always requires a confirm tap before Firestore is changed.

## Firestore Apply Behavior

Premium due list confirmation:

- Writes due records to `premium_due_items`.
- Uses document id based on `agentCode + policyNumber + fupMonth`.
- Saves lapsed status, premium year type, due amount, GST, total premium, estimated commission, import id, and import timestamp.
- If a row is lapsed, updates matching policy with `policyStatus = LAPSED`.

Commission bill confirmation:

- Writes payment records to `premium_payment_history`.
- Uses document id based on `agentCode + policyNumber + dueDate + adjustmentDate + status`.
- Positive rows are saved as paid rows.
- Negative premium/commission rows are saved as cooling-off/reversal rows.
- Paid rows update matching policy with:
  - `policyStatus = ACTIVE`
  - `lastPremiumPaidDate`
  - `lastPaidDueDate`
  - `lastPaymentAdjustmentDate`
  - import metadata
- Paid rows delete the matching due record using the due month derived from `DueDt`.
- Cooling-off/reversal rows update matching policy with `policyStatus = COOLING_OFF_REVERSAL`.

Role guard:

- Advisors can import only PDFs matching their own advisor code.
- Admin/DO and superadmin can import advisor PDFs.
- Firestore rules now include `premium_due_items` and `premium_payment_history`.
- Advisors can write import records only for their own advisor code and admin id.
- Advisors can update only the narrow import/payment status fields on their own policy documents.
- Admin/DO and superadmin can access advisor-scoped import records according to their role.

Agent code normalization:

- LIC PDFs may contain agent code like `LIC0189927C`.
- The app/database stores advisor code like `0189927C`.
- Imports now store `agentCode` in the app/database format, for example `0189927C`.
- Imports also store `sourceAgentCode` with the exact PDF value, for example `LIC0189927C`, for traceability.
- Permission checks normalize both formats so `LIC0189927C` and `0189927C` are treated as the same advisor.

## Recommended Next Steps

1. Add screens to browse imported due items and payment history.
2. Surface due/lapsed/payment status on policy cards.
3. Add an unmatched policy section in the review UI.
4. Add tests with the three sample PDFs.
5. Optionally build the debug APK and test import on device.

## Suggested Data To Store Later

For due records:

- policy number
- due month/FUP
- premium year type
- due amount
- lapsed status
- source PDF metadata
- import timestamp
- matched policy id if available

For payment records:

- policy number
- due date
- adjustment date
- premium amount
- commission amount
- status: paid or cooling-off/reversal
- source PDF metadata
- import timestamp
- matched policy id if available

## Important Guardrails

- Do not silently update policies from imported PDFs.
- Always show a review screen first.
- Keep cooling-off/reversal rows in history.
- Let paid commission rows clear matching due items.
- Keep lapsed rows visible until payment is confirmed or user reviews them.
