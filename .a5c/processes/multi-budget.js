/**
 * @process multi-budget
 * @description Add multiple named budgets to the Android budget manager app.
 *              Each budget is an independent wallet with its own transactions, recurring transactions, and summary.
 *              Includes migration flow to import existing data into a named budget.
 * @inputs { projectDir: string, androidDir: string, maxConvergenceIterations: number }
 * @outputs { success: boolean, phases: object }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const {
    projectDir = '/home/ubuntu/projects/budget_manager',
    androidDir = '/home/ubuntu/projects/budget_manager/android',
    maxConvergenceIterations = 5
  } = inputs;

  const featureSpec = {
    concept: 'Separate wallets - each budget is an independent container with its own transactions, recurring transactions, and summary',
    navigation: 'Top bar dropdown for quick budget switching, always visible',
    budgetFields: {
      name: 'required, string, unique',
      description: 'optional, string',
      currency: 'string, default ILS (₪)',
      monthlyTarget: 'optional, number, monthly spending target/limit'
    },
    migration: 'On first launch after update, if existing data exists, show a prompt to import existing budget into a named budget. User fills a budget details card (name, description, currency, target) to migrate their existing data.',
    defaultBehavior: 'After migration or fresh install, user can create additional budgets. All screens filter by active budget.'
  };

  // ============================================================================
  // PHASE 1: ANALYSIS & PLANNING
  // ============================================================================

  const planResult = await ctx.task(analysisAndPlanningTask, {
    projectDir, androidDir, featureSpec
  });

  await ctx.breakpoint({
    question: 'Review the multi-budget implementation plan. Approve to proceed with implementation?',
    title: 'Multi-Budget Plan Review',
    context: {
      runId: ctx.runId,
      files: [
        { path: 'artifacts/multi-budget-plan.md', format: 'markdown' }
      ]
    }
  });

  // ============================================================================
  // PHASE 2: DATABASE LAYER - BudgetEntity + Schema Migration
  // ============================================================================

  const dbLayerResult = await ctx.task(implementDatabaseLayerTask, {
    projectDir, androidDir, featureSpec, plan: planResult
  });

  // Database build convergence
  let dbConverged = false;
  let dbIteration = 0;
  while (!dbConverged && dbIteration < maxConvergenceIterations) {
    dbIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'database-layer', iteration: dbIteration
    });
    dbConverged = buildResult.allPassing === true;
    if (!dbConverged && dbIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'database-layer', buildResult, iteration: dbIteration
      });
    }
  }

  // ============================================================================
  // PHASE 3: DOMAIN & REPOSITORY LAYER
  // ============================================================================

  const domainLayerResult = await ctx.task(implementDomainLayerTask, {
    projectDir, androidDir, featureSpec, plan: planResult, dbLayer: dbLayerResult
  });

  // Domain build convergence
  let domainConverged = false;
  let domainIteration = 0;
  while (!domainConverged && domainIteration < maxConvergenceIterations) {
    domainIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'domain-layer', iteration: domainIteration
    });
    domainConverged = buildResult.allPassing === true;
    if (!domainConverged && domainIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'domain-layer', buildResult, iteration: domainIteration
      });
    }
  }

  // ============================================================================
  // PHASE 4: VIEWMODEL LAYER - Budget management + budget-scoped queries
  // ============================================================================

  const viewModelResult = await ctx.task(implementViewModelLayerTask, {
    projectDir, androidDir, featureSpec, plan: planResult,
    dbLayer: dbLayerResult, domainLayer: domainLayerResult
  });

  // ViewModel build convergence
  let vmConverged = false;
  let vmIteration = 0;
  while (!vmConverged && vmIteration < maxConvergenceIterations) {
    vmIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'viewmodel-layer', iteration: vmIteration
    });
    vmConverged = buildResult.allPassing === true;
    if (!vmConverged && vmIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'viewmodel-layer', buildResult, iteration: vmIteration
      });
    }
  }

  // ============================================================================
  // PHASE 5: UI - Budget management screens + top bar selector + migration dialog
  // ============================================================================

  const uiResult = await ctx.task(implementUILayerTask, {
    projectDir, androidDir, featureSpec, plan: planResult,
    viewModelLayer: viewModelResult
  });

  // UI build convergence
  let uiConverged = false;
  let uiIteration = 0;
  while (!uiConverged && uiIteration < maxConvergenceIterations) {
    uiIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'ui-layer', iteration: uiIteration
    });
    uiConverged = buildResult.allPassing === true;
    if (!uiConverged && uiIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'ui-layer', buildResult, iteration: uiIteration
      });
    }
  }

  // ============================================================================
  // PHASE 6: INTEGRATION - Wire everything together, update navigation, backup
  // ============================================================================

  const integrationResult = await ctx.task(implementIntegrationTask, {
    projectDir, androidDir, featureSpec, plan: planResult,
    dbLayer: dbLayerResult, domainLayer: domainLayerResult,
    viewModelLayer: viewModelResult, uiLayer: uiResult
  });

  // Full integration build convergence
  let integrationConverged = false;
  let integrationIteration = 0;
  while (!integrationConverged && integrationIteration < maxConvergenceIterations) {
    integrationIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'full-integration', iteration: integrationIteration
    });
    integrationConverged = buildResult.allPassing === true;
    if (!integrationConverged && integrationIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'full-integration', buildResult, iteration: integrationIteration
      });
    }
  }

  // ============================================================================
  // PHASE 7: FINAL QUALITY REVIEW & APK BUILD
  // ============================================================================

  const qualityResult = await ctx.task(qualityReviewTask, {
    projectDir, androidDir, featureSpec, plan: planResult
  });

  // Quality fix convergence if issues found
  let qualityConverged = qualityResult.passesQuality === true;
  let qualityIteration = 0;
  while (!qualityConverged && qualityIteration < maxConvergenceIterations) {
    qualityIteration++;
    await ctx.task(fixQualityIssuesTask, {
      androidDir, qualityResult, iteration: qualityIteration
    });
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'quality-fix', iteration: qualityIteration
    });
    qualityConverged = buildResult.allPassing === true;
  }

  // Build APK
  const apkResult = await ctx.task(buildApkTask, {
    androidDir
  });

  await ctx.breakpoint({
    question: `Multi-budget feature implementation complete. DB: ${dbIteration} iterations, Domain: ${domainIteration}, VM: ${vmIteration}, UI: ${uiIteration}, Integration: ${integrationIteration}. APK built: ${apkResult.apkBuilt}. Approve final result?`,
    title: 'Multi-Budget Feature Complete',
    context: {
      runId: ctx.runId,
      files: [
        { path: 'artifacts/multi-budget-plan.md', format: 'markdown' }
      ]
    }
  });

  return {
    success: integrationConverged && apkResult.apkBuilt === true,
    phases: {
      planning: { completed: true },
      database: { completed: true, convergenceIterations: dbIteration },
      domain: { completed: true, convergenceIterations: domainIteration },
      viewModel: { completed: true, convergenceIterations: vmIteration },
      ui: { completed: true, convergenceIterations: uiIteration },
      integration: { completed: true, convergenceIterations: integrationIteration },
      quality: { completed: true, convergenceIterations: qualityIteration },
      apk: { completed: true, apkBuilt: apkResult.apkBuilt }
    },
    metadata: {
      processId: 'multi-budget',
      timestamp: ctx.now()
    }
  };
}

// ============================================================================
// TASK DEFINITIONS
// ============================================================================

export const analysisAndPlanningTask = defineTask('analysis-planning', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 1: Analyze codebase and plan multi-budget implementation',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android architect specializing in Kotlin, Jetpack Compose, Room DB, and MVVM architecture',
      task: `Analyze the existing Android budget manager app and create a detailed implementation plan for adding multiple named budgets. Write the plan to ${args.projectDir}/artifacts/multi-budget-plan.md (create the artifacts directory if needed).`,
      context: {
        featureSpec: args.featureSpec,
        projectDir: args.projectDir,
        androidDir: args.androidDir
      },
      instructions: [
        `Read all existing Android source files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ to understand the current architecture`,
        'Analyze the existing database schema (TransactionEntity, RecurringTransactionEntity), DAOs, repositories, ViewModels, and Compose screens',
        'Design the multi-budget feature with these specifics:',
        '1. NEW BudgetEntity: id (auto), name (unique, required), description (optional), currency (default "ILS"), monthlyTarget (optional Double), isActive (boolean for current selection), createdAt',
        '2. MODIFY TransactionEntity: add budgetId FK column referencing budgets table',
        '3. MODIFY RecurringTransactionEntity: add budgetId FK column referencing budgets table',
        '4. Room DB migration from version 1 to 2: add budgets table, add budgetId columns, create default budget for existing data',
        '5. BudgetDao: CRUD operations, observe all budgets, get/set active budget',
        '6. BudgetRepository: wraps BudgetDao with domain model mapping',
        '7. BudgetViewModel: manages budget CRUD, active budget selection',
        '8. ActiveBudgetManager: singleton that holds/emits the currently active budget ID via StateFlow, persisted in DataStore',
        '9. All existing DAOs/repositories/ViewModels must filter by active budgetId',
        '10. UI changes: top bar dropdown in MainScreen showing budget name + switcher, budget management screen (create/edit/delete budgets)',
        '11. Migration flow: on app open, if budgets table is empty but transactions exist, show MigrationDialog composable that prompts user to create their first budget from existing data',
        '12. Backup/restore must include budgets table',
        `Write the complete plan as markdown to ${args.projectDir}/artifacts/multi-budget-plan.md`,
        'Return a summary of the plan'
      ],
      outputFormat: 'JSON with summary (string), filesCreated (array of paths), filesModified (array of paths), newEntities (array), modifiedEntities (array), migrationSteps (array)'
    },
    outputSchema: {
      type: 'object',
      required: ['summary'],
      properties: {
        summary: { type: 'string' },
        filesCreated: { type: 'array', items: { type: 'string' } },
        filesModified: { type: 'array', items: { type: 'string' } },
        newEntities: { type: 'array', items: { type: 'string' } },
        modifiedEntities: { type: 'array', items: { type: 'string' } },
        migrationSteps: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['planning', 'architecture']
}));

export const implementDatabaseLayerTask = defineTask('implement-database-layer', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 2: Implement database layer (BudgetEntity, migration, modified DAOs)',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer expert in Room DB, Kotlin, and database migrations',
      task: 'Implement the database layer changes for multi-budget support in the Android budget manager app.',
      context: {
        featureSpec: args.featureSpec,
        plan: args.plan,
        androidDir: args.androidDir
      },
      instructions: [
        `Read the existing database files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/data/`,
        'Create BudgetEntity.kt in data/entity/ with fields: id (Int, auto PK), name (String, unique), description (String, default ""), currency (String, default "ILS"), monthlyTarget (Double?, nullable), isActive (Int, default 0), createdAt (String)',
        'Create BudgetDao.kt in data/dao/ with: insert, update, deleteById, observeAll (Flow), observeById (Flow), getById, getActiveBudget, setActiveBudget (deactivate all then activate one), getAll, deleteAll, insertAll',
        'Modify TransactionEntity.kt: add budgetId (Long, default 0) column with FK to budgets table and index',
        'Modify RecurringTransactionEntity.kt: add budgetId (Long, default 0) column with FK to budgets table and index',
        'Modify TransactionDao.kt: add budgetId parameter to all observe/query methods so results are filtered by budget. Keep existing method signatures but add budget-scoped variants.',
        'Modify RecurringTransactionDao.kt: add budgetId parameter similarly',
        'Update BudgetDatabase.kt: bump version to 2, add BudgetEntity to entities, add budgetDao(), add Migration(1,2) that: (a) creates budgets table, (b) inserts a default budget named "My Budget" with isActive=1, (c) adds budgetId column to transactions with DEFAULT referencing the new budget id, (d) adds budgetId column to recurring_transactions similarly',
        'Make sure foreign keys and indices are properly defined',
        'Return summary of all changes made'
      ],
      outputFormat: 'JSON with summary (string), filesCreated (array), filesModified (array), allPassing (boolean)'
    },
    outputSchema: {
      type: 'object',
      required: ['summary'],
      properties: {
        summary: { type: 'string' },
        filesCreated: { type: 'array', items: { type: 'string' } },
        filesModified: { type: 'array', items: { type: 'string' } },
        allPassing: { type: 'boolean' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['database', 'implementation']
}));

export const implementDomainLayerTask = defineTask('implement-domain-layer', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 3: Implement domain layer (Budget model, BudgetRepository, ActiveBudgetManager)',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer expert in clean architecture, Kotlin, and Hilt DI',
      task: 'Implement the domain and repository layer changes for multi-budget support.',
      context: {
        featureSpec: args.featureSpec,
        plan: args.plan,
        androidDir: args.androidDir,
        dbLayer: args.dbLayer
      },
      instructions: [
        `Read the existing domain and repository files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/`,
        'Create Budget.kt domain model in domain/model/: data class Budget(id: Long, name: String, description: String, currency: String, monthlyTarget: Double?, isActive: Boolean, createdAt: String)',
        'Create BudgetRepository.kt interface in data/repository/: observeAll, observeById, create, update, delete, getActiveBudget, setActiveBudget, getAll, deleteAll, insertAll',
        'Create BudgetRepositoryImpl.kt implementing BudgetRepository with entity↔domain mapping',
        'Create ActiveBudgetManager.kt in domain/usecase/ (or a dedicated manager package): a @Singleton that uses DataStore to persist the active budget ID and exposes it as StateFlow<Long>. Methods: getActiveBudgetId(), setActiveBudgetId(id), observeActiveBudgetId(). On init, reads from DataStore.',
        'Modify TransactionRepository/Impl: add budgetId parameter to observe and query methods',
        'Modify RecurringRepository/Impl: add budgetId parameter to observe and query methods',
        'Modify Transaction domain model: add budgetId field (Long)',
        'Modify RecurringTransaction domain model: add budgetId field (Long)',
        'Update BackupData model to include budgets list',
        'Modify BackupRepository/Impl: export and import budgets, include budgetId in transaction/recurring mapping',
        'Update Hilt RepositoryModule to bind BudgetRepository -> BudgetRepositoryImpl',
        'Update Hilt AppModule to provide BudgetDao from database and provide ActiveBudgetManager as singleton',
        'Return summary of all changes'
      ],
      outputFormat: 'JSON with summary (string), filesCreated (array), filesModified (array), allPassing (boolean)'
    },
    outputSchema: {
      type: 'object',
      required: ['summary'],
      properties: {
        summary: { type: 'string' },
        filesCreated: { type: 'array', items: { type: 'string' } },
        filesModified: { type: 'array', items: { type: 'string' } },
        allPassing: { type: 'boolean' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['domain', 'repository', 'implementation']
}));

export const implementViewModelLayerTask = defineTask('implement-viewmodel-layer', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 4: Implement ViewModel layer (BudgetViewModel, budget-scoped queries)',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer expert in Jetpack Compose, ViewModel, StateFlow, and Hilt',
      task: 'Implement the ViewModel layer changes for multi-budget support.',
      context: {
        featureSpec: args.featureSpec,
        plan: args.plan,
        androidDir: args.androidDir,
        dbLayer: args.dbLayer,
        domainLayer: args.domainLayer
      },
      instructions: [
        `Read the existing ViewModel files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/viewmodel/`,
        'Create BudgetViewModel.kt: manages budget CRUD operations and active budget selection',
        '  - Inject BudgetRepository and ActiveBudgetManager',
        '  - UiState: budgets list, activeBudget, isLoading, showCreateDialog, showEditDialog, editingBudget, showDeleteConfirmation, error',
        '  - Methods: loadBudgets, createBudget(name, description, currency, monthlyTarget), updateBudget, deleteBudget, setActiveBudget(id), showCreateDialog, showEditDialog, dismissDialog',
        '  - On init: observe all budgets and active budget ID',
        'Create MigrationViewModel.kt: handles the one-time migration flow',
        '  - Inject BudgetRepository, TransactionRepository, ActiveBudgetManager',
        '  - UiState: needsMigration (boolean), migrationName, migrationDescription, migrationCurrency, migrationTarget, isMigrating, migrationComplete, error',
        '  - On init: check if budgets table is empty but transactions exist → set needsMigration=true',
        '  - Methods: setName, setDescription, setCurrency, setTarget, performMigration (creates budget, updates all existing transactions/recurring with new budgetId, sets as active)',
        'Modify TransactionListViewModel: inject ActiveBudgetManager, filter transactions by active budget ID. React to budget changes.',
        'Modify AddEditTransactionViewModel: inject ActiveBudgetManager, set budgetId on new transactions from active budget',
        'Modify MonthlySummaryViewModel: inject ActiveBudgetManager, filter summary by active budget',
        'Modify RecurringViewModel: inject ActiveBudgetManager, filter and create recurring transactions with active budget ID',
        'Modify SettingsViewModel: update backup/restore to handle budgets',
        'Return summary of all changes'
      ],
      outputFormat: 'JSON with summary (string), filesCreated (array), filesModified (array), allPassing (boolean)'
    },
    outputSchema: {
      type: 'object',
      required: ['summary'],
      properties: {
        summary: { type: 'string' },
        filesCreated: { type: 'array', items: { type: 'string' } },
        filesModified: { type: 'array', items: { type: 'string' } },
        allPassing: { type: 'boolean' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['viewmodel', 'implementation']
}));

export const implementUILayerTask = defineTask('implement-ui-layer', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 5: Implement UI layer (budget selector, management screens, migration dialog)',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer expert in Jetpack Compose, Material Design 3, and responsive UI',
      task: 'Implement the UI layer for multi-budget support in the Android budget manager app.',
      context: {
        featureSpec: args.featureSpec,
        plan: args.plan,
        androidDir: args.androidDir,
        viewModelLayer: args.viewModelLayer
      },
      instructions: [
        `Read all existing UI/screen files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/`,
        'Create BudgetSelectorDropdown.kt composable in ui/components/:',
        '  - A Material3 ExposedDropdownMenuBox in the top app bar',
        '  - Shows current budget name',
        '  - Dropdown lists all budgets with name and currency indicator',
        '  - "Manage Budgets" item at bottom with icon to navigate to budget management',
        '  - Calls onBudgetSelected(budgetId) and onManageBudgetsClick()',
        'Create BudgetManagementScreen.kt in ui/screens/budget/:',
        '  - Shows list of all budgets as cards',
        '  - Each card: name, description, currency, monthly target (if set), active indicator',
        '  - FAB to add new budget',
        '  - Swipe-to-delete or long-press to delete (with confirmation)',
        '  - Tap card to edit, tap radio/check to set active',
        '  - Uses BudgetViewModel',
        'Create BudgetFormDialog.kt composable in ui/screens/budget/:',
        '  - AlertDialog for creating/editing a budget',
        '  - Fields: name (required), description (optional), currency (dropdown with common currencies, default ILS), monthly target (optional number)',
        '  - Validation: name required and unique',
        'Create MigrationScreen.kt (or MigrationDialog.kt) in ui/screens/migration/:',
        '  - Full-screen dialog or screen shown on first launch when existing data needs migration',
        '  - Welcoming message explaining the new multi-budget feature',
        '  - Budget details card form: name, description, currency, monthly target',
        '  - "Import Existing Data" button that creates budget and migrates data',
        '  - Progress indicator during migration',
        '  - Success state with "Get Started" button',
        '  - Uses MigrationViewModel',
        'Modify BudgetNavHost.kt:',
        '  - Add BudgetManagement screen route',
        '  - Add migration check: if needsMigration, show MigrationScreen before main content',
        '  - Pass BudgetViewModel to screens that need budget context',
        'Modify MainScreen / Scaffold:',
        '  - Add BudgetSelectorDropdown to the TopAppBar on all main screens',
        '  - Connect dropdown to BudgetViewModel for switching',
        'Update Screen.kt sealed class: add BudgetManagement route',
        'Ensure consistent Material3 theming across new screens',
        'Return summary of all UI changes'
      ],
      outputFormat: 'JSON with summary (string), filesCreated (array), filesModified (array), allPassing (boolean)'
    },
    outputSchema: {
      type: 'object',
      required: ['summary'],
      properties: {
        summary: { type: 'string' },
        filesCreated: { type: 'array', items: { type: 'string' } },
        filesModified: { type: 'array', items: { type: 'string' } },
        allPassing: { type: 'boolean' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['ui', 'compose', 'implementation']
}));

export const implementIntegrationTask = defineTask('implement-integration', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 6: Wire everything together, update navigation and backup',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer expert in full-stack Android architecture and integration',
      task: 'Wire together all multi-budget components and ensure full integration across the app.',
      context: {
        featureSpec: args.featureSpec,
        plan: args.plan,
        androidDir: args.androidDir,
        dbLayer: args.dbLayer,
        domainLayer: args.domainLayer,
        viewModelLayer: args.viewModelLayer,
        uiLayer: args.uiLayer
      },
      instructions: [
        `Read all source files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ to understand current state`,
        'Verify and fix all integration points:',
        '1. Database migration runs correctly: BudgetDatabase version 2 with MIGRATION_1_2',
        '2. Hilt DI provides all new dependencies: BudgetDao, BudgetRepository, ActiveBudgetManager',
        '3. Navigation routes are properly registered for BudgetManagement screen',
        '4. Budget selector dropdown appears in TopAppBar on all main screens',
        '5. Migration flow triggers correctly on first launch with existing data',
        '6. All transaction/recurring/summary screens properly filter by active budget',
        '7. Creating a new transaction assigns it to the active budget',
        '8. Backup export includes budgets, backup import restores budgets correctly',
        '9. Recurring transaction generation respects active budget',
        '10. WorkManager recurring worker generates for ALL active budgets (not just active one)',
        '11. Budget deletion: prompt what to do with transactions (delete or move to another budget)',
        '12. Edge cases: no budgets left, deleting active budget switches to another',
        'Fix any compilation errors, missing imports, or wiring issues',
        'Ensure all existing functionality still works within the budget context',
        'Return summary of integration work done'
      ],
      outputFormat: 'JSON with summary (string), filesModified (array), integrationIssuesFixed (array), allPassing (boolean)'
    },
    outputSchema: {
      type: 'object',
      required: ['summary'],
      properties: {
        summary: { type: 'string' },
        filesModified: { type: 'array', items: { type: 'string' } },
        integrationIssuesFixed: { type: 'array', items: { type: 'string' } },
        allPassing: { type: 'boolean' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['integration', 'wiring']
}));

export const buildAndVerifyTask = defineTask('build-and-verify', (args, taskCtx) => ({
  kind: 'agent',
  title: `Build & verify (${args.scope}, iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android build engineer',
      task: `Build the Android project and check for compilation errors. Scope: ${args.scope}, iteration ${args.iteration}.`,
      context: {
        androidDir: args.androidDir,
        scope: args.scope,
        iteration: args.iteration
      },
      instructions: [
        `cd ${args.androidDir} && ./gradlew assembleDebug 2>&1`,
        'If the build succeeds, return allPassing: true',
        'If the build fails, capture the full error output including file paths, line numbers, and error messages',
        'Categorize errors: compilation errors, missing imports, type mismatches, unresolved references, etc.',
        'Return the complete error details so they can be fixed'
      ],
      outputFormat: 'JSON with allPassing (boolean), errors (array of {file, line, message, type}), buildOutput (string summary)'
    },
    outputSchema: {
      type: 'object',
      required: ['allPassing'],
      properties: {
        allPassing: { type: 'boolean' },
        errors: { type: 'array', items: { type: 'object' } },
        buildOutput: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['build', 'verification', args.scope]
}));

export const fixBuildFailuresTask = defineTask('fix-build-failures', (args, taskCtx) => ({
  kind: 'agent',
  title: `Fix build failures (${args.scope}, iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer and debugger',
      task: `Fix all build failures in the Android project for scope: ${args.scope}.`,
      context: {
        androidDir: args.androidDir,
        scope: args.scope,
        buildResult: args.buildResult,
        iteration: args.iteration
      },
      instructions: [
        'Analyze the build errors provided in buildResult',
        `Read the failing files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/`,
        'Fix each compilation error:',
        '- Missing imports: add correct import statements',
        '- Type mismatches: fix type annotations and conversions',
        '- Unresolved references: check if classes/methods exist, create or fix references',
        '- Missing method implementations: implement required interface methods',
        '- Incorrect constructor parameters: align with actual class definitions',
        'After fixing, verify the fix makes sense in context of the multi-budget feature',
        'Do NOT introduce regressions to existing functionality',
        'Return summary of fixes applied'
      ],
      outputFormat: 'JSON with summary (string), fixesApplied (array of {file, issue, fix}), filesModified (array)'
    },
    outputSchema: {
      type: 'object',
      required: ['summary'],
      properties: {
        summary: { type: 'string' },
        fixesApplied: { type: 'array', items: { type: 'object' } },
        filesModified: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['fix', 'debugging', args.scope]
}));

export const qualityReviewTask = defineTask('quality-review', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 7: Quality review of multi-budget implementation',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android QA engineer and code reviewer',
      task: 'Review the complete multi-budget implementation for quality, correctness, and completeness.',
      context: {
        featureSpec: args.featureSpec,
        plan: args.plan,
        androidDir: args.androidDir
      },
      instructions: [
        `Read all source files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/`,
        'Verify against the feature spec:',
        '1. BudgetEntity exists with correct fields (name, description, currency, monthlyTarget, isActive)',
        '2. TransactionEntity and RecurringTransactionEntity have budgetId FK',
        '3. Room migration from v1→v2 is correct and handles existing data',
        '4. BudgetDao has all required CRUD and query methods',
        '5. BudgetRepository correctly maps entities to domain models',
        '6. ActiveBudgetManager persists active budget ID in DataStore',
        '7. All ViewModels properly filter by active budget',
        '8. Budget selector dropdown appears in top bar',
        '9. Budget management screen allows full CRUD',
        '10. Migration dialog appears when needed and works correctly',
        '11. Backup/restore includes budgets',
        '12. No hardcoded budget IDs or assumptions about single budget',
        'Check for: missing null safety, missing error handling, potential crashes, incorrect Room annotations',
        'If all checks pass, return passesQuality: true',
        'If issues found, return detailed list of issues with file paths and line numbers'
      ],
      outputFormat: 'JSON with passesQuality (boolean), issues (array of {file, line, severity, description, fix}), summary (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['passesQuality', 'summary'],
      properties: {
        passesQuality: { type: 'boolean' },
        issues: { type: 'array', items: { type: 'object' } },
        summary: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['quality', 'review']
}));

export const fixQualityIssuesTask = defineTask('fix-quality-issues', (args, taskCtx) => ({
  kind: 'agent',
  title: `Fix quality issues (iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer',
      task: 'Fix the quality issues identified in the review.',
      context: {
        androidDir: args.androidDir,
        qualityResult: args.qualityResult,
        iteration: args.iteration
      },
      instructions: [
        'Fix each quality issue identified in the review',
        'Read the affected files, apply fixes, and ensure correctness',
        'Return summary of fixes'
      ],
      outputFormat: 'JSON with summary (string), fixesApplied (array), filesModified (array)'
    },
    outputSchema: {
      type: 'object',
      required: ['summary'],
      properties: {
        summary: { type: 'string' },
        fixesApplied: { type: 'array', items: { type: 'object' } },
        filesModified: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['quality', 'fix']
}));

export const buildApkTask = defineTask('build-apk', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Build debug APK',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android build engineer',
      task: 'Build the debug APK for the Android app.',
      context: {
        androidDir: args.androidDir
      },
      instructions: [
        `cd ${args.androidDir} && ./gradlew assembleDebug 2>&1`,
        'If the build succeeds, find the APK path (usually app/build/outputs/apk/debug/app-debug.apk)',
        'Return the APK path and build status'
      ],
      outputFormat: 'JSON with apkBuilt (boolean), apkPath (string), buildOutput (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['apkBuilt'],
      properties: {
        apkBuilt: { type: 'boolean' },
        apkPath: { type: 'string' },
        buildOutput: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['build', 'apk']
}));
