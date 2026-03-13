package com.budgetmanager.app.di

import android.content.Context
import androidx.room.Room
import com.budgetmanager.app.data.dao.RecurringTransactionDao
import com.budgetmanager.app.data.dao.TransactionDao
import com.budgetmanager.app.data.db.BudgetDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
        ).build()
    }

    @Provides
    fun provideTransactionDao(database: BudgetDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideRecurringTransactionDao(database: BudgetDatabase): RecurringTransactionDao {
        return database.recurringTransactionDao()
    }
}
