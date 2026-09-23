# LIC Udaan — UI/UX Redesign Specification

> **Audience:** Codex (implementation).
> **Companion file:** `ui_redesign_mockups.html` (visual reference for every screen described below — open it side-by-side while coding).
> **Goal:** Redesign every screen under `app/src/main/java/com/viplove/licadvisornative/ui/screens/*` and the auth screens in `MainActivity.kt` to the navy + gold "LIC Udaan" visual language shown in the mockups. **Every existing feature must remain reachable** — no feature removal, no behaviour change.

---

## 0. Hard constraints — read first

1. **No feature removal.** Every button, tab, dialog, filter, calculator, dropdown, sort chip, upload action, copy/WhatsApp/payment-link button, expand/collapse group, profile pic upload, etc. listed in §7 must remain. If the existing screen has it, the redesigned screen must have it. When in doubt, keep it.
2. **Do not change public composable signatures referenced from `MainActivity.kt`'s `AppNavigator`:** `LoginScreen`, `RegistrationScreen`, `ForgotPasswordScreen`, `AgentDashboardScreen`, `AdminDashboardScreen`, `SuperadminDashboardScreen`, `DataCollectionScreen`, `ReviewScreen`, `FooterSelectionScreen`, `GraphicsManagementScreen`, `GraphicsEditorScreen`, and `SplashScreen`. Internal helpers may be renamed.
3. **Do not change ViewModel APIs, state shapes, navigation routes, or string-resource keys.** All redesigned UI consumes the existing flows. New string keys are fine; mirror them in `values/strings.xml` AND `values-hi/strings.xml`.
4. **Do not touch `network/`, `domain/`, `model/`, `firebase/`, `worker/`, `util/` (except `LocaleManager` use), or `viewmodel/`.** Backend is Firebase via the `FirebaseApi` shim — stay above it.
5. **Material 3 Compose only.** No M2 imports. No new third-party UI libs beyond what's already in `gradle/libs.versions.toml` (Coil is allowed and already used).
6. **Responsive:** Every screen renders correctly on widths from **320 dp** (small Android Go) to **600 dp** (large phone / unfolded foldable). Never assume landscape. Pillar widths must `.weight(1f)` — never fixed.
7. **Accessibility:** Every `IconButton` has a non-null `contentDescription`. Min tap target `48.dp`. Body text ≥ `14.sp`. Honour system font scale.
8. **Dark mode parity:** Every colour comes from `MaterialTheme.colorScheme.*` or the new extended palette in `Color.kt`. No raw `Color(0xFF…)` in screen code except the chart accents declared once in `Color.kt`.
9. **App name is "LIC Udaan"** — already in `strings.xml` as `app_name`. The brand mark is the navy shield with gold arrows; assets live in `mipmap-*/logo*.webp` and `mipmap-anydpi-v26/logo*.xml`.

---

## 1. Design system

### 1.1 Brand palette — replace `ui/theme/Color.kt`

```kotlin
package com.viplove.licadvisornative.ui.theme
import androidx.compose.ui.graphics.Color

// Brand — pulled from the LIC Udaan shield logo
val BrandNavy           = Color(0xFF1B3A8A)   // primary
val BrandNavyDeep       = Color(0xFF0F2566)   // gradient end on top bar
val BrandNavyContainer  = Color(0xFFDDE5F7)   // primaryContainer (chips, pills)
val BrandGold           = Color(0xFFF4B400)   // accent — secondary/tertiary
val BrandGoldSoft       = Color(0xFFFDEFC4)   // gold container

// Surfaces
val LightBackground     = Color(0xFFF4F6FB)
val CardBackground      = Color(0xFFFFFFFF)
val TextPrimary         = Color(0xFF0F172A)
val TextSecondary       = Color(0xFF64748B)
val DividerSubtle       = Color(0xFFE2E8F0)

val DarkBackground      = Color(0xFF0B1020)
val DarkSurface         = Color(0xFF141A2C)
val DarkOnBackground    = Color(0xFFE8ECF4)
val DarkOnSurface       = Color(0xFFE8ECF4)
val DarkTextSecondary   = Color(0xFF9AA6BE)
val DarkDivider         = Color(0xFF243049)

val BrandSuccess        = Color(0xFF0EA371)
val BrandSuccessBg      = Color(0xFFD1FAE5)
val BrandDanger         = Color(0xFFDC2626)
val BrandDangerBg       = Color(0xFFFEE2E2)

// Chart accents (only raw Color literals allowed outside this file are these)
val ChartGreen = Color(0xFF10B981)
val ChartRed   = Color(0xFFEF4444)
val ChartAmber = Color(0xFFF59E0B)
val ChartBlue  = Color(0xFF3B82F6)
```

Update `ui/theme/Theme.kt` color schemes:

- `primary` = BrandNavy, `onPrimary` = White, `primaryContainer` = BrandNavyContainer, `onPrimaryContainer` = BrandNavy
- `secondary` = BrandGold, `onSecondary` = Color(0xFF3B2A00), `secondaryContainer` = BrandGoldSoft, `onSecondaryContainer` = Color(0xFF6B4F00)
- `tertiary` = BrandGold (same as secondary — used for ANANDA badges, accents)
- `error` = BrandDanger, `onError` = White, `errorContainer` = BrandDangerBg
- `background`, `surface`, `surfaceVariant`, `outlineVariant` per light/dark.
- Status bar: tinted to BrandNavy in dashboards (set via `WindowCompat` in `LICAdvisorNativeTheme`).

### 1.2 New files

Create `ui/theme/Dimens.kt`:

```kotlin
object Dimens {
    val GutterXs = 4.dp; val GutterSm = 8.dp; val GutterMd = 12.dp
    val GutterLg = 16.dp; val GutterXl = 24.dp
    val ScreenHPadding = 16.dp
    val CardCorner = 14.dp
    val PillCorner = 999.dp
    val FieldHeight = 56.dp
    val ButtonHeight = 48.dp
    val IconBtnMin = 48.dp
    val FabBottomInset = 88.dp     // last LazyColumn item padding so FAB doesn't cover content
}
```

Create `ui/theme/Shapes.kt`:

```kotlin
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(14.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
```

Wire `AppShapes` into `MaterialTheme(...)` in `Theme.kt`.

### 1.3 Typography — adjust `ui/theme/Type.kt`

Use Material 3 defaults but tighten:
- `displaySmall` weight 700, letterSpacing `(-0.5).sp`
- `headlineSmall` / `titleLarge` weight 600
- `titleMedium` weight 600, letterSpacing `0.sp`
- `labelLarge` weight 600 (button text)
- Default font remains system; do not add a custom font.

### 1.4 Reusable components — create under `ui/components/`

| File / Composable | Purpose |
|---|---|
| `AppScaffold.kt` — `AppScaffold(...)` | Standard scaffold with gradient (BrandNavy → BrandNavyDeep) top bar, brand mark, title, optional greet line, actions, optional `OfflineBanner` above content, optional `bottomBar`, optional FAB. |
| `BrandMark.kt` — `BrandMark(size: Dp = 26.dp)` | The small white-rounded square with the gold upward-arrow glyph that sits left of the top-bar title. |
| `BrandLogo.kt` — `BrandLogo(size: Dp = 64.dp)` | Full shield logo: read `R.mipmap.logo_foreground` via `painterResource` and render. Used on Splash, Login. |
| `SectionCard.kt` — `SectionCard(title: String? = null, trailing: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit)` | White card, corner = `AppShapes.medium`, padding `Dimens.GutterLg`. |
| `StatTile.kt` — `StatTile(label: String, value: String, delta: String? = null, deltaUp: Boolean? = null, accent: Color = MaterialTheme.colorScheme.primary)` | Label (9.sp muted) + value (large) + optional delta arrow row. Used in 2-up / 3-up / 4-up grids. |
| `PillChip.kt` — `PillChip(label: String, selected: Boolean, onClick: () -> Unit, leadingIcon: ImageVector? = null, modifier: Modifier = Modifier)` | Selected = `primaryContainer` bg with `primary` text. Unselected = `surface` with `outlineVariant` border. |
| `SolidPill.kt` — `SolidPill(label, selected, onClick)` | Same as PillChip but selected style is solid `primary` (for sub-tabs inside ULIP). |
| `EmptyState.kt` — `EmptyState(icon, title, message, ctaLabel = null, onCta = {})` | Centred icon + title + message + optional CTA. |
| `LabeledTextField.kt` — `LabeledTextField(value, onValueChange, label, helper = null, error = null, keyboardType = KeyboardType.Text, imeAction = ImeAction.Next, leadingIcon = null, trailingIcon = null, isMono = false)` | OutlinedTextField wrapper, consistent height + helper + error slot. `isMono` switches to a monospace font for PAN/Aadhaar/amounts. |
| `AppSearchBar.kt` — `AppSearchBar(value, onValueChange, placeholder, modifier)` | Outlined search row with leading magnifier and clear icon when `value.isNotEmpty()`. |
| `PullToRefreshSurface.kt` — `PullToRefreshSurface(isRefreshing, onRefresh, content)` | Wraps `PullToRefreshBox` with `BrandNavy` spinner; used on Agent Policies + Admin Proposals tabs. |
| `StepHeader.kt` — `StepHeader(stepIndex: Int, totalSteps: Int, title: String, subtitle: String? = null)` | Wizard header: linear progress (gold), "Step N of M" + step title row. |
| `BottomActionBar.kt` — `BottomActionBar(onBack: () -> Unit, onPrimary: () -> Unit, primaryLabel: String = "Next", isLastStep: Boolean = false)` | Sticky bottom: outline "Back" + filled `primary` "Next" (or `success` green "Submit" on last step). Honours `WindowInsets.navigationBars`. |
| `UploadButtonRow.kt` — `UploadButtonRow(actions: List<UploadAction>)` | Row of `FilledButton`s with leading upload icon. Used at top of Proposals tab for "Upload Proposals" (navy) + "Upload Premium" (gold). |
| `SortChipsRow.kt` — `SortChipsRow(label: String = "Sort by:", chips: List<SortChipState>)` | Right-aligned label + list of `SortChip`s. Each chip has selected/asc/desc state. Replaces `SortChip` in `CommonUi.kt` while preserving its public API. |
| `Avatar.kt` — `Avatar(initials: String, palette: AvatarPalette = AvatarPalette.Blue, size: Dp = 32.dp)` | Circular initials avatar with palette presets: Blue (primary container), Gold, Green, Amber, Neutral. |
| `ExpandableGroupCard.kt` — `ExpandableGroupCard(title: String, subtitle: String? = null, isExpanded: Boolean, onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit)` | Used in Proposals tab (per-advisor) and Premium Summary tab. |
| `OutlinedDateButton.kt` — `OutlinedDateButton(label: String?, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier)` | Calendar icon + label inside an `OutlinedButton`. Replaces the existing date-range outlined buttons. |
| `OfflineBanner.kt` (keep in `CommonUi.kt`) | Restyle to gold `BrandGoldSoft` background with a `wifi-off` icon (currently uses `Share` icon — wrong). |

All helpers accept `modifier: Modifier = Modifier`.

### 1.5 Restyle existing helpers in `CommonUi.kt` (keep names + signatures)

- `FilterDropdown(...)` — keep API. Internally swap the underlying `OutlinedTextField` styling so it looks like the input cards in mockups (rounded corner = `AppShapes.small`, label `9.sp muted`).
- `StatCard(label, value)` — keep API. Internally delegate to `StatTile`.
- `PolicyCard(policy)` — keep, restyle to match `DetailedPolicyCard` lite version.
- `DetailedPolicyCard(policy)` — **keep every existing element** (shortName title, Policy No + copy button, Plan + Mode, DOC, Last Paid, Due Date with LATE warning, Premium, ENACH, WhatsApp share, Payment Link). Restyle per §3.5.1.
- `PremiumSummaryCard(summary)` — keep, restyle per §3.6.2.
- `SortChip(text, isSelected, sortOrder, onClick)` — keep API; restyle to pill (see `SortChipsRow`).
- `OfflineBanner(isOnline)` — replace `Share` icon with `Icons.Filled.WifiOff` and recolour as above.
- `SyncStatusBar(lastSyncTime, isOnline)` — keep API; restyle the chip.
- `formatDateRange`, `formatDate`, `formatDateTime` — unchanged.

---

## 2. Global chrome

### 2.1 Top bar (dashboards)

All three dashboards use `AppScaffold` with:
- `containerColor` gradient `Brush.linearGradient(listOf(BrandNavy, BrandNavyDeep), Offset.Zero, Offset(size.width, size.height))` — apply via a `Box` wrapper since M3 `TopAppBar` doesn't accept Brush.
- Title row: `BrandMark` (26.dp) + Column { titleText (`titleMedium`, white) + optional greet line (`labelSmall`, `Color.White.copy(alpha=0.85f)`) }.
- Actions: `Refresh`, `AccountCircle` (agent only), `Logout`.
- Status bar tinted to BrandNavy via `WindowCompat.getInsetsController(...).isAppearanceLightStatusBars = false`.

### 2.2 Tab row

**Replace `ScrollableTabRow` everywhere** with a `LazyRow` of `PillChip`s. `contentPadding = PaddingValues(horizontal = Dimens.ScreenHPadding, vertical = Dimens.GutterSm)`. Selected pill = `primaryContainer` + `primary` text + weight 500. The `selectedTabIndex` integer continues to drive the `when (selectedTabIndex)` block — do not change indices or branches.

For the inner sub-tabs inside ULIP (4 sub-tabs) use `SolidPill` (selected style is solid BrandNavy, not pill-container) to differentiate visually.

### 2.3 FAB (Agent only)

Extended FAB on width ≥ `360.dp`: icon + "New client". Below: icon-only. Soft shadow per M3 default. Bottom inset `Dimens.GutterMd`, right `Dimens.GutterMd`.

### 2.4 `OfflineBanner` placement

Render `OfflineBanner` inside `AppScaffold` content slot at the top of the column, above the pill tab row. Already a `Composable` that's a no-op when online.

---

## 3. Screen-by-screen redesign

For every screen below: **preserve all existing ViewModel calls, dialogs, nav arguments, error/success Toasts, and `LaunchedEffect` blocks**. Only restyle and reorganise the layout. Mockup file numbers (e.g. `mockup §2.1`) refer to phone tiles in `ui_redesign_mockups.html`.

### 3.1 Splash (mockup §1.1)

`SplashScreen` in `MainActivity.kt`:
- Background = `BrandGoldSoft` → `BrandNavyContainer` vertical gradient (use the existing `gradient-hero` look).
- Center: `BrandLogo(size = 120.dp)`, below it the wordmark **"LIC UDAAN"** in `displaySmall` weight 700 navy, letter spacing `2.sp`, then "Loading your session…" in `labelMedium muted`.
- Below text: `CircularProgressIndicator` 28.dp, BrandNavy track.
- Bottom: version text `labelSmall muted`.

Preserve the existing routing logic (`TokenManager.isLoggedIn()`, role recovery via `ApiClient.api.me()`, role-based `safeNavigate`).

### 3.2 Login (mockup §1.2) — `LoginScreen` in `MainActivity.kt`

Replace the existing column with:
- Hero: `BrandLogo(64.dp)` + wordmark "LIC UDAAN" + "Sign in to continue" subtitle.
- `SectionCard(title = "Sign in")` containing:
  - `LabeledTextField` Email (KeyboardType.Email, ImeAction.Next).
  - `LabeledTextField` Password with the existing visibility toggle (preserve `passwordVisible` state, `Icons.Filled.Visibility` / `VisibilityOff`).
  - Row: `Checkbox` "Remember me" (left) | `TextButton` "Forgot?" (right).
  - Filled `Button` "Login" `height = Dimens.ButtonHeight`. Shows the existing `CircularProgressIndicator` while `uiState is Loading`.
- Below card: `TextButton` "New advisor? Register".

Wrap content in `Modifier.widthIn(max = 480.dp).align(CenterHorizontally)` so tablets don't stretch fields.

Preserve all `LoginViewModel.LoginUiState` handling, role-based navigation, Toast errors.

### 3.3 Registration (mockup §1.3) — `RegistrationScreen`

Three `SectionCard`s inside a `verticalScroll`:
- **Your details** card: Full name, Phone (existing 10-digit filter), Email.
- **Role** card: Two `PillChip`s ("Advisor" / "Admin (DO)") driven by `selectedRole`. Below them: conditional `LabeledTextField` — "Advisor code" if Advisor selected, "DO code" if Admin selected. (Existing fields `userCode` and `adminDoCode` — keep both, just show the relevant one.)
- **Password** card: Password + Confirm password, both with visibility toggles (preserve `passwordVisible`, `confirmPasswordVisible`).

Bottom: `Button` "Register" (BrandNavy). `TextButton` "Already have an account? Sign in".

Preserve all `RegistrationViewModel.RegistrationState` handling, focus moves, Toast feedback, navigation on success.

### 3.4 Forgot password (mockup §1.4) — `ForgotPasswordScreen`

- Top nav with back arrow.
- Centred icon-in-circle (`BrandGoldSoft` bg, `Icons.Filled.VpnKey` or `Icons.Filled.Key` gold).
- Headline "Forgot your password?" + instruction.
- `SectionCard` with `LabeledTextField` Email + filled `Button` "Send reset link".
- `TextButton` "Back to login".

Preserve all existing ViewModel calls and Toast.

### 3.5 Agent dashboard (`AgentDashboardScreen`)

Tabs (keep order & indices): **My Policies, ULIP, Data Analysis, Forms, Drafts, Checker, Graphics**.

Apply §2 chrome. Top bar greet line: `"Hi ${firstName} · ${agencyCode}"` (use `agentUiState.currentUser`).

Profile dialog — preserve all existing content (email, agency code, start date InfoRows, EN/HI language radio with `LocaleManager.updateAppLocale(context, langCode)`, Close button). Restyle to use `M3 AlertDialog` with `containerColor = surface`, `shape = AppShapes.large`.

#### 3.5.1 My Policies tab (`AgentPoliciesTab`) — mockup §2.1

Wrap entire body in `PullToRefreshSurface` (existing refresh logic). Content in `LazyColumn`:

1. `AppSearchBar` "Search by name / policy / plan" (preserve `agentViewModel.onSearchQueryChanged`).
2. **Late filter checkbox**: `Row { Checkbox(checked=showOnlyLate, onCheckedChange=...); Text("Show only late policies (>30 days overdue)") }` — preserve `onLateFilterToggled`.
3. **4 filter dropdowns** in 2×2 grid (existing `FilterDropdown` API): Plan, Mode, FY, Agency Year. Preserve `onPlanSelected`, `onModeSelected`, `onFinancialYearSelected`, `onAppraisalYearSelected`, and the `enabled = uiState.startDate == null` rule.
4. `OutlinedDateButton` for date range, enabled only when both FY+Appraisal are "None" (preserve existing rule). Opens existing `DateRangePicker` dialog.
5. 2-tile `StatTile` row: "Total policies" (count) + "ANANDA" (count, gold accent).
6. Divider.
7. Policies: render each via the redesigned `DetailedPolicyCard` (§3.5.2). Empty → `EmptyState` (icon `Icons.Outlined.Inbox`).
8. Last item: `Spacer(Modifier.height(Dimens.FabBottomInset))`.

#### 3.5.2 `DetailedPolicyCard` redesign (used by Agent + Admin ULIP + others)

Layout inside a `Card(shape = AppShapes.medium)`:

```
Row 1 (top):
  Column (weight 1f):
    Text(shortName)       // titleMedium, weight 600
    Row {
      Text("Policy no: ${policyNumber}")  // labelSmall muted
      IconButton(Icons.Filled.ContentCopy, size 16.dp) → clipboard + Toast "Copied!"   // PRESERVE
    }
  Badge:
    if isAnanda → "ANANDA" gold badge (BrandGoldSoft + gold-dark text)
    else if isLate → "Late ${days}d" danger badge (BrandDangerBg + BrandDanger text)
    else if isUlip → "ULIP" info badge (BrandNavyContainer + BrandNavy text)

Lines (labelSmall, muted, 1.6 line-height):
  "Plan: <b>${plan}</b> · Mode: <b>${mode}</b>"
  "DOC: <b>${formattedDoc}</b>" + if lastPremiumPaidDate → " · Last paid: <span primary>${formattedLastPaid}</span>"
  if isLate → red "Due: ${dueDate} ⚠ LATE" (BrandDanger weight 500)
  else → "Due: ${dueDate}" muted

Row (premium right-aligned):
  Text("Premium", labelSmall muted)
  Text("₹${premium}", titleMedium weight 600)

if enachDate.isNotEmpty() → small "ENACH: ${enachDate}" line, labelSmall muted, between Lines and Premium.

Row 2 (actions, right-aligned, margin-top 8dp):
  Filled green WhatsApp button (32×32 square, corner 8dp, bg #25D366, icon ti-brand-whatsapp white)
    onClick = PRESERVE existing share-intent code (com.whatsapp package + fallback chooser)
  OutlinedButton "Payment link"
    onClick = PRESERVE existing URL intent
```

`isPolicyLate` and `calculateDueDate` helpers in `CommonUi.kt` stay private — keep as-is.

#### 3.5.3 ULIP tab (renders `UlipScreen(policies = ulipPolicies)`)

See §3.8.

#### 3.5.4 Data Analysis tab (`AgentDateAnalysisTab`) — mockup §2.6

Three `PillChip`s row at top: **Single Day / Compare Dates / Compare Periods** driven by `analysisMode`.

`SectionCard(title="Date selection")` with the existing `OutlinedDateButton`s — preserve the sequential `showDatePicker1/2`, `showPeriod1StartPicker/EndPicker`, etc. logic (do not refactor — it's working).

Stats display:
- **Single mode**: When `selectedDate1 != null`, render redesigned `DateStatsCard` ("Your performance on ${date}") with 3 `StatTile`s in a `FlowRow`: Policies, Premium (`₹${formatPremium}`), ANANDA. Below it, the existing auto month comparison via redesigned `ComparisonStatsCard` (current month vs same month last year).
- **Compare mode**: When both dates set, `ComparisonStatsCard` showing per-metric bar pairs (see §3.5.5).
- **Period mode**: Same `ComparisonStatsCard` pattern for the two periods.

Empty states: `Card` with centered muted text (preserve existing wording).

#### 3.5.5 `ComparisonStatsCard` redesign

Replace the existing 4-column table with **per-metric bar pairs**:

```
Card(title = "${date1} vs ${date2}"):
  for each metric in [Policies, Premium, ANANDA]:
    Column:
      Row { Text(label muted) ; Text(delta) }   // delta: "+N ↑" success or "-N ↓" danger or "—"
      Row { Box(width = ratio1 * maxWidth, height 6.dp, BrandNavy) ; Text(value1) }
      Row { Box(width = ratio2 * maxWidth, height 6.dp, #CBD5E1) ; Text(value2 muted) }
```

For ANANDA, use `BrandGold` and `BrandGoldSoft` instead of navy/grey to differentiate. `ratio = value / max(value1, value2)`, clamp min width to 4.dp so zero is visible. Preserve `ComparisonRow` for table data export if used elsewhere.

#### 3.5.6 Forms tab → §3.10
#### 3.5.7 Drafts tab (`DraftsTab`) — mockup §2.7

`LazyColumn`:
1. `AppSearchBar` "Search by lead name" (preserve `onDraftsSearchQueryChanged`).
2. **Segmented quick-jump bar** (3 segments: Drafts · {count} / Submitted · {count} / Archived · {count}). Tapping a segment calls `lazyListState.animateScrollToItem(indexOfHeader)`. The three sections are still rendered linearly — segment is just a jump aid.
3. Section header "Saved drafts" → each `DraftCard`:
   - Leading `Avatar` with initials from `proposerDetails.name`.
   - Two-line text: name (`titleMedium`), "Updated · ${relativeTime(lastUpdated)}" (`labelSmall muted`).
   - Trailing `IconButton(Icons.Default.Delete, tint=BrandDanger)`.
   - Whole row clickable → existing nav.
4. Section header "Submitted" → each `SubmittedFormCard`:
   - Avatar + name + "Submitted · ${formatDate}".
   - Trailing two filled-tonal icon buttons: Download, Archive.
   - Whole row click → `review_screen/${form.id}`.
5. Section header "Recently archived" (only if non-empty) → `ArchivedFormCard`:
   - `containerColor = surfaceVariant`.
   - Trailing "Restore" tonal button (text + icon).

Preserve all three confirmation dialogs (`Delete?`, `Archive?`, `DownloadOptions`) — restyle dialog shape to `AppShapes.large`, otherwise content untouched.

`relativeTime(millis: Long): String` helper:
```
< 60s → "Just now"
< 60m → "${n}m ago"
< 24h → "${n}h ago"
< 48h → "Yesterday"
else → formatDate(millis)
```

Add helper to `ui/screens/CommonUi.kt`.

#### 3.5.8 Checker tab → §3.9
#### 3.5.9 Graphics tab → §3.11

### 3.6 Admin (DO) dashboard (`AdminDashboardScreen`)

Tabs (keep order & indices): **Proposals, ULIP, Data Analysis, Forms, Circulars, Premium Summaries, Performance, Advisor Management, Leads, Analytics, Checker, Graphics**.

Top bar greet line not needed (admin). Apply §2 chrome.

#### 3.6.1 Proposals tab (`ProposalsTab`) — mockup §3.1 — **MUST KEEP UPLOAD BUTTONS AND ALL FILTERS**

Wrap in `PullToRefreshSurface` (existing pull-to-refresh state).

`LazyColumn`, in order:

1. **`UploadButtonRow`** — TWO buttons, side by side, equal weight:
   - "Upload proposals" — leading `Icons.Default.UploadFile`, BrandNavy bg, white text. `onClick = proposalFilePicker.launch("text/plain")` (PRESERVE).
   - "Upload premium" — leading `Icons.Default.UploadFile`, BrandGold bg, dark text. `onClick = premiumFilePicker.launch("*/*")` (PRESERVE).
2. Divider.
3. `AppSearchBar` "Search by name, policy no or plan" (PRESERVE `onSearchQueryChanged`).
4. **Late filter checkbox**: same as Agent (PRESERVE `onLateFilterToggled`).
5. **Filter row 1**: 2-column `FilterDropdown`s — Plan, Mode (PRESERVE `onPlanSelected`, `onModeSelected`).
6. **Date filter section**: "Filter by Date:" label + `OutlinedDateButton` full-width opening `DateRangePicker` (PRESERVE `onDateRangeSelected`).
7. **Filter row 2**: 2-column `FilterDropdown`s — Financial Year, Appraisal Year (PRESERVE `onFinancialYearSelected`, `onAppraisalYearSelected`).
8. **Stat row**: 2 `StatTile`s — "Total proposals" (count) + "ANANDA count" (count, gold accent).
9. **Sort chips row** (`SortChipsRow`): label "Sort by:" + two chips:
   - "Code" — selected/asc/desc via `proposalsUiState.proposalSortOption == CODE` and `proposalSortOrder`. `onClick = onProposalSortChanged(CODE)`. PRESERVE.
   - "Policy count" — same pattern, `POLICY_COUNT`.
10. Divider.
11. **Per-advisor `ExpandableGroupCard`s** (PRESERVE `policiesByAgent` grouping + `expandedAgentCodes` state):
    - Title: `"${agentName} (${agentCode}) — ${policyCount} policies"`.
    - When expanded: list of `DetailedPolicyCard`s for that advisor.
12. Empty state → muted "No policies found for the selected filters." (preserve existing string).

**Duplicates dialog** (PRESERVE): when `proposalsUiState.duplicatePolicies != null && pendingUpload != null`, show dialog:
- Title: "Duplicates found"
- Body: "The file contains ${duplicateCount} polic(ies) that already exist in the database. How would you like to proceed?"
- Buttons: "Overwrite all" (primary), "Skip duplicates" (text), "Cancel" (text)
- Calls `confirmPolicyUpload(overwrite=true/false)` / `cancelPolicyUpload()`.

Restyle dialog as `M3 AlertDialog` with `shape = AppShapes.large`. Keep all callbacks.

#### 3.6.2 Premium Summaries tab (`PremiumSummaryTab`) — mockup §3.2

`Column` with:
- `FilterDropdown` "Select month" (PRESERVE `onMonthSelected`).
- `SortChipsRow` with chips "Code" + "Premium" (PRESERVE `onSummarySortChanged`).
- `LazyColumn` of `PremiumSummaryCard`s (existing — restyle below).
- **Sticky footer** at the bottom: `footer-totals` with two rows: "Monthly total" = `₹${monthlyTotalPremium}` and "Period total" = `₹${appraisalYearTotalPremium}` (PRESERVE these fields). Use a `Box` with `Modifier.align(BottomCenter)` + `HorizontalDivider` above; OR a separate `Column` below `LazyColumn` outside it. Preserve `contentPadding = PaddingValues(bottom = 80.dp)` so totals never cover the last card.

`PremiumSummaryCard` restyle:
- Header `Row` (clickable to toggle):
  - Leading: `Icon(ArrowDownward/ArrowUpward)` (PRESERVE `isExpanded` state).
  - Column: `agentName` (`titleMedium` weight 600) + `agencyCode` (`labelSmall muted`).
  - Trailing: `"₹${totalScheduledPremium}"` in BrandNavy `titleMedium` weight 600.
- Expanded body: monthly breakdown rows, each `Row { Text(month muted) ; Text("₹${premium}") }`.

#### 3.6.3 Performance tab (`AgentPerformanceTab`) — mockup §3.3 — **3 ranking cards must remain**

`LazyColumn`:
1. Header row: "Performance report" title + period dropdown (`FilterDropdown` "This Month / This Quarter / This Year") (PRESERVE `selectedPeriod`, `periodExpanded`).
2. `SummaryStatCard` row (2-up): "Total policies" + "Total premium", each with `delta` line "vs ${previousValue}" with trending-up/down icon (PRESERVE growth logic).
3. **Three `PerformanceRankingCard`s** (PRESERVE all three):
   - "Top performers — Policies" with trophy icon (gold).
   - "Top performers — Premium" with coin icon (gold).
   - "Growth leaders" with trending-up icon (gold).

   Each card restyles the inner medals: top-3 use `RankBadge` circular (gold, silver, bronze). Rest use neutral grey badges. Existing `valueSelector` + `growthSelector` callbacks preserved. Growth text shown to the right.

4. Section header "All advisors".
5. List of `AgentPerformanceCard`s (PRESERVE) — restyled:
   - Header: name + code + growth indicator (TrendingUp/Down + percent).
   - Body: 4-tile `StatTile` row: Policies (with growth delta), Premium, ANANDA, ULIP (preserve `MetricItem` data).

#### 3.6.4 Advisor management tab (`AdvisorManagementTab`) — mockup §3.4

`LazyColumn`:
1. **Admin profile card** (PRESERVE `AdminProfileCard`):
   - `SectionCard` with `BrandNavyContainer` background.
   - Title "My profile".
   - `InfoRow` "DO code" = `admin.doCode`.
   - `OutlinedDateButton` "Appraisal start: ${formatted}" — preserve `selectedStartDate` state and `Set Appraisal Start Date` text when null.
   - When `hasChanges`: filled "Save" button (PRESERVE `onSaveClick(admin.uid, it)`).
2. **`AgentList`**: two sections (PRESERVE):
   - Section "Pending advisor registrations · ${count}": each `AgentCard` (pending variant) with avatar + name + email + filled "Approve" button. Preserve `onApproveClick(agent.uid)`.
   - Section "Registered advisors · ${count}": each `AgentCard` (approved variant) with avatar + name + email + `IconButton(Delete, tint=BrandDanger)` (PRESERVE `showDeleteDialog`). Below: "Advisor code: ${agencyCode}" + `OutlinedDateButton` "Start date: ${formatted}" + Save button when `hasChanges` (PRESERVE `onSaveClick(uid, date)`).

`AgentCard` redesign: wrap in `SectionCard`. Avatar = `Avatar(initials, palette = if pending Gold else Blue, size = 32.dp)`. Background = `#FFFBEB` with `border-color BrandGold` for pending; default for approved.

Preserve the delete confirmation dialog content; restyle shape.

#### 3.6.5 Leads tab (`LeadsTab`) — mockup §3.5

`Column`:
1. `AppSearchBar` "Search by name or advisor code" (PRESERVE `onLeadSearchQueryChanged`).
2. Two-pill toggle: "Active leads" / "Archived leads" using `PillChip` with leading icons (`List` / `Archive`). PRESERVE `showArchived` toggle.
3. `LazyColumn` of `LeadCard`:
   - Avatar + name + "From: ${agentName} (${agentCode})" + "Updated · ${relativeTime}" — PRESERVE existing data lookups (`agentInfoMap`, `agentCodeToNameMap`).
   - Below the row, right-aligned icon buttons: View, Download, Archive/Unarchive (PRESERVE `onViewClick`, `onDownloadClick`, `onArchiveActionClick`).
4. Empty state per `showArchived`.

Preserve all three lead dialogs (`showDownloadDialog`, `showArchiveDialog`, `showRestoreDialog`).

#### 3.6.6 Analytics tab (`AnalyticsTab`) — mockup §3.6

`LazyColumn`:
1. `SectionCard(title="Policy count by plan")`:
   - For each `state.planCounts`: a `bar-row` with label (plan name) + count value + horizontal bar (width proportional to max plan count). Replace the existing plain table.
2. `SectionCard(title="Policy count by advisor")`:
   - For each `state.agentPerformance`: rank badge (top 3 medal colors, rest neutral) + email + count. Preserve order.

`Loading` and `Success` branches preserved.

#### 3.6.7 Data Analysis tab (`AdminDateAnalysisTab`)

Same pattern as Agent §3.5.4 (Single / Compare / Periods modes, pickers, comparison cards). The admin variant uses `AgentBreakdownCard`, `AgentComparisonCard`, `AgentPeriodComparisonCard` instead of `DateStatsCard` — keep these composables and restyle them as `SectionCard`s with the same bar-pair pattern.

#### 3.6.8 Forms / Circulars / Checker / Graphics → §3.10–3.11

### 3.7 Superadmin dashboard (`SuperadminDashboardScreen`)

Tabs (keep order & indices): **Admin Management, Graphics, ULIP Plans, Forms, Circulars**.

Use `PillChip` row (no `ScrollableTabRow`, no `TabRow`).

#### 3.7.1 Admin Management (`AdminManagementTab` + `AdminListContent`) — mockup §4.1

Two sections:
- "Pending approval · ${count}" with each `AdminItemCard` (pending variant):
  - `SectionCard` with `#FFFBEB` background + BrandGold border.
  - Avatar gold + name + email + filled "Approve" button (PRESERVE `onApproveClick(uid)`).
- "Approved · ${count}" with each `AdminItemCard` (approved variant):
  - Avatar green + name + email + green success badge "✓ Approved".

#### 3.7.2 Graphics (`GraphicsManagementScreen`) — mockup §4.2 — **PRESERVE the feature gate flag**

> **Important:** The existing file has `val isFeatureEnabled = false` at the top, with the full upload UI guarded behind it. **Keep the gate variable**. When `isFeatureEnabled == true`, render the redesigned UI below. When `false`, render the existing fallback message ("Feature coming soon" or whatever the else branch currently shows — do not change).

When enabled, `LazyColumn`:

1. `SectionCard(title="Upload new template")`:
   - `LabeledTextField` "Template name" (PRESERVE `templateName` state).
   - Label "Visible to" + `radio-grp` of 3 `PillChip`s ("All users" / "Admins only" / "Advisors only") driven by `selectedRole` (PRESERVE key map: `"all" / "admin" / "advisor"`).
   - Selected image hint text: "Image selected. Ready to upload." when `uiState.selectedImageUri != null`.
   - Row of two buttons:
     - "Select image" (outlined navy) → `imagePickerLauncher.launch("image/*")` (PRESERVE).
     - "Upload as template" (filled gold) → `uploadGraphicTemplate(templateName, selectedRole)` (PRESERVE).
2. Divider + section title "Manage templates · ${count}".
3. `LazyVerticalGrid(GridCells.Adaptive(160.dp))` of `GraphicTemplateCard`:
   - Image (Coil `AsyncImage`, `ContentScale.Crop`, aspect 4:5).
   - Footer row in white with template name + `IconButton(Delete, tint=BrandDanger)` (PRESERVE `onDelete`).
4. `SectionCard(title="Upload new footer")`:
   - `LabeledTextField` "Footer name" (PRESERVE `footerName`).
   - Filled "Upload as footer" button (PRESERVE `uploadGraphicFooter(footerName)` — note: needs `selectedImageUri` too, keep enabled logic).
5. Section title "Manage footers · ${count}".
6. Similar grid of `GraphicFooterCard`.

Preserve all Toast / error handling.

#### 3.7.3 ULIP Plans (`UlipPlanManagementScreen`) — mockup §4.3

`Column`:
1. Header: "Manage ULIP plan numbers" (`titleMedium`) + helper sentence (preserve existing).
2. **Add row**: `LabeledTextField` "New plan number (e.g. 849)" (isMono, digits-only filter — PRESERVE) + a 42-dp `FilledIconButton` "+ " (PRESERVE `onAdd`).
3. `LazyColumn` of `UlipPlanCard`:
   - Plan number as large monospace text (BrandNavy weight 600).
   - Trailing `IconButton(Delete, tint=BrandDanger)` → `removePlanNumber(plan)` (PRESERVE).

#### 3.7.4 Forms / Circulars → §3.10

### 3.8 ULIP screen (`UlipScreen`) — used by Agent + Admin

Inner tabs (4) as `SolidPill` row: **Policy Status, NAV, Fund Value Calculator, Maturity Calculator**.

#### 3.8.1 Policy Status (`UlipExistingPoliciesTab`) — mockup §2.2

Column:
- `AppSearchBar` "Search by name or policy no" (PRESERVE `onSearchQueryChanged`).
- `LazyColumn` of `DetailedPolicyCard` (§3.5.2) — preserve.
- Empty state: "No ULIP policies found."

#### 3.8.2 NAV (`NavTab`) — mockup §2.3

`Box` containing `LazyColumn`:
- Top header `Row`:
  - Column: "Live fund NAVs" (`titleMedium` weight 600) + subtitle:
    - "Refreshing data in background…" when `isFetchingAllNavs && cacheInitialized`
    - "Data cached for today." otherwise (PRESERVE existing logic).
  - Trailing `IconButton(Icons.Default.FilterList)` → `showFilterDialog = true`.
- For each plan group: `CollapsibleNavGroupCard` (PRESERVE):
  - Header: plan name + animated rotation chevron (`ArrowDropDown`).
  - Expanded body: for each `NavData` render `FundDataRow` (PRESERVE) — restyle the row:
    - Column: fund name + SFIN (muted) + "Launch: ${launchDate}" (muted).
    - Trailing: "₹${nav}" BrandNavy `titleMedium` weight 600.
    - Horizontal scroll fallback for narrow widths.

`NavFilterDialog` — keep all checkbox logic. Restyle to `M3 AlertDialog` with `shape = AppShapes.large`.

Loading + error states preserved.

#### 3.8.3 Fund Value Calculator (`UlipFundValueCalculatorTab`) — mockup §2.4

**Preserve the existing "Coming Soon" placeholder** — do not invent a form. Restyle to `EmptyState`:
- Icon: `Icons.Default.Build` or `Icons.Outlined.Construction`, gold.
- Title: "Coming soon"
- Message: existing text ("This feature will be enabled once the method for retrieving the number of units for an existing policy is finalized.")

#### 3.8.4 Maturity Calculator (`UlipMaturityCalculatorTab`) — mockup §2.5

`LazyColumn`. Wrap each existing block in `SectionCard`s:
- Header: "New policy maturity projection" (`titleMedium`).
- `SectionCard(title="Projection inputs")` containing all existing dropdowns/fields **in the same order**:
  - Plan `ExposedDropdownMenuBox` (PRESERVE `onSelectedPlanChanged`).
  - Row: Age + Term (Yrs) (PRESERVE focus moves).
  - Row: Mode + Premium (PRESERVE `isModeSelectionEnabled` toggle and "Single Premium" label fallback).
  - Row: Sum Assured multiplier + Calculated SA (PRESERVE `saMultiplier` and `saOptions = if age <=50 listOf(7,10) else listOf(7)`).
  - Row: Fund choice + CAGR (PRESERVE `cagrOptions` dynamic list + `selectedCagrLabel`).
  - Filled "Calculate projection" button (PRESERVE).
- When `projectionResult != null`:
  - `SectionCard(title="Maturity projection")` showing:
    - "Projected maturity: ${currencyFormat.format(finalMaturityValue)}" (large BrandNavy)
    - "Net yield: ${numberFormat.format(finalNetYield)}%" (BrandSuccess labelMedium)
    - **Projection table** inside a `surfaceVariant` background, `Modifier.horizontalScroll(rememberScrollState())`:
      - Header row: Yr | Premium | Charges | Fund Value
      - Each `ProjectionResultRow` per the existing data (PRESERVE columns + sum-of-charges logic).

### 3.9 Non-Medical Checker (`NonMedicalCheckerScreen`) — mockup §6.3

`LazyColumn`:
1. `SectionCard(title=stringResource(R.string.checker_personal_income_details))`:
   - `LabeledTextField` Age (Number).
   - `LabeledTextField` Annual income (Number, mono).
   - `Row { Checkbox(isResidentIndian) ; Text(...) }` × 3 (Resident Indian, Is minor, Is major student) — PRESERVE all three.
2. `SectionCard(title=stringResource(R.string.checker_qual_occupation_details))`:
   - `FilterDropdown` Qualification (PRESERVE option list).
   - `FilterDropdown` Profession (PRESERVE option list).
3. `SectionCard(title=stringResource(R.string.checker_plan_details))`:
   - `FilterDropdown` Plan number (PRESERVE `availablePlans`).
4. Filled "Check eligibility" `Button` (PRESERVE `checkEligibility()`).
5. **Result section** (only when `resultCategory != null`):
   - `SectionCard(title="Result")`:
     - Eligible (≠ "Ineligible") → `Row` with green circle check icon + "Eligible · ${category}" + below: "Max sum assured: ₹${resultSaLimit} Lakhs".
     - Ineligible → red x icon + "Not eligible" + body text "The case does not fall under any non-medical category."

Preserve every existing string resource and ViewModel method.

### 3.10 Forms (`FormsScreen`) + Circulars (`CircularsScreen`) — mockup §6.1, §6.2

Same shared pattern:
- `AppScaffold` with `topBar` using `containerColor = primaryContainer` (BrandNavyContainer) — preserves existing M3 `topAppBarColors`.
- Right-side action: `IconButton(Icons.Filled.CloudUpload)` visible only when `canUpload`. Opens existing `UploadFormDialog` (Forms) or equivalent (Circulars). PRESERVE all upload flow.
- Body `Column`:
  1. `AppSearchBar`.
  2. `LazyRow` of `PillChip` categories. First chip "All", then `DocumentCategories.ALL_CATEGORIES`. Selected → `viewModel.onCategorySelected(...)`. PRESERVE.
  3. Result count `labelSmall muted` (e.g. "28 forms").
  4. `LazyColumn` of `FormCard` / `CircularCard`:
     - Leading icon by file type: PDF (red doc icon in `BrandDangerBg` square), DOC (blue), default (gray). 32-dp square corner 9.dp.
     - Title (form / circular name `titleMedium` weight 500) + subtitle ("${category} · ${uploadedDate}" `labelSmall muted`).
     - Trailing icon buttons: View (eye, BrandNavy) — opens URL (existing intent). Download (BrandNavy) — only if `downloadUrl != null`.
     - Whole row clickable for View.
- Empty state → `EmptyState`.

### 3.11 Graphics suite (advisor / admin entry from dashboards)

#### 3.11.1 `GraphicsSelectionScreen` — mockup §6.4

Replace `LazyVerticalGrid(GridCells.Fixed(2))` with `LazyVerticalGrid(GridCells.Adaptive(minSize = 160.dp))`. Each `GraphicTemplateGridItem`:
- `Card(shape = AppShapes.medium, modifier = .clickable { ... })`.
- `AsyncImage(model = template.imageUrl, contentScale = ContentScale.Crop, aspectRatio 4:5)`.
- Footer overlay: `Box(align=BottomStart) { Surface(color = surface.copy(alpha=0.95f)) { Text(template.name, padding 8dp, labelMedium weight 500) } }`.
- Preserve URL encoding nav to `graphics_footer_selection/${templateId}?imageUrl=...`.
- Preserve Toast on `uiState.error`.

Empty state → "No graphic templates are available yet." (PRESERVE wording).

#### 3.11.2 `FooterSelectionScreen` — mockup §6.5

`AppScaffold` with `TopAppBar` "Pick a footer":
- `SectionCard(title="Selected template")` with the main image preview.
- "Footers" section header.
- `LazyVerticalGrid(GridCells.Adaptive(120.dp))` of footer cards. Selected card gets `border = 2.dp solid BrandNavy`.
- `BottomActionBar` with "Back" (outline) + "Continue" (primary) — Continue navigates to `graphics_editor/...` (PRESERVE existing logic).

#### 3.11.3 `GraphicsEditorScreen` — mockup §6.6

Preserve `TopAppBar` "Customize graphic" with back arrow. Preserve `FloatingActionButton` "Download Graphic".

Body restyle:
- `SectionCard` containing the layered preview (PRESERVE existing `Box` with main image + footer overlay + profile pic position).
- Below, a `FlowRow` of `PillChip` action buttons: **Profile pic**, **Text**, **Color**. The Profile pic chip triggers `profileImagePickerLauncher.launch("image/*")` (PRESERVE `uploadProfilePicture`). Text / Color are existing edit controls if present — keep their existing actions; if not implemented, render as disabled placeholders.

Preserve Toast error handling.

### 3.12 Data Collection wizard (`DataCollectionScreen` + `DataCollectionWizard`)

Keep every `when (uiState.currentStepTitle)` branch and every `Step_*` composable signature. **Do not change `currentStepTitle` string values** — they're used as state keys by the ViewModel.

#### 3.12.1 Chrome

- `Scaffold` with:
  - `topBar`: standard nav-bar with back arrow (`if (uiState.currentStepIndex > 0) previousStep() else navController.popBackStack()` — PRESERVE), translated `stepTitle` (PRESERVE existing `when` mapping for translation), and trailing "Saved" indicator (PRESERVE — `CircularProgressIndicator` if `isSaving`, "Saved" text otherwise).
  - **Immediately under top bar**: `StepHeader(stepIndex = currentStepIndex, totalSteps = totalSteps, title = stepTitle, subtitle = stepSubtitleMap[currentStepTitle])`.
  - `bottomBar`: `BottomActionBar(onBack = { previousStep() }, onPrimary = { ... }, primaryLabel = if isLastStep "Submit" else "Next", isLastStep = currentStepIndex == totalSteps - 1)` — preserve existing `previousStep()` / `nextStep()` / `submitDataSheet()` calls.

`stepSubtitleMap` — add a `private val` map at the top of the file:
```kotlin
private val stepSubtitleMap = mapOf(
    "Initial Questions" to "Who & what",
    "Plan Details" to "Plan & premium",
    "Personal Details" to "Identity",
    "Life Assured: Personal Details" to "Identity",
    "Proposer: Personal Details" to "Identity",
    "Life Assured (Spouse): Personal Details" to "Identity",
    "Life Assured: Occupation Details" to "Work & income",
    "Proposer: Occupation Details" to "Work & income",
    "Life Assured (Spouse): Occupation Details" to "Work & income",
    "Life Assured: Family & Health" to "Medical history",
    "Proposer: Family & Health" to "Medical history",
    "Life Assured (Spouse): Family & Health" to "Medical history",
    "Life Assured: Nominee & Bank" to "Payouts",
    "Proposer: Nominee & Bank" to "Payouts",
    "Female Insured Info" to "Maternity",
    "Life Assured (Child) Details" to "Minor details",
    "Document Uploads" to "Documents",
    "Review & Submit" to "Final check",
)
```

#### 3.12.2 Field groups

Each `Step_*` composable: wrap its fields in `SectionCard`s. Convert `LazyColumn { item { OutlinedTextField(...) } }` patterns to grouped sections.

- `Step0_InitialQuestions` → one `SectionCard` with the radio groups for plan type / proposer=LA / minor / female insured. Each question rendered as a small heading + `PillChip` group instead of plain radios.
- `Step_PlanDetails` → `SectionCard("Plan")` (Plan dropdown, Sum assured, Term + Paying term row) + `SectionCard("Premium")` (Premium, Mode pills, NACH yes/no).
- `Step_PersonalDetails` → `SectionCard("Identity")` (Aadhaar, PAN, both mono) + `SectionCard("Personal info")` (Gender pills, DOB, Height/Weight row) + `SectionCard("Address")` (address line, City + Pincode row). Preserve all existing fields.
- `Step_OccupationDetails` → one `SectionCard("Occupation")` with all existing fields including Education pills.
- `Step_FamilyHealth` → `SectionCard("Family members")` listing `FamilyMemberCard`s (restyled to `surfaceVariant` rounded rows, dismiss-X icon top right, "Add member" dashed border CTA) + `SectionCard("Health declarations")` with each question as a row (label + small "Yes/No" pills).
- `Step_NomineeBank` → `SectionCard("Nominee(s)")` with `NomineeCard` list + Add CTA + `SectionCard("Appointee")` (conditional, preserve existing condition) + `SectionCard("Bank details${if NACH " · NACH ✓" else ""}")` with `BankDetailsCard`.
- `Step_FemaleInsuredInfo` → `SectionCard("Pregnancy")` + `SectionCard("Menstrual & reproductive")`.
- `Step_LifeAssuredChild` → single `SectionCard("Child")` with all existing fields.

For short option groups (Gender M/F/Other, Mode YLY/HLY/QLY/MLY, NACH Yes/No, Education levels): use `PillChip` rows instead of radios.

**Every existing field must remain.** Every existing date picker, dropdown, and add/remove handler must remain. Only restyle, do not refactor logic.

#### 3.12.3 `DocumentUploadStep` (in `DocumentUploadScreen.kt`)

- Header `SectionCard` "Document slots" with one-liner.
- For each slot:
  - Empty: `DocumentSlotCard` styled as **dashed border** (use `Modifier.drawBehind` with `drawRoundRect` and a dashed `Stroke(pathEffect = PathEffect.dashPathEffect(...))`), centered cloud-upload icon + slot name + helper "Tap to upload PDF / image".
  - Filled: `Card` with thumbnail `AsyncImage` or PDF icon + filename + size, trailing two icon buttons: Replace (refresh, BrandNavy) + Remove (X, BrandDanger).
- Preserve `UploadDocumentDialog` (in `ui/components/UploadDocumentDialog.kt`) and Camera/Files chooser.
- `A4PreviewLayout` and `PreviewImageCard` — keep as-is (print layout, do not restyle).

#### 3.12.4 `ReviewAndSubmitStep` / `ReviewAndSubmitScreen.kt` / `DataSheetView`

- Each existing section (`SectionTitle`) becomes a `SectionCard` with an inline Edit icon (`Icons.Default.Edit` BrandNavy) that calls a no-op stub by default (do not wire to navigation — there's no existing wiring; keep as visual affordance only).
- `InfoRow`: 2-column row with label (`titleSmall` weight 500) + value (`bodyMedium muted` — wait, value should be foreground, label should be primary text. Read mockup §5.10). Use `Modifier.weight(1f)` for value so it wraps.
- Tables (`FamilyHistoryTable`, `BankDetailsTable`, `NomineeDetailsTable`, `AppointeeTable`) — keep the table structure, add zebra striping (`surfaceVariant` every other row), wrap each in `Modifier.horizontalScroll(rememberScrollState())` for narrow screens.
- "Documents" `SectionCard`: show attached doc count as a `success` badge "${n} / ${total}" + flow row of small `info` badges per attached doc filename.

No "Submit" button inside the step itself — submission stays in the `BottomActionBar`.

### 3.13 `ReviewScreen` (read-only review of submitted form)

- `TopAppBar` "Form review" with back nav and trailing `IconButton(Icons.Filled.Download, tint=BrandNavy, contentDescription="Download PDF")` — if the existing screen exposes a download action, wire it; otherwise leave the icon visible and add a `TODO()` comment for codex review.
- Body reuses the redesigned `DataSheetView` from §3.12.4. At the very top, render a summary `SectionCard`: client name + "Submitted ${date}" + green "Submitted" badge.

### 3.14 Splash → already in §3.1

---

## 4. Responsive rules (apply everywhere)

- Top-level body wrapper: `Modifier.fillMaxSize()` + content inside `BoxWithConstraints`. Compute `val compact = maxWidth < 360.dp`. Use it to switch 2-column rows to single-column on small phones.
- Multi-column `Row`s: each child `Modifier.weight(1f)`. Never set fixed widths > 100.dp on form fields.
- `LazyVerticalGrid`: prefer `GridCells.Adaptive(minSize = …)` over `Fixed(N)`.
- Top app bar titles: `maxLines = 1, overflow = TextOverflow.Ellipsis`.
- Tables in `DataSheetView` and ULIP projection: wrap with `Modifier.horizontalScroll(rememberScrollState())`.
- Every `LazyColumn` that overlaps a FAB or `BottomActionBar`: end with `Spacer(Modifier.height(Dimens.FabBottomInset))`.

---

## 5. Animation & polish

- Tab content swap: `Crossfade(targetState = selectedTabIndex)`.
- Cards inside lists: `Modifier.animateItemPlacement()`.
- Pull-to-refresh: `PullToRefreshBox` (existing) with `BrandNavy` spinner.
- `AnimatedVisibility` for `ExpandableGroupCard` body and `PremiumSummaryCard` expansion (already used — keep + extend).
- Skip skeleton shimmers (extra work, low value); keep centred `CircularProgressIndicator` for loading states.

---

## 6. Strings (Hindi mirror)

For every new label introduced in the redesign (e.g. "Show only late policies (>30 days)", "Sort by:", "Performance report", "Top performers — Policies", "Manage ULIP plan numbers", etc.), add the key to BOTH `values/strings.xml` and `values-hi/strings.xml`. If you don't have a Hindi translation, copy the English text and add `<!-- TODO: hi translation -->` above it.

**Do not delete or rename existing string keys.** Many are referenced from ViewModels and tests.

---

## 7. Feature inventory — every item below must remain reachable

### Auth
- [ ] Splash: auto-route per role + role recovery via `ApiClient.api.me()` + clear session on unresolvable role.
- [ ] Login: email + password, show/hide password, remember me, forgot link, register link, role-based redirect, error toasts.
- [ ] Registration: full name, phone (10-digit filter), email, role pills (Advisor/DO), advisor code OR DO code (conditional), password + confirm password (both with visibility toggle).
- [ ] Forgot password: email + send reset link.

### Agent dashboard
- [ ] **My Policies**: search by name/policy/plan, late filter checkbox, Plan dropdown, Mode dropdown, FY dropdown, Agency Year dropdown, date-range picker (enabled only when FY+Agency are "None"), Total + ANANDA stat tiles, list of `DetailedPolicyCard`s, **pull-to-refresh**, FAB → `data_collection/new`.
- [ ] `DetailedPolicyCard`: shortName, policy number with copy button (clipboard + Toast), Plan + Mode, DOC, Last paid (if any), ENACH (if any), Due date with LATE warning (red), Premium, **WhatsApp share button** (constructs share text per existing code + WhatsApp package + chooser fallback), **Payment Link** button (opens existing LIC URL).
- [ ] **ULIP**: all 4 sub-tabs (Status, NAV, Fund Calc placeholder, Maturity Calc).
- [ ] **Data Analysis**: Single Day / Compare Dates / Compare Periods modes + sequential date pickers + auto month comparison on single mode.
- [ ] **Forms** tab (advisor view).
- [ ] **Drafts**: search, three sections (Drafts / Submitted / Archived) with counts, delete confirmation, archive confirmation, restore (archived), download options dialog (Separate / Combined PDFs).
- [ ] **Checker** (Non-Medical) with all inputs + result section.
- [ ] **Graphics**: template grid → footer selection → editor.
- [ ] Profile dialog: email, agency code, start date, EN/HI language radio via `LocaleManager`, close.
- [ ] Refresh, Logout, Offline banner.

### Admin (DO) dashboard
- [ ] **Proposals**: **Upload Proposals button** (`text/plain` picker → `processAndUploadFile`), **Upload Premium button** (`*/*` picker → `processPremiumSummary`), search, late filter, Plan dropdown, Mode dropdown, **date range** picker (no FY/Agency restriction here), Financial Year dropdown, Appraisal Year dropdown, Total Proposals + ANANDA stat tiles, **Sort chips (Code asc/desc / Policy count asc/desc)**, **expandable per-advisor groups** with policies underneath (`DetailedPolicyCard` per policy).
- [ ] **Duplicates found dialog** (Overwrite all / Skip duplicates / Cancel) on both Proposals AND Premium upload.
- [ ] **ULIP** tab (filters policies to ULIP only and renders `UlipScreen`).
- [ ] **Data Analysis** tab with all 3 modes + agent breakdown / agent comparison / agent period comparison cards.
- [ ] **Forms** tab.
- [ ] **Circulars** tab.
- [ ] **Premium Summaries**: month dropdown, sort chips (Code / Premium), expandable cards with **monthly breakdown** per advisor, **sticky footer** with Monthly Total + Period Total.
- [ ] **Performance**: period dropdown (This Month / Quarter / Year), Total Policies + Total Premium summary tiles with vs-previous delta, **3 ranking cards** (Top — Policies / Top — Premium / Growth Leaders), All Advisors detailed list with growth % + 4 metric tiles (Policies / Premium / ANANDA / ULIP).
- [ ] **Advisor Management**: Admin profile card (DO code, **Appraisal Start Date** picker + Save), Pending advisors (Approve), Registered advisors (Delete + advisor code + Start Date picker + Save).
- [ ] **Leads**: search (name OR advisor code), Active / Archived toggle pills, lead card with View / Download / Archive-Restore icons + Download dialog + Archive/Restore confirmation dialogs.
- [ ] **Analytics**: Policy count by plan (bar list), Policy count by advisor (ranked list).
- [ ] **Checker** (shared screen).
- [ ] **Graphics** (shared screen).
- [ ] Refresh, Logout, Offline banner.

### Superadmin
- [ ] **Admin Management**: Pending approval (Approve buttons) + Approved (read-only).
- [ ] **Graphics**: gated by `isFeatureEnabled` flag — when enabled: Upload Template (name + visibility radio + Select Image + Upload), Manage Templates grid (delete per card), Upload Footer (name + Upload), Manage Footers grid (delete per card).
- [ ] **ULIP Plans**: Add plan number (digits-only input + add button), list of plan numbers with delete.
- [ ] **Forms** + **Circulars** tabs.
- [ ] Logout.

### Data Collection wizard
- [ ] Every `Step_*` from the existing `when (uiState.currentStepTitle)` block.
- [ ] Step progression (`previousStep`, `nextStep`, `submitDataSheet`), draft-saved indicator.
- [ ] Document uploads with `UploadDocumentDialog` (Camera / Files), A4 preview, slot replace / remove.
- [ ] Review & Submit with all tables (Family history, Bank, Nominee, Appointee, etc.) and InfoRows.
- [ ] Read-only mode via `readOnly` nav arg (in route).

### Cross-cutting
- [ ] `OfflineBanner`, `SyncStatusBar`.
- [ ] Pull-to-refresh on Agent Policies + Admin Proposals.
- [ ] EN/HI locale switch.
- [ ] All Toast error/success messages preserved.
- [ ] `MainActivity.AppNavigator` routes byte-for-byte unchanged.

---

## 8. Implementation order

1. **Theme** — `Color.kt`, `Theme.kt`, `Dimens.kt`, `Shapes.kt`, `Type.kt`.
2. **Components** — `BrandMark`, `BrandLogo`, `AppScaffold`, `SectionCard`, `StatTile`, `PillChip`, `SolidPill`, `EmptyState`, `LabeledTextField`, `AppSearchBar`, `PullToRefreshSurface`, `StepHeader`, `BottomActionBar`, `UploadButtonRow`, `SortChipsRow`, `Avatar`, `ExpandableGroupCard`, `OutlinedDateButton`, `relativeTime` helper.
3. **CommonUi.kt** restyle: `FilterDropdown`, `StatCard`, `DetailedPolicyCard`, `PremiumSummaryCard`, `SortChip`, `OfflineBanner`, `SyncStatusBar` — keep names, restyle internals.
4. **Auth screens**: Splash, Login, Registration, Forgot password.
5. **Agent dashboard**: chrome → My Policies → Drafts → Data Analysis → other tabs.
6. **Admin dashboard**: Proposals (most complex, do this first to validate the patterns) → Premium Summaries → Performance → Advisor Management → Leads → Analytics → Data Analysis.
7. **Superadmin**: Admin Management → ULIP Plans → Graphics Management.
8. **ULIP screen**: Policy Status → NAV → Fund Calc placeholder → Maturity Calc.
9. **Non-Medical Checker**.
10. **Forms** + **Circulars**.
11. **Graphics suite**: Selection → Footer → Editor.
12. **Data Collection wizard**: chrome + StepHeader → each step's field groupings → Document uploads → Review & Submit.
13. **ReviewScreen**.
14. **Strings**: add new keys to EN + HI.
15. **Manual verification**: build, install, test on 320×568, 360×640, 411×891 emulators in light + dark + EN + HI.

---

## 9. Out of scope

- Backend changes (`network/`, `firebase/`, Cloud Functions).
- ViewModel logic, state shapes, repositories.
- New business features.
- New dependencies beyond what's in `gradle/libs.versions.toml`.
- Changing nav routes or arguments in `MainActivity.kt`.

---

## 10. Acceptance checklist

- [ ] `./gradlew assembleDebug` compiles cleanly.
- [ ] No new third-party dependencies.
- [ ] No public composable in `MainActivity.AppNavigator` had its name or parameter list changed.
- [ ] Every feature in §7 verified by manual click-through on 320×568, 360×640, 411×891 emulators in BOTH light AND dark mode AND BOTH English AND Hindi.
- [ ] No `Color(0xFF…)` literal in screen code outside `ui/theme/Color.kt` (except the documented chart accents).
- [ ] No hardcoded `Dp` value > `4.dp` outside `Dimens.kt` and component-internal layout maths.
- [ ] Lint passes with no new warnings about `contentDescription`, deprecated APIs, or unused resources.
- [ ] `ui_redesign_mockups.html` visually matches the implementation for every numbered tile.
