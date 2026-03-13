/**
 * @process android-budget-manager
 * @description Build a native Android budget manager app (Kotlin, Jetpack Compose, Material Design 3, Room DB, Google Sign-In)
 *              with TDD methodology, quality-gated convergence loops, and APK delivery via GitHub Release.
 * @inputs { appName: string, packageName: string, features: array, githubRepo: string }
 * @outputs { success: boolean, phases: object, apkPath: string }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const {
    appName = 'BudgetManager',
    packageName = 'com.budgetmanager.app',
    githubRepo = 'hagaybar/budget-manager',
    projectDir = '/home/ubuntu/projects/budget_manager',
    androidDir = '/home/ubuntu/projects/budget_manager/android',
    maxConvergenceIterations = 5,
    features = [
      'Google Sign-In authentication',
      'Transaction CRUD (income/expense with type, amount, category, description, date)',
      'Monthly summary with category breakdowns and net balance',
      'Recurring transactions (weekly/monthly with generate)',
      'Backup/restore and export/import (JSON)',
      'Currency: Israeli New Shekel (₪)',
      'Material Design 3 with dynamic colors',
      'Bottom navigation (Transactions, Summary, Recurring, Settings)',
      'Swipe-to-delete transactions',
      'Date picker for transaction dates',
      'Filter transactions by type, category, date range'
    ]
  } = inputs;

  // ============================================================================
  // PHASE 1: ARCHITECTURE & PLANNING
  // ============================================================================

  const planResult = await ctx.task(architecturePlanningTask, {
    appName, packageName, features, projectDir, androidDir
  });

  // ============================================================================
  // PHASE 2: ANDROID PROJECT SCAFFOLDING
  // ============================================================================

  const scaffoldResult = await ctx.task(scaffoldAndroidProjectTask, {
    appName, packageName, projectDir, androidDir, plan: planResult
  });

  // ============================================================================
  // PHASE 3: DATA LAYER (Room DB + Repository)
  // ============================================================================

  // 3a: Write data layer tests first (TDD Red)
  const dataTestsResult = await ctx.task(writeDataLayerTestsTask, {
    appName, packageName, androidDir, plan: planResult, iteration: 1
  });

  // 3b: Implement data layer (TDD Green)
  const dataLayerResult = await ctx.task(implementDataLayerTask, {
    appName, packageName, androidDir, plan: planResult, tests: dataTestsResult
  });

  // 3c: Data layer convergence loop
  let dataConverged = false;
  let dataIteration = 0;
  while (!dataConverged && dataIteration < maxConvergenceIterations) {
    dataIteration++;
    const buildResult = await ctx.task(buildAndTestTask, {
      androidDir, layer: 'data', iteration: dataIteration
    });
    dataConverged = buildResult.allPassing === true;
    if (!dataConverged && dataIteration < maxConvergenceIterations) {
      await ctx.task(fixFailuresTask, {
        androidDir, layer: 'data', buildResult, iteration: dataIteration
      });
    }
  }

  // ============================================================================
  // PHASE 4: GOOGLE SIGN-IN & AUTH
  // ============================================================================

  const authResult = await ctx.task(implementAuthTask, {
    appName, packageName, androidDir, plan: planResult
  });

  // ============================================================================
  // PHASE 5: UI LAYER (Jetpack Compose + Material Design 3)
  // ============================================================================

  // 5a: Theme + base composables
  const themeResult = await ctx.task(implementThemeTask, {
    appName, packageName, androidDir, plan: planResult
  });

  // 5b: Screen composables + ViewModels
  const screensResult = await ctx.task(implementScreensTask, {
    appName, packageName, androidDir, plan: planResult,
    features, theme: themeResult, dataLayer: dataLayerResult, auth: authResult
  });

  // 5c: Navigation + integration
  const navigationResult = await ctx.task(implementNavigationTask, {
    appName, packageName, androidDir, plan: planResult,
    screens: screensResult, auth: authResult
  });

  // ============================================================================
  // PHASE 6: FULL BUILD CONVERGENCE LOOP
  // ============================================================================

  let fullConverged = false;
  let fullIteration = 0;
  while (!fullConverged && fullIteration < maxConvergenceIterations) {
    fullIteration++;
    const fullBuildResult = await ctx.task(buildAndTestTask, {
      androidDir, layer: 'full', iteration: fullIteration
    });
    fullConverged = fullBuildResult.allPassing === true;
    if (!fullConverged && fullIteration < maxConvergenceIterations) {
      await ctx.task(fixFailuresTask, {
        androidDir, layer: 'full', buildResult: fullBuildResult, iteration: fullIteration
      });
    }
  }

  // ============================================================================
  // PHASE 7: BUILD DEBUG APK
  // ============================================================================

  const apkResult = await ctx.task(buildApkTask, {
    androidDir, appName
  });

  // ============================================================================
  // PHASE 8: QUALITY VERIFICATION BREAKPOINT
  // ============================================================================

  await ctx.breakpoint({
    question: `Android APK built for ${appName}. Data layer converged in ${dataIteration} iterations, full build in ${fullIteration} iterations. APK at: ${apkResult.apkPath}. Ready to push to GitHub and create release?`,
    title: 'APK Build Review'
  });

  // ============================================================================
  // PHASE 9: PUSH TO GITHUB & CREATE RELEASE
  // ============================================================================

  const deliveryResult = await ctx.task(deliverToGithubTask, {
    projectDir, androidDir, githubRepo, apkPath: apkResult.apkPath, appName
  });

  return {
    success: fullConverged && apkResult.apkBuilt === true,
    appName,
    phases: {
      planning: { completed: true },
      scaffold: { completed: true },
      dataLayer: { completed: true, convergenceIterations: dataIteration },
      auth: { completed: true },
      ui: { completed: true },
      fullBuild: { completed: true, convergenceIterations: fullIteration },
      apk: { completed: true, apkPath: apkResult.apkPath },
      delivery: { completed: true, releaseUrl: deliveryResult.releaseUrl }
    },
    metadata: {
      processId: 'android-budget-manager',
      timestamp: ctx.now()
    }
  };
}

// ============================================================================
// TASK DEFINITIONS
// ============================================================================

export const architecturePlanningTask = defineTask('architecture-planning', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 1: Architecture & Planning for Android App',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android architect specializing in Kotlin, Jetpack Compose, and Material Design 3',
      task: `Design the complete architecture for the "${args.appName}" native Android app. This app replicates an existing web-based budget manager with these features. Create ANDROID_PLAN.md at ${args.projectDir}/ANDROID_PLAN.md`,
      context: {
        appName: args.appName,
        packageName: args.packageName,
        features: args.features,
        projectDir: args.projectDir,
        androidDir: args.androidDir,
        existingWebApp: 'The existing web app uses FastAPI+SQLite with transactions (CRUD, filters), monthly summaries with category breakdowns, recurring transactions (weekly/monthly), and backup/restore. Currency is ILS (₪).'
      },
      instructions: [
        'Read the existing web app code at ' + args.projectDir + '/app/ to understand all features, schemas, and business logic',
        'Create ANDROID_PLAN.md with complete architecture:',
        '- Tech stack: Kotlin, Jetpack Compose, Material Design 3, Room DB, Hilt DI, Kotlin Coroutines/Flow, Google Sign-In',
        '- Package structure: data (db, entities, dao, repository), domain (models, usecases), ui (theme, screens, viewmodels, components, navigation), auth',
        '- Room entities matching the existing SQLite schema: Transaction (id, type, amount, category, description, date, createdAt, recurringId), RecurringTransaction (id, type, amount, category, description, frequency, dayOfWeek, dayOfMonth, startDate, endDate, isActive, createdAt)',
        '- DAOs with Flow-based reactive queries for all CRUD operations plus filters and monthly aggregation',
        '- Repository pattern with interfaces',
        '- ViewModels for each screen: TransactionList, AddEditTransaction, MonthlySummary, RecurringTransactions, Settings/Backup',
        '- Navigation: Bottom nav with Transactions, Summary, Recurring, Settings tabs',
        '- Google Sign-In flow: sign in -> create/load user profile -> access budget data',
        '- Backup/restore via JSON export/import',
        '- List all Gradle dependencies with versions',
        'Write the plan file to disk',
        'Return a JSON summary'
      ],
      outputFormat: 'JSON with screens (array), entities (array), daos (array), viewModels (array), dependencies (array), summary (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['screens', 'entities', 'summary'],
      properties: {
        screens: { type: 'array', items: { type: 'string' } },
        entities: { type: 'array', items: { type: 'string' } },
        daos: { type: 'array', items: { type: 'string' } },
        viewModels: { type: 'array', items: { type: 'string' } },
        dependencies: { type: 'array', items: { type: 'string' } },
        summary: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['planning', 'android', 'architecture']
}));

export const scaffoldAndroidProjectTask = defineTask('scaffold-android-project', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 2: Scaffold Android Project',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer with deep Gradle and project setup expertise',
      task: `Scaffold a complete Android project at ${args.androidDir} for "${args.appName}"`,
      context: {
        appName: args.appName,
        packageName: args.packageName,
        androidDir: args.androidDir,
        plan: args.plan,
        androidSdkPath: '/home/ubuntu/android-sdk',
        javaVersion: '17'
      },
      instructions: [
        'Read the ANDROID_PLAN.md at ' + args.projectDir + '/ANDROID_PLAN.md for the complete architecture',
        'Create the Android project directory structure at ' + args.androidDir + ':',
        '- settings.gradle.kts, build.gradle.kts (project-level), gradle.properties',
        '- app/build.gradle.kts with all dependencies: Compose BOM, Material3, Room, Hilt, Navigation-Compose, Google Sign-In, Coroutines, Lifecycle',
        '- Set compileSdk=34, minSdk=24, targetSdk=34',
        '- app/src/main/AndroidManifest.xml with INTERNET permission',
        '- Create package directories for: ' + args.packageName.replace(/\\./g, '/'),
        '- local.properties pointing to sdk.dir=/home/ubuntu/android-sdk',
        '- gradle/wrapper/ with gradle-wrapper.properties (use Gradle 8.5)',
        '- Download gradle wrapper: run "cd ' + args.androidDir + ' && gradle wrapper --gradle-version 8.5" or create the wrapper files manually',
        '- Ensure the project compiles with: cd ' + args.androidDir + ' && ANDROID_HOME=/home/ubuntu/android-sdk ./gradlew assembleDebug (if it fails, fix until it compiles)',
        'IMPORTANT: Make sure all files are actually written to disk, not just described',
        'IMPORTANT: Set ANDROID_HOME=/home/ubuntu/android-sdk in all gradle commands',
        'Return summary of files created'
      ],
      outputFormat: 'JSON with filesCreated (array), compiles (boolean), summary (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['filesCreated', 'compiles'],
      properties: {
        filesCreated: { type: 'array', items: { type: 'string' } },
        compiles: { type: 'boolean' },
        summary: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['scaffold', 'android', 'gradle']
}));

export const writeDataLayerTestsTask = defineTask('write-data-tests', (args, taskCtx) => ({
  kind: 'agent',
  title: `Phase 3a: Write Data Layer Tests (TDD Red) - iteration ${args.iteration}`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android test engineer specializing in Room database testing',
      task: `Write unit tests for the data layer of "${args.appName}" (TDD Red phase)`,
      context: {
        packageName: args.packageName,
        androidDir: args.androidDir,
        plan: args.plan
      },
      instructions: [
        'Read the ANDROID_PLAN.md for entity and DAO specifications',
        'Read the existing web app schemas at /home/ubuntu/projects/budget_manager/app/schemas.py and database.py for exact field definitions',
        'Create test files in ' + args.androidDir + '/app/src/test/java/' + args.packageName.replace(/\\./g, '/') + '/data/:',
        '- TransactionDaoTest.kt: Test all CRUD operations, filtering by type/category/date range, ordering',
        '- RecurringTransactionDaoTest.kt: Test CRUD, generate occurrences logic',
        '- TransactionRepositoryTest.kt: Test repository methods with mocked DAO',
        '- SummaryRepositoryTest.kt: Test monthly summary aggregation (total income, total expenses, net balance, category breakdowns)',
        'Use JUnit4, kotlinx-coroutines-test, Room in-memory database for DAO tests, Mockk for repository tests',
        'Cover edge cases: empty results, date boundary conditions, recurring generation with duplicates',
        'Tests should be written to FAIL initially (TDD Red) since implementation doesnt exist yet',
        'Actually write all test files to disk',
        'Return summary'
      ],
      outputFormat: 'JSON with testFiles (array), testCount (number)'
    },
    outputSchema: {
      type: 'object',
      required: ['testFiles', 'testCount'],
      properties: {
        testFiles: { type: 'array', items: { type: 'string' } },
        testCount: { type: 'number' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['tdd', 'red', 'data-layer', 'testing']
}));

export const implementDataLayerTask = defineTask('implement-data-layer', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 3b: Implement Data Layer (TDD Green)',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer specializing in Room database and Kotlin',
      task: `Implement the complete data layer for "${args.appName}" to make the tests pass`,
      context: {
        packageName: args.packageName,
        androidDir: args.androidDir,
        plan: args.plan,
        tests: args.tests
      },
      instructions: [
        'Read the ANDROID_PLAN.md and the existing web app code to understand exact business logic',
        'Read the test files to understand what the implementation needs to satisfy',
        'Implement in ' + args.androidDir + '/app/src/main/java/' + args.packageName.replace(/\\./g, '/') + '/data/:',
        '- entities/TransactionEntity.kt: Room @Entity with all fields (id, type[income/expense], amount, category, description, date, createdAt, recurringId FK)',
        '- entities/RecurringTransactionEntity.kt: Room @Entity (id, type, amount, category, description, frequency[weekly/monthly], dayOfWeek, dayOfMonth, startDate, endDate, isActive, createdAt)',
        '- dao/TransactionDao.kt: @Dao interface with Flow-based queries, CRUD, filters (type, category, dateFrom, dateTo), monthly aggregation query',
        '- dao/RecurringTransactionDao.kt: @Dao interface with CRUD',
        '- db/BudgetDatabase.kt: RoomDatabase with both entities, TypeConverters for dates',
        '- repository/TransactionRepository.kt: Interface + implementation wrapping DAO with coroutine dispatchers',
        '- repository/RecurringRepository.kt: Interface + implementation including generate occurrences logic (ported from Python)',
        '- repository/SummaryRepository.kt: Monthly summary calculation (total income, expenses, net balance, category breakdowns)',
        '- di/DatabaseModule.kt: Hilt @Module providing database, DAOs, repositories',
        'Match the exact business logic from the existing Python app (read /home/ubuntu/projects/budget_manager/app/routers/ for reference)',
        'Actually write all files to disk',
        'Return summary'
      ],
      outputFormat: 'JSON with filesCreated (array), entities (array), daos (array), repositories (array)'
    },
    outputSchema: {
      type: 'object',
      required: ['filesCreated'],
      properties: {
        filesCreated: { type: 'array', items: { type: 'string' } },
        entities: { type: 'array', items: { type: 'string' } },
        daos: { type: 'array', items: { type: 'string' } },
        repositories: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['tdd', 'green', 'data-layer', 'implementation']
}));

export const buildAndTestTask = defineTask('build-and-test', (args, taskCtx) => ({
  kind: 'agent',
  title: `Build & Test (${args.layer}, iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android build engineer and test runner',
      task: `Build and run tests for the ${args.layer} layer of the Android project`,
      context: {
        androidDir: args.androidDir,
        layer: args.layer,
        iteration: args.iteration
      },
      instructions: [
        'cd ' + args.androidDir,
        'Set ANDROID_HOME=/home/ubuntu/android-sdk',
        'Run: ANDROID_HOME=/home/ubuntu/android-sdk ./gradlew test --stacktrace 2>&1',
        'If the build itself fails, capture and report the build errors',
        'If tests run, parse results from build output and XML test reports',
        'Report total tests, passed, failed, errors',
        'Provide specific feedback on what needs fixing if tests fail',
        'Return structured results'
      ],
      outputFormat: 'JSON with allPassing (boolean), totalTests (number), passed (number), failed (number), errors (array), feedback (string), rawOutput (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['allPassing'],
      properties: {
        allPassing: { type: 'boolean' },
        totalTests: { type: 'number' },
        passed: { type: 'number' },
        failed: { type: 'number' },
        errors: { type: 'array', items: { type: 'string' } },
        feedback: { type: 'string' },
        rawOutput: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['build', 'test', args.layer, `iteration-${args.iteration}`]
}));

export const fixFailuresTask = defineTask('fix-failures', (args, taskCtx) => ({
  kind: 'agent',
  title: `Fix ${args.layer} failures (iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android debugger and developer',
      task: `Fix build/test failures in the ${args.layer} layer - iteration ${args.iteration}`,
      context: {
        androidDir: args.androidDir,
        layer: args.layer,
        buildResult: args.buildResult,
        iteration: args.iteration
      },
      instructions: [
        'Review the build/test failure output carefully',
        'Read the failing source and test files',
        'Determine root cause - could be: missing imports, wrong Gradle config, API misuse, logic errors, missing files',
        'Fix the root cause in the source code (prefer fixing app code over weakening tests)',
        'Make targeted fixes - do not rewrite entire files unless necessary',
        'After fixing, do a quick sanity build: cd ' + args.androidDir + ' && ANDROID_HOME=/home/ubuntu/android-sdk ./gradlew compileDebugKotlin --stacktrace 2>&1 | tail -30',
        'Return summary of fixes'
      ],
      outputFormat: 'JSON with filesFixed (array), fixesSummary (string), rootCauses (array)'
    },
    outputSchema: {
      type: 'object',
      required: ['filesFixed', 'fixesSummary'],
      properties: {
        filesFixed: { type: 'array', items: { type: 'string' } },
        fixesSummary: { type: 'string' },
        rootCauses: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['fix', args.layer, `iteration-${args.iteration}`]
}));

export const implementAuthTask = defineTask('implement-auth', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 4: Implement Google Sign-In',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer specializing in Google Sign-In and Firebase Auth',
      task: `Implement Google Sign-In authentication for "${args.appName}"`,
      context: {
        packageName: args.packageName,
        androidDir: args.androidDir,
        plan: args.plan
      },
      instructions: [
        'Read ANDROID_PLAN.md for auth architecture',
        'Implement in ' + args.androidDir + '/app/src/main/java/' + args.packageName.replace(/\\./g, '/') + '/auth/:',
        '- GoogleSignInManager.kt: Wrapper around Google Identity Services (Credential Manager API)',
        '- AuthViewModel.kt: ViewModel managing auth state (signedIn, signedOut, loading)',
        '- AuthState.kt: Sealed class for auth states',
        '- SignInScreen.kt: Compose screen with Google Sign-In button, app logo, welcome text',
        'Add google-services.json placeholder (note to user: they need to configure their own Firebase project)',
        'For now, create a mock/demo mode that allows sign-in without actual Google credentials (for APK testing)',
        'The sign-in screen should be the first screen shown, with option to "Continue as Guest" for testing',
        'After sign-in, navigate to main app',
        'Store user profile (name, email, photo URL) in DataStore',
        'Show user info in Settings screen',
        'Actually write all files to disk',
        'Return summary'
      ],
      outputFormat: 'JSON with filesCreated (array), authFlow (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['filesCreated'],
      properties: {
        filesCreated: { type: 'array', items: { type: 'string' } },
        authFlow: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['auth', 'google-signin']
}));

export const implementThemeTask = defineTask('implement-theme', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 5a: Material Design 3 Theme + Base Composables',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android UI/UX developer specializing in Material Design 3 and Jetpack Compose',
      task: `Create a beautiful Material Design 3 theme and reusable composables for "${args.appName}"`,
      context: {
        packageName: args.packageName,
        androidDir: args.androidDir,
        plan: args.plan
      },
      instructions: [
        'Read ANDROID_PLAN.md for UI architecture',
        'Implement in ' + args.androidDir + '/app/src/main/java/' + args.packageName.replace(/\\./g, '/') + '/ui/theme/:',
        '- Color.kt: Budget-themed color scheme (blues/greens for income, reds for expenses, professional look)',
        '- Type.kt: Typography scale with Google Fonts (e.g., Inter or Roboto)',
        '- Shape.kt: Rounded shapes for cards and buttons',
        '- Theme.kt: MaterialTheme composable with light/dark support and dynamic colors (Android 12+)',
        'Implement in .../ui/components/:',
        '- AmountText.kt: Formatted ₪ amount display (green for income, red for expense)',
        '- TransactionCard.kt: Card showing transaction details with category icon, amount, date',
        '- CategoryChip.kt: Chip for category selection/display',
        '- EmptyStateView.kt: Illustrated empty state for no transactions',
        '- LoadingView.kt: Centered loading indicator',
        '- DatePickerButton.kt: Date picker dialog trigger',
        '- SummaryCard.kt: Card for monthly summary stats (income, expense, balance)',
        '- FilterBar.kt: Horizontal scrollable filter chips',
        'Make the design polished and professional - use proper spacing, elevation, animations',
        'Actually write all files to disk',
        'Return summary'
      ],
      outputFormat: 'JSON with filesCreated (array), composables (array)'
    },
    outputSchema: {
      type: 'object',
      required: ['filesCreated', 'composables'],
      properties: {
        filesCreated: { type: 'array', items: { type: 'string' } },
        composables: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['ui', 'theme', 'material3', 'composables']
}));

export const implementScreensTask = defineTask('implement-screens', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 5b: Screen Composables + ViewModels',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android UI developer with deep Jetpack Compose expertise',
      task: `Build all screen composables and ViewModels for "${args.appName}"`,
      context: {
        packageName: args.packageName,
        androidDir: args.androidDir,
        plan: args.plan,
        features: args.features,
        theme: args.theme,
        dataLayer: args.dataLayer,
        auth: args.auth
      },
      instructions: [
        'Read ANDROID_PLAN.md and all existing code files for context',
        'Read the existing web app UI at /home/ubuntu/projects/budget_manager/app/static/index.html for feature reference',
        'Implement screens in .../ui/screens/:',
        '',
        '1. TransactionListScreen.kt + TransactionListViewModel.kt:',
        '   - LazyColumn of TransactionCards with swipe-to-delete',
        '   - FAB to add new transaction',
        '   - FilterBar at top (type, category, date range)',
        '   - Pull-to-refresh',
        '   - Empty state when no transactions',
        '',
        '2. AddEditTransactionScreen.kt + AddEditTransactionViewModel.kt:',
        '   - Form with: type toggle (income/expense), amount input (₪), category dropdown, description, date picker',
        '   - Validation (amount > 0, category required, valid date)',
        '   - Save/cancel actions',
        '   - Edit mode: pre-fill form from existing transaction',
        '',
        '3. MonthlySummaryScreen.kt + MonthlySummaryViewModel.kt:',
        '   - Month/year selector (arrows to navigate months)',
        '   - Summary cards: total income (₪), total expenses (₪), net balance (₪)',
        '   - Category breakdown list with amounts and counts',
        '   - Visual indicators (green/red for income/expense)',
        '',
        '4. RecurringScreen.kt + RecurringViewModel.kt:',
        '   - List of recurring transaction definitions',
        '   - Add/edit recurring transaction form (type, amount, category, frequency, day, start/end date)',
        '   - Toggle active/inactive',
        '   - Generate button to create transactions from recurring for a date range',
        '',
        '5. SettingsScreen.kt + SettingsViewModel.kt:',
        '   - User profile info (from Google Sign-In)',
        '   - Sign out button',
        '   - Export data (JSON)',
        '   - Import data (from JSON file)',
        '   - Backup/restore',
        '   - About section',
        '',
        'Use Hilt @HiltViewModel for all ViewModels',
        'Use StateFlow for UI state',
        'Use Material Design 3 components throughout',
        'Make UX polished: proper animations, loading states, error handling, haptic feedback',
        'Actually write ALL files to disk - this is critical',
        'Return summary'
      ],
      outputFormat: 'JSON with filesCreated (array), screens (array), viewModels (array)'
    },
    outputSchema: {
      type: 'object',
      required: ['filesCreated', 'screens'],
      properties: {
        filesCreated: { type: 'array', items: { type: 'string' } },
        screens: { type: 'array', items: { type: 'string' } },
        viewModels: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['ui', 'screens', 'viewmodels', 'compose']
}));

export const implementNavigationTask = defineTask('implement-navigation', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 5c: Navigation + MainActivity Integration',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android navigation and integration specialist',
      task: `Implement navigation and wire everything together in "${args.appName}"`,
      context: {
        packageName: args.packageName,
        androidDir: args.androidDir,
        plan: args.plan,
        screens: args.screens,
        auth: args.auth
      },
      instructions: [
        'Read all existing code files in the android project',
        'Implement in .../ui/navigation/:',
        '- Screen.kt: Sealed class defining all navigation destinations',
        '- NavGraph.kt: NavHost composable with all screen destinations',
        '- BottomNavBar.kt: Material 3 NavigationBar with items (Transactions, Summary, Recurring, Settings)',
        '',
        'Implement/update:',
        '- MainActivity.kt: @AndroidEntryPoint, setContent with theme, NavGraph, handle auth state (show sign-in or main app)',
        '- BudgetManagerApp.kt: @HiltAndroidApp Application class',
        '- MainScreen.kt: Scaffold with BottomNavBar + NavHost content area',
        '',
        'Navigation flow:',
        '1. App launch → check auth state',
        '2. Not signed in → SignInScreen (with "Continue as Guest" option)',
        '3. Signed in → MainScreen with BottomNavBar',
        '4. Bottom tabs: Transactions (default), Summary, Recurring, Settings',
        '5. Transactions → FAB → AddEditTransaction (full screen)',
        '6. Transaction item click → AddEditTransaction (edit mode)',
        '',
        'Make sure all imports resolve and the app compiles:',
        'Run: cd ' + args.androidDir + ' && ANDROID_HOME=/home/ubuntu/android-sdk ./gradlew compileDebugKotlin --stacktrace 2>&1 | tail -50',
        'Fix any compilation errors',
        'Actually write all files to disk',
        'Return summary'
      ],
      outputFormat: 'JSON with filesCreated (array), navigationRoutes (array), compiles (boolean)'
    },
    outputSchema: {
      type: 'object',
      required: ['filesCreated'],
      properties: {
        filesCreated: { type: 'array', items: { type: 'string' } },
        navigationRoutes: { type: 'array', items: { type: 'string' } },
        compiles: { type: 'boolean' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['navigation', 'integration', 'mainactivity']
}));

export const buildApkTask = defineTask('build-apk', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 7: Build Debug APK',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android build engineer',
      task: `Build a debug APK for "${args.appName}"`,
      context: {
        androidDir: args.androidDir
      },
      instructions: [
        'cd ' + args.androidDir,
        'Run: ANDROID_HOME=/home/ubuntu/android-sdk ./gradlew assembleDebug --stacktrace 2>&1',
        'If build fails, analyze errors, fix them, and retry (up to 5 attempts)',
        'Common fixes: missing imports, wrong dependency versions, Compose compiler issues',
        'After successful build, find the APK: find ' + args.androidDir + ' -name "*.apk" -type f',
        'Report the APK file path and size',
        'Return results'
      ],
      outputFormat: 'JSON with apkBuilt (boolean), apkPath (string), apkSizeBytes (number), buildAttempts (number)'
    },
    outputSchema: {
      type: 'object',
      required: ['apkBuilt', 'apkPath'],
      properties: {
        apkBuilt: { type: 'boolean' },
        apkPath: { type: 'string' },
        apkSizeBytes: { type: 'number' },
        buildAttempts: { type: 'number' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['build', 'apk', 'delivery']
}));

export const deliverToGithubTask = defineTask('deliver-to-github', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 9: Push to GitHub & Create Release',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'DevOps engineer',
      task: `Push the Android app to GitHub and create a release with the APK`,
      context: {
        projectDir: args.projectDir,
        androidDir: args.androidDir,
        githubRepo: args.githubRepo,
        apkPath: args.apkPath,
        appName: args.appName
      },
      instructions: [
        'cd ' + args.projectDir,
        'Stage the android/ directory and any new files: git add android/ ANDROID_PLAN.md',
        'Commit: git commit -m "feat: add native Android app with Jetpack Compose, Material Design 3, Room DB, and Google Sign-In"',
        'Check if remote origin exists: git remote -v',
        'If no remote, add it: git remote add origin https://github.com/' + args.githubRepo + '.git',
        'Push to GitHub: git push -u origin master (or main, whichever branch exists)',
        'Create a GitHub release with the APK attached:',
        'gh release create v1.0.0-android-beta --title "Budget Manager Android v1.0.0 Beta" --notes "First Android beta release. Features: transaction management, monthly summaries, recurring transactions, backup/restore, Google Sign-In." ' + args.apkPath,
        'If gh is not available, provide the manual steps',
        'Return the release URL'
      ],
      outputFormat: 'JSON with committed (boolean), pushed (boolean), releaseCreated (boolean), releaseUrl (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['committed'],
      properties: {
        committed: { type: 'boolean' },
        pushed: { type: 'boolean' },
        releaseCreated: { type: 'boolean' },
        releaseUrl: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['delivery', 'github', 'release']
}));
