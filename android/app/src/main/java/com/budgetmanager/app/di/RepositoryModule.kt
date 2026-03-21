package com.budgetmanager.app.di

import com.budgetmanager.app.data.repository.BackupRepository
import com.budgetmanager.app.data.repository.BackupRepositoryImpl
import com.budgetmanager.app.data.repository.BudgetRepository
import com.budgetmanager.app.data.repository.BudgetRepositoryImpl
import com.budgetmanager.app.data.repository.RecurringRepository
import com.budgetmanager.app.data.repository.RecurringRepositoryImpl
import com.budgetmanager.app.data.repository.TransactionRepository
import com.budgetmanager.app.data.repository.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindRecurringRepository(
        impl: RecurringRepositoryImpl
    ): RecurringRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        impl: BackupRepositoryImpl
    ): BackupRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        impl: BudgetRepositoryImpl
    ): BudgetRepository
}
