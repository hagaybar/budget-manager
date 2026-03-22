# Multi-Budget (Named Wallets) Implementation Plan

## 1. Overview

Add support for multiple named budgets (wallets) to the Android budget manager app. Each budget is an independent container with its own transactions, recurring transactions, and summary. Users can switch between budgets via a top-bar dropdown that is always visible.

### Current Architecture Summary

The app follows a clean MVVM architecture with Hilt DI:

- **Database**: Room v1 with two tables (`transactions`, `recurring_transactions`), no ORM beyond Room
- **Entities**: `TransactionEntity`, `RecurringTransactionEntity`
- **DAOs**: `TransactionDao`, `RecurringTransactionDao` -- all queries are unscoped (no budget filtering)
- **Repositories**: `TransactionRepository`/`Impl`, `RecurringRepository`/`Impl`, `BackupRepository`/`Impl`
- **Domain Models**: `Transaction`, `RecurringTransaction`, `MonthlySummary`, `CategoryBreakdown`, `BackupData`
- **ViewModels**: `TransactionListViewModel`, `AddEditTransactionViewModel`, `MonthlySummaryViewModel`, `RecurringViewModel`, `SettingsViewModel`, `AuthViewModel`
- **Use Cases**: `GenerateRecurringTransactionsUseCase`
- **Worker**: `RecurringTransactionWorker`
- **Navigation**: `BudgetNavHost` with `BottomNavBar`, 6 screens (Transactions, Summary, Recurring, Settings, AddEditTransaction, SignIn)
- **DI Modules**: `AppModule` (database, DAOs), `RepositoryModule` (repository bindings)

---

## 2. Database Changes

### 2.1 New Entity: BudgetEntity

**File**: `app/src/main/java/com/budgetmanager/app/data/entity/BudgetEntity.kt` (NEW)

```kotlin
@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["is_active"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description", defaultValue = "")
    val description: String = "",

    @ColumnInfo(name = "currency", defaultValue = "ILS")
    val currency: String = "ILS",

    @ColumnInfo(name = "monthly_target")
    val monthlyTarget: Double? = null,

    @ColumnInfo(name = "is_active", defaultValue = "0")
    val isActive: Int = 0,

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: String = ""
)
```

### 2.2 Modify TransactionEntity

**File**: `app/src/main/java/com/budgetmanager/app/data/entity/TransactionEntity.kt` (MODIFY)

Add `budgetId` column with a foreign key referencing `budgets.id`:

```kotlin
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = RecurringTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurring_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["category"]),
        Index(value = ["recurring_id"]),
        Index(value = ["budget_id"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "description", defaultValue = "") val description: String = "",
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP") val createdAt: String = "",
    @ColumnInfo(name = "recurring_id") val recurringId: Long? = null,
    @ColumnInfo(name = "budget_id") val budgetId: Long = 0   // <-- NEW
)
```

### 2.3 Modify RecurringTransactionEntity

**File**: `app/src/main/java/com/budgetmanager/app/data/entity/RecurringTransactionEntity.kt` (MODIFY)

Add `budgetId` column with a foreign key referencing `budgets.id`:

```kotlin
@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["is_active"]),
        Index(value = ["frequency"]),
        Index(value = ["budget_id"])
    ]
)
data class RecurringTransactionEntity(
    // ... all existing fields unchanged ...
    @ColumnInfo(name = "budget_id") val budgetId: Long = 0   // <-- NEW
)
```

### 2.4 Room Database Migration (v1 -> v2)

**File**: `app/src/main/java/com/budgetmanager/app/data/db/BudgetDatabase.kt` (MODIFY)

```kotlin
@Database(
    entities = [
        TransactionEntity::class,
        RecurringTransactionEntity::class,
        BudgetEntity::class                           // <-- NEW
    ],
    version = 2,                                       // <-- CHANGED from 1
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun budgetDao(): BudgetDao               // <-- NEW
}
```

**Migration definition** (added to `AppModule.kt` or a dedicated `Migrations.kt`):

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create budgets table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `budgets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL DEFAULT '',
                `currency` TEXT NOT NULL DEFAULT 'ILS',
                `monthly_target` REAL,
                `is_active` INTEGER NOT NULL DEFAULT 0,
                `created_at` TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_name` ON `budgets` (`name`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_is_active` ON `budgets` (`is_active`)")

        // 2. Insert a default budget for existing data (only if data exists)
        db.execSQL("""
            INSERT INTO `budgets` (`name`, `description`, `currency`, `is_active`)
            SELECT 'My Budget', '', 'ILS', 1
            WHERE EXISTS (SELECT 1 FROM `transactions` LIMIT 1)
               OR EXISTS (SELECT 1 FROM `recurring_transactions` LIMIT 1)
        """)

        // 3. Add budget_id column to transactions (default 0, will be updated)
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `budget_id` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_budget_id` ON `transactions` (`budget_id`)")

        // 4. Add budget_id column to recurring_transactions
        db.execSQL("ALTER TABLE `recurring_transactions` ADD COLUMN `budget_id` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_budget_id` ON `recurring_transactions` (`budget_id`)")

        // 5. Link existing data to the default budget
        db.execSQL("""
            UPDATE `transactions` SET `budget_id` = (
                SELECT `id` FROM `budgets` WHERE `name` = 'My Budget' LIMIT 1
            ) WHERE `budget_id` = 0
        """)
        db.execSQL("""
            UPDATE `recurring_transactions` SET `budget_id` = (
                SELECT `id` FROM `budgets` WHERE `name` = 'My Budget' LIMIT 1
            ) WHERE `budget_id` = 0
        """)
    }
}
```

Migration steps summary:
1. Create the `budgets` table with unique name index
2. Conditionally insert a default "My Budget" if any existing data exists
3. Add `budget_id` column to `transactions` table
4. Add `budget_id` column to `recurring_transactions` table
5. Backfill `budget_id` on all existing rows to point to the default budget

---

## 3. New DAO: BudgetDao

**File**: `app/src/main/java/com/budgetmanager/app/data/dao/BudgetDao.kt` (NEW)

```kotlin
@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets ORDER BY created_at ASC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE is_active = 1 LIMIT 1")
    fun observeActive(): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE id = :id")
    fun observeById(id: Long): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets ORDER BY id ASC")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun count(): Int

    // Set active budget: deactivate all, then activate target
    @Query("UPDATE budgets SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE budgets SET is_active = 1 WHERE id = :id")
    suspend fun activate(id: Long)

    @Transaction
    suspend fun setActiveBudget(id: Long) {
        deactivateAll()
        activate(id)
    }
}
```

---

## 4. Modify Existing DAOs

### 4.1 TransactionDao -- Add budgetId Filtering

**File**: `app/src/main/java/com/budgetmanager/app/data/dao/TransactionDao.kt` (MODIFY)

Every existing query must be updated to accept a `budgetId` parameter. Add new budget-scoped variants alongside (or replace) existing queries:

```kotlin
// REPLACE existing observeAll:
@Query("SELECT * FROM transactions WHERE budget_id = :budgetId ORDER BY date DESC, id DESC")
fun observeAll(budgetId: Long): Flow<List<TransactionEntity>>

// REPLACE existing observeFiltered:
@Query("""
    SELECT * FROM transactions
    WHERE budget_id = :budgetId
      AND (:type IS NULL OR type = :type)
      AND (:category IS NULL OR category = :category)
      AND (:dateFrom IS NULL OR date >= :dateFrom)
      AND (:dateTo IS NULL OR date <= :dateTo)
    ORDER BY date DESC, id DESC
""")
fun observeFiltered(
    budgetId: Long,
    type: String?, category: String?, dateFrom: String?, dateTo: String?
): Flow<List<TransactionEntity>>

// REPLACE observeByDateRange:
@Query("""
    SELECT * FROM transactions
    WHERE budget_id = :budgetId AND date >= :startDate AND date <= :endDate
    ORDER BY date DESC, id DESC
""")
fun observeByDateRange(budgetId: Long, startDate: String, endDate: String): Flow<List<TransactionEntity>>

// REPLACE observeCategories:
@Query("SELECT DISTINCT category FROM transactions WHERE budget_id = :budgetId ORDER BY category ASC")
fun observeCategories(budgetId: Long): Flow<List<String>>

// REPLACE getMonthlyCategoryBreakdown:
@Query("""
    SELECT type, SUM(amount) as total, COUNT(*) as count, category
    FROM transactions
    WHERE budget_id = :budgetId AND date >= :startDate AND date <= :endDate
    GROUP BY type, category
    ORDER BY category ASC
""")
fun getMonthlyCategoryBreakdown(
    budgetId: Long, startDate: String, endDate: String
): Flow<List<CategoryAggregation>>

// REPLACE getMonthlyTotals:
@Query("""
    SELECT
        COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) as totalIncome,
        COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) as totalExpenses,
        COUNT(*) as transactionCount
    FROM transactions
    WHERE budget_id = :budgetId AND date >= :startDate AND date <= :endDate
""")
fun getMonthlyTotals(budgetId: Long, startDate: String, endDate: String): Flow<MonthlyTotals>

// REPLACE countByRecurringAndDate:
@Query("""
    SELECT COUNT(*) FROM transactions
    WHERE recurring_id = :recurringId AND date = :date AND budget_id = :budgetId
""")
suspend fun countByRecurringAndDate(recurringId: Long, date: String, budgetId: Long): Int

// KEEP: getAll() with no filter for backup export purposes (exports ALL budgets)
// KEEP: deleteAll() for backup import purposes
// KEEP: getById(), observeById(), insert(), update(), deleteById(), insertAll(), nullifyRecurringId()
```

### 4.2 RecurringTransactionDao -- Add budgetId Filtering

**File**: `app/src/main/java/com/budgetmanager/app/data/dao/RecurringTransactionDao.kt` (MODIFY)

```kotlin
// REPLACE observeAll:
@Query("SELECT * FROM recurring_transactions WHERE budget_id = :budgetId ORDER BY created_at DESC")
fun observeAll(budgetId: Long): Flow<List<RecurringTransactionEntity>>

// REPLACE observeActive:
@Query("SELECT * FROM recurring_transactions WHERE budget_id = :budgetId AND is_active = 1 ORDER BY created_at DESC")
fun observeActive(budgetId: Long): Flow<List<RecurringTransactionEntity>>

// REPLACE getActive:
@Query("SELECT * FROM recurring_transactions WHERE budget_id = :budgetId AND is_active = 1 ORDER BY id ASC")
suspend fun getActive(budgetId: Long): List<RecurringTransactionEntity>

// KEEP: getAll() for backup export (all budgets)
// KEEP: getById(), observeById(), insert(), update(), deleteById(), insertAll(), deleteAll()
```

---

## 5. New Domain Model: Budget

**File**: `app/src/main/java/com/budgetmanager/app/domain/model/Budget.kt` (NEW)

```kotlin
data class Budget(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val currency: String = "ILS",
    val monthlyTarget: Double? = null,
    val isActive: Boolean = false,
    val createdAt: String = ""
)
```

### 5.1 Modify Existing Domain Models

**Transaction** -- add `budgetId`:
```kotlin
data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val description: String = "",
    val date: String,
    val createdAt: String = "",
    val recurringId: Long? = null,
    val budgetId: Long = 0           // <-- NEW
)
```

**RecurringTransaction** -- add `budgetId`:
```kotlin
data class RecurringTransaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val description: String = "",
    val frequency: Frequency,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val startDate: String,
    val endDate: String? = null,
    val isActive: Boolean = true,
    val createdAt: String = "",
    val budgetId: Long = 0           // <-- NEW
)
```

### 5.2 Modify BackupData

**File**: `app/src/main/java/com/budgetmanager/app/domain/model/BackupData.kt` (MODIFY)

Add `BackupBudget` serializable class and include budgets list in `BackupData`:

```kotlin
@Serializable
data class BackupData(
    @SerialName("created_at") val createdAt: String,
    val budgets: List<BackupBudget> = emptyList(),       // <-- NEW (default empty for backward compat)
    val transactions: List<BackupTransaction>,
    @SerialName("recurring_transactions") val recurringTransactions: List<BackupRecurringTransaction>
)

@Serializable
data class BackupBudget(
    val id: Long,
    val name: String,
    val description: String = "",
    val currency: String = "ILS",
    @SerialName("monthly_target") val monthlyTarget: Double? = null,
    @SerialName("is_active") val isActive: Int = 0,
    @SerialName("created_at") val createdAt: String = ""
)

// Modify BackupTransaction to add:
@Serializable
data class BackupTransaction(
    // ... existing fields ...
    @SerialName("budget_id") val budgetId: Long = 0      // <-- NEW
)

// Modify BackupRecurringTransaction to add:
@Serializable
data class BackupRecurringTransaction(
    // ... existing fields ...
    @SerialName("budget_id") val budgetId: Long = 0      // <-- NEW
)
```

---

## 6. New Repository: BudgetRepository

**File**: `app/src/main/java/com/budgetmanager/app/data/repository/BudgetRepository.kt` (NEW)

```kotlin
interface BudgetRepository {
    fun observeAll(): Flow<List<Budget>>
    fun observeActive(): Flow<Budget?>
    fun observeById(id: Long): Flow<Budget?>
    suspend fun create(budget: Budget): Long
    suspend fun update(budget: Budget)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): Budget?
    suspend fun getAll(): List<Budget>
    suspend fun count(): Int
    suspend fun setActiveBudget(id: Long)
}
```

**File**: `app/src/main/java/com/budgetmanager/app/data/repository/BudgetRepositoryImpl.kt` (NEW)

```kotlin
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun observeAll(): Flow<List<Budget>> =
        budgetDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeActive(): Flow<Budget?> =
        budgetDao.observeActive().map { it?.toDomain() }

    override fun observeById(id: Long): Flow<Budget?> =
        budgetDao.observeById(id).map { it?.toDomain() }

    override suspend fun create(budget: Budget): Long =
        budgetDao.insert(budget.toEntity())

    override suspend fun update(budget: Budget) =
        budgetDao.update(budget.toEntity())

    override suspend fun delete(id: Long) =
        budgetDao.deleteById(id)  // CASCADE will delete associated transactions

    override suspend fun getById(id: Long): Budget? =
        budgetDao.getById(id)?.toDomain()

    override suspend fun getAll(): List<Budget> =
        budgetDao.getAll().map { it.toDomain() }

    override suspend fun count(): Int =
        budgetDao.count()

    override suspend fun setActiveBudget(id: Long) =
        budgetDao.setActiveBudget(id)

    private fun BudgetEntity.toDomain() = Budget(
        id = id, name = name, description = description,
        currency = currency, monthlyTarget = monthlyTarget,
        isActive = isActive == 1, createdAt = createdAt
    )

    private fun Budget.toEntity() = BudgetEntity(
        id = id, name = name, description = description,
        currency = currency, monthlyTarget = monthlyTarget,
        isActive = if (isActive) 1 else 0, createdAt = createdAt
    )
}
```

---

## 7. ActiveBudgetManager (Singleton)

**File**: `app/src/main/java/com/budgetmanager/app/domain/manager/ActiveBudgetManager.kt` (NEW)

A singleton that holds and emits the currently active budget ID via `StateFlow`, persisted in DataStore. All ViewModels and repositories observe this to scope their queries.

```kotlin
@Singleton
class ActiveBudgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val budgetRepository: BudgetRepository
) {
    private val Context.budgetDataStore: DataStore<Preferences> by preferencesDataStore("active_budget")

    private object Keys {
        val ACTIVE_BUDGET_ID = longPreferencesKey("active_budget_id")
    }

    private val _activeBudgetId = MutableStateFlow<Long?>(null)
    val activeBudgetId: StateFlow<Long?> = _activeBudgetId.asStateFlow()

    // Emits the full Budget object for the active budget
    val activeBudget: Flow<Budget?> = _activeBudgetId.flatMapLatest { id ->
        if (id != null) budgetRepository.observeById(id)
        else flowOf(null)
    }

    init {
        // Restore from DataStore on init
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val prefs = context.budgetDataStore.data.first()
            val storedId = prefs[Keys.ACTIVE_BUDGET_ID]
            if (storedId != null) {
                _activeBudgetId.value = storedId
                budgetRepository.setActiveBudget(storedId)
            }
        }
    }

    suspend fun setActiveBudget(budgetId: Long) {
        _activeBudgetId.value = budgetId
        budgetRepository.setActiveBudget(budgetId)
        context.budgetDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_BUDGET_ID] = budgetId
        }
    }

    /**
     * Called during migration flow: checks if migration is needed
     * (budgets table empty but transactions exist).
     */
    suspend fun needsMigration(): Boolean {
        return budgetRepository.count() == 0
    }
}
```

---

## 8. Modify Existing Repositories

### 8.1 TransactionRepository / TransactionRepositoryImpl

**File**: `app/src/main/java/com/budgetmanager/app/data/repository/TransactionRepository.kt` (MODIFY)

Add `budgetId` parameter to all observation methods:

```kotlin
interface TransactionRepository {
    fun observeAll(budgetId: Long): Flow<List<Transaction>>
    fun observeFiltered(budgetId: Long, type: String?, category: String?, dateFrom: String?, dateTo: String?): Flow<List<Transaction>>
    fun observeById(id: Long): Flow<Transaction?>
    fun observeCategories(budgetId: Long): Flow<List<String>>
    fun getMonthlySummary(budgetId: Long, year: Int, month: Int): Flow<MonthlySummary>
    suspend fun create(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): Transaction?
    suspend fun getAll(): List<Transaction>
    suspend fun deleteAll()
    suspend fun insertAll(transactions: List<Transaction>)
}
```

**TransactionRepositoryImpl** (MODIFY): Update all method implementations to pass `budgetId` through to the DAO. Update `toDomain()` and `toEntity()` mappers to include `budgetId`.

### 8.2 RecurringRepository / RecurringRepositoryImpl

**File**: `app/src/main/java/com/budgetmanager/app/data/repository/RecurringRepository.kt` (MODIFY)

```kotlin
interface RecurringRepository {
    fun observeAll(budgetId: Long): Flow<List<RecurringTransaction>>
    fun observeActive(budgetId: Long): Flow<List<RecurringTransaction>>
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

**RecurringRepositoryImpl** (MODIFY): Update `observeAll()`, `observeActive()` to pass `budgetId`. Update `generateTransactions()` to set `budgetId` on created `TransactionEntity` rows. Update mappers.

### 8.3 BackupRepositoryImpl

**File**: `app/src/main/java/com/budgetmanager/app/data/repository/BackupRepositoryImpl.kt` (MODIFY)

- `exportToJson()`: Include `budgetDao.getAll()` in the export, map to `BackupBudget` list
- `importFromJson()`: Within the transaction, also `budgetDao.deleteAll()` and `budgetDao.insertAll()` the budgets first (before transactions, to satisfy FK constraints)
- `validateBackupData()`: Add validation for budgets list
- Add `BackupBudget` <-> `BudgetEntity` mapper functions
- Add backward compatibility: if `data.budgets` is empty (old backup format), create a default budget and assign all imported transactions/recurring to it

---

## 9. Modify Use Case and Worker

### 9.1 GenerateRecurringTransactionsUseCase

**File**: `app/src/main/java/com/budgetmanager/app/domain/usecase/GenerateRecurringTransactionsUseCase.kt` (MODIFY)

The use case currently iterates over all active recurring transactions and generates for today. With multi-budget, it must:

1. Continue to iterate ALL active recurring transactions across ALL budgets (the worker runs globally, not per-budget)
2. When creating a `TransactionEntity`, copy the `budgetId` from the `RecurringTransactionEntity`
3. Pass `budgetId` to `countByRecurringAndDate()` for accurate duplicate detection

```kotlin
// In the for loop, change:
val transaction = TransactionEntity(
    type = recurring.type,
    amount = recurring.amount,
    category = recurring.category,
    description = recurring.description,
    date = todayStr,
    recurringId = recurring.id,
    budgetId = recurring.budgetId     // <-- NEW: inherit budget from recurring definition
)

// Also update duplicate check:
val alreadyExists = transactionDao.countByRecurringAndDate(
    recurring.id, todayStr, recurring.budgetId
) > 0
```

### 9.2 RecurringTransactionWorker

**File**: `app/src/main/java/com/budgetmanager/app/worker/RecurringTransactionWorker.kt` (NO CHANGES NEEDED)

The worker delegates to `GenerateRecurringTransactionsUseCase` which handles all budgets internally.

---

## 10. New ViewModel: BudgetViewModel

**File**: `app/src/main/java/com/budgetmanager/app/ui/viewmodel/BudgetViewModel.kt` (NEW)

```kotlin
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val activeBudgetManager: ActiveBudgetManager
) : ViewModel() {

    data class UiState(
        val budgets: List<Budget> = emptyList(),
        val activeBudget: Budget? = null,
        val isLoading: Boolean = true,
        val showCreateDialog: Boolean = false,
        val editingBudget: Budget? = null,
        val showDeleteConfirmation: Budget? = null,
        val needsMigration: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadBudgets()
        observeActiveBudget()
        checkMigration()
    }

    private fun loadBudgets() { /* observe budgetRepository.observeAll() */ }
    private fun observeActiveBudget() { /* observe activeBudgetManager.activeBudget */ }
    private fun checkMigration() { /* check activeBudgetManager.needsMigration() */ }

    fun createBudget(budget: Budget) { /* insert + setActive */ }
    fun updateBudget(budget: Budget) { /* update */ }
    fun deleteBudget(id: Long) { /* delete, switch active if needed */ }
    fun switchBudget(budgetId: Long) { /* activeBudgetManager.setActiveBudget() */ }
    fun showCreateDialog() { /* toggle */ }
    fun showEditDialog(budget: Budget) { /* toggle */ }
    fun dismissDialog() { /* toggle */ }
    fun completeMigration(budget: Budget) { /* create budget from migration, clear flag */ }
}
```

---

## 11. Modify Existing ViewModels

### 11.1 TransactionListViewModel

**File**: `app/src/main/java/com/budgetmanager/app/ui/viewmodel/TransactionListViewModel.kt` (MODIFY)

- Inject `ActiveBudgetManager`
- In `init`, collect `activeBudgetManager.activeBudgetId` and re-trigger `loadTransactions()` whenever it changes
- Pass `budgetId` to `repository.observeFiltered()` and `repository.observeCategories()`

```kotlin
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val generateRecurringTransactions: GenerateRecurringTransactionsUseCase,
    private val activeBudgetManager: ActiveBudgetManager    // <-- NEW
) : ViewModel() {
    // ...
    init {
        viewModelScope.launch {
            activeBudgetManager.activeBudgetId.filterNotNull().collect { budgetId ->
                generateRecurringTransactionsEagerly()
                loadTransactions(budgetId)
                loadCategories(budgetId)
            }
        }
    }
    // ...
}
```

### 11.2 AddEditTransactionViewModel

**File**: `app/src/main/java/com/budgetmanager/app/ui/viewmodel/AddEditTransactionViewModel.kt` (MODIFY)

- Inject `ActiveBudgetManager`
- When saving a new transaction, set `budgetId` from `activeBudgetManager.activeBudgetId.value`
- When editing, preserve the existing `budgetId` from the loaded transaction

### 11.3 MonthlySummaryViewModel

**File**: `app/src/main/java/com/budgetmanager/app/ui/viewmodel/MonthlySummaryViewModel.kt` (MODIFY)

- Inject `ActiveBudgetManager`
- Pass `budgetId` to `repository.getMonthlySummary()`
- Re-load summary when active budget changes

### 11.4 RecurringViewModel

**File**: `app/src/main/java/com/budgetmanager/app/ui/viewmodel/RecurringViewModel.kt` (MODIFY)

- Inject `ActiveBudgetManager`
- Pass `budgetId` to `repository.observeAll()` and when creating new recurring transactions
- Re-load when active budget changes

### 11.5 SettingsViewModel

**File**: `app/src/main/java/com/budgetmanager/app/ui/viewmodel/SettingsViewModel.kt` (NO CHANGES NEEDED)

Backup/restore operates on the entire database (all budgets), so the SettingsViewModel does not need budget scoping.

---

## 12. DI Module Changes

### 12.1 AppModule

**File**: `app/src/main/java/com/budgetmanager/app/di/AppModule.kt` (MODIFY)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BudgetDatabase {
        return Room.databaseBuilder(
            context,
            BudgetDatabase::class.java,
            "budget_manager.db"
        )
        .addMigrations(MIGRATION_1_2)       // <-- NEW
        .build()
    }

    // ... existing DAO providers ...

    @Provides
    fun provideBudgetDao(database: BudgetDatabase): BudgetDao {    // <-- NEW
        return database.budgetDao()
    }
}
```

### 12.2 RepositoryModule

**File**: `app/src/main/java/com/budgetmanager/app/di/RepositoryModule.kt` (MODIFY)

```kotlin
@Binds @Singleton
abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository  // <-- NEW
```

---

## 13. Navigation Changes

### 13.1 Screen.kt

**File**: `app/src/main/java/com/budgetmanager/app/ui/navigation/Screen.kt` (MODIFY)

Add a new screen for budget management:

```kotlin
data object BudgetManagement : Screen("budgets", "Budgets", Icons.Default.AccountBalanceWallet)
```

Note: This screen is accessed from the budget dropdown menu, not from the bottom nav bar. `bottomNavItems` remains unchanged.

### 13.2 BudgetNavHost.kt

**File**: `app/src/main/java/com/budgetmanager/app/ui/navigation/BudgetNavHost.kt` (MODIFY)

- Add a `composable(Screen.BudgetManagement.route)` destination for the budget management screen
- Wrap the `Scaffold` to include the budget dropdown in the top bar area (see UI section below)
- Obtain `BudgetViewModel` at the nav host level so the dropdown is shared across all screens

---

## 14. UI Changes

### 14.1 BudgetDropdown (Top Bar Component)

**File**: `app/src/main/java/com/budgetmanager/app/ui/components/BudgetDropdown.kt` (NEW)

A composable showing the active budget name with a dropdown arrow. Tapping reveals a dropdown menu listing all budgets plus a "Manage Budgets" option.

```kotlin
@Composable
fun BudgetDropdown(
    activeBudget: Budget?,
    allBudgets: List<Budget>,
    onBudgetSelected: (Long) -> Unit,
    onManageBudgets: () -> Unit
)
```

Behavior:
- Shows current budget name (or "No Budget" if null)
- Dropdown lists all budgets with radio-button selection
- Divider + "Manage Budgets" item at bottom navigates to BudgetManagementScreen
- Placed in a TopAppBar that wraps the main Scaffold in BudgetNavHost

### 14.2 BudgetManagementScreen

**File**: `app/src/main/java/com/budgetmanager/app/ui/screens/budget/BudgetManagementScreen.kt` (NEW)

A full screen for creating, editing, and deleting budgets:

- TopAppBar with back arrow and title "Budgets"
- LazyColumn listing all budgets as cards, each showing: name, description, currency, monthly target, active indicator
- FAB to add new budget
- Tap card to edit, swipe to delete (with confirmation dialog)
- Cannot delete the last remaining budget (show snackbar warning)
- Cannot delete the currently active budget without switching first (or auto-switch to another)

### 14.3 BudgetFormDialog

**File**: `app/src/main/java/com/budgetmanager/app/ui/screens/budget/BudgetFormDialog.kt` (NEW)

An AlertDialog for creating or editing a budget:

Fields:
- Name (required, OutlinedTextField, validated for uniqueness)
- Description (optional, OutlinedTextField)
- Currency (OutlinedTextField, default "ILS")
- Monthly Target (optional, number keyboard)

### 14.4 MigrationDialog

**File**: `app/src/main/java/com/budgetmanager/app/ui/screens/budget/MigrationDialog.kt` (NEW)

Shown on app open when budgets table is empty but transactions exist (post-migration from v1 to v2 where the auto-inserted "My Budget" name may need user customization, OR the conditional insert didn't fire and no budget exists yet).

The dialog:
- Title: "Welcome to Multi-Budget!"
- Body: "Your existing transactions need to be assigned to a budget. Please create your first budget below."
- Embedded budget details card with fields: Name, Description, Currency, Monthly Target
- "Create Budget" button
- Cannot be dismissed without completing (no outside-click dismiss)
- On completion: creates the budget, assigns all orphaned transactions/recurring to it, sets it as active

### 14.5 Modify BudgetNavHost Layout

The `BudgetNavHost` currently uses a `Scaffold` with only a `BottomNavBar`. Modify to add a `TopAppBar` containing the `BudgetDropdown`:

```kotlin
@Composable
fun BudgetNavHost() {
    val budgetViewModel: BudgetViewModel = hiltViewModel()
    val budgetState by budgetViewModel.uiState.collectAsState()
    // ...

    Scaffold(
        topBar = {
            if (showTopBar) {  // hide on sign-in and add/edit screens
                TopAppBar(
                    title = {
                        BudgetDropdown(
                            activeBudget = budgetState.activeBudget,
                            allBudgets = budgetState.budgets,
                            onBudgetSelected = { budgetViewModel.switchBudget(it) },
                            onManageBudgets = { navController.navigate(Screen.BudgetManagement.route) }
                        )
                    }
                )
            }
        },
        bottomBar = { if (showBottomBar) BottomNavBar(navController) }
    ) { ... }

    // Migration dialog (shown as overlay when needed)
    if (budgetState.needsMigration) {
        MigrationDialog(
            onComplete = { budget -> budgetViewModel.completeMigration(budget) }
        )
    }
}
```

---

## 15. File-by-File Change Summary

### New Files (14 files)

| # | File Path | Purpose |
|---|-----------|---------|
| 1 | `data/entity/BudgetEntity.kt` | Room entity for budgets table |
| 2 | `data/dao/BudgetDao.kt` | DAO for budget CRUD and active-budget management |
| 3 | `data/db/Migrations.kt` | Room migration from v1 to v2 |
| 4 | `data/repository/BudgetRepository.kt` | Budget repository interface |
| 5 | `data/repository/BudgetRepositoryImpl.kt` | Budget repository implementation |
| 6 | `domain/model/Budget.kt` | Budget domain model |
| 7 | `domain/manager/ActiveBudgetManager.kt` | Singleton managing active budget ID via DataStore + StateFlow |
| 8 | `ui/viewmodel/BudgetViewModel.kt` | ViewModel for budget CRUD and switching |
| 9 | `ui/components/BudgetDropdown.kt` | Top bar dropdown for budget switching |
| 10 | `ui/screens/budget/BudgetManagementScreen.kt` | Full screen for budget list management |
| 11 | `ui/screens/budget/BudgetFormDialog.kt` | Dialog for creating/editing a budget |
| 12 | `ui/screens/budget/MigrationDialog.kt` | One-time migration prompt for existing users |
| 13 | `test/.../data/BudgetRepositoryTest.kt` | Unit tests for BudgetRepositoryImpl |
| 14 | `test/.../data/MigrationTest.kt` | Tests for Room migration v1->v2 |

### Modified Files (16 files)

| # | File Path | Changes |
|---|-----------|---------|
| 1 | `data/entity/TransactionEntity.kt` | Add `budgetId` column + FK + index |
| 2 | `data/entity/RecurringTransactionEntity.kt` | Add `budgetId` column + FK + index |
| 3 | `data/db/BudgetDatabase.kt` | Add `BudgetEntity` to entities, bump version to 2, add `budgetDao()` |
| 4 | `data/dao/TransactionDao.kt` | Add `budgetId` param to all query methods |
| 5 | `data/dao/RecurringTransactionDao.kt` | Add `budgetId` param to observation/active queries |
| 6 | `data/repository/TransactionRepository.kt` | Add `budgetId` param to interface methods |
| 7 | `data/repository/TransactionRepositoryImpl.kt` | Pass `budgetId` through, update mappers |
| 8 | `data/repository/RecurringRepository.kt` | Add `budgetId` param to interface methods |
| 9 | `data/repository/RecurringRepositoryImpl.kt` | Pass `budgetId` through, update mappers |
| 10 | `data/repository/BackupRepositoryImpl.kt` | Export/import budgets table, backward compat |
| 11 | `domain/model/Transaction.kt` | Add `budgetId` field |
| 12 | `domain/model/RecurringTransaction.kt` | Add `budgetId` field |
| 13 | `domain/model/BackupData.kt` | Add `BackupBudget` class, `budgetId` to backup models |
| 14 | `domain/usecase/GenerateRecurringTransactionsUseCase.kt` | Set `budgetId` on generated transactions |
| 15 | `di/AppModule.kt` | Add migration, provide `BudgetDao` |
| 16 | `di/RepositoryModule.kt` | Bind `BudgetRepository` |
| 17 | `ui/viewmodel/TransactionListViewModel.kt` | Inject `ActiveBudgetManager`, filter by budget |
| 18 | `ui/viewmodel/AddEditTransactionViewModel.kt` | Inject `ActiveBudgetManager`, set `budgetId` on save |
| 19 | `ui/viewmodel/MonthlySummaryViewModel.kt` | Inject `ActiveBudgetManager`, filter by budget |
| 20 | `ui/viewmodel/RecurringViewModel.kt` | Inject `ActiveBudgetManager`, filter by budget |
| 21 | `ui/navigation/Screen.kt` | Add `BudgetManagement` screen |
| 22 | `ui/navigation/BudgetNavHost.kt` | Add top bar with BudgetDropdown, migration dialog, budget management route |
| 23 | `test/.../data/TransactionRepositoryTest.kt` | Update test calls to pass `budgetId` |
| 24 | `test/.../data/RecurringRepositoryTest.kt` | Update test calls to pass `budgetId` |

---

## 16. Implementation Order

### Phase 1: Data Layer (Foundation)
1. Create `BudgetEntity`
2. Create `BudgetDao`
3. Create `Migrations.kt` with `MIGRATION_1_2`
4. Modify `BudgetDatabase` (add entity, bump version, add dao accessor)
5. Modify `TransactionEntity` (add `budgetId`)
6. Modify `RecurringTransactionEntity` (add `budgetId`)
7. Modify `TransactionDao` (add `budgetId` params)
8. Modify `RecurringTransactionDao` (add `budgetId` params)
9. Update `AppModule` (add migration, provide BudgetDao)

### Phase 2: Domain Layer
10. Create `Budget` domain model
11. Modify `Transaction` domain model (add `budgetId`)
12. Modify `RecurringTransaction` domain model (add `budgetId`)
13. Modify `BackupData` (add `BackupBudget`, add `budgetId` fields)

### Phase 3: Repository Layer
14. Create `BudgetRepository` interface
15. Create `BudgetRepositoryImpl`
16. Modify `TransactionRepository` interface (add `budgetId` params)
17. Modify `TransactionRepositoryImpl` (pass `budgetId`, update mappers)
18. Modify `RecurringRepository` interface (add `budgetId` params)
19. Modify `RecurringRepositoryImpl` (pass `budgetId`, update mappers)
20. Modify `BackupRepositoryImpl` (export/import budgets, backward compat)
21. Update `RepositoryModule` (bind BudgetRepository)

### Phase 4: Manager + Use Case
22. Create `ActiveBudgetManager`
23. Modify `GenerateRecurringTransactionsUseCase` (set `budgetId`)

### Phase 5: ViewModel Layer
24. Create `BudgetViewModel`
25. Modify `TransactionListViewModel` (inject ActiveBudgetManager)
26. Modify `AddEditTransactionViewModel` (inject ActiveBudgetManager)
27. Modify `MonthlySummaryViewModel` (inject ActiveBudgetManager)
28. Modify `RecurringViewModel` (inject ActiveBudgetManager)

### Phase 6: UI Layer
29. Create `BudgetDropdown` component
30. Create `BudgetFormDialog`
31. Create `MigrationDialog`
32. Create `BudgetManagementScreen`
33. Modify `Screen.kt` (add BudgetManagement)
34. Modify `BudgetNavHost` (add top bar, migration dialog, budget route)

### Phase 7: Tests
35. Create `BudgetRepositoryTest`
36. Create `MigrationTest`
37. Update `TransactionRepositoryTest`
38. Update `RecurringRepositoryTest`
39. Update `SummaryCalculationTest`

---

## 17. Edge Cases and Design Decisions

1. **Single budget minimum**: The app must always have at least one budget. Prevent deletion of the last budget via a check in `BudgetViewModel.deleteBudget()`.

2. **Active budget on delete**: If the user deletes the currently active budget, automatically switch to the first remaining budget.

3. **Backup backward compatibility**: When importing an old backup (no `budgets` field), create a "My Budget" default and assign all transactions/recurring to it.

4. **Fresh install**: On fresh install, no migration dialog is shown (no existing data). The user must create their first budget before adding transactions. The app shows a "Create Your First Budget" prompt on the BudgetManagementScreen.

5. **Currency per budget**: The `currency` field on `Budget` is stored but the initial implementation uses it only for display. The `AmountText` component could be extended to format based on the active budget's currency, but this is a future enhancement.

6. **Monthly target**: Stored on the budget for future budget-vs-actual comparisons on the summary screen. Initial implementation stores it but does not render progress bars.

7. **CASCADE deletes**: When a budget is deleted, all its transactions and recurring transactions are automatically deleted by the FK CASCADE rule. The delete confirmation dialog must clearly warn the user about this.

8. **Worker scope**: The `RecurringTransactionWorker` generates transactions for ALL budgets globally. This is intentional -- recurring transactions should fire regardless of which budget is "active" in the UI.
