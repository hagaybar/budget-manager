package com.budgetmanager.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Shared table recreation logic used by both MIGRATION_1_2 and MIGRATION_2_3.
 * Recreates transactions and recurring_transactions tables with proper FK constraints,
 * since SQLite's ALTER TABLE cannot add foreign keys.
 */
private fun recreateTablesWithForeignKeys(db: SupportSQLiteDatabase) {
    // Recreate recurring_transactions with FK constraint on budget_id
    // (must be done before transactions, since transactions has FK to recurring_transactions)
    db.execSQL("""
        CREATE TABLE IF NOT EXISTS `recurring_transactions_new` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `type` TEXT NOT NULL,
            `amount` REAL NOT NULL,
            `category` TEXT NOT NULL,
            `description` TEXT NOT NULL DEFAULT '',
            `frequency` TEXT NOT NULL,
            `day_of_week` INTEGER,
            `day_of_month` INTEGER,
            `start_date` TEXT NOT NULL,
            `end_date` TEXT,
            `is_active` INTEGER NOT NULL DEFAULT 1,
            `created_at` TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `budget_id` INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(`budget_id`) REFERENCES `budgets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
    """)
    db.execSQL("""
        INSERT INTO `recurring_transactions_new` (`id`, `type`, `amount`, `category`, `description`, `frequency`, `day_of_week`, `day_of_month`, `start_date`, `end_date`, `is_active`, `created_at`, `budget_id`)
        SELECT `id`, `type`, `amount`, `category`, `description`, `frequency`, `day_of_week`, `day_of_month`, `start_date`, `end_date`, `is_active`, `created_at`, COALESCE(`budget_id`, 0)
        FROM `recurring_transactions`
    """)
    db.execSQL("DROP TABLE `recurring_transactions`")
    db.execSQL("ALTER TABLE `recurring_transactions_new` RENAME TO `recurring_transactions`")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_is_active` ON `recurring_transactions` (`is_active`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_frequency` ON `recurring_transactions` (`frequency`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_budget_id` ON `recurring_transactions` (`budget_id`)")

    // Recreate transactions with FK constraints on both recurring_id and budget_id
    db.execSQL("""
        CREATE TABLE IF NOT EXISTS `transactions_new` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `type` TEXT NOT NULL,
            `amount` REAL NOT NULL,
            `category` TEXT NOT NULL,
            `description` TEXT NOT NULL DEFAULT '',
            `date` TEXT NOT NULL,
            `created_at` TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `recurring_id` INTEGER,
            `budget_id` INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(`recurring_id`) REFERENCES `recurring_transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
            FOREIGN KEY(`budget_id`) REFERENCES `budgets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        )
    """)
    db.execSQL("""
        INSERT INTO `transactions_new` (`id`, `type`, `amount`, `category`, `description`, `date`, `created_at`, `recurring_id`, `budget_id`)
        SELECT `id`, `type`, `amount`, `category`, `description`, `date`, `created_at`, `recurring_id`, COALESCE(`budget_id`, 0)
        FROM `transactions`
    """)
    db.execSQL("DROP TABLE `transactions`")
    db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category` ON `transactions` (`category`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_recurring_id` ON `transactions` (`recurring_id`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_budget_id` ON `transactions` (`budget_id`)")
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create budgets table first (referenced by FKs)
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

        // 3. Recreate tables with proper FK constraints
        recreateTablesWithForeignKeys(db)

        // 4. Link existing data to the default budget
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

/**
 * MIGRATION_2_3: Repair migration for databases left in a corrupted state by the
 * previous buggy MIGRATION_1_2 (which used ALTER TABLE ADD COLUMN instead of
 * table recreation, leaving tables without FK constraints on budget_id).
 *
 * This migration recreates the tables with proper FK constraints.
 * Also handles the case where budgets table exists but has no data,
 * or where budget_id columns were added without FK constraints.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Ensure budgets table exists (it should from the previous migration)
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

        // Insert a default budget if none exists but data does
        db.execSQL("""
            INSERT OR IGNORE INTO `budgets` (`name`, `description`, `currency`, `is_active`)
            SELECT 'My Budget', '', 'ILS', 1
            WHERE NOT EXISTS (SELECT 1 FROM `budgets` LIMIT 1)
              AND (EXISTS (SELECT 1 FROM `transactions` LIMIT 1)
                   OR EXISTS (SELECT 1 FROM `recurring_transactions` LIMIT 1))
        """)

        // Recreate tables with proper FK constraints
        recreateTablesWithForeignKeys(db)

        // Link any orphaned data to the default budget
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
