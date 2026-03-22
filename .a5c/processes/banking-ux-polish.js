/**
 * @process banking-ux-polish
 * @description Major visual improvement: banking-app-inspired UX with prominent balance,
 *              inspiring design, and usability-first approach.
 * @inputs { androidDir: string, maxIter: number }
 * @outputs { success: boolean }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const {
    androidDir = '/home/ubuntu/projects/budget_manager/android',
    maxIter = 5
  } = inputs;

  // ============================================================================
  // PHASE 1: Redesign TransactionListScreen — hero balance card + banking feel
  // ============================================================================

  const heroResult = await ctx.task(redesignMainScreenTask, { androidDir });

  let heroOk = false;
  let heroI = 0;
  while (!heroOk && heroI < maxIter) {
    heroI++;
    const b = await ctx.task(buildTask, { androidDir, step: 'hero', iteration: heroI });
    heroOk = b.allPassing;
    if (!heroOk && heroI < maxIter) await ctx.task(fixTask, { androidDir, buildResult: b, step: 'hero', iteration: heroI });
  }

  // ============================================================================
  // PHASE 2: Redesign MonthlySummaryScreen — visual analytics, progress rings
  // ============================================================================

  const summaryResult = await ctx.task(redesignSummaryScreenTask, { androidDir });

  let sumOk = false;
  let sumI = 0;
  while (!sumOk && sumI < maxIter) {
    sumI++;
    const b = await ctx.task(buildTask, { androidDir, step: 'summary', iteration: sumI });
    sumOk = b.allPassing;
    if (!sumOk && sumI < maxIter) await ctx.task(fixTask, { androidDir, buildResult: b, step: 'summary', iteration: sumI });
  }

  // ============================================================================
  // PHASE 3: Polish remaining screens for consistency
  // ============================================================================

  const polishResult = await ctx.task(polishRemainingScreensTask, { androidDir });

  let polOk = false;
  let polI = 0;
  while (!polOk && polI < maxIter) {
    polI++;
    const b = await ctx.task(buildTask, { androidDir, step: 'polish', iteration: polI });
    polOk = b.allPassing;
    if (!polOk && polI < maxIter) await ctx.task(fixTask, { androidDir, buildResult: b, step: 'polish', iteration: polI });
  }

  // ============================================================================
  // PHASE 4: Build final APK
  // ============================================================================

  const apk = await ctx.task(buildTask, { androidDir, step: 'final', iteration: 1 });

  await ctx.breakpoint({
    question: `Visual polish complete. APK built: ${apk.allPassing}. Please test the APK and confirm.`,
    title: 'Banking UX Polish Complete'
  });

  return { success: apk.allPassing, metadata: { processId: 'banking-ux-polish', timestamp: ctx.now() } };
}

export const redesignMainScreenTask = defineTask('redesign-main', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 1: Redesign TransactionListScreen with hero balance + banking feel',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android UI engineer specializing in fintech/banking app design with Jetpack Compose',
      task: 'Completely redesign the TransactionListScreen to feel like a modern banking app. The current balance should be the FIRST and BIGGEST thing the user sees.',
      context: { androidDir: args.androidDir },
      instructions: [
        `Read the current TransactionListScreen: ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/screens/transactions/TransactionListScreen.kt`,
        `Read the theme files for available tokens: ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/theme/`,
        `Read the TransactionListViewModel: ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/viewmodel/TransactionListViewModel.kt`,
        `Read AmountText component: ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/components/AmountText.kt`,
        '',
        'REDESIGN TransactionListScreen with this banking-app layout:',
        '',
        '1. HERO BALANCE CARD (top of screen, first thing you see):',
        '   - Large, prominent current balance number (use displayLarge or custom 36-40sp bold)',
        '   - Currency symbol (₪) next to the number',
        '   - Semantic color: green if positive, red if negative',
        '   - Label above: "Current Balance" in bodySmall, onSurfaceVariant',
        '   - Below the balance: a Row with Income total (green, with up arrow) and Expense total (red, with down arrow)',
        '   - The hero card should have a subtle gradient or elevated surface treatment',
        '   - Use primaryContainer or a custom gradient for the card background',
        '   - The balance should be calculated from uiState (sum income - sum expense)',
        '',
        '2. QUICK STATS ROW (below hero):',
        '   - Two compact chips/pills showing: "This Month: ₪X,XXX spent" and transaction count',
        '   - Use surfaceContainerHigh with small corner radius',
        '',
        '3. TRANSACTION LIST (main content):',
        '   - Section header: "Recent Transactions" with bodyLarge, semibold',
        '   - Each transaction card should be clean and scannable:',
        '     - Left: colored category dot (small circle)',
        '     - Center: category name (titleSmall bold) + description (bodySmall, onSurfaceVariant) stacked',
        '     - Right: amount (semantic colored, medium weight) + date below (labelSmall, onSurfaceVariant)',
        '   - Minimal card elevation, use dividers instead of card borders between items',
        '   - Keep swipe-to-delete',
        '',
        '4. FAB: Keep the floating action button for adding transactions',
        '',
        '5. Filter bar: Move it to be accessible but not dominant (collapsible or icon-triggered)',
        '',
        'The ViewModel already provides uiState.transactions — compute the balance from those.',
        'Use the existing design system tokens (Spacing, CornerRadius, LocalFinanceColors, MaterialTheme).',
        'DO NOT modify the ViewModel — only the screen composable.',
        'DO NOT modify MainActivity.kt.',
        'IMPORTANT: Use only APIs available in Compose BOM 2024.02.00 and navigation-compose 2.7.7.',
        'IMPORTANT: If using AnimatedVisibility inside LazyColumn items, use the fully qualified name: androidx.compose.animation.AnimatedVisibility',
        'Return summary of changes.'
      ],
      outputFormat: 'JSON with summary (string), filesModified (array)'
    },
    outputSchema: { type: 'object', required: ['summary'], properties: { summary: { type: 'string' }, filesModified: { type: 'array', items: { type: 'string' } } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['main-screen', 'hero']
}));

export const redesignSummaryScreenTask = defineTask('redesign-summary', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 2: Redesign MonthlySummaryScreen — visual analytics',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android UI engineer specializing in data visualization and fintech design',
      task: 'Redesign the MonthlySummaryScreen to be more visual, inspiring, and banking-app-like.',
      context: { androidDir: args.androidDir },
      instructions: [
        `Read: ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/screens/summary/MonthlySummaryScreen.kt`,
        `Read: ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/viewmodel/MonthlySummaryViewModel.kt`,
        `Read theme: ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/theme/Theme.kt`,
        '',
        'REDESIGN MonthlySummaryScreen:',
        '',
        '1. MONTH HERO SECTION:',
        '   - Large month/year header (headlineMedium)',
        '   - Clean prev/next month navigation arrows',
        '   - Net balance for the month (large, semantic colored)',
        '',
        '2. INCOME vs EXPENSE VISUAL:',
        '   - Two large cards side by side:',
        '     - Income card: green-tinted surface, up arrow icon, total amount large',
        '     - Expense card: red-tinted surface, down arrow icon, total amount large',
        '   - A visual balance bar or ratio indicator between them',
        '',
        '3. CATEGORY BREAKDOWN:',
        '   - Section header: "Spending by Category"',
        '   - Each category as a clean row with:',
        '     - Category name + count label',
        '     - Progress bar (relative to largest category)',
        '     - Amount on the right',
        '   - Sorted by amount descending',
        '   - Use subtle colors for the progress bars (tertiary or secondary tints)',
        '',
        '4. Transaction count as a subtle footer stat',
        '',
        'Use existing design system tokens. DO NOT modify ViewModels or MainActivity.',
        'Return summary.'
      ],
      outputFormat: 'JSON with summary (string), filesModified (array)'
    },
    outputSchema: { type: 'object', required: ['summary'], properties: { summary: { type: 'string' }, filesModified: { type: 'array', items: { type: 'string' } } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['summary-screen']
}));

export const polishRemainingScreensTask = defineTask('polish-remaining', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 3: Polish remaining screens for banking-app consistency',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android UI engineer',
      task: 'Polish the remaining screens to feel consistent with the new banking-app-inspired main and summary screens.',
      context: { androidDir: args.androidDir },
      instructions: [
        `Read ALL screens under: ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/screens/`,
        '',
        'Polish for consistency with the new banking feel:',
        '',
        '1. AddEditTransactionScreen: clean form layout, prominent save button, type toggle with color feedback',
        '2. RecurringScreen: clean card layout with next-occurrence prominently shown',
        '3. SettingsScreen: clean section grouping',
        '4. SignInScreen: make sure it feels premium and connected to the banking theme',
        '5. BudgetManagementScreen: clean budget cards',
        '',
        'Focus on: consistent spacing, typography hierarchy, making important info big and scannable.',
        'DO NOT modify ViewModels or MainActivity.kt.',
        'Only make changes where they meaningfully improve the user experience.',
        'Return summary.'
      ],
      outputFormat: 'JSON with summary (string), filesModified (array)'
    },
    outputSchema: { type: 'object', required: ['summary'], properties: { summary: { type: 'string' }, filesModified: { type: 'array', items: { type: 'string' } } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['polish']
}));

export const buildTask = defineTask('build', (args, taskCtx) => ({
  kind: 'agent',
  title: `Build (${args.step}, iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android build engineer',
      task: `Build the Android project. Step: ${args.step}.`,
      context: { androidDir: args.androidDir },
      instructions: [`cd ${args.androidDir} && ./gradlew assembleDebug 2>&1`, 'Return allPassing and any errors'],
      outputFormat: 'JSON with allPassing (boolean), errors (array), buildOutput (string)'
    },
    outputSchema: { type: 'object', required: ['allPassing'], properties: { allPassing: { type: 'boolean' }, errors: { type: 'array' }, buildOutput: { type: 'string' } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['build', args.step]
}));

export const fixTask = defineTask('fix', (args, taskCtx) => ({
  kind: 'agent',
  title: `Fix (${args.step}, iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer',
      task: `Fix build failures for: ${args.step}. DO NOT modify MainActivity.kt.`,
      context: { androidDir: args.androidDir, buildResult: args.buildResult },
      instructions: ['Fix each build error. Return summary.'],
      outputFormat: 'JSON with summary (string), filesModified (array)'
    },
    outputSchema: { type: 'object', required: ['summary'], properties: { summary: { type: 'string' }, filesModified: { type: 'array' } } }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['fix', args.step]
}));
