/**
 * @process fix-crash-on-launch
 * @description Diagnose and fix crash-on-launch bug in the Android budget manager app.
 * @inputs { androidDir: string, maxConvergenceIterations: number }
 * @outputs { success: boolean, issuesFound: array, issuesFixed: array }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const {
    androidDir = '/home/ubuntu/projects/budget_manager/android',
    maxConvergenceIterations = 5
  } = inputs;

  // ============================================================================
  // PHASE 1: DIAGNOSE — Deep audit of all crash-on-launch vectors
  // ============================================================================

  const diagnosisResult = await ctx.task(diagnoseAllCrashCausesTask, { androidDir });

  // ============================================================================
  // PHASE 2: FIX — Apply all fixes
  // ============================================================================

  const fixResult = await ctx.task(applyAllFixesTask, {
    androidDir, diagnosis: diagnosisResult
  });

  // ============================================================================
  // PHASE 3: BUILD CONVERGENCE — Ensure it compiles
  // ============================================================================

  let converged = false;
  let iteration = 0;
  while (!converged && iteration < maxConvergenceIterations) {
    iteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'crash-fix', iteration
    });
    converged = buildResult.allPassing === true;
    if (!converged && iteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, buildResult, iteration
      });
    }
  }

  // ============================================================================
  // PHASE 4: SECOND PASS — Look for any remaining runtime crash risks
  // ============================================================================

  const secondPassResult = await ctx.task(secondPassAuditTask, {
    androidDir, previousFixes: fixResult
  });

  if (secondPassResult.hasMoreIssues) {
    await ctx.task(applySecondPassFixesTask, {
      androidDir, issues: secondPassResult
    });

    // Build again
    let converged2 = false;
    let iteration2 = 0;
    while (!converged2 && iteration2 < maxConvergenceIterations) {
      iteration2++;
      const buildResult = await ctx.task(buildAndVerifyTask, {
        androidDir, scope: 'second-pass-fix', iteration: iteration2
      });
      converged2 = buildResult.allPassing === true;
      if (!converged2 && iteration2 < maxConvergenceIterations) {
        await ctx.task(fixBuildFailuresTask, {
          androidDir, buildResult, iteration: iteration2
        });
      }
    }
  }

  // ============================================================================
  // PHASE 5: BUILD FINAL APK
  // ============================================================================

  const apkResult = await ctx.task(buildApkTask, { androidDir });

  return {
    success: apkResult.apkBuilt === true,
    issuesFound: diagnosisResult.issues || [],
    issuesFixed: fixResult.fixes || [],
    metadata: { processId: 'fix-crash-on-launch', timestamp: ctx.now() }
  };
}

// ============================================================================
// TASK DEFINITIONS
// ============================================================================

export const diagnoseAllCrashCausesTask = defineTask('diagnose-crash', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 1: Diagnose all crash-on-launch causes',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android debugger specializing in crash diagnosis, Hilt DI, Room DB, and Jetpack Compose',
      task: 'Thoroughly audit the Android budget manager app for ALL possible crash-on-launch causes.',
      context: { androidDir: args.androidDir },
      instructions: [
        `Read these files carefully and check for crash-causing issues:`,
        '',
        `1. ${args.androidDir}/app/src/main/java/com/budgetmanager/app/MainActivity.kt`,
        `   - Is installSplashScreen() called BEFORE super.onCreate()?`,
        `   - Is setContent inside onCreate?`,
        `   - Is @AndroidEntryPoint present?`,
        '',
        `2. ${args.androidDir}/app/src/main/java/com/budgetmanager/app/domain/manager/ActiveBudgetManager.kt`,
        `   - Does init use runBlocking? This WILL crash on main thread during Hilt singleton creation`,
        `   - Check DataStore initialization`,
        '',
        `3. ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/theme/Theme.kt`,
        `   - Are CompositionLocal defaults properly set?`,
        `   - Does BudgetManagerTheme provide LocalFinanceColors?`,
        '',
        `4. ${args.androidDir}/app/src/main/java/com/budgetmanager/app/di/AppModule.kt`,
        `   - Is MIGRATION_1_2 added to Room builder?`,
        `   - Are all DAOs provided?`,
        `   - Is ActiveBudgetManager injectable?`,
        '',
        `5. ${args.androidDir}/app/src/main/java/com/budgetmanager/app/data/db/Migrations.kt`,
        `   - Check migration SQL for syntax errors`,
        `   - Check if FK constraints match entity definitions`,
        '',
        `6. ${args.androidDir}/app/src/main/java/com/budgetmanager/app/data/db/BudgetDatabase.kt`,
        `   - Version matches migration?`,
        `   - All entities listed?`,
        '',
        `7. ${args.androidDir}/app/src/main/java/com/budgetmanager/app/data/entity/TransactionEntity.kt`,
        `   - Check FK definitions match what migration creates`,
        `   - Check if budget_id column default matches migration`,
        '',
        `8. ${args.androidDir}/app/src/main/java/com/budgetmanager/app/data/entity/RecurringTransactionEntity.kt`,
        `   - Same FK and column checks`,
        '',
        `9. ${args.androidDir}/app/src/main/java/com/budgetmanager/app/data/entity/BudgetEntity.kt`,
        `   - Check Room annotations match migration SQL`,
        '',
        `10. ${args.androidDir}/app/src/main/res/values/themes.xml`,
        `    - Does Theme.BudgetManager.Splash parent exist?`,
        `    - Is postSplashScreenTheme valid?`,
        '',
        `11. ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/navigation/BudgetNavHost.kt`,
        `    - Any initialization code that could crash?`,
        `    - ViewModel injection issues?`,
        '',
        `12. ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/viewmodel/MigrationViewModel.kt`,
        `    - Does init block crash if DB is empty?`,
        '',
        `13. ${args.androidDir}/app/src/main/java/com/budgetmanager/app/BudgetManagerApp.kt`,
        `    - WorkManager config issues?`,
        '',
        `14. Check Room schema validation: entities define FK constraints that ALTER TABLE cannot add`,
        `    - Room validates schema at open. If entities declare ForeignKey on budget_id but migration used ALTER TABLE (which cant add FKs in SQLite), Room will throw IllegalStateException for schema mismatch.`,
        `    - This is a CRITICAL check.`,
        '',
        'List ALL issues found with severity, file, line, and exact description.',
        'Return the complete list'
      ],
      outputFormat: 'JSON with issues (array of {severity, file, line, description, fix}), summary (string)'
    },
    outputSchema: {
      type: 'object', required: ['issues', 'summary'],
      properties: {
        issues: { type: 'array', items: { type: 'object' } },
        summary: { type: 'string' }
      }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['diagnosis', 'crash']
}));

export const applyAllFixesTask = defineTask('apply-fixes', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 2: Apply all crash fixes',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer specializing in crash fixes, Room DB migrations, and Hilt DI',
      task: 'Apply ALL fixes for the crash-on-launch issues identified in the diagnosis.',
      context: { androidDir: args.androidDir, diagnosis: args.diagnosis },
      instructions: [
        'Fix EVERY issue identified in the diagnosis. Read the actual files before making changes.',
        '',
        'KNOWN CRITICAL ISSUES TO FIX:',
        '',
        '1. ActiveBudgetManager.kt - runBlocking on main thread:',
        '   - Replace runBlocking init with async scope.launch pattern',
        '   - Initialize _activeBudgetId with MutableStateFlow(0L)',
        '   - Read from DataStore in scope.launch and update the flow',
        '   - getActiveBudgetId() should return _activeBudgetId.value',
        '',
        '2. Room schema mismatch - FK constraints on budget_id:',
        '   This is the MOST CRITICAL issue. Room validates schema at open time.',
        '   Entities declare ForeignKey on budget_id columns, but ALTER TABLE cannot add FK constraints in SQLite.',
        '   Room compares the expected schema (from @Entity annotations with FK) against actual schema (from migration SQL).',
        '   If they dont match, Room throws: "Expected: foreignKeys=[ForeignKey{...}], Found: foreignKeys=[]"',
        '',
        '   FIX: The migration must recreate tables with proper FK constraints instead of ALTER TABLE ADD COLUMN.',
        '   Update Migrations.kt MIGRATION_1_2 to:',
        '   a) Create budgets table (already correct)',
        '   b) For transactions table: CREATE new temp table with full schema including FK on budget_id,',
        '      COPY data from old table, DROP old table, RENAME temp to transactions, recreate indices',
        '   c) Same for recurring_transactions table',
        '   d) Backfill budget_id values',
        '',
        '   The new table schemas must EXACTLY match what Room expects from the Entity annotations.',
        `   Read the entity files to get the exact column names, types, defaults, and FK definitions:`,
        `   - ${args.androidDir}/app/src/main/java/com/budgetmanager/app/data/entity/TransactionEntity.kt`,
        `   - ${args.androidDir}/app/src/main/java/com/budgetmanager/app/data/entity/RecurringTransactionEntity.kt`,
        `   - ${args.androidDir}/app/src/main/java/com/budgetmanager/app/data/entity/BudgetEntity.kt`,
        '',
        '3. Fix any other issues from the diagnosis.',
        '',
        'After applying ALL fixes, return summary of what was fixed.'
      ],
      outputFormat: 'JSON with fixes (array of {file, issue, fix}), summary (string), filesModified (array)'
    },
    outputSchema: {
      type: 'object', required: ['fixes', 'summary'],
      properties: {
        fixes: { type: 'array', items: { type: 'object' } },
        summary: { type: 'string' },
        filesModified: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['fix', 'crash']
}));

export const secondPassAuditTask = defineTask('second-pass-audit', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 4: Second pass — check for remaining runtime crash risks',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android crash debugger',
      task: 'After the initial fixes, do a second pass to find any remaining crash risks.',
      context: { androidDir: args.androidDir, previousFixes: args.previousFixes },
      instructions: [
        `Read the fixed files to verify the fixes are correct:`,
        `- ${args.androidDir}/app/src/main/java/com/budgetmanager/app/domain/manager/ActiveBudgetManager.kt`,
        `- ${args.androidDir}/app/src/main/java/com/budgetmanager/app/data/db/Migrations.kt`,
        '',
        'Then check for remaining issues:',
        '1. Verify migration SQL creates tables with correct FK constraints',
        '2. Verify migration column names/types/defaults match Entity annotations exactly',
        '3. Verify ActiveBudgetManager no longer uses runBlocking',
        '4. Check if any ViewModel init blocks could crash',
        '5. Check null safety in all ViewModels when budget ID is 0',
        '6. Check if MigrationViewModel.checkMigrationNeeded() could crash on empty DB',
        '',
        'Return hasMoreIssues: true/false and list of any issues'
      ],
      outputFormat: 'JSON with hasMoreIssues (boolean), issues (array), summary (string)'
    },
    outputSchema: {
      type: 'object', required: ['hasMoreIssues'],
      properties: {
        hasMoreIssues: { type: 'boolean' },
        issues: { type: 'array', items: { type: 'object' } },
        summary: { type: 'string' }
      }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['audit', 'second-pass']
}));

export const applySecondPassFixesTask = defineTask('apply-second-fixes', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Apply second pass fixes',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer',
      task: 'Fix remaining crash issues found in second pass.',
      context: { androidDir: args.androidDir, issues: args.issues },
      instructions: ['Read affected files and fix each issue', 'Return summary'],
      outputFormat: 'JSON with summary (string), filesModified (array)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: { summary: { type: 'string' }, filesModified: { type: 'array', items: { type: 'string' } } }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['fix', 'second-pass']
}));

export const buildAndVerifyTask = defineTask('build-verify', (args, taskCtx) => ({
  kind: 'agent',
  title: `Build & verify (${args.scope}, iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android build engineer',
      task: `Build the Android project. Scope: ${args.scope}, iteration ${args.iteration}.`,
      context: { androidDir: args.androidDir },
      instructions: [`cd ${args.androidDir} && ./gradlew assembleDebug 2>&1`, 'Return allPassing and errors'],
      outputFormat: 'JSON with allPassing (boolean), errors (array), buildOutput (string)'
    },
    outputSchema: {
      type: 'object', required: ['allPassing'],
      properties: { allPassing: { type: 'boolean' }, errors: { type: 'array' }, buildOutput: { type: 'string' } }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['build', args.scope]
}));

export const fixBuildFailuresTask = defineTask('fix-build', (args, taskCtx) => ({
  kind: 'agent',
  title: `Fix build failures (iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer',
      task: 'Fix build failures.',
      context: { androidDir: args.androidDir, buildResult: args.buildResult },
      instructions: ['Analyze and fix each build error', 'Return summary'],
      outputFormat: 'JSON with summary (string), filesModified (array)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: { summary: { type: 'string' }, filesModified: { type: 'array' } }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['fix', 'build']
}));

export const buildApkTask = defineTask('build-apk', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Build final APK',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android build engineer',
      task: 'Build debug APK.',
      context: { androidDir: args.androidDir },
      instructions: [`cd ${args.androidDir} && ./gradlew assembleDebug 2>&1`, 'Return APK path and status'],
      outputFormat: 'JSON with apkBuilt (boolean), apkPath (string)'
    },
    outputSchema: {
      type: 'object', required: ['apkBuilt'],
      properties: { apkBuilt: { type: 'boolean' }, apkPath: { type: 'string' } }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['build', 'apk']
}));
