# UI Audit & Design Direction Document

## Budget Manager Android App
**Audit Date:** 2026-03-21
**Auditor:** Senior Android Product Designer / UI Engineer
**App Version:** 1.3.0 (versionCode 4)

---

## 1. Current UI Stack & Architecture

### Technology Stack
- **UI Framework:** Jetpack Compose (Kotlin Compiler Extension 1.5.8)
- **Design System:** Material Design 3 via `androidx.compose.material3`
- **Compose BOM:** 2024.02.00
- **compileSdk / targetSdk:** 34 (Android 14)
- **minSdk:** 24 (Android 7.0)
- **DI:** Hilt 2.50 with `hilt-navigation-compose`
- **Navigation:** Jetpack Navigation Compose 2.7.7
- **Icons:** Material Icons Extended
- **State Management:** StateFlow + collectAsState pattern
- **Architecture:** MVVM with ViewModels per screen

### Theme Structure
- **Theme.kt** -- `BudgetManagerTheme` wrapping `MaterialTheme` with custom light/dark color schemes
- **Color.kt** -- Manual M3 tonal palettes (Blue, Green, Tertiary amber, Red, Neutral, NeutralVariant) plus semantic colors for income/expense
- **Type.kt** -- Full M3 typography scale using `FontFamily.Default` (system Roboto)
- Dynamic Color supported on Android 12+ (Material You), falls back to custom scheme
- Status bar color is set to `colorScheme.primary` (solid opaque), which is dated behavior post-Android 15

### Navigation Architecture
- `BudgetNavHost` serves as the single root composable with `Scaffold` containing a `TopAppBar` (budget selector) and `BottomNavBar`
- 4 bottom nav destinations: Transactions, Summary, Recurring, Settings
- Additional routes: AddEditTransaction, SignIn, BudgetManagement
- Bottom bar hidden for SignIn, AddEdit, and BudgetManagement
- Migration overlay rendered as a full-screen `Surface` on top of the main scaffold via `Box`

---

## 2. Screen-by-Screen Visual Weaknesses

### 2.1 Sign-In Screen (`SignInScreen.kt`)
**Current State:** The strongest screen visually. Uses gradient background (primary -> primaryContainer -> surface), decorative circles, fade-in animation, branded icon.

**Weaknesses:**
- Decorative circles have very low opacity (0.04-0.08) and are barely visible; they add complexity without payoff
- The gradient relies entirely on the M3 primary color, which under dynamic color can produce jarring combinations
- Hard-coded `Color.White` throughout ignores dark theme semantics -- if dynamic color produces a dark primary, the contrast may fail WCAG AA
- The icon is `Icons.Default.AccountBalance` (a bank building), which is generic and not a brand asset
- Currency badge ("ILS") feels arbitrary and disconnected from the visual hierarchy
- RoundedCornerShape(12.dp) for buttons doesn't match Material 3's default full-rounded buttons (28.dp radius)

### 2.2 Transaction List Screen (`TransactionListScreen.kt`)
**Current State:** Functional list with FilterBar, FAB, pull-to-refresh, swipe-to-delete.

**Weaknesses:**
- No section headers or date grouping -- transactions render as a flat undifferentiated list
- Card vertical padding is only 4.dp, making the list feel cramped
- No visual hierarchy between today's transactions and older ones
- The FilterBar scrolls horizontally but has no visual indication of scrollability (no fade edge or scroll indicator)
- Empty state is adequate but generic; the icon size (72.dp) is slightly too large for the message length
- No balance/totals summary visible at the top of the list -- users must navigate to Summary tab
- The `Scaffold` within `TransactionListScreen` creates a nested scaffold situation (parent `BudgetNavHost` already provides one), which is a structural anti-pattern
- Pull-to-refresh indicator (`PullToRefreshContainer`) floats without background, can overlap the filter bar

### 2.3 Add/Edit Transaction Screen (`AddEditTransactionScreen.kt`)
**Current State:** Simple form layout with type toggle, text fields, date picker, save/cancel buttons.

**Weaknesses:**
- Error text uses hard-coded `Color.Red` (`androidx.compose.ui.graphics.Color.Red`) instead of `MaterialTheme.colorScheme.error` -- this is a theme violation and looks jarring
- Type toggle uses `FilterChip` in a plain `Row` without any visual grouping or background -- it looks disconnected from the form
- No input validation feedback until save is attempted (no inline validation)
- Spacer-based layout with uniform 16.dp gaps creates a monotonous vertical rhythm
- No "unsaved changes" warning on back navigation
- The Cancel/Save button row has no top visual separator from the form content
- Category input is a plain text field with no autocomplete or suggestion chips from existing categories
- Date is displayed as raw `LocalDate.toString()` (e.g., "2026-03-21") with no friendly formatting

### 2.4 Monthly Summary Screen (`MonthlySummaryScreen.kt`)
**Current State:** Month navigator, three summary cards (Income/Expenses/Balance), category breakdown list.

**Weaknesses:**
- The three summary cards use default `Card` with no explicit elevation or color differentiation -- they look identical and blend into the background
- Card internal padding is only 12.dp, making the content feel squeezed
- Labels ("Income", "Expenses", "Balance") use `labelMedium` which is too small for scanability
- Category breakdown is a plain `Row` list with no visual indicators (bars, icons, or progress) -- it's just text
- The `SummaryChart` component exists but is NOT used on this screen -- the screen misses an opportunity for data visualization
- No percentage display for category proportions
- No total row at the bottom of the category breakdown
- Month navigation arrows are small icon buttons with no visual affordance (no background/tint)
- The entire screen has no pull-to-refresh capability

### 2.5 Recurring Transactions Screen (`RecurringScreen.kt`)
**Current State:** List of recurring items as Cards with category, frequency, amount, next occurrence, and active toggle.

**Weaknesses:**
- Cards use default styling with no elevation or border -- they merge visually into each other
- Information density is high but unstructured; category, frequency, type, description, amount, and next date are stacked with minimal hierarchy
- The Switch for active/inactive toggle has no label -- accessibility concern
- The "Add" and "Edit" forms are AlertDialogs, which are cramped for the amount of content they contain (6+ form fields in a dialog is a UX anti-pattern)
- The `RecurringFormDialog` is 150+ lines of form inside an `AlertDialog` text slot -- the scrolling experience is poor
- No visual distinction between active and inactive recurring items (only the Switch state differs)
- The "Paused" label has insufficient visual weight compared to the active "Next: ..." label
- Detail dialog (`ViewRecurringDialog`) puts Edit and Delete icons in the title row, which is unconventional and can cause misclicks

### 2.6 Settings Screen (`SettingsScreen.kt`)
**Current State:** Scrollable column with Account, Backup, and About sections. Cards with surfaceVariant backgrounds.

**Weaknesses:**
- Section headers use `titleSmall` with `FontWeight.Bold` in primary color -- visually adequate but inconsistent with other screen headers
- All cards use `surfaceVariant` container color, creating visual monotony
- The Account card layout differs between SignedIn, Guest, and SignedOut states, but the transitions are abrupt (no animation)
- Divider between account info and sign-out button uses `outline.copy(alpha = 0.3f)` which may be invisible on some dynamic color themes
- Import/Export buttons are vertically stacked but could benefit from icon-leading prominence
- The "About" section shows only app name and version -- feels sparse
- No visual branding in the About card

### 2.7 Budget Management Screen (`BudgetManagementScreen.kt`)
**Current State:** List of budget cards with name, description, currency badge, target badge, and active indicator.

**Weaknesses:**
- Active budget uses `primaryContainer` while inactive uses `surfaceVariant` -- the distinction is subtle on some color schemes
- Long-press for delete is a hidden gesture with no discoverability (no tooltip, onboarding, or visual cue)
- No swipe-to-delete consistency with TransactionListScreen (transactions use swipe, budgets use long-press)
- The form dialogs for create/edit are AlertDialogs with scrollable content -- same UX concern as RecurringScreen

### 2.8 Migration Screen (`MigrationScreen.kt`)
**Current State:** Full-screen form for initial budget setup during data migration. Has a success state with checkmark.

**Weaknesses:**
- The success state icon (80.dp `CheckCircle`) is large and looks static -- no celebratory animation
- The form uses the same plain `OutlinedTextField` styling as other screens with no distinct "onboarding" feel
- Error message placement (between form and button) can be missed

### 2.9 Budget Selector Dropdown (`BudgetSelectorDropdown.kt`)
**Current State:** `ExposedDropdownMenuBox` in the TopAppBar title slot.

**Weaknesses:**
- The dropdown trigger is a `TextButton` which doesn't look like a selector -- users may not realize it's tappable
- The `ArrowDropDown` icon provides the only affordance and is small
- Currency badges in the dropdown use `secondaryContainer` which may not contrast well with the menu background

---

## 3. Color Palette Analysis

### Current Palette (Custom Light Scheme)

| Role | Color | Hex | Assessment |
|------|-------|-----|------------|
| Primary | Blue40 | `#1A73E8` | Google Blue -- recognizable but generic; feels borrowed rather than owned |
| Secondary | Green40 | `#00796B` | Teal -- good for income semantics but clashes with the warmer neutral tones |
| Tertiary | Tertiary40 | `#8B5E00` | Dark amber -- rarely used, feels disconnected from the blue/teal pairing |
| Error | Red40 | `#BA1A1A` | Standard M3 error red -- functional but used double-duty for expenses, creating cognitive overload (error = expense = bad?) |
| Background | Neutral99 | `#FFFBFF` | Near-white with slight warm cast -- appropriate |
| Surface | Neutral99 | `#FFFBFF` | Same as background -- no surface layering differentiation |
| SurfaceVariant | NeutralVariant90 | `#E7E0EC` | Slightly purple-tinted neutral -- clashes with the blue primary |

### Semantic Colors

| Purpose | Light | Dark | Issue |
|---------|-------|------|-------|
| Income | `#2E7D32` (IncomeGreen) | `#81C784` | Adequate green but very similar to the teal secondary -- confusing |
| Expense | `#C62828` (ExpenseRed) | `#EF9A9A` | Dark red, reads aggressive; the dark mode variant `#EF9A9A` is quite washed out |
| Balance+ | `#1B5E20` | `#A5D6A7` | Very dark green, almost unreadable on dark surfaces |
| Balance- | `#B71C1C` | `#EF9A9A` | Same dark-mode color as ExpenseRed -- not differentiated |

### What Works
- The blue primary is universally trusted for finance apps
- Having dedicated semantic colors for income/expense is correct practice
- The tonal palette structure (10/20/30/40/50/80/90/95/99) follows M3 conventions
- Chart colors are varied and distinguishable

### What Doesn't Work
- **Purple-tinted neutrals** (NeutralVariant90 = `#E7E0EC`) clash with the cool blue primary; they create an unintentional lilac undertone
- **Secondary (teal) and IncomeGreen are too similar** in hue, causing confusion between "secondary action" and "positive amount"
- **Error and ExpenseRed overlap semantically** -- expenses are not errors, but they share the red signaling
- The **Tertiary amber is orphaned** -- barely used anywhere in the UI
- **Surface and Background are identical** (`#FFFBFF`), defeating M3's surface tinting model
- Dark mode semantic colors are too pastel, reducing the visual impact of amount displays
- No `surfaceTint`, `inverseSurface`, or `scrim` colors are defined

### Dynamic Color Override
The theme defaults to `dynamicColor = true` on Android 12+. This means the carefully defined palette is only used on older devices. On newer devices, the wallpaper-derived colors completely replace the custom scheme, which may produce palettes that conflict with the hardcoded semantic colors (`IncomeGreen`, `ExpenseRed`, etc.).

---

## 4. Typography Analysis

### Current Scale
The typography scale is a nearly stock M3 definition using `FontFamily.Default` (Roboto on Android) with minor weight adjustments:
- Headlines use `SemiBold` instead of M3's default `Normal`
- `titleLarge` uses `Bold` (non-standard for M3)
- Letter spacing matches M3 defaults

### Hierarchy Issues
- **No custom font family** -- the app looks identical to every default Material app; no typographic personality
- **Headlines are overweight** -- `SemiBold` headlines paired with `Bold` titleLarge creates a heavy, shouty feel
- **bodyLarge letterSpacing (0.5sp) is too wide** for a finance app where numbers and currency symbols need tight spacing
- **labelSmall (11sp)** is used for dates in TransactionCard -- this is below the recommended 12sp minimum for readability
- **No numeric-specific styling** -- amounts are displayed with the same text styles as labels, despite being the most important data on screen
- The typography scale is defined but individual screens frequently override `fontWeight` inline (`FontWeight.Bold`, `FontWeight.SemiBold`), creating inconsistency

### Readability Concerns
- On the Summary screen, `labelMedium` (12sp) is used for card headers ("Income", "Expenses", "Balance") -- these are too small to scan quickly
- Amount text uses `titleMedium` (16sp) for transaction amounts, which is adequate, but the `fontWeight = SemiBold` applied in `AmountText` combined with `titleMedium`'s own `SemiBold` default is redundant
- No optimized line heights for numeric content

---

## 5. Spacing & Layout Consistency

### Current Spacing Patterns (Observed)

| Context | Value | Frequency | Issue |
|---------|-------|-----------|-------|
| Screen padding | 16.dp | 8 screens | Consistent |
| Card internal padding | 16.dp | Most cards | Consistent |
| Card internal padding | 12.dp | Summary cards | **Inconsistent** |
| Vertical spacing between sections | 16.dp | Common | Consistent |
| Vertical spacing between sections | 24.dp | Settings | **Inconsistent** |
| Card vertical margin | 4.dp | Transaction/Recurring list | Consistent |
| Button row gap | 16.dp | AddEdit screen | Adequate |
| Filter chip gap | 8.dp | FilterBar | Consistent |
| Dialog content gap | 12.dp | Recurring/Budget forms | Consistent |

### Layout Issues
- **No spacing scale** -- values appear to be ad-hoc (2.dp, 4.dp, 8.dp, 12.dp, 16.dp, 24.dp, 32.dp). While most are multiples of 4, 2.dp spacers in TransactionCard break the grid
- **Summary cards use 12.dp padding** while all other cards use 16.dp -- visually jarring when switching between tabs
- **Sign-in screen uses 32.dp padding** while all other screens use 16.dp -- intentional for onboarding but creates a noticeable density shift
- **No consistent content width constraint** -- on tablets, all content stretches to full width
- **Nested Scaffolds** -- TransactionListScreen and RecurringScreen both contain their own `Scaffold` inside the parent `BudgetNavHost` Scaffold, creating double padding zones and redundant FAB containers

---

## 6. Component Reuse Patterns & Inconsistencies

### Well-Reused Components
| Component | Used In | Assessment |
|-----------|---------|------------|
| `AmountText` | TransactionCard, SummaryScreen, RecurringScreen | Good reuse; consistent formatting |
| `EmptyStateView` | TransactionList, RecurringScreen, BudgetManagement | Good reuse with customizable message/icon |
| `CategoryChip` / `FilterChip` | FilterBar, AddEdit, RecurringForm | Adequate |
| `TransactionCard` | TransactionListScreen | Used only once; could be reused in Summary |
| `SwipeToDeleteContainer` | TransactionListScreen | Used only once; NOT used for Recurring or Budget items |
| `DatePickerField` | AddEditTransaction | Used only once |
| `SummaryChart` | **NOWHERE** | Defined but never integrated -- dead code |

### Inconsistencies
1. **Delete gesture varies by screen:**
   - Transactions: swipe-to-delete
   - Recurring: dialog with delete button
   - Budgets: long-press -> confirmation dialog
   This is a major consistency failure.

2. **Form presentation varies by screen:**
   - Add/Edit Transaction: full-screen with TopAppBar
   - Add/Edit Recurring: AlertDialog
   - Add/Edit Budget: AlertDialog
   Forms with similar complexity use different containers.

3. **Type toggle pattern varies:**
   - AddEditTransaction: `FilterChip` in a `Row` with `Spacer`
   - RecurringFormDialog: `FilterChip` in a `Row` with `Arrangement.spacedBy`
   Same widget, different layout parameters.

4. **Loading indicators vary:**
   - TransactionList, RecurringScreen, BudgetManagement: centered `CircularProgressIndicator`
   - Settings backup: inline `CircularProgressIndicator` inside button
   Both patterns exist but no shared `LoadingOverlay` component.

5. **Navigation patterns vary:**
   - AddEditTransaction, BudgetManagement: TopAppBar with back arrow
   - Summary, Recurring, Transactions: no TopAppBar (budget selector shown instead)
   - Settings: no TopAppBar at all; screen title is inline `headlineMedium`

---

## 7. Dark Mode Quality Assessment

### Implementation
- Full light/dark color schemes are defined with appropriate M3 tonal mappings
- Dynamic color is enabled by default on Android 12+
- `isSystemInDarkTheme()` is checked in `AmountText`, `SummaryChart`, and `SwipeToDeleteContainer` to select semantic colors

### Issues
1. **Semantic colors bypass the theme system:** `AmountText` calls `isSystemInDarkTheme()` directly instead of reading from `MaterialTheme.colorScheme`. This means dynamic color is completely ignored for income/expense colors -- users on Android 12+ see wallpaper-derived UI chrome but hard-coded greens and reds for amounts.

2. **Status bar color is opaque primary:** `window.statusBarColor = colorScheme.primary.toArgb()` creates a solid colored status bar. On Android 15+ (SDK 35), this API is deprecated and the system enforces edge-to-edge. The current implementation will break.

3. **No surface tinting:** Dark mode surfaces are flat `Neutral10` (`#1C1B1F`) with no elevation-based tinting, making cards indistinguishable from the background.

4. **Hard-coded `Color.White`:** The Sign-In screen uses `Color.White` for text and icon tints. If the primary color is light (possible with dynamic color), white text will be invisible.

5. **Dark mode pastel amounts:** `IncomeGreenDark = #81C784` and `ExpenseRedDark = #EF9A9A` are too pale on dark surfaces, reducing the visual punch that makes finance amounts scannable.

6. **SwipeToDeleteBackground uses `Color.White` for the icon tint** regardless of theme -- acceptable on the red background but not theme-aware.

7. **No `values-night/themes.xml`:** The XML theme `Theme.BudgetManager` only has a light variant (`android:Theme.Material.Light.NoActionBar`). The splash screen will flash white before Compose renders in dark mode.

---

## 8. Low-Effort, High-Impact Opportunities

Ranked by impact-to-effort ratio (highest first):

### 8.1 Fix the hardcoded `Color.Red` in AddEditTransactionScreen
**Effort:** 1 minute | **Impact:** High
Replace `androidx.compose.ui.graphics.Color.Red` with `MaterialTheme.colorScheme.error`. This is a theme violation that looks broken.

### 8.2 Integrate `SummaryChart` into MonthlySummaryScreen
**Effort:** 15 minutes | **Impact:** High
The component exists and works. Adding it to the Summary screen instantly adds data visualization and visual richness.

### 8.3 Add date grouping headers to TransactionList
**Effort:** 30 minutes | **Impact:** High
Group transactions by date with sticky headers ("Today", "Yesterday", "March 19"). This is the single biggest readability improvement for the primary screen.

### 8.4 Move semantic colors into the theme's extended color scheme
**Effort:** 20 minutes | **Impact:** Medium-High
Create a `CompositionLocal` for budget-specific semantic colors (income, expense, balance) that adapts to light/dark/dynamic. Eliminates all `isSystemInDarkTheme()` calls in components.

### 8.5 Increase TransactionCard vertical padding to 8.dp
**Effort:** 1 minute | **Impact:** Medium
Change `vertical = 4.dp` to `vertical = 6.dp` in the list item padding. Instantly improves list readability and touch target spacing.

### 8.6 Replace nested Scaffolds with plain Columns
**Effort:** 15 minutes | **Impact:** Medium
TransactionListScreen and RecurringScreen should not wrap themselves in `Scaffold`. Move FABs to the parent `BudgetNavHost` scaffold or use a shared pattern.

### 8.7 Unify delete patterns
**Effort:** 30 minutes | **Impact:** Medium
Use swipe-to-delete consistently across Transactions, Recurring, and Budgets. Reuse `SwipeToDeleteContainer`.

### 8.8 Add edge-to-edge support
**Effort:** 30 minutes | **Impact:** Medium
Replace `window.statusBarColor` with `enableEdgeToEdge()` and handle insets properly. Prevents breakage on Android 15.

### 8.9 Add a custom font family
**Effort:** 20 minutes | **Impact:** Medium
Adopt Inter, Plus Jakarta Sans, or DM Sans as the app font. A single font change elevates perceived quality significantly.

### 8.10 Add a values-night/themes.xml
**Effort:** 5 minutes | **Impact:** Low-Medium
Create `<style name="Theme.BudgetManager" parent="android:Theme.Material.NoActionBar" />` for dark mode splash screen.

---

## 9. Proposed Design Direction

### Design Philosophy
**"Calm Finance"** -- The app should feel like a trusted financial advisor: composed, clear, and quietly confident. Every element should reduce cognitive load and reinforce the user's sense of control over their money.

### 9.1 Primary Color Palette

The palette moves from Google Blue to a deeper, more distinctive indigo-blue that conveys trust and sophistication without feeling generic. Green is repositioned as a true semantic color (not secondary UI), and a calming sage replaces teal as the secondary.

| Role | Hex | Name | Usage |
|------|-----|------|-------|
| **Primary** | `#2B5EA7` | Deep Trust Blue | App bar, FABs, primary actions, active states |
| **Primary Container** | `#D4E3FF` | Light Blue Wash | Selected states, active budget card background |
| **Secondary** | `#526070` | Slate Blue | Secondary UI elements, inactive states |
| **Secondary Container** | `#D6E4F0` | Mist | Chips, badges, secondary cards |
| **Tertiary** | `#6B5778` | Soft Plum | Accent moments, category highlights |
| **Tertiary Container** | `#F2DAFF` | Lavender Mist | Tertiary badges, target indicators |
| **Background** | `#FAFCFF` | Ice White | Screen backgrounds |
| **Surface** | `#FFFFFF` | Pure White | Cards, sheets, dialogs |
| **Surface Variant** | `#E0E3E8` | Cool Grey | Input fields, inactive chips |
| **Error** | `#BA1B1B` | Alert Red | Validation errors, destructive actions |
| **On Error Container** | `#410002` | Deep Red | Error text on error container |
| **Income (semantic)** | `#1A7D36` | Growth Green | Income amounts, positive balance |
| **Expense (semantic)** | `#C4442B` | Warm Red | Expense amounts, negative balance |
| **Warning (semantic)** | `#8B6914` | Amber | Budget limit warnings, approaching targets |
| **Success (semantic)** | `#1A7D36` | Growth Green | Confirmation states, migration complete |

#### Dark Mode Adjustments

| Role | Hex | Name |
|------|-----|------|
| Primary | `#A5C8FF` | Bright Blue |
| Primary Container | `#0E4483` | Deep Blue |
| Background | `#111418` | Charcoal |
| Surface | `#1A1C20` | Dark Slate |
| Surface Variant | `#42474E` | Medium Slate |
| Income (semantic) | `#6ECF81` | Bright Green |
| Expense (semantic) | `#FFB4A8` | Soft Coral |

### 9.2 Typography Scale

Adopt **Inter** (or **DM Sans** as fallback) as the primary font family. Inter is optimized for screen readability, has excellent numeric character support, and conveys modernity without trendiness.

| Style | Font | Weight | Size | Line Height | Letter Spacing | Usage |
|-------|------|--------|------|-------------|----------------|-------|
| Display Large | Inter | Regular | 57sp | 64sp | -0.25sp | Not used in-app |
| Display Medium | Inter | Regular | 45sp | 52sp | 0sp | Not used in-app |
| Display Small | Inter | Medium | 36sp | 44sp | 0sp | Sign-in title |
| Headline Large | Inter | SemiBold | 28sp | 36sp | 0sp | Screen titles (Settings) |
| Headline Medium | Inter | SemiBold | 24sp | 32sp | 0sp | Dialog titles, section heads |
| Headline Small | Inter | Medium | 20sp | 28sp | 0sp | Card section headers |
| Title Large | Inter | SemiBold | 20sp | 28sp | 0sp | TopAppBar titles |
| Title Medium | Inter | Medium | 16sp | 24sp | 0.15sp | Card titles, category names |
| Title Small | Inter | Medium | 14sp | 20sp | 0.1sp | Section labels |
| Body Large | Inter | Regular | 16sp | 24sp | 0.25sp | Primary body text |
| Body Medium | Inter | Regular | 14sp | 20sp | 0.25sp | Secondary text, descriptions |
| Body Small | Inter | Regular | 12sp | 16sp | 0.4sp | Tertiary text, dates |
| Label Large | Inter | Medium | 14sp | 20sp | 0.1sp | Buttons, chip labels |
| Label Medium | Inter | Medium | 12sp | 16sp | 0.5sp | Badges, tags |
| Label Small | Inter | Medium | 11sp | 16sp | 0.5sp | Micro labels (use sparingly) |
| **Amount Display** | Inter | SemiBold | 20sp | 24sp | -0.15sp | Primary amount in cards |
| **Amount Large** | Inter | Bold | 28sp | 32sp | -0.25sp | Summary totals |
| **Amount Small** | Inter | Medium | 14sp | 20sp | 0sp | Category breakdowns |

Key changes:
- Reduced `headlineLarge` from 32sp to 28sp (32sp was never appropriate for mobile screens)
- Introduced dedicated Amount styles with tighter letter spacing for numeric readability
- Reduced letter spacing on body text (0.5sp -> 0.25sp) for less airy, more controlled feel
- All headings use consistent SemiBold-to-Medium weight gradient

### 9.3 Spacing Scale (4dp Grid)

All spacing must be multiples of 4dp. No 2.dp spacers.

| Token | Value | Usage |
|-------|-------|-------|
| `space-xs` | 4dp | Inline element gaps, icon-to-text |
| `space-sm` | 8dp | Between related items (chip gaps, label-to-value) |
| `space-md` | 12dp | Form field gaps, card content sections |
| `space-lg` | 16dp | Screen padding, card internal padding, section gaps |
| `space-xl` | 24dp | Between major sections |
| `space-2xl` | 32dp | Top/bottom screen padding, sign-in spacing |
| `space-3xl` | 48dp | Feature section dividers |

### 9.4 Corner Radius System

| Token | Value | Usage |
|-------|-------|-------|
| `radius-xs` | 4dp | Badges, small tags |
| `radius-sm` | 8dp | Chips, small cards, input fields |
| `radius-md` | 12dp | Standard cards, dialogs |
| `radius-lg` | 16dp | Bottom sheets, large cards |
| `radius-xl` | 24dp | FABs, sign-in buttons |
| `radius-full` | 50% | Avatar containers, circular badges |

Currently the app uses a mix of 4dp, 8dp, and 12dp radii inconsistently. The proposed system maintains M3 conventions while standardizing usage.

### 9.5 Elevation Strategy

Adopt M3 tonal elevation rather than shadow-based elevation for a modern, flat-but-layered aesthetic.

| Level | Elevation | Tonal Tint | Usage |
|-------|-----------|------------|-------|
| Level 0 | 0dp | None | Background surfaces |
| Level 1 | 1dp | +5% primary | Standard cards (TransactionCard, BudgetCard) |
| Level 2 | 3dp | +8% primary | Active/focused cards, elevated summary cards |
| Level 3 | 6dp | +11% primary | Dialogs, dropdown menus |
| Level 4 | 8dp | +12% primary | Navigation bar, bottom sheets |
| Level 5 | 12dp | +14% primary | FABs |

Current issue: `TransactionCard` uses `defaultElevation = 1.dp` but surface and background are the same color, so the 1dp shadow is the only differentiation. With tonal elevation, cards will be subtly tinted even at 1dp.

### 9.6 Icon Style Direction

- **Primary icons:** Material Symbols Rounded (not the current Filled style). Rounded icons feel friendlier and more approachable, matching the "calm" design brief.
- **Icon weight:** 400 (Regular) for navigation and chrome; 500 (Medium) for action buttons.
- **Icon optical size:** 24dp for navigation, 20dp for in-card icons, 48dp+ for empty states.
- **Category icons:** Introduce a mapped set of category-specific icons (Food -> Restaurant, Transport -> DirectionsCar, etc.) to add visual scannability to the transaction list.
- **App icon:** Redesign with the new Deep Trust Blue (`#2B5EA7`) primary and a simplified shekel motif. The current green (#1B5E20) app icon conflicts with the blue primary used throughout the app.

### 9.7 Motion Language Principles

| Principle | Implementation |
|-----------|---------------|
| **Responsive, not performative** | Transitions should acknowledge user actions (ripple, scale) but not delay them. Max transition duration: 300ms for navigation, 200ms for micro-interactions. |
| **Shared element transitions** | When navigating from TransactionList to AddEditTransaction, the card should expand into the form (shared element container transform). |
| **Staggered list loading** | Transaction list items should appear with a subtle 40ms staggered fade+slide-up animation on first load (not on scroll). |
| **Chart animation** | SummaryChart progress bars already animate well (600ms + 80ms stagger). Keep this but reduce to 400ms + 50ms for snappier feel. |
| **Tab transitions** | Bottom navigation tab switches should use fade-through (M3 standard) rather than the current instant swap. |
| **Swipe delete** | Current implementation is good. Add a subtle haptic feedback when the dismiss threshold is crossed. |
| **Amount counting** | When summary amounts load, animate from 0 to final value with an ease-out curve over 400ms. |

---

## 10. Execution Plan (Priority Order)

### Phase 1: Foundation (Week 1) -- Critical Fixes & Theme
1. **Fix Color.Red bug** in AddEditTransactionScreen -- replace with `MaterialTheme.colorScheme.error`
2. **Create extended color scheme** with `CompositionLocal` for semantic colors (income, expense, warning, success) that respects light/dark/dynamic
3. **Remove all `isSystemInDarkTheme()` calls** from components; use the new semantic color system
4. **Update Color.kt** with proposed palette (Deep Trust Blue system)
5. **Update Theme.kt** to support edge-to-edge, remove deprecated `statusBarColor` usage
6. **Add `values-night/themes.xml`** for dark splash screen
7. **Fix neutrals** -- replace purple-tinted NeutralVariant with cool grey tones

### Phase 2: Typography & Spacing (Week 1-2) -- Visual Consistency
8. **Integrate Inter font family** via Google Fonts or bundled .ttf
9. **Update Type.kt** with proposed scale including Amount styles
10. **Create spacing tokens** as Compose constants (`object Spacing { val xs = 4.dp ... }`)
11. **Audit and fix all spacing** to conform to 4dp grid (eliminate 2.dp spacers)
12. **Standardize card padding** to 16.dp across all cards

### Phase 3: Component Polish (Week 2) -- Reusable Elements
13. **Add date-grouped headers** to TransactionListScreen with sticky header support
14. **Integrate SummaryChart** into MonthlySummaryScreen
15. **Create Amount display component variants** (AmountLarge, AmountSmall) using the new typography
16. **Redesign summary cards** with proper elevation, larger labels, and visual differentiation
17. **Unify delete patterns** -- implement swipe-to-delete for Recurring and Budgets
18. **Replace nested Scaffolds** with simple Columns; centralize FAB management

### Phase 4: Screen Redesigns (Week 2-3) -- Individual Screen Polish
19. **Redesign TransactionListScreen** -- add inline balance summary, improve card spacing, add scroll-edge fade on FilterBar
20. **Redesign MonthlySummaryScreen** -- add bar chart, percentage breakdowns, totals row, visual card hierarchy
21. **Convert RecurringFormDialog to bottom sheet** -- AlertDialog is too cramped for 6+ fields
22. **Convert BudgetFormDialog to bottom sheet** -- same rationale
23. **Polish SignInScreen** -- replace decorative circles with subtle geometric pattern, fix Color.White hardcodes
24. **Add category autocomplete** to AddEditTransactionScreen with suggestion chips

### Phase 5: Motion & Delight (Week 3-4) -- Premium Feel
25. **Add shared element transitions** for card-to-edit navigation
26. **Add staggered list animations** for initial load
27. **Add amount counting animation** for summary totals
28. **Add fade-through navigation transitions** between bottom nav tabs
29. **Add haptic feedback** for swipe-to-delete threshold crossing
30. **Redesign app icon** with new Deep Trust Blue palette

### Phase 6: Accessibility & Polish (Week 4) -- Final Quality
31. **Audit all touch targets** -- ensure minimum 48dp
32. **Add content descriptions** to all decorative elements and toggles
33. **Test all screens** against WCAG AA contrast requirements
34. **Add tablet layout support** with max content width constraints
35. **Test dynamic color compatibility** -- ensure semantic colors work with all wallpaper-derived schemes
36. **Performance audit** -- lazy layout optimization, recomposition tracking
