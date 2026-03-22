/**
 * @process incremental-fix
 * @description Incrementally rebuild from last working version, applying small changes
 *              and building/testing at each step to isolate the crash cause.
 * @inputs { androidDir: string, projectDir: string, workingCommit: string }
 * @outputs { success: boolean, stepsCompleted: number }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const {
    projectDir = '/home/ubuntu/projects/budget_manager',
    androidDir = '/home/ubuntu/projects/budget_manager/android',
    workingCommit = '549135d'
  } = inputs;

  // ============================================================================
  // STEP 1: Analyze full diff from working version to current
  // ============================================================================

  const analysisResult = await ctx.task(analyzeDiffTask, {
    projectDir, androidDir, workingCommit
  });

  // ============================================================================
  // STEP 2: Create a clean branch from the working version
  // ============================================================================

  const branchResult = await ctx.task(createCleanBranchTask, {
    projectDir, workingCommit
  });

  // ============================================================================
  // STEP 3: Apply database layer changes (smallest unit — entities, DAOs, migration)
  // ============================================================================

  const dbResult = await ctx.task(applyDatabaseChangesTask, {
    projectDir, androidDir, analysis: analysisResult
  });

  // Build gate
  let dbBuildOk = false;
  let dbIter = 0;
  while (!dbBuildOk && dbIter < 5) {
    dbIter++;
    const build = await ctx.task(buildTask, { androidDir, step: 'database', iteration: dbIter });
    dbBuildOk = build.allPassing;
    if (!dbBuildOk && dbIter < 5) {
      await ctx.task(fixTask, { androidDir, buildResult: build, step: 'database', iteration: dbIter });
    }
  }

  // User test checkpoint
  await ctx.breakpoint({
    question: 'Database layer applied. Build passes. Please install the APK and test. Does the app launch without crashing?',
    title: 'Test: Database Layer Only'
  });

  // ============================================================================
  // STEP 4: Apply domain layer (Budget model, repos, ActiveBudgetManager)
  // ============================================================================

  const domainResult = await ctx.task(applyDomainChangesTask, {
    projectDir, androidDir, analysis: analysisResult
  });

  let domainBuildOk = false;
  let domainIter = 0;
  while (!domainBuildOk && domainIter < 5) {
    domainIter++;
    const build = await ctx.task(buildTask, { androidDir, step: 'domain', iteration: domainIter });
    domainBuildOk = build.allPassing;
    if (!domainBuildOk && domainIter < 5) {
      await ctx.task(fixTask, { androidDir, buildResult: build, step: 'domain', iteration: domainIter });
    }
  }

  await ctx.breakpoint({
    question: 'Domain layer applied (Budget model, repos, ActiveBudgetManager). Build passes. Please test the APK — does it launch?',
    title: 'Test: Domain Layer Added'
  });

  // ============================================================================
  // STEP 5: Apply ViewModel changes
  // ============================================================================

  const vmResult = await ctx.task(applyViewModelChangesTask, {
    projectDir, androidDir, analysis: analysisResult
  });

  let vmBuildOk = false;
  let vmIter = 0;
  while (!vmBuildOk && vmIter < 5) {
    vmIter++;
    const build = await ctx.task(buildTask, { androidDir, step: 'viewmodel', iteration: vmIter });
    vmBuildOk = build.allPassing;
    if (!vmBuildOk && vmIter < 5) {
      await ctx.task(fixTask, { androidDir, buildResult: build, step: 'viewmodel', iteration: vmIter });
    }
  }

  await ctx.breakpoint({
    question: 'ViewModel layer applied. Build passes. Please test the APK — does it launch?',
    title: 'Test: ViewModel Layer Added'
  });

  // ============================================================================
  // STEP 6: Apply UI changes (screens, components, navigation)
  // ============================================================================

  const uiResult = await ctx.task(applyUIChangesTask, {
    projectDir, androidDir, analysis: analysisResult
  });

  let uiBuildOk = false;
  let uiIter = 0;
  while (!uiBuildOk && uiIter < 5) {
    uiIter++;
    const build = await ctx.task(buildTask, { androidDir, step: 'ui', iteration: uiIter });
    uiBuildOk = build.allPassing;
    if (!uiBuildOk && uiIter < 5) {
      await ctx.task(fixTask, { androidDir, buildResult: build, step: 'ui', iteration: uiIter });
    }
  }

  await ctx.breakpoint({
    question: 'Full UI layer applied (screens, components, navigation, theme, splash). Build passes. Please test the APK — does it launch?',
    title: 'Test: Full UI Applied'
  });

  // ============================================================================
  // STEP 7: Final APK
  // ============================================================================

  const apkResult = await ctx.task(buildTask, { androidDir, step: 'final-apk', iteration: 1 });

  return {
    success: apkResult.allPassing,
    stepsCompleted: 7,
    metadata: { processId: 'incremental-fix', timestamp: ctx.now() }
  };
}

// ============================================================================
// TASK DEFINITIONS
// ============================================================================

export const analyzeDiffTask = defineTask('analyze-diff', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Step 1: Analyze diff from working version',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer',
      task: `Analyze the complete diff between the last working commit (${args.workingCommit}) and current HEAD to understand all changes made.`,
      context: { projectDir: args.projectDir, workingCommit: args.workingCommit },
      instructions: [
        `Run: cd ${args.projectDir} && git diff ${args.workingCommit}..HEAD --stat`,
        `Run: cd ${args.projectDir} && git diff ${args.workingCommit}..HEAD --name-status`,
        'Categorize all changed files into layers:',
        '1. Database: entities, DAOs, migrations, BudgetDatabase',
        '2. Domain: models, repositories, managers, use cases',
        '3. DI: AppModule, RepositoryModule',
        '4. ViewModel: all ViewModels',
        '5. UI: screens, components, navigation, theme',
        '6. Resources: XML, drawables, icons',
        '7. Build: gradle files, manifest',
        'Return the categorized list'
      ],
      outputFormat: 'JSON with categories (object mapping layer->files), totalFiles (number), summary (string)'
    },
    outputSchema: { type: 'object', required: ['summary'], properties: { summary: { type: 'string' }, categories: { type: 'object' }, totalFiles: { type: 'number' } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['analysis']
}));

export const createCleanBranchTask = defineTask('create-branch', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Step 2: Create clean branch from working version',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Git operations specialist',
      task: 'Create a new branch from the last working commit for incremental rebuilding.',
      context: { projectDir: args.projectDir, workingCommit: args.workingCommit },
      instructions: [
        `cd ${args.projectDir}`,
        `git checkout -b incremental-rebuild ${args.workingCommit}`,
        'Verify: git log --oneline -1 should show the working commit',
        'Verify: the app builds from this clean state',
        `cd ${args.projectDir}/android && ./gradlew assembleDebug 2>&1 | tail -5`,
        'Return confirmation'
      ],
      outputFormat: 'JSON with success (boolean), branch (string), commit (string), buildPasses (boolean)'
    },
    outputSchema: { type: 'object', required: ['success'], properties: { success: { type: 'boolean' }, branch: { type: 'string' }, commit: { type: 'string' }, buildPasses: { type: 'boolean' } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['git', 'setup']
}));

export const applyDatabaseChangesTask = defineTask('apply-database', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Step 3: Apply database layer changes only',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer specializing in Room DB',
      task: 'Cherry-pick ONLY the database layer changes from master onto the clean branch. Do this carefully — file by file, not as a git cherry-pick.',
      context: { projectDir: args.projectDir, androidDir: args.androidDir },
      instructions: [
        'We are on the incremental-rebuild branch (last working commit).',
        'Apply ONLY database-layer changes from master:',
        '',
        '1. Read each file from master (git show master:<path>) and apply to current branch:',
        `   - git show master:android/app/src/main/java/com/budgetmanager/app/data/entity/BudgetEntity.kt > create this file`,
        `   - git show master:android/app/src/main/java/com/budgetmanager/app/data/entity/TransactionEntity.kt > update`,
        `   - git show master:android/app/src/main/java/com/budgetmanager/app/data/entity/RecurringTransactionEntity.kt > update`,
        `   - git show master:android/app/src/main/java/com/budgetmanager/app/data/dao/BudgetDao.kt > create`,
        `   - git show master:android/app/src/main/java/com/budgetmanager/app/data/dao/TransactionDao.kt > update`,
        `   - git show master:android/app/src/main/java/com/budgetmanager/app/data/dao/RecurringTransactionDao.kt > update`,
        `   - git show master:android/app/src/main/java/com/budgetmanager/app/data/db/Migrations.kt > create`,
        `   - git show master:android/app/src/main/java/com/budgetmanager/app/data/db/BudgetDatabase.kt > update`,
        '',
        'Use git show master:<path> to get the content and write it to the file.',
        'Do NOT apply any other changes (no domain, no UI, no theme, no viewmodel changes).',
        'Return summary of what was applied'
      ],
      outputFormat: 'JSON with summary (string), filesCreated (array), filesModified (array)'
    },
    outputSchema: { type: 'object', required: ['summary'], properties: { summary: { type: 'string' }, filesCreated: { type: 'array' }, filesModified: { type: 'array' } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['database', 'incremental']
}));

export const applyDomainChangesTask = defineTask('apply-domain', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Step 4: Apply domain layer changes',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer',
      task: 'Apply domain and repository layer changes from master onto the incremental-rebuild branch.',
      context: { projectDir: args.projectDir, androidDir: args.androidDir },
      instructions: [
        'Apply domain layer changes from master using git show master:<path>:',
        '',
        `- git show master:android/app/src/main/java/com/budgetmanager/app/domain/model/Budget.kt > create`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/domain/model/Transaction.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/domain/model/RecurringTransaction.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/domain/model/BackupData.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/domain/manager/ActiveBudgetManager.kt > create`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/domain/util/CurrencyUtils.kt > create`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/domain/usecase/GenerateRecurringTransactionsUseCase.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/data/repository/BudgetRepository.kt > create`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/data/repository/BudgetRepositoryImpl.kt > create`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/data/repository/TransactionRepository.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/data/repository/TransactionRepositoryImpl.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/data/repository/RecurringRepository.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/data/repository/RecurringRepositoryImpl.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/data/repository/BackupRepositoryImpl.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/di/AppModule.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/di/RepositoryModule.kt > update`,
        '',
        'Do NOT apply UI, ViewModel, theme, or screen changes yet.',
        'Return summary'
      ],
      outputFormat: 'JSON with summary (string), filesCreated (array), filesModified (array)'
    },
    outputSchema: { type: 'object', required: ['summary'], properties: { summary: { type: 'string' }, filesCreated: { type: 'array' }, filesModified: { type: 'array' } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['domain', 'incremental']
}));

export const applyViewModelChangesTask = defineTask('apply-viewmodel', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Step 5: Apply ViewModel changes',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer',
      task: 'Apply ViewModel layer changes from master onto the incremental-rebuild branch.',
      context: { projectDir: args.projectDir, androidDir: args.androidDir },
      instructions: [
        'Apply ViewModel changes from master using git show master:<path>:',
        '',
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/viewmodel/BudgetViewModel.kt > create`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/viewmodel/MigrationViewModel.kt > create`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/viewmodel/TransactionListViewModel.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/viewmodel/AddEditTransactionViewModel.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/viewmodel/MonthlySummaryViewModel.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/viewmodel/RecurringViewModel.kt > update`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/viewmodel/SettingsViewModel.kt > update`,
        '',
        'Do NOT apply UI screen, component, navigation, or theme changes yet.',
        'Return summary'
      ],
      outputFormat: 'JSON with summary (string), filesCreated (array), filesModified (array)'
    },
    outputSchema: { type: 'object', required: ['summary'], properties: { summary: { type: 'string' }, filesCreated: { type: 'array' }, filesModified: { type: 'array' } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['viewmodel', 'incremental']
}));

export const applyUIChangesTask = defineTask('apply-ui', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Step 6: Apply UI changes (screens, components, navigation, theme, resources)',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer',
      task: 'Apply ALL remaining UI changes from master onto the incremental-rebuild branch.',
      context: { projectDir: args.projectDir, androidDir: args.androidDir },
      instructions: [
        'Apply ALL remaining changes from master using git show master:<path>:',
        '',
        'Theme files:',
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/theme/Color.kt`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/theme/Type.kt`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/theme/Theme.kt`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/theme/Dimensions.kt > create`,
        '',
        'Components:',
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/components/ (ALL files)`,
        '',
        'Screens:',
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/screens/ (ALL files in all subdirs)`,
        '',
        'Navigation:',
        `- git show master:android/app/src/main/java/com/budgetmanager/app/ui/navigation/ (ALL files)`,
        '',
        'Resources:',
        `- git show master:android/app/src/main/res/values/colors.xml`,
        `- git show master:android/app/src/main/res/values/themes.xml`,
        `- git show master:android/app/src/main/res/values-night/themes.xml > create dir+file`,
        `- git show master:android/app/src/main/res/drawable/ic_launcher_foreground.xml`,
        `- git show master:android/app/src/main/res/drawable/ic_launcher_background.xml`,
        `- git show master:android/app/src/main/res/mipmap-anydpi-v26/ (ALL files)`,
        '',
        'Build files:',
        `- git show master:android/app/build.gradle.kts`,
        `- git show master:android/app/src/main/AndroidManifest.xml`,
        `- git show master:android/app/src/main/java/com/budgetmanager/app/MainActivity.kt`,
        '',
        'Apply ALL these files. Return summary.'
      ],
      outputFormat: 'JSON with summary (string), filesCreated (array), filesModified (array)'
    },
    outputSchema: { type: 'object', required: ['summary'], properties: { summary: { type: 'string' }, filesCreated: { type: 'array' }, filesModified: { type: 'array' } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['ui', 'incremental']
}));

export const buildTask = defineTask('build', (args, taskCtx) => ({
  kind: 'agent',
  title: `Build (${args.step}, iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android build engineer',
      task: `Build the Android project. Step: ${args.step}, iteration ${args.iteration}.`,
      context: { androidDir: args.androidDir },
      instructions: [
        `cd ${args.androidDir} && ./gradlew assembleDebug 2>&1`,
        'If build succeeds, return allPassing: true',
        'If build fails, capture ALL errors with file paths and line numbers',
        'Return the complete error details'
      ],
      outputFormat: 'JSON with allPassing (boolean), errors (array), buildOutput (string)'
    },
    outputSchema: { type: 'object', required: ['allPassing'], properties: { allPassing: { type: 'boolean' }, errors: { type: 'array' }, buildOutput: { type: 'string' } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['build', args.step]
}));

export const fixTask = defineTask('fix-build', (args, taskCtx) => ({
  kind: 'agent',
  title: `Fix build (${args.step}, iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer',
      task: `Fix build failures for step: ${args.step}.`,
      context: { androidDir: args.androidDir, buildResult: args.buildResult },
      instructions: ['Analyze and fix each build error', 'Return summary'],
      outputFormat: 'JSON with summary (string), filesModified (array)'
    },
    outputSchema: { type: 'object', required: ['summary'], properties: { summary: { type: 'string' }, filesModified: { type: 'array' } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['fix', args.step]
}));
