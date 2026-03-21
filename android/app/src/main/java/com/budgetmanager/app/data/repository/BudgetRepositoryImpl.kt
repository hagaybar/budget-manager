package com.budgetmanager.app.data.repository

import com.budgetmanager.app.data.dao.BudgetDao
import com.budgetmanager.app.data.entity.BudgetEntity
import com.budgetmanager.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun observeAll(): Flow<List<Budget>> =
        budgetDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Budget?> =
        budgetDao.observeById(id).map { it?.toDomain() }

    override fun observeActive(): Flow<Budget?> =
        budgetDao.observeActive().map { it?.toDomain() }

    override suspend fun create(budget: Budget): Long =
        budgetDao.insert(budget.toEntity())

    override suspend fun update(budget: Budget) =
        budgetDao.update(budget.toEntity())

    override suspend fun delete(id: Long) =
        budgetDao.deleteById(id)

    override suspend fun getActiveBudget(): Budget? =
        budgetDao.getActiveBudget()?.toDomain()

    override suspend fun setActiveBudget(id: Long) =
        budgetDao.setActiveBudget(id)

    override suspend fun getAll(): List<Budget> =
        budgetDao.getAll().map { it.toDomain() }

    override suspend fun deleteAll() =
        budgetDao.deleteAll()

    override suspend fun insertAll(budgets: List<Budget>) =
        budgetDao.insertAll(budgets.map { it.toEntity() })

    private fun BudgetEntity.toDomain() = Budget(
        id = id,
        name = name,
        description = description,
        currency = currency,
        monthlyTarget = monthlyTarget,
        isActive = isActive == 1,
        createdAt = createdAt
    )

    private fun Budget.toEntity() = BudgetEntity(
        id = id,
        name = name,
        description = description,
        currency = currency,
        monthlyTarget = monthlyTarget,
        isActive = if (isActive) 1 else 0,
        createdAt = createdAt
    )
}
