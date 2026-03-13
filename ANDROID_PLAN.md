# BudgetManager Android App - Architecture Plan

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 1.9.22 |
| UI Framework | Jetpack Compose | BOM 2024.02.00 |
| Design System | Material Design 3 | 1.2.0 |
| Local Database | Room | 2.6.1 |
| Dependency Injection | Hilt | 2.50 |
| Async | Kotlin Coroutines + Flow | 1.7.3 |
| Navigation | Compose Navigation | 2.7.7 |
| Auth | Google Sign-In (Credential Manager) | 1.2.2 |
| Serialization | Kotlinx Serialization | 1.6.2 |
| Build System | Gradle (Kotlin DSL) | 8.2.2 (AGP) |
| Min SDK | 26 | - |
| Target SDK | 34 | - |

---

## Package Structure

```
com.budgetmanager.app/
|
+-- BudgetManagerApp.kt               # @HiltAndroidApp Application class
+-- MainActivity.kt                    # Single-activity, setContent { BudgetManagerTheme }
|
+-- data/
|   +-- db/
|   |   +-- BudgetDatabase.kt          # @Database(entities, version=1)
|   |   +-- Converters.kt              # TypeConverters for LocalDate, etc.
|   +-- entity/
|   |   +-- TransactionEntity.kt
|   |   +-- RecurringTransactionEntity.kt
|   +-- dao/
|   |   +-- TransactionDao.kt
|   |   +-- RecurringTransactionDao.kt
|   +-- repository/
|       +-- TransactionRepository.kt        # Interface
|       +-- TransactionRepositoryImpl.kt
|       +-- RecurringRepository.kt           # Interface
|       +-- RecurringRepositoryImpl.kt
|       +-- BackupRepository.kt              # Interface
|       +-- BackupRepositoryImpl.kt
|
+-- domain/
|   +-- model/
|   |   +-- Transaction.kt              # Domain model
|   |   +-- RecurringTransaction.kt      # Domain model
|   |   +-- TransactionType.kt           # enum class TransactionType { INCOME, EXPENSE }
|   |   +-- Frequency.kt                 # enum class Frequency { WEEKLY, MONTHLY }
|   |   +-- MonthlySummary.kt            # Data class with totals + category breakdowns
|   |   +-- CategoryBreakdown.kt
|   |   +-- BackupData.kt
|   +-- usecase/
|       +-- GetTransactionsUseCase.kt
|       +-- CreateTransactionUseCase.kt
|       +-- UpdateTransactionUseCase.kt
|       +-- DeleteTransactionUseCase.kt
|       +-- GetMonthlySummaryUseCase.kt
|       +-- GetRecurringTransactionsUseCase.kt
|       +-- CreateRecurringUseCase.kt
|       +-- UpdateRecurringUseCase.kt
|       +-- DeleteRecurringUseCase.kt
|       +-- GenerateRecurringUseCase.kt
|       +-- ExportDataUseCase.kt
|       +-- ImportDataUseCase.kt
|
+-- ui/
|   +-- theme/
|   |   +-- Theme.kt                    # Material 3 dynamic color theme
|   |   +-- Color.kt
|   |   +-- Type.kt
|   +-- navigation/
|   |   +-- BudgetNavHost.kt            # NavHost with all routes
|   |   +-- Screen.kt                   # Sealed class for routes
|   |   +-- BottomNavBar.kt             # Bottom navigation composable
|   +-- screens/
|   |   +-- signin/
|   |   |   +-- SignInScreen.kt
|   |   +-- transactions/
|   |   |   +-- TransactionListScreen.kt
|   |   |   +-- AddEditTransactionScreen.kt
|   |   +-- summary/
|   |   |   +-- MonthlySummaryScreen.kt
|   |   +-- recurring/
|   |   |   +-- RecurringScreen.kt
|   |   +-- settings/
|   |       +-- SettingsScreen.kt
|   +-- viewmodel/
|   |   +-- TransactionListViewModel.kt
|   |   +-- AddEditTransactionViewModel.kt
|   |   +-- MonthlySummaryViewModel.kt
|   |   +-- RecurringViewModel.kt
|   |   +-- SettingsViewModel.kt
|   |   +-- AuthViewModel.kt
|   +-- components/
|       +-- TransactionCard.kt
|       +-- CategoryChip.kt
|       +-- AmountText.kt               # Formats with ILS currency symbol
|       +-- SummaryChart.kt             # Bar/pie chart for monthly breakdown
|       +-- DatePickerField.kt
|       +-- FilterBar.kt
|       +-- EmptyStateView.kt
|       +-- SwipeToDeleteContainer.kt
|
+-- auth/
|   +-- GoogleSignInManager.kt          # Wraps Credential Manager API
|   +-- AuthState.kt                    # sealed: Authenticated, Guest, SignedOut
|
+-- di/
    +-- AppModule.kt                    # @Module: provides Database, DAOs
    +-- RepositoryModule.kt             # @Module: binds repository interfaces
    +-- UseCaseModule.kt                # @Module: provides use cases (optional)
```

---

## Room Entities

### TransactionEntity

```kotlin
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = RecurringTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurring_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["category"]),
        Index(value = ["recurring_id"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "type")
    val type: String,                    // "income" or "expense"

    @ColumnInfo(name = "amount")
    val amount: Double,                  // Must be > 0

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "description", defaultValue = "")
    val description: String = "",

    @ColumnInfo(name = "date")
    val date: String,                    // "YYYY-MM-DD"

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: String = "",

    @ColumnInfo(name = "recurring_id")
    val recurringId: Long? = null
)
```

### RecurringTransactionEntity

```kotlin
@Entity(
    tableName = "recurring_transactions",
    indices = [
        Index(value = ["is_active"]),
        Index(value = ["frequency"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "type")
    val type: String,                    // "income" or "expense"

    @ColumnInfo(name = "amount")
    val amount: Double,                  // Must be > 0

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "description", defaultValue = "")
    val description: String = "",

    @ColumnInfo(name = "frequency")
    val frequency: String,              // "weekly" or "monthly"

    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int? = null,         // 0=Monday..6=Sunday (nullable)

    @ColumnInfo(name = "day_of_month")
    val dayOfMonth: Int? = null,        // 1-31 (nullable)

    @ColumnInfo(name = "start_date")
    val startDate: String,              // "YYYY-MM-DD"

    @ColumnInfo(name = "end_date")
    val endDate: String? = null,        // "YYYY-MM-DD" or null (no end)

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Int = 1,              // 0 or 1

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: String = ""
)
```

---

## DAOs

### TransactionDao

```kotlin
@Dao
interface TransactionDao {

    // --- Reactive queries (Flow) ---

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE (:type IS NULL OR type = :type)
          AND (:category IS NULL OR category = :category)
          AND (:dateFrom IS NULL OR date >= :dateFrom)
          AND (:dateTo IS NULL OR date <= :dateTo)
        ORDER BY date DESC, id DESC
    """)
    fun observeFiltered(
        type: String?,
        category: String?,
        dateFrom: String?,
        dateTo: String?
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("""
        SELECT * FROM transactions
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date DESC, id DESC
    """)
    fun observeByDateRange(startDate: String, endDate: String): Flow<List<TransactionEntity>>

    @Query("SELECT DISTINCT category FROM transactions ORDER BY category ASC")
    fun observeCategories(): Flow<List<String>>

    // --- Monthly aggregation ---

    @Query("""
        SELECT type, SUM(amount) as total, COUNT(*) as count, category
        FROM transactions
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY type, category
        ORDER BY category ASC
    """)
    fun getMonthlyCategoryBreakdown(
        startDate: String,
        endDate: String
    ): Flow<List<CategoryAggregation>>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) as totalIncome,
            COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) as totalExpenses,
            COUNT(*) as transactionCount
        FROM transactions
        WHERE date >= :startDate AND date <= :endDate
    """)
    fun getMonthlyTotals(startDate: String, endDate: String): Flow<MonthlyTotals>

    // --- CRUD (suspend) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE recurring_id = :recurringId AND date = :date
    """)
    suspend fun countByRecurringAndDate(recurringId: Long, date: String): Int

    @Query("UPDATE transactions SET recurring_id = NULL WHERE recurring_id = :recurringId")
    suspend fun nullifyRecurringId(recurringId: Long)
}

// Helper data class for aggregation query results
data class CategoryAggregation(
    val type: String,
    val total: Double,
    val count: Int,
    val category: String
)

data class MonthlyTotals(
    val totalIncome: Double,
    val totalExpenses: Double,
    val transactionCount: Int
)
```

### RecurringTransactionDao

```kotlin
@Dao
interface RecurringTransactionDao {

    // --- Reactive queries (Flow) ---

    @Query("SELECT * FROM recurring_transactions ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE is_active = 1 ORDER BY created_at DESC")
    fun observeActive(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    fun observeById(id: Long): Flow<RecurringTransactionEntity?>

    // --- CRUD (suspend) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recurring: List<RecurringTransactionEntity>)

    @Update
    suspend fun update(recurring: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions ORDER BY id ASC")
    suspend fun getAll(): List<RecurringTransactionEntity>
}
```

---

## Repository Interfaces

### TransactionRepository

```kotlin
interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeFiltered(type: String?, category: String?, dateFrom: String?, dateTo: String?): Flow<List<Transaction>>
    fun observeById(id: Long): Flow<Transaction?>
    fun observeCategories(): Flow<List<String>>
    fun getMonthlySummary(year: Int, month: Int): Flow<MonthlySummary>
    suspend fun create(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): Transaction?
    suspend fun getAll(): List<Transaction>
    suspend fun deleteAll()
    suspend fun insertAll(transactions: List<Transaction>)
}
```

### RecurringRepository

```kotlin
interface RecurringRepository {
    fun observeAll(): Flow<List<RecurringTransaction>>
    fun observeActive(): Flow<List<RecurringTransaction>>
    fun observeById(id: Long): Flow<RecurringTransaction?>
    suspend fun create(recurring: RecurringTransaction): Long
    suspend fun update(recurring: RecurringTransaction)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): RecurringTransaction?
    suspend fun getAll(): List<RecurringTransaction>
    suspend fun deleteAll()
    suspend fun insertAll(recurring: List<RecurringTransaction>)
    suspend fun generateTransactions(recurringId: Long, startDate: String, endDate: String): List<Transaction>
}
```

### BackupRepository

```kotlin
interface BackupRepository {
    suspend fun exportToJson(): BackupData
    suspend fun importFromJson(data: BackupData)
    suspend fun exportToUri(uri: Uri)
    suspend fun importFromUri(uri: Uri)
}
```

---

## ViewModels

### TransactionListViewModel

```kotlin
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : ViewModel() {

    // UI State
    data class UiState(
        val transactions: List<Transaction> = emptyList(),
        val isLoading: Boolean = true,
        val filterType: String? = null,        // null = all, "income", "expense"
        val filterCategory: String? = null,
        val filterDateFrom: String? = null,
        val filterDateTo: String? = null,
        val availableCategories: List<String> = emptyList(),
        val error: String? = null
    )

    val uiState: StateFlow<UiState>

    fun setFilter(type: String?, category: String?, dateFrom: String?, dateTo: String?)
    fun deleteTransaction(id: Long)
    fun clearError()
}
```

### AddEditTransactionViewModel

```kotlin
@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    savedStateHandle: SavedStateHandle               // receives transactionId for edit mode
) : ViewModel() {

    data class UiState(
        val isEditMode: Boolean = false,
        val type: TransactionType = TransactionType.EXPENSE,
        val amount: String = "",
        val category: String = "",
        val description: String = "",
        val date: LocalDate = LocalDate.now(),
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false,
        val error: String? = null
    )

    val uiState: StateFlow<UiState>

    fun setType(type: TransactionType)
    fun setAmount(amount: String)
    fun setCategory(category: String)
    fun setDescription(description: String)
    fun setDate(date: LocalDate)
    fun save()
}
```

### MonthlySummaryViewModel

```kotlin
@HiltViewModel
class MonthlySummaryViewModel @Inject constructor(
    private val getMonthlySummaryUseCase: GetMonthlySummaryUseCase
) : ViewModel() {

    data class UiState(
        val year: Int = LocalDate.now().year,
        val month: Int = LocalDate.now().monthValue,
        val summary: MonthlySummary? = null,
        val isLoading: Boolean = true
    )

    val uiState: StateFlow<UiState>

    fun setMonth(year: Int, month: Int)
    fun previousMonth()
    fun nextMonth()
}
```

### RecurringViewModel

```kotlin
@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val getRecurringUseCase: GetRecurringTransactionsUseCase,
    private val createRecurringUseCase: CreateRecurringUseCase,
    private val updateRecurringUseCase: UpdateRecurringUseCase,
    private val deleteRecurringUseCase: DeleteRecurringUseCase,
    private val generateRecurringUseCase: GenerateRecurringUseCase
) : ViewModel() {

    data class UiState(
        val recurringTransactions: List<RecurringTransaction> = emptyList(),
        val isLoading: Boolean = true,
        val showAddDialog: Boolean = false,
        val editingRecurring: RecurringTransaction? = null,
        val generateResult: String? = null,
        val error: String? = null
    )

    val uiState: StateFlow<UiState>

    fun create(recurring: RecurringTransaction)
    fun update(recurring: RecurringTransaction)
    fun delete(id: Long)
    fun toggleActive(id: Long)
    fun generate(id: Long, startDate: String, endDate: String)
    fun showAddDialog()
    fun dismissDialog()
}
```

### SettingsViewModel

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val authViewModel: AuthViewModel          // or GoogleSignInManager
) : ViewModel() {

    data class UiState(
        val isExporting: Boolean = false,
        val isImporting: Boolean = false,
        val message: String? = null,
        val authState: AuthState = AuthState.SignedOut
    )

    val uiState: StateFlow<UiState>

    fun exportData(uri: Uri)
    fun importData(uri: Uri)
    fun signOut()
}
```

### AuthViewModel

```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val googleSignInManager: GoogleSignInManager
) : ViewModel() {

    val authState: StateFlow<AuthState>

    fun signInWithGoogle(activityContext: Context)
    fun continueAsGuest()
    fun signOut()
}
```

---

## Navigation

### Screen Routes (Sealed Class)

```kotlin
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Transactions : Screen("transactions", "Transactions", Icons.Default.Receipt)
    object Summary : Screen("summary", "Summary", Icons.Default.PieChart)
    object Recurring : Screen("recurring", "Recurring", Icons.Default.Repeat)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    // Non-bottom-nav screens
    object AddEditTransaction : Screen("transactions/add_edit?id={id}", "Add/Edit", Icons.Default.Add)
    object SignIn : Screen("sign_in", "Sign In", Icons.Default.Login)
}
```

### Bottom Navigation

Four tabs:
1. **Transactions** - List with filters, FAB to add, swipe-to-delete
2. **Summary** - Monthly income/expense totals, category breakdown, month picker
3. **Recurring** - List recurring definitions, toggle active, generate
4. **Settings** - Backup/restore, sign in/out, about

---

## Google Sign-In Flow

```
App Launch
    |
    v
[Check stored auth state]
    |
    +-- Previously signed in --> Main App (auto-refresh token)
    |
    +-- Guest mode stored ----> Main App (no sync)
    |
    +-- No stored state ------> SignInScreen
                                    |
                                    +-- "Sign in with Google" --> Credential Manager API --> Main App
                                    |
                                    +-- "Continue as Guest" --> Main App (local only)
```

- Auth state persisted in DataStore (Preferences)
- Google Sign-In uses `androidx.credentials:credentials` (Credential Manager API)
- Guest mode: all features work locally, no cloud backup
- Sign-in enables future cloud sync (not in v1 scope)

---

## Backup / Restore

### Export Flow
1. User taps "Export Data" in Settings
2. Android file picker (`ACTION_CREATE_DOCUMENT`) opens
3. User chooses save location and filename
4. App serializes all transactions + recurring to JSON (matching existing web format)
5. JSON written to chosen URI via `ContentResolver`

### Import Flow
1. User taps "Import Data" in Settings
2. Android file picker (`ACTION_OPEN_DOCUMENT`) opens
3. User selects a `.json` file
4. App reads and validates the JSON structure
5. Confirmation dialog: "This will replace all current data. Continue?"
6. On confirm: clear DB, insert all records from JSON

### JSON Format (matches existing web API)

```json
{
  "created_at": "2026-03-13T10:00:00",
  "transactions": [
    {
      "id": 1,
      "type": "expense",
      "amount": 150.0,
      "category": "Groceries",
      "description": "Weekly shopping",
      "date": "2026-03-10",
      "created_at": "2026-03-10 12:00:00",
      "recurring_id": null
    }
  ],
  "recurring_transactions": [
    {
      "id": 1,
      "type": "expense",
      "amount": 5000.0,
      "category": "Rent",
      "description": "Monthly rent",
      "frequency": "monthly",
      "day_of_week": null,
      "day_of_month": 1,
      "start_date": "2026-01-01",
      "end_date": null,
      "is_active": 1,
      "created_at": "2026-01-01 00:00:00"
    }
  ]
}
```

---

## Gradle Dependencies

### build.gradle.kts (app-level)

```kotlin
plugins {
    id("com.android.application") version "8.2.2"
    id("org.jetbrains.kotlin.android") version "1.9.22"
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
    id("com.google.dagger.hilt.android") version "2.50"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.budgetmanager.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.budgetmanager.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")              // 1.2.0
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Kotlinx Serialization (for JSON backup/restore)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Google Sign-In (Credential Manager)
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    // DataStore (for auth state + preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Core KTX
    implementation("androidx.core:core-ktx:1.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.50")
}
```

---

## Feature Details

### Currency Formatting
- All amounts displayed with ILS symbol: `NumberFormat.getCurrencyInstance(Locale("he", "IL"))`
- Composable helper `AmountText` applies green for income, red for expense
- Format: `+150.00` (income) / `-150.00` (expense)

### Swipe-to-Delete
- `SwipeToDismissBox` (Material 3) on each transaction card
- Background reveals red delete icon
- Snackbar with "Undo" action (re-inserts deleted transaction)

### Date Picker
- Material 3 `DatePickerDialog` composable
- Default to today's date for new transactions
- ISO-8601 format stored internally

### Filter Transactions
- `FilterBar` composable at top of TransactionListScreen
- Filter chips: Type (Income/Expense), Category (dynamic from DB), Date Range
- Filters applied reactively via Room Flow queries

### Material Design 3 with Dynamic Colors
- `dynamicDarkColorScheme` / `dynamicLightColorScheme` on Android 12+
- Fallback to custom ILS-themed color scheme (blue/green palette)
- Support system dark/light mode

### Recurring Transaction Generation
- Dialog to select date range for generation
- Shows preview count before confirming
- Skips duplicate dates (same recurring_id + date combo)
- Toast/Snackbar showing "Generated X transactions"

---

## Screen Descriptions

### SignInScreen
- Google Sign-In button (Material 3 outlined button with Google logo)
- "Continue as Guest" text button below
- App logo and tagline at top

### TransactionListScreen
- Top app bar with filter icon
- FilterBar (expandable) with type/category/date chips
- LazyColumn of TransactionCard items
- Each card shows: type icon, category, description, date, amount (formatted ILS)
- Swipe-to-delete with undo snackbar
- FAB to navigate to AddEditTransactionScreen

### AddEditTransactionScreen
- Top app bar with back arrow, title "Add Transaction" or "Edit Transaction"
- Segmented button for Income/Expense toggle
- Amount text field (numeric keyboard, ILS prefix)
- Category text field with autocomplete from existing categories
- Description text field (optional)
- Date picker field
- Save button (filled) and Cancel button (outlined)

### MonthlySummaryScreen
- Month/year selector with left/right arrows
- Summary cards: Total Income, Total Expenses, Net Balance
- Category breakdown list with progress bars
- Each category row: icon, name, total amount, transaction count

### RecurringScreen
- LazyColumn of recurring transaction cards
- Each card: type, amount, category, frequency badge, active toggle switch
- Tap card to edit, long-press for options (generate, delete)
- FAB to add new recurring definition
- Bottom sheet dialog for add/edit form

### SettingsScreen
- Profile section (Google account info or "Guest Mode")
- Sign out / Sign in button
- Backup section: Export Data, Import Data buttons
- App info: version, source link
