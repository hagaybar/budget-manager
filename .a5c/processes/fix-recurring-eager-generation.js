/**
 * @process fix-recurring-eager-generation
 * @description Fix recurring transactions not appearing on app open by adding eager generation alongside WorkManager
 * @inputs { projectDir: string, androidDir: string, packageName: string, maxConvergenceIterations: number }
 * @outputs { success: boolean, filesModified: array }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const {
    projectDir = '/home/ubuntu/projects/budget_manager',
    androidDir = '/home/ubuntu/projects/budget_manager/android',
    packageName = 'com.budgetmanager.app',
    maxConvergenceIterations = 3
  } = inputs;

  // ============================================================================
  // PHASE 1: ANALYZE & IMPLEMENT THE FIX
  // ============================================================================

  const implementResult = await ctx.task(implementEagerGenerationTask, {
    androidDir, packageName, projectDir
  });

  // ============================================================================
  // PHASE 2: BUILD & FIX CONVERGENCE LOOP
  // ============================================================================

  let buildConverged = false;
  let buildIteration = 0;
  while (!buildConverged && buildIteration < maxConvergenceIterations) {
    buildIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, iteration: buildIteration
    });
    buildConverged = buildResult.buildSuccess === true;
    if (!buildConverged && buildIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildTask, {
        androidDir, packageName, buildResult, iteration: buildIteration
      });
    }
  }

  // ============================================================================
  // PHASE 3: RUN BACKEND TESTS (regression check)
  // ============================================================================

  const testResult = await ctx.task(runBackendTestsTask, {
    projectDir
  });

  // ============================================================================
  // PHASE 4: VERIFY FIX LOGIC (code review)
  // ============================================================================

  const reviewResult = await ctx.task(verifyFixLogicTask, {
    androidDir, packageName
  });

  // ============================================================================
  // PHASE 5: USER REVIEW BREAKPOINT
  // ============================================================================

  await ctx.breakpoint({
    question: `Fix implemented and build succeeds. The eager generation logic runs when the app opens to ensure recurring transactions are always up-to-date. Backend tests: ${testResult.allPassed ? 'ALL PASSED' : 'SOME FAILED'}. Code review: ${reviewResult.approved ? 'APPROVED' : 'CONCERNS FOUND'}. Approve to finalize?`,
    title: 'Recurring Transaction Fix Review'
  });

  return {
    success: buildConverged,
    implementResult,
    buildConverged,
    buildIterations: buildIteration,
    testResult,
    reviewResult
  };
}

// ============================================================================
// TASK DEFINITIONS
// ============================================================================

const implementEagerGenerationTask = defineTask('implement-eager-generation', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Implement eager recurring transaction generation on app open',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer with Kotlin and Jetpack Compose expertise',
      task: `Fix the recurring transactions bug in the Budget Manager Android app.

**ROOT CAUSE:** The app relies solely on WorkManager (PeriodicWorkRequest, ~24h interval) to auto-generate transactions from recurring definitions. WorkManager doesn't guarantee timely execution—it can be delayed by Doze mode, battery optimization, and OEM restrictions. When the user opens the app, if WorkManager hasn't fired yet today, they won't see today's recurring transactions.

**THE FIX:** Add eager/synchronous recurring transaction generation that runs when the app opens, so transactions are always up-to-date when the user views them. Keep WorkManager as a background fallback.

Working directory: ${args.androidDir}
Package: ${args.packageName}

**Implementation steps:**

1. **Create a use case / helper class** (e.g. \`GenerateRecurringTransactionsUseCase.kt\` in \`domain/usecase/\`) that contains the core generation logic:
   - Get all active recurring transactions from the DAO
   - For each, check if today matches the schedule (weekly: dayOfWeek match, monthly: dayOfMonth match)
   - Check if within date range (start_date/end_date)
   - Check if transaction already exists for today (duplicate prevention via countByRecurringAndDate)
   - If all checks pass, insert a new TransactionEntity with recurringId set
   - This extracts the logic from RecurringTransactionWorker so both the worker and eager path share the same code

2. **Update RecurringTransactionWorker.kt** to use the new shared use case instead of duplicating logic.

3. **Trigger the eager generation** when the app opens. The best place is in a ViewModel (e.g. the main/home ViewModel or TransactionListViewModel) during init{}, or create a dedicated startup use case that's called when the main screen loads. Use a coroutine scope (viewModelScope) to run it.

4. **Make sure the generation is idempotent** — the duplicate check (countByRecurringAndDate) already handles this, so calling it multiple times is safe.

**Key files to read first:**
- app/src/main/java/com/budgetmanager/app/worker/RecurringTransactionWorker.kt
- app/src/main/java/com/budgetmanager/app/data/dao/RecurringTransactionDao.kt
- app/src/main/java/com/budgetmanager/app/data/dao/TransactionDao.kt
- app/src/main/java/com/budgetmanager/app/data/entity/TransactionEntity.kt
- app/src/main/java/com/budgetmanager/app/data/entity/RecurringTransactionEntity.kt
- app/src/main/java/com/budgetmanager/app/ui/screens/transactions/TransactionListViewModel.kt (or equivalent main ViewModel)
- app/src/main/java/com/budgetmanager/app/BudgetManagerApp.kt
- app/src/main/java/com/budgetmanager/app/di/ (Hilt modules for DI)

**IMPORTANT:**
- Actually create/modify the files. Do not just describe what to do.
- Use Hilt for dependency injection (the project already uses @HiltAndroidApp and @HiltWorker)
- Maintain the existing code style and patterns
- The generation must be idempotent (safe to call multiple times)`,
      outputFormat: 'JSON with { implemented: boolean, filesCreated: string[], filesModified: string[], summary: string }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));

const buildAndVerifyTask = defineTask('build-and-verify', (args, taskCtx) => ({
  kind: 'agent',
  title: `Build and verify Android project (iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android build engineer',
      task: `Build the Android project and report results.

Working directory: ${args.androidDir}

Run: cd ${args.androidDir} && ./gradlew assembleDebug 2>&1

Report the build result as JSON: { "buildSuccess": true/false, "errors": ["error messages if any"] }

If the build succeeds, report buildSuccess: true.
If it fails, capture the ACTUAL error messages from the build output (not just "build failed") and include them in the errors array. Include file paths and line numbers.`,
      outputFormat: 'JSON with { buildSuccess: boolean, errors: string[] }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));

const fixBuildTask = defineTask('fix-build', (args, taskCtx) => ({
  kind: 'agent',
  title: `Fix build errors (iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer',
      task: `Fix build errors in the Android project.

Working directory: ${args.androidDir}
Package: ${args.packageName}

Build errors from previous attempt:
${JSON.stringify(args.buildResult?.errors || [], null, 2)}

Read the failing files, understand the errors, and fix them. Common issues:
- Missing imports
- Incorrect Hilt/DI annotations
- Type mismatches
- Missing suspend keywords
- Incorrect package declarations

After fixing, verify the build compiles by running:
cd ${args.androidDir} && ./gradlew assembleDebug 2>&1

IMPORTANT: Actually fix the code files. Do not just describe what to do.`,
      outputFormat: 'JSON with { fixed: boolean, changesApplied: string[] }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));

const runBackendTestsTask = defineTask('run-backend-tests', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Run backend pytest tests for regression check',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'QA engineer',
      task: `Run the existing backend tests to verify no regressions were introduced.

Working directory: ${args.projectDir}

Run: cd ${args.projectDir} && python -m pytest tests/ -v 2>&1

Report the test results as JSON: { "allPassed": true/false, "totalTests": number, "passed": number, "failed": number, "errors": ["any failure messages"] }

Note: These are Python/FastAPI backend tests. They test the API endpoints and recurring generation logic. Even though we only changed Android code, these tests validate the shared business logic patterns.`,
      outputFormat: 'JSON with { allPassed: boolean, totalTests: number, passed: number, failed: number, errors: string[] }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));

const verifyFixLogicTask = defineTask('verify-fix-logic', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Code review: verify the fix is correct and complete',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android code reviewer',
      task: `Review the recurring transaction eager generation fix for correctness and completeness.

Working directory: ${args.androidDir}
Package: ${args.packageName}

Check these critical aspects:

1. **Shared logic**: The generation logic should be shared between the WorkManager worker and the eager path (no duplication)
2. **Idempotency**: Calling generation multiple times must be safe (duplicate prevention via countByRecurringAndDate)
3. **Day-of-week mapping**: 0=Monday..6=Sunday (Python convention) maps to Java DayOfWeek 1=Monday..7=Sunday via +1
4. **Monthly edge cases**: Day 31 on short months should trigger on last day
5. **Date range respect**: startDate/endDate bounds are checked
6. **DI correctness**: Hilt injection is properly configured
7. **Coroutine scope**: Eager generation runs in appropriate scope (viewModelScope, not blocking main thread)
8. **Worker still works**: RecurringTransactionWorker still functions as background fallback

Read the modified files and provide a review:
- Any new use case / helper class created
- RecurringTransactionWorker.kt (should now delegate to shared logic)
- The ViewModel that triggers eager generation
- Any DI module changes

Report as JSON: { "approved": boolean, "issues": ["list of issues found"], "suggestions": ["list of improvement suggestions"] }`,
      outputFormat: 'JSON with { approved: boolean, issues: string[], suggestions: string[] }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));
