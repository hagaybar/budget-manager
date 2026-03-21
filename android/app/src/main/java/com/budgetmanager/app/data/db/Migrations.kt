package com.budgetmanager.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
               AND EXISTS (SELECT 1 FROM `budgets` WHERE `name` = 'My Budget')
        """)
        db.execSQL("""
            UPDATE `recurring_transactions` SET `budget_id` = (
                SELECT `id` FROM `budgets` WHERE `name` = 'My Budget' LIMIT 1
            ) WHERE `budget_id` = 0
               AND EXISTS (SELECT 1 FROM `budgets` WHERE `name` = 'My Budget')
        """)
    }
}
