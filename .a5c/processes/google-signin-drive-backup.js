/**
 * @process google-signin-drive-backup
 * @description Implement Google Sign-In with Credential Manager + SAF-based backup/restore to Google Drive
 * @inputs { projectDir: string, androidDir: string, packageName: string, maxConvergenceIterations: number }
 * @outputs { success: boolean, filesModified: array, version: string }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const {
    projectDir = '/home/ubuntu/projects/budget_manager',
    androidDir = '/home/ubuntu/projects/budget_manager/android',
    packageName = 'com.budgetmanager.app',
    maxConvergenceIterations = 4
  } = inputs;

  // ============================================================================
  // PHASE 1: IMPLEMENT GOOGLE SIGN-IN (Credential Manager)
  // ============================================================================

  const signInResult = await ctx.task(implementGoogleSignInTask, {
    androidDir, packageName, projectDir
  });

  // ============================================================================
  // PHASE 2: BUILD & FIX CONVERGENCE (Sign-In)
  // ============================================================================

  let signInBuildConverged = false;
  let signInBuildIteration = 0;
  while (!signInBuildConverged && signInBuildIteration < maxConvergenceIterations) {
    signInBuildIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, phase: 'google-sign-in', iteration: signInBuildIteration
    });
    signInBuildConverged = buildResult.buildSuccess === true;
    if (!signInBuildConverged && signInBuildIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildTask, {
        androidDir, packageName, buildResult, iteration: signInBuildIteration, phase: 'google-sign-in'
      });
    }
  }

  // ============================================================================
  // PHASE 3: IMPLEMENT SAF BACKUP/RESTORE + SETTINGS UI
  // ============================================================================

  const backupResult = await ctx.task(implementBackupRestoreTask, {
    androidDir, packageName, projectDir
  });

  // ============================================================================
  // PHASE 4: BUILD & FIX CONVERGENCE (Backup/Restore)
  // ============================================================================

  let backupBuildConverged = false;
  let backupBuildIteration = 0;
  while (!backupBuildConverged && backupBuildIteration < maxConvergenceIterations) {
    backupBuildIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, phase: 'backup-restore', iteration: backupBuildIteration
    });
    backupBuildConverged = buildResult.buildSuccess === true;
    if (!backupBuildConverged && backupBuildIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildTask, {
        androidDir, packageName, buildResult, iteration: backupBuildIteration, phase: 'backup-restore'
      });
    }
  }

  // ============================================================================
  // PHASE 5: INTEGRATION & POLISH
  // ============================================================================

  const integrationResult = await ctx.task(integrationPolishTask, {
    androidDir, packageName
  });

  // ============================================================================
  // PHASE 6: FINAL BUILD & FIX CONVERGENCE
  // ============================================================================

  let finalBuildConverged = false;
  let finalBuildIteration = 0;
  while (!finalBuildConverged && finalBuildIteration < maxConvergenceIterations) {
    finalBuildIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, phase: 'final', iteration: finalBuildIteration
    });
    finalBuildConverged = buildResult.buildSuccess === true;
    if (!finalBuildConverged && finalBuildIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildTask, {
        androidDir, packageName, buildResult, iteration: finalBuildIteration, phase: 'final'
      });
    }
  }

  // ============================================================================
  // PHASE 7: BACKEND REGRESSION TESTS
  // ============================================================================

  const testResult = await ctx.task(runBackendTestsTask, { projectDir });

  // ============================================================================
  // PHASE 8: CODE REVIEW
  // ============================================================================

  const reviewResult = await ctx.task(codeReviewTask, {
    androidDir, packageName
  });

  // ============================================================================
  // PHASE 9: USER APPROVAL
  // ============================================================================

  await ctx.breakpoint({
    question: `Google Sign-In + SAF backup/restore implemented.\n\nBuild: ${finalBuildConverged ? 'SUCCESS' : 'FAILED'}\nBackend tests: ${testResult.allPassed ? 'ALL PASSED' : 'ISSUES'}\nCode review: ${reviewResult.approved ? 'APPROVED' : 'CONCERNS'}\n\nReview issues: ${JSON.stringify(reviewResult.issues || [])}\n\nApprove to version bump, build APK, and release?`,
    title: 'Google Sign-In + Drive Backup Review'
  });

  // ============================================================================
  // PHASE 10: VERSION BUMP, BUILD, RELEASE
  // ============================================================================

  const releaseResult = await ctx.task(buildAndReleaseTask, {
    androidDir, projectDir
  });

  return {
    success: finalBuildConverged && releaseResult.success,
    signInResult,
    backupResult,
    integrationResult,
    testResult,
    reviewResult,
    releaseResult
  };
}

// ============================================================================
// TASK DEFINITIONS
// ============================================================================

const implementGoogleSignInTask = defineTask('implement-google-sign-in', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Implement real Google Sign-In with Credential Manager',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer with Google Identity expertise',
      task: `Implement real Google Sign-In using the Credential Manager API in the Budget Manager Android app.

Working directory: ${args.androidDir}
Package: ${args.packageName}

CURRENT STATE:
- There is a DEMO/STUB implementation of Google Sign-In that creates a fake "Demo User"
- Files exist: auth/GoogleSignInManager.kt, auth/AuthState.kt, ui/viewmodel/AuthViewModel.kt, ui/screens/signin/SignInScreen.kt
- The app uses Hilt for DI, Jetpack Compose for UI, DataStore for preferences

WHAT TO IMPLEMENT:

1. **Add dependencies** to app/build.gradle.kts:
   - \`implementation("androidx.credentials:credentials:1.3.0")\`
   - \`implementation("androidx.credentials:credentials-play-services-auth:1.3.0")\`
   - \`implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")\`

2. **Update GoogleSignInManager.kt** to use Credential Manager API:
   - Replace the demo signInWithGoogle() with real Credential Manager flow
   - Use GetGoogleIdOption with a configurable WEB_CLIENT_ID
   - Store the WEB_CLIENT_ID as a string resource or BuildConfig field (placeholder value for now: "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com")
   - Parse the Google ID token to get user name, email, photo URL
   - Persist the auth state to DataStore (already done)
   - Handle errors gracefully (no Google Play Services, user cancelled, etc.)
   - Keep continueAsGuest() working as-is

3. **Update SignInScreen.kt** if needed:
   - The sign-in button should trigger the real Credential Manager flow
   - Show loading state during sign-in
   - Show error messages if sign-in fails

4. **Update AuthViewModel.kt** if needed to pass Context for Credential Manager

5. **Add a string resource** for the web client ID in res/values/strings.xml (or use BuildConfig):
   - \`<string name="default_web_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>\`

6. **Keep backward compatibility**:
   - App should work in guest mode without Google credentials
   - If Google Sign-In fails (no client ID configured), show a helpful error but don't crash
   - The "Continue as Guest" flow must always work

READ THESE FILES FIRST:
- app/src/main/java/com/budgetmanager/app/auth/GoogleSignInManager.kt
- app/src/main/java/com/budgetmanager/app/auth/AuthState.kt
- app/src/main/java/com/budgetmanager/app/ui/viewmodel/AuthViewModel.kt
- app/src/main/java/com/budgetmanager/app/ui/screens/signin/SignInScreen.kt
- app/build.gradle.kts
- app/src/main/res/values/strings.xml

IMPORTANT: Actually create/modify the files. Do not just describe what to do.`,
      outputFormat: 'JSON with { implemented: boolean, filesCreated: string[], filesModified: string[], summary: string }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));

const implementBackupRestoreTask = defineTask('implement-backup-restore', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Implement SAF-based backup/restore with Settings UI',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer with Jetpack Compose expertise',
      task: `Implement backup/restore functionality using Android's Storage Access Framework (SAF) and wire up the Settings screen in the Budget Manager Android app.

Working directory: ${args.androidDir}
Package: ${args.packageName}

CURRENT STATE:
- BackupRepository interface exists with exportToJson/importFromJson/exportToUri/importFromUri
- BackupRepositoryImpl exists with working export/import logic
- SettingsScreen.kt has stub buttons (TODO: Launch file picker)
- SettingsViewModel.kt has export/import methods but they need URI parameters
- BackupData model uses kotlinx-serialization

WHAT TO IMPLEMENT:

1. **Wire up SettingsScreen.kt** with SAF file picker:
   - "Export Data" button: Opens SAF create-document picker (application/json, suggested filename "budget_backup_YYYY-MM-DD.json")
   - "Import Data" button: Opens SAF open-document picker (application/json)
   - Use \`rememberLauncherForActivityResult\` with \`ActivityResultContracts.CreateDocument\` and \`ActivityResultContracts.OpenDocument\`
   - Show progress indicator during export/import
   - Show success/error snackbar messages
   - The file picker automatically shows Google Drive if the user has Drive app installed

2. **Update SettingsViewModel.kt**:
   - Add methods that accept Uri from SAF picker
   - Call BackupRepository.exportToUri(uri) and importFromUri(uri)
   - Handle errors gracefully with user-friendly messages
   - Track isExporting/isImporting state

3. **Polish the Settings screen UI**:
   - Account section: Show signed-in user info (name, email) from AuthState, or "Guest" if not signed in
   - Sign Out button (functional)
   - Backup section with Export and Import buttons
   - Show last backup date if available (store in DataStore)
   - App version display
   - Clean Material 3 design consistent with the rest of the app

4. **Handle edge cases**:
   - Large databases (show progress)
   - Invalid/corrupt backup files (show error message)
   - Permission denied (handle gracefully)
   - Empty database (still allow export)

READ THESE FILES FIRST:
- app/src/main/java/com/budgetmanager/app/data/repository/BackupRepository.kt
- app/src/main/java/com/budgetmanager/app/data/repository/BackupRepositoryImpl.kt
- app/src/main/java/com/budgetmanager/app/ui/screens/settings/SettingsScreen.kt
- app/src/main/java/com/budgetmanager/app/ui/viewmodel/SettingsViewModel.kt
- app/src/main/java/com/budgetmanager/app/domain/model/BackupData.kt
- app/src/main/java/com/budgetmanager/app/auth/AuthState.kt
- app/src/main/java/com/budgetmanager/app/ui/viewmodel/AuthViewModel.kt

IMPORTANT: Actually create/modify the files. Do not just describe what to do.`,
      outputFormat: 'JSON with { implemented: boolean, filesCreated: string[], filesModified: string[], summary: string }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));

const integrationPolishTask = defineTask('integration-polish', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Integration testing and UI polish',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer and UX engineer',
      task: `Review and polish the Google Sign-In + backup/restore integration in the Budget Manager Android app.

Working directory: ${args.androidDir}
Package: ${args.packageName}

Check and fix these integration points:

1. **Sign-In → Settings flow**: After signing in, the Settings screen should show the user's name and email. Verify the AuthState flows correctly through the navigation.

2. **Guest mode**: Verify that backup/restore works in guest mode too (no sign-in required for local file backup).

3. **Navigation**: Ensure the sign-in screen is accessible from Settings (if not signed in) and that sign-out returns to appropriate screen.

4. **UI consistency**: All screens should use consistent Material 3 theming, spacing, typography.

5. **Error handling**: Verify all error paths show user-friendly messages (snackbars, dialogs).

6. **String resources**: Move any hardcoded strings to strings.xml.

7. **Import validation**: When importing a backup, verify the JSON structure is valid before wiping existing data. Show confirmation dialog before import ("This will replace all current data. Continue?").

READ the key files, identify any issues, and FIX them:
- app/src/main/java/com/budgetmanager/app/ui/screens/settings/SettingsScreen.kt
- app/src/main/java/com/budgetmanager/app/ui/viewmodel/SettingsViewModel.kt
- app/src/main/java/com/budgetmanager/app/ui/screens/signin/SignInScreen.kt
- app/src/main/java/com/budgetmanager/app/auth/GoogleSignInManager.kt
- app/src/main/java/com/budgetmanager/app/ui/navigation/BudgetNavHost.kt
- app/src/main/java/com/budgetmanager/app/data/repository/BackupRepositoryImpl.kt

IMPORTANT: Actually fix any issues found. Do not just describe what to do.`,
      outputFormat: 'JSON with { polished: boolean, issuesFixed: string[], filesModified: string[], summary: string }'
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
If it fails, capture the ACTUAL error messages from the build output (not just "build failed") and include them in the errors array. Include file paths and line numbers where possible.`,
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
  title: `Fix build errors (${args.phase} - iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer',
      task: `Fix build errors in the Android project.

Working directory: ${args.androidDir}
Package: ${args.packageName}
Phase: ${args.phase}

Build errors from previous attempt:
${JSON.stringify(args.buildResult?.errors || [], null, 2)}

Read the failing files, understand the errors, and fix them. Common issues:
- Missing imports
- Incorrect Hilt/DI annotations
- Type mismatches
- Missing suspend keywords
- Incorrect Compose API usage
- Missing string resources
- Credential Manager API usage issues

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
      task: `Run the existing backend tests to verify no regressions.

Working directory: ${args.projectDir}

Run: cd ${args.projectDir} && python -m pytest tests/ -v --ignore=tests/test_ui_e2e.py 2>&1

Report as JSON: { "allPassed": true/false, "totalTests": number, "passed": number, "failed": number, "errors": ["failure messages"] }`,
      outputFormat: 'JSON with { allPassed: boolean, totalTests: number, passed: number, failed: number, errors: string[] }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));

const codeReviewTask = defineTask('code-review', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Code review: Google Sign-In + backup/restore',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android code reviewer and security engineer',
      task: `Review the Google Sign-In and backup/restore implementation for correctness, security, and completeness.

Working directory: ${args.androidDir}
Package: ${args.packageName}

Check these critical aspects:

1. **Google Sign-In**: Credential Manager API usage is correct, error handling is comprehensive, guest mode works
2. **Backup/Restore**: SAF integration is correct, data integrity is preserved, import validates JSON before replacing data
3. **Security**: No sensitive data exposed, tokens stored securely, no hardcoded credentials
4. **DI**: Hilt injection is properly configured for new components
5. **UI/UX**: Settings screen is polished, error messages are user-friendly, loading states shown
6. **Edge cases**: Empty database, corrupt backup, cancelled operations, no network

Read all modified/created files related to auth, backup, and settings.

Report as JSON: { "approved": boolean, "issues": ["blocking issues"], "suggestions": ["non-blocking improvements"] }`,
      outputFormat: 'JSON with { approved: boolean, issues: string[], suggestions: string[] }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));

const buildAndReleaseTask = defineTask('build-and-release', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Version bump, build APK, commit, push, and create GitHub release',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android release engineer',
      task: `Bump version, build APK, commit, push, and create a GitHub release.

Working directory: ${args.androidDir}
Project directory: ${args.projectDir}

Steps:

1. **Version bump** in app/build.gradle.kts:
   - Change versionCode from 3 to 4
   - Change versionName from "1.2.0" to "1.3.0"

2. **Build the APK**:
   cd ${args.androidDir} && ./gradlew assembleDebug 2>&1

3. **Git commit** all changes:
   cd ${args.projectDir} && git add -A && git commit -m "feat: add Google Sign-In and SAF-based backup/restore to Google Drive

- Replace demo Google Sign-In with Credential Manager API
- Wire up Settings screen with SAF file picker for export/import
- Backup files can be saved to Google Drive via Android file picker
- Guest mode continues to work without Google credentials
- Add import confirmation dialog and data validation
- Bump version to 1.3.0

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"

4. **Push to GitHub**:
   cd ${args.projectDir} && git push origin master

5. **Create GitHub release**:
   cd ${args.projectDir} && gh release create v1.3.0 ${args.androidDir}/app/build/outputs/apk/debug/app-debug.apk --title "Budget Manager v1.3.0" --notes "## What's New in v1.3.0

### Google Sign-In
- Sign in with your Google account
- Your account info is displayed in Settings
- Guest mode still available for use without Google

### Backup & Restore
- **Export** your data as a JSON backup file
- **Import** data from a previous backup
- Save backups directly to **Google Drive** via Android's file picker
- Restore from Google Drive or any other storage location
- Import validates data and asks for confirmation before replacing

### Setup (for Google Sign-In)
To enable Google Sign-In, you need to:
1. Create a Google Cloud project at console.cloud.google.com
2. Create an OAuth 2.0 Client ID (Web type)
3. Replace the placeholder in strings.xml with your client ID
4. Register your debug signing key SHA-1

Backup/restore works without Google Sign-In (guest mode).

## Install
Download the APK file below and install on your Android device.
Upgrading from v1.2.0 preserves all your data.

🤖 Generated with [Claude Code](https://claude.ai/code)"

Report as JSON: { "success": boolean, "apkPath": string, "releaseUrl": string }

IMPORTANT: Actually execute these commands. Do not just describe what to do.`,
      outputFormat: 'JSON with { success: boolean, apkPath: string, releaseUrl: string }'
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/output.json`
  }
}));
