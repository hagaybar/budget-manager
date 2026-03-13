/**
 * @process recurring-auto-generation
 * @description Add WorkManager auto-generation of recurring transactions, next-occurrence display, build & publish APK v1.1.0
 * @inputs { androidDir: string, packageName: string, githubRepo: string, maxConvergenceIterations: number, features: array }
 * @outputs { success: boolean, apkPath: string, releaseUrl: string }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const {
    projectDir = '/home/ubuntu/projects/budget_manager',
    androidDir = '/home/ubuntu/projects/budget_manager/android',
    packageName = 'com.budgetmanager.app',
    githubRepo = 'hagaybar/budget-manager',
    maxConvergenceIterations = 3
  } = inputs;

  // ============================================================================
  // PHASE 1: ADD WORKMANAGER DEPENDENCY & WORKER IMPLEMENTATION
  // ============================================================================

  const workerResult = await ctx.task(implementWorkerTask, {
    androidDir, packageName, projectDir
  });

  // ============================================================================
  // PHASE 2: BUILD & FIX CONVERGENCE LOOP (WorkManager integration)
  // ============================================================================

  let buildConverged = false;
  let buildIteration = 0;
  while (!buildConverged && buildIteration < maxConvergenceIterations) {
    buildIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, phase: 'worker', iteration: buildIteration
    });
    buildConverged = buildResult.buildSuccess === true;
    if (!buildConverged && buildIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildTask, {
        androidDir, packageName, buildResult, iteration: buildIteration
      });
    }
  }

  // ============================================================================
  // PHASE 3: NEXT OCCURRENCE UI DISPLAY
  // ============================================================================

  const nextOccurrenceResult = await ctx.task(implementNextOccurrenceTask, {
    androidDir, packageName
  });

  // ============================================================================
  // PHASE 4: BUILD & FIX CONVERGENCE LOOP (UI changes)
  // ============================================================================

  let uiConverged = false;
  let uiIteration = 0;
  while (!uiConverged && uiIteration < maxConvergenceIterations) {
    uiIteration++;
    const uiBuildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, phase: 'ui', iteration: uiIteration
    });
    uiConverged = uiBuildResult.buildSuccess === true;
    if (!uiConverged && uiIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildTask, {
        androidDir, packageName, buildResult: uiBuildResult, iteration: uiIteration
      });
    }
  }

  // ============================================================================
  // PHASE 5: VERSION BUMP, BUILD APK, PUBLISH GITHUB RELEASE
  // ============================================================================

  const releaseResult = await ctx.task(buildAndReleaseTask, {
    androidDir, projectDir, githubRepo
  });

  return {
    success: true,
    workerResult,
    nextOccurrenceResult,
    apkPath: releaseResult.apkPath,
    releaseUrl: releaseResult.releaseUrl
  };
}

// ============================================================================
// TASK DEFINITIONS
// ============================================================================

const implementWorkerTask = defineTask('implement-worker', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Implement WorkManager recurring transaction auto-generation',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer',
      task: `Implement WorkManager-based automatic recurring transaction generation for the Budget Manager Android app.

Working directory: ${args.androidDir}
Package: ${args.packageName}

You need to do ALL of the following:

1. **Add WorkManager dependency** to app/build.gradle.kts:
   - Add \`implementation("androidx.work:work-runtime-ktx:2.9.0")\`
   - Add \`implementation("androidx.hilt:hilt-work:1.1.0")\`
   - Add \`ksp("androidx.hilt:hilt-compiler:1.1.0")\`

2. **Create RecurringTransactionWorker.kt** at \`app/src/main/java/com/budgetmanager/app/worker/RecurringTransactionWorker.kt\`:
   - Extend \`CoroutineWorker\`
   - Annotate with \`@HiltWorker\`
   - Inject \`RecurringTransactionDao\` and \`TransactionDao\` via \`@AssistedInject\`
   - In doWork(): get all active recurring transactions, check if today matches their schedule (day of week for weekly, day of month for monthly), create Transaction entries that don't already exist for today
   - Return Result.success()

3. **Update BudgetManagerApp.kt** to implement \`Configuration.Provider\`:
   - Inject \`HiltWorkerFactory\`
   - Override \`getWorkManagerConfiguration()\`
   - Schedule the worker in onCreate() using PeriodicWorkRequestBuilder (once per day, with ExistingPeriodicWorkPolicy.KEEP)

4. **Update AndroidManifest.xml** to disable default WorkManager initializer:
   - Add provider block to remove default initializer

Read the existing code files first to understand the current architecture before making changes. The key files are:
- app/build.gradle.kts (dependencies)
- app/src/main/java/com/budgetmanager/app/BudgetManagerApp.kt (Application class)
- app/src/main/java/com/budgetmanager/app/data/dao/RecurringTransactionDao.kt
- app/src/main/java/com/budgetmanager/app/data/dao/TransactionDao.kt
- app/src/main/java/com/budgetmanager/app/data/entity/TransactionEntity.kt
- app/src/main/java/com/budgetmanager/app/data/entity/RecurringTransactionEntity.kt
- app/src/main/AndroidManifest.xml

IMPORTANT: Actually create/modify the files. Do not just describe what to do.`,
      outputFormat: 'JSON with { implemented: boolean, filesCreated: string[], filesModified: string[] }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));

const buildAndVerifyTask = defineTask('build-and-verify', (args, taskCtx) => ({
  kind: 'agent',
  title: `Build and verify (${args.phase} - iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android build engineer',
      task: `Build the Android project and report results.

Working directory: ${args.androidDir}

Run: cd ${args.androidDir} && ./gradlew assembleDebug 2>&1

Report the build result as JSON: { "buildSuccess": true/false, "errors": ["error messages if any"] }

If the build succeeds, report buildSuccess: true.
If it fails, capture the actual error messages from the build output and include them in the errors array.`,
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

Read the failing files, understand the errors, and fix them. Then verify the build compiles by running:
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

const implementNextOccurrenceTask = defineTask('implement-next-occurrence', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Add next occurrence display to recurring transaction cards',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer with Jetpack Compose expertise',
      task: `Add "next occurrence" display to recurring transaction cards in the Budget Manager Android app.

Working directory: ${args.androidDir}
Package: ${args.packageName}

You need to:

1. **Create a utility function** \`getNextOccurrence(recurring: RecurringTransaction): LocalDate\` in a new file or in the domain model:
   - For MONTHLY frequency: find the next date with dayOfMonth matching (if today is past that day, use next month)
   - For WEEKLY frequency: find the next date with dayOfWeek matching (Monday=0, Tuesday=1, etc.)
   - If the recurring transaction is inactive, return null or indicate inactive

2. **Update RecurringScreen.kt** to show the next occurrence on each card:
   - Below the existing content (frequency, type, amount), add a line like "Next: Mar 15, 2026" in a subtle style
   - Use MaterialTheme.typography.bodySmall with onSurfaceVariant color
   - Format the date nicely (e.g., "Mar 15" if same year, "Mar 15, 2027" if different year)
   - For inactive transactions, show "Inactive" instead of a date

3. **Also update the ViewRecurringDialog** to show the next occurrence date in the detail view.

Read existing files first:
- app/src/main/java/com/budgetmanager/app/ui/screens/recurring/RecurringScreen.kt
- app/src/main/java/com/budgetmanager/app/domain/model/RecurringTransaction.kt
- app/src/main/java/com/budgetmanager/app/domain/model/Frequency.kt

IMPORTANT: Actually create/modify the files. Do not just describe what to do.`,
      outputFormat: 'JSON with { implemented: boolean, filesCreated: string[], filesModified: string[] }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));

const buildAndReleaseTask = defineTask('build-and-release', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Version bump, build APK, publish GitHub release v1.1.0',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android release engineer',
      task: `Bump the version, build the APK, and publish a GitHub release for the Budget Manager Android app.

Working directory: ${args.androidDir}
Project directory: ${args.projectDir}
GitHub repo: ${args.githubRepo}

Steps:

1. **Version bump** in app/build.gradle.kts:
   - Change versionCode from 1 to 2
   - Change versionName from "1.0.0" to "1.1.0"

2. **Build the APK**:
   cd ${args.androidDir} && ./gradlew assembleDebug 2>&1

3. **Git commit** all changes:
   cd ${args.projectDir} && git add -A && git commit -m "feat: add WorkManager auto-generation of recurring transactions and next-occurrence display

- Add WorkManager background worker that auto-generates transactions from active recurring templates daily
- Show next occurrence date on recurring transaction cards
- Add edit and delete functionality for recurring transactions
- Bump version to 1.1.0

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"

4. **Push to GitHub**:
   cd ${args.projectDir} && git push origin master

5. **Create GitHub release**:
   First get the GitHub token: GH_TOKEN=$(echo "protocol=https\nhost=github.com" | git credential fill | grep password | cut -d= -f2)
   Then: GH_TOKEN=$GH_TOKEN gh release create v1.1.0 ${args.androidDir}/app/build/outputs/apk/debug/app-debug.apk --title "Budget Manager v1.1.0" --notes "## What's New in v1.1.0

- **Auto-generate recurring transactions**: WorkManager runs daily to automatically create transactions from your active recurring templates
- **Next occurrence display**: Each recurring transaction card now shows when it will next take effect
- **Edit recurring transactions**: Tap any recurring transaction to view details, edit (pencil icon), or delete (trash icon)
- **Delete with confirmation**: Safe deletion with confirmation dialog

## Install
Download the APK file below and install on your Android device."

Report the result as JSON with apkPath, releaseUrl, and success status.

IMPORTANT: Actually execute these commands. Do not just describe what to do.`,
      outputFormat: 'JSON with { success: boolean, apkPath: string, releaseUrl: string }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));
