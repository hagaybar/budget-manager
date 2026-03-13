package com.budgetmanager.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.budgetmanager.app.data.dao.RecurringTransactionDao
import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.entity.RecurringTransactionEntity
import com.budgetmanager.app.data.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
}
