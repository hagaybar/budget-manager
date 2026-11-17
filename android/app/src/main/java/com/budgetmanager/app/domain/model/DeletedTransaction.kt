package com.budgetmanager.app.domain.model

/** A transaction sitting in the recycle bin, with the time it was deleted. */
data class DeletedTransaction(
    val transaction: Transaction,
    val deletedAt: String
)
