/**
 * @process visual-redesign
 * @description Complete visual redesign of the Android budget manager app.
 *              Transform it from developer-made to a polished, premium consumer app
 *              with cohesive design system, refined screens, motion, dark mode, and app icon.
 * @inputs { androidDir: string, projectDir: string, maxConvergenceIterations: number }
 * @outputs { success: boolean, phases: object }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const {
    projectDir = '/home/ubuntu/projects/budget_manager',
    androidDir = '/home/ubuntu/projects/budget_manager/android',
    maxConvergenceIterations = 5
  } = inputs;

  const designBrief = {
    feel: 'trustworthy, modern, clean, pleasant, slightly premium, calm, efficient, visually memorable, finance/productivity',
    communicate: 'clarity, control, confidence, simplicity, financial awareness, reduced stress',
    avoid: 'random styles, too many accents, cluttered dashboards, overused shadows/gradients, inconsistent corner radii, template look, breaking flows'
  };

  // ============================================================================
  // PHASE 1: AUDIT — Inspect current UI, identify weaknesses, plan direction
  // ============================================================================

  const auditResult = await ctx.task(auditCurrentUITask, {
    androidDir, projectDir, designBrief
  });

  await ctx.breakpoint({
    question: 'Review the UI audit and proposed design direction. Approve to proceed with design system creation?',
    title: 'Visual Redesign Audit Review',
    context: {
      runId: ctx.runId,
      files: [
        { path: 'artifacts/ui-audit.md', format: 'markdown' }
      ]
    }
  });

  // ============================================================================
  // PHASE 2: DESIGN SYSTEM — Define and implement foundation
  // ============================================================================

  const designSystemResult = await ctx.task(createDesignSystemTask, {
    androidDir, projectDir, designBrief, audit: auditResult
  });

  // Build convergence for design system
  let dsConverged = false;
  let dsIteration = 0;
  while (!dsConverged && dsIteration < maxConvergenceIterations) {
    dsIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'design-system', iteration: dsIteration
    });
    dsConverged = buildResult.allPassing === true;
    if (!dsConverged && dsIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'design-system', buildResult, iteration: dsIteration
      });
    }
  }

  // ============================================================================
  // PHASE 3: APP ICON & SPLASH — Brand identity foundation
  // ============================================================================

  const brandResult = await ctx.task(implementAppIconAndSplashTask, {
    androidDir, projectDir, designBrief, designSystem: designSystemResult
  });

  // Build convergence
  let brandConverged = false;
  let brandIteration = 0;
  while (!brandConverged && brandIteration < maxConvergenceIterations) {
    brandIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'brand-identity', iteration: brandIteration
    });
    brandConverged = buildResult.allPassing === true;
    if (!brandConverged && brandIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'brand-identity', buildResult, iteration: brandIteration
      });
    }
  }

  // ============================================================================
  // PHASE 4: GLOBAL COMPONENTS — Shared UI components and theme application
  // ============================================================================

  const globalComponentsResult = await ctx.task(implementGlobalComponentsTask, {
    androidDir, projectDir, designBrief, designSystem: designSystemResult
  });

  // Build convergence
  let gcConverged = false;
  let gcIteration = 0;
  while (!gcConverged && gcIteration < maxConvergenceIterations) {
    gcIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'global-components', iteration: gcIteration
    });
    gcConverged = buildResult.allPassing === true;
    if (!gcConverged && gcIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'global-components', buildResult, iteration: gcIteration
      });
    }
  }

  // ============================================================================
  // PHASE 5: SCREEN REFINEMENT — Screen-by-screen visual improvements
  // ============================================================================

  // 5a: High-impact screens (Transactions, Summary, Sign-In)
  const highImpactResult = await ctx.task(refineHighImpactScreensTask, {
    androidDir, projectDir, designBrief, designSystem: designSystemResult
  });

  let hiConverged = false;
  let hiIteration = 0;
  while (!hiConverged && hiIteration < maxConvergenceIterations) {
    hiIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'high-impact-screens', iteration: hiIteration
    });
    hiConverged = buildResult.allPassing === true;
    if (!hiConverged && hiIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'high-impact-screens', buildResult, iteration: hiIteration
      });
    }
  }

  // 5b: Secondary screens (AddEdit, Recurring, Settings, Budget Management, Migration)
  const secondaryResult = await ctx.task(refineSecondaryScreensTask, {
    androidDir, projectDir, designBrief, designSystem: designSystemResult
  });

  let secConverged = false;
  let secIteration = 0;
  while (!secConverged && secIteration < maxConvergenceIterations) {
    secIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'secondary-screens', iteration: secIteration
    });
    secConverged = buildResult.allPassing === true;
    if (!secConverged && secIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'secondary-screens', buildResult, iteration: secIteration
      });
    }
  }

  // ============================================================================
  // PHASE 6: MOTION & MICRO-INTERACTIONS
  // ============================================================================

  const motionResult = await ctx.task(implementMotionTask, {
    androidDir, projectDir, designBrief, designSystem: designSystemResult
  });

  let motionConverged = false;
  let motionIteration = 0;
  while (!motionConverged && motionIteration < maxConvergenceIterations) {
    motionIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'motion', iteration: motionIteration
    });
    motionConverged = buildResult.allPassing === true;
    if (!motionConverged && motionIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'motion', buildResult, iteration: motionIteration
      });
    }
  }

  // ============================================================================
  // PHASE 7: DARK MODE POLISH
  // ============================================================================

  const darkModeResult = await ctx.task(polishDarkModeTask, {
    androidDir, projectDir, designBrief, designSystem: designSystemResult
  });

  let dmConverged = false;
  let dmIteration = 0;
  while (!dmConverged && dmIteration < maxConvergenceIterations) {
    dmIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'dark-mode', iteration: dmIteration
    });
    dmConverged = buildResult.allPassing === true;
    if (!dmConverged && dmIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'dark-mode', buildResult, iteration: dmIteration
      });
    }
  }

  // ============================================================================
  // PHASE 8: PREMIUM POLISH PASS — Final consistency and refinement
  // ============================================================================

  const polishResult = await ctx.task(premiumPolishPassTask, {
    androidDir, projectDir, designBrief, designSystem: designSystemResult
  });

  let polishConverged = false;
  let polishIteration = 0;
  while (!polishConverged && polishIteration < maxConvergenceIterations) {
    polishIteration++;
    const buildResult = await ctx.task(buildAndVerifyTask, {
      androidDir, scope: 'polish-pass', iteration: polishIteration
    });
    polishConverged = buildResult.allPassing === true;
    if (!polishConverged && polishIteration < maxConvergenceIterations) {
      await ctx.task(fixBuildFailuresTask, {
        androidDir, scope: 'polish-pass', buildResult, iteration: polishIteration
      });
    }
  }

  // ============================================================================
  // PHASE 9: FINAL QUALITY REVIEW & APK
  // ============================================================================

  const qualityResult = await ctx.task(designQualityReviewTask, {
    androidDir, projectDir, designBrief
  });

  // Fix any quality issues
  if (!qualityResult.passesQuality) {
    await ctx.task(fixDesignQualityIssuesTask, {
      androidDir, qualityResult, iteration: 1
    });

    let fixConverged = false;
    let fixIteration = 0;
    while (!fixConverged && fixIteration < maxConvergenceIterations) {
      fixIteration++;
      const buildResult = await ctx.task(buildAndVerifyTask, {
        androidDir, scope: 'quality-fix', iteration: fixIteration
      });
      fixConverged = buildResult.allPassing === true;
      if (!fixConverged && fixIteration < maxConvergenceIterations) {
        await ctx.task(fixBuildFailuresTask, {
          androidDir, scope: 'quality-fix', buildResult, iteration: fixIteration
        });
      }
    }
  }

  // Build final APK
  const apkResult = await ctx.task(buildApkTask, { androidDir });

  await ctx.breakpoint({
    question: `Visual redesign complete. APK built: ${apkResult.apkBuilt}. Approve final result?`,
    title: 'Visual Redesign Complete',
    context: {
      runId: ctx.runId,
      files: [
        { path: 'artifacts/ui-audit.md', format: 'markdown' }
      ]
    }
  });

  return {
    success: apkResult.apkBuilt === true,
    phases: {
      audit: { completed: true },
      designSystem: { completed: true, convergenceIterations: dsIteration },
      brandIdentity: { completed: true, convergenceIterations: brandIteration },
      globalComponents: { completed: true, convergenceIterations: gcIteration },
      highImpactScreens: { completed: true, convergenceIterations: hiIteration },
      secondaryScreens: { completed: true, convergenceIterations: secIteration },
      motion: { completed: true, convergenceIterations: motionIteration },
      darkMode: { completed: true, convergenceIterations: dmIteration },
      polishPass: { completed: true, convergenceIterations: polishIteration },
      apk: { completed: true, apkBuilt: apkResult.apkBuilt }
    },
    metadata: { processId: 'visual-redesign', timestamp: ctx.now() }
  };
}

// ============================================================================
// TASK DEFINITIONS
// ============================================================================

export const auditCurrentUITask = defineTask('audit-ui', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 1: Audit current UI and plan design direction',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android product designer and UI engineer specializing in Material Design 3, Jetpack Compose, and premium finance app design',
      task: `Audit the current Android budget manager app UI and produce a detailed design direction document. Write it to ${args.projectDir}/artifacts/ui-audit.md.`,
      context: { androidDir: args.androidDir, designBrief: args.designBrief },
      instructions: [
        `Read ALL UI files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/ — every screen, component, navigation file, and theme file`,
        `Read the theme files (Color.kt, Type.kt, Theme.kt) under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/theme/`,
        `Read resource files: ${args.androidDir}/app/src/main/res/values/colors.xml, strings.xml, themes.xml if they exist`,
        'Analyze and document in the audit:',
        '1. Current UI stack and architecture (Compose, Material3 version, theme structure)',
        '2. List every screen with current visual weaknesses',
        '3. Current color palette analysis — what works, what doesnt',
        '4. Typography analysis — hierarchy, readability issues',
        '5. Spacing and layout consistency issues',
        '6. Component reuse patterns and inconsistencies',
        '7. Dark mode quality assessment',
        '8. Low-effort high-impact opportunities',
        '9. Proposed design direction:',
        '   - Primary color palette (specific hex codes for primary, secondary, tertiary, surface, background, error, success, warning)',
        '   - Typography scale (headline, title, body, label sizes)',
        '   - Spacing scale (4dp grid)',
        '   - Corner radius system (small=8dp, medium=12dp, large=16dp, xl=24dp)',
        '   - Elevation strategy',
        '   - Icon style direction',
        '   - Motion language principles',
        '10. Execution plan with priority order',
        `Write the complete audit to ${args.projectDir}/artifacts/ui-audit.md`,
        'Return summary'
      ],
      outputFormat: 'JSON with summary (string), weaknesses (array), designDirection (object), executionPlan (array)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: {
        summary: { type: 'string' },
        weaknesses: { type: 'array', items: { type: 'string' } },
        designDirection: { type: 'object' },
        executionPlan: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['audit', 'design']
}));

export const createDesignSystemTask = defineTask('create-design-system', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 2: Create and implement design system foundation',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android UI engineer specializing in Material Design 3 theming, Jetpack Compose design systems, and premium app aesthetics',
      task: 'Create and implement a cohesive design system foundation for the budget manager app.',
      context: { androidDir: args.androidDir, designBrief: args.designBrief, audit: args.audit },
      instructions: [
        `Read the current theme files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/theme/`,
        `Read the audit at ${args.projectDir}/artifacts/ui-audit.md for the proposed design direction`,
        'Implement the following design system files:',
        '',
        '1. REWRITE Color.kt: Define a premium finance-app color palette',
        '   - Choose a refined primary color (deep teal, navy, or rich green that feels trustworthy)',
        '   - Warm neutrals for surfaces (not pure gray)',
        '   - Semantic colors: success green, warning amber, error red, info blue',
        '   - Careful light/dark variants with proper contrast ratios',
        '   - Surface tint colors for elevation in Material3',
        '   - Use md_theme_light_* and md_theme_dark_* naming convention',
        '',
        '2. REWRITE Type.kt: Define a polished typography scale',
        '   - Use a clean sans-serif font (default Roboto is fine but set proper weights)',
        '   - Clear hierarchy: displayLarge→labelSmall',
        '   - Consistent letter spacing',
        '   - Line height tuning for readability',
        '',
        '3. CREATE Dimensions.kt (or Shape.kt) in theme/:',
        '   - Spacing scale object: xs=4.dp, sm=8.dp, md=12.dp, lg=16.dp, xl=24.dp, xxl=32.dp',
        '   - Corner radius tokens: small=8.dp, medium=12.dp, large=16.dp, extraLarge=24.dp',
        '   - Elevation tokens if needed',
        '   - Card padding constants',
        '   - Content padding constants',
        '',
        '4. REWRITE Theme.kt: Wire the new color scheme and typography',
        '   - Use MaterialTheme with proper lightColorScheme and darkColorScheme',
        '   - Ensure dynamic color is supported but has a good fallback',
        '   - Set proper shapes (RoundedCornerShape with design system tokens)',
        '',
        '5. CREATE or UPDATE colors.xml in res/values/ if needed for legacy compatibility',
        '',
        'The design system should feel: trustworthy, modern, calm, premium, suitable for finance',
        'Colors should NOT be: neon, overly saturated, childish, or generic Material defaults',
        'Return summary of what was created'
      ],
      outputFormat: 'JSON with summary (string), filesCreated (array), filesModified (array), colorPalette (object with primary/secondary/tertiary hex codes)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: {
        summary: { type: 'string' },
        filesCreated: { type: 'array', items: { type: 'string' } },
        filesModified: { type: 'array', items: { type: 'string' } },
        colorPalette: { type: 'object' }
      }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['design-system', 'theme']
}));

export const implementAppIconAndSplashTask = defineTask('app-icon-splash', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 3: Implement app icon and splash screen',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android UI engineer and brand designer',
      task: 'Design and implement a premium app icon and splash screen for the budget manager app.',
      context: { androidDir: args.androidDir, designBrief: args.designBrief, designSystem: args.designSystem },
      instructions: [
        `Read the current theme at ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/theme/Color.kt to get the color palette`,
        `Check existing icon resources: ${args.androidDir}/app/src/main/res/mipmap-*/`,
        `Check existing splash: ${args.androidDir}/app/src/main/res/values/themes.xml and drawable/`,
        '',
        '1. APP ICON:',
        '   - Create adaptive icon XML files (ic_launcher.xml, ic_launcher_round.xml in mipmap-anydpi-v26/)',
        '   - Create ic_launcher_foreground.xml vector drawable (a simple, clean finance icon — stylized shield, wallet, or abstract graph mark)',
        '   - Create ic_launcher_background.xml (solid primary color or subtle gradient)',
        '   - The icon should be: simple at small sizes, modern, recognizable, finance-related but not cliché',
        '   - Use the primary color from the design system',
        '',
        '2. SPLASH SCREEN:',
        '   - Implement Android 12+ SplashScreen API if not already',
        '   - Set windowSplashScreenBackground to the primary/surface color',
        '   - Set windowSplashScreenAnimatedIcon to the app icon',
        '   - Create a clean splash theme in themes.xml',
        '   - If Compose-based, create a brief branded splash composable with logo and app name',
        '   - Transition should feel fast and intentional',
        '',
        '3. Update AndroidManifest.xml if needed for splash theme',
        'Return summary'
      ],
      outputFormat: 'JSON with summary (string), filesCreated (array), filesModified (array)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: {
        summary: { type: 'string' },
        filesCreated: { type: 'array', items: { type: 'string' } },
        filesModified: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['brand', 'icon', 'splash']
}));

export const implementGlobalComponentsTask = defineTask('global-components', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 4: Implement polished global components',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android UI engineer specializing in reusable Jetpack Compose components and Material Design 3',
      task: 'Create and polish shared UI components that define the apps visual quality baseline.',
      context: { androidDir: args.androidDir, designBrief: args.designBrief, designSystem: args.designSystem },
      instructions: [
        `Read ALL existing components under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/components/`,
        `Read the new design system theme files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/theme/`,
        '',
        'Create or improve these reusable components:',
        '',
        '1. BudgetCard.kt — A premium card component with consistent corner radius, elevation, padding',
        '   - Supports title, subtitle, and content slot',
        '   - Subtle shadow and surface tint in light mode',
        '   - Proper dark mode surface colors',
        '',
        '2. AmountDisplay.kt — Formatted currency display with semantic coloring',
        '   - Income: success color, Expense: error color',
        '   - Large/medium/small size variants',
        '   - Currency symbol styling',
        '',
        '3. EmptyState.kt — Polished empty state component',
        '   - Icon, title, description, optional action button',
        '   - Centered, properly spaced',
        '   - Tasteful illustration or icon',
        '',
        '4. LoadingState.kt — Consistent loading indicator',
        '   - Shimmer placeholder or circular indicator',
        '   - Proper sizing and positioning',
        '',
        '5. BudgetTopBar.kt — Unified top app bar component',
        '   - Consistent with design system',
        '   - Integrates budget selector dropdown',
        '   - Proper title typography',
        '',
        '6. Update TransactionCard.kt — More polished card design',
        '   - Better spacing, type hierarchy, icon usage',
        '   - Subtle category color indicator',
        '   - Clean amount display with semantic colors',
        '',
        '7. Update FilterBar.kt — Cleaner filter UI',
        '   - Proper chip styling with design system colors',
        '   - Consistent spacing',
        '',
        '8. Update BottomNavBar.kt — Polished bottom navigation',
        '   - Proper active/inactive states',
        '   - Subtle indicator animation',
        '   - Consistent icon sizing',
        '',
        'All components must use the design system tokens (colors, typography, spacing, shapes)',
        'Return summary'
      ],
      outputFormat: 'JSON with summary (string), filesCreated (array), filesModified (array)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: {
        summary: { type: 'string' },
        filesCreated: { type: 'array', items: { type: 'string' } },
        filesModified: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['components', 'ui']
}));

export const refineHighImpactScreensTask = defineTask('refine-high-impact', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 5a: Refine high-impact screens (Transactions, Summary, Sign-In)',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android UI engineer specializing in premium Jetpack Compose screen design',
      task: 'Visually refine the three most visible screens in the budget manager app.',
      context: { androidDir: args.androidDir, designBrief: args.designBrief, designSystem: args.designSystem },
      instructions: [
        `Read these screen files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/screens/`,
        `Read the design system theme files for reference`,
        '',
        '1. TransactionListScreen.kt — The most-used screen:',
        '   - Clean header section with total balance card (prominent, large amount)',
        '   - Polished filter bar with well-styled chips',
        '   - Transaction list items with: category icon/color dot, description, date, and semantic-colored amount',
        '   - Proper spacing between items (not too dense, not too sparse)',
        '   - Improved FAB styling',
        '   - Pull-to-refresh visual polish',
        '   - Empty state when no transactions',
        '',
        '2. MonthlySummaryScreen.kt — Visual analytics:',
        '   - Clean month navigation with styled buttons',
        '   - Summary cards (income, expense, balance) as visually distinct, well-spaced cards',
        '   - Balance card with semantic color (green positive, red negative)',
        '   - Category breakdown as clean bar chart or styled list with progress bars',
        '   - Transaction count as a subtle detail',
        '   - Proper typography hierarchy throughout',
        '',
        '3. SignInScreen.kt — First impression:',
        '   - Refine the gradient background to use design system primary colors',
        '   - Polish app logo presentation',
        '   - Clean typography for app name and tagline',
        '   - Well-styled Google Sign-In and Guest buttons',
        '   - Proper spacing and visual balance',
        '',
        'Use design system tokens for ALL colors, typography, spacing, and shapes',
        'Ensure dark mode works well on all three screens',
        'Return summary'
      ],
      outputFormat: 'JSON with summary (string), filesModified (array)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: { summary: { type: 'string' }, filesModified: { type: 'array', items: { type: 'string' } } }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['screens', 'high-impact']
}));

export const refineSecondaryScreensTask = defineTask('refine-secondary', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 5b: Refine secondary screens (AddEdit, Recurring, Settings, Budgets, Migration)',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android UI engineer specializing in form design, settings screens, and Jetpack Compose',
      task: 'Visually refine all secondary screens in the budget manager app for consistency with the design system.',
      context: { androidDir: args.androidDir, designBrief: args.designBrief, designSystem: args.designSystem },
      instructions: [
        `Read ALL screen files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/screens/`,
        `Read the design system theme files for reference`,
        '',
        '1. AddEditTransactionScreen.kt:',
        '   - Clean form layout with proper field spacing',
        '   - Polished input fields with consistent styling',
        '   - Type toggle (Income/Expense) as styled segmented button',
        '   - Date picker field with calendar icon',
        '   - Save button as prominent filled button',
        '   - Proper top bar with clean back navigation',
        '',
        '2. RecurringScreen.kt:',
        '   - Clean list of recurring transactions with styled cards',
        '   - Each item: frequency badge, next occurrence, amount',
        '   - Active/inactive toggle polish',
        '   - Add/edit dialog with consistent form styling',
        '',
        '3. SettingsScreen.kt:',
        '   - Clean section headers',
        '   - Styled list items with proper icons',
        '   - Auth section with user avatar/info card',
        '   - Backup section with clear action buttons',
        '   - Consistent dividers and spacing',
        '',
        '4. BudgetManagementScreen.kt:',
        '   - Budget cards with currency badge, active indicator',
        '   - Clean FAB and dialog styling',
        '   - Empty state',
        '',
        '5. MigrationScreen.kt:',
        '   - Welcoming design with proper visual hierarchy',
        '   - Form fields consistent with the design system',
        '   - Progress/success states',
        '',
        '6. BudgetFormDialog.kt:',
        '   - Consistent dialog styling',
        '   - Clean form fields',
        '',
        'All screens must use design system tokens consistently',
        'Return summary'
      ],
      outputFormat: 'JSON with summary (string), filesModified (array)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: { summary: { type: 'string' }, filesModified: { type: 'array', items: { type: 'string' } } }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['screens', 'secondary']
}));

export const implementMotionTask = defineTask('implement-motion', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 6: Add tasteful motion and micro-interactions',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android motion designer and Compose animation specialist',
      task: 'Add subtle, meaningful motion and micro-interactions throughout the app.',
      context: { androidDir: args.androidDir, designBrief: args.designBrief },
      instructions: [
        `Read key screen and component files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/`,
        '',
        'Add tasteful motion (must be subtle, quick, meaningful, not distracting):',
        '',
        '1. Screen transitions: AnimatedNavHost or shared element transitions if Compose supports it',
        '2. List item animations: AnimatedVisibility for items appearing/disappearing',
        '3. FAB animation: slight scale/fade on scroll',
        '4. Card interactions: subtle pressed state feedback (scale or elevation change)',
        '5. Tab/bottom nav transitions: crossfade between sections',
        '6. Amount counter animation: animateFloatAsState for number changes in summary',
        '7. Filter chip selection: smooth color transition',
        '8. Swipe-to-delete: smooth dismiss animation',
        '9. Loading states: smooth fade transitions',
        '10. Budget selector dropdown: smooth expand/collapse',
        '',
        'Use Compose animation APIs: animateContentSize, AnimatedVisibility, animateColorAsState, etc.',
        'All animations should have 200-300ms duration max',
        'Use Material3 motion tokens (EaseInOutCubic, tween) where possible',
        'Return summary'
      ],
      outputFormat: 'JSON with summary (string), filesModified (array)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: { summary: { type: 'string' }, filesModified: { type: 'array', items: { type: 'string' } } }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['motion', 'animation']
}));

export const polishDarkModeTask = defineTask('polish-dark-mode', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 7: Polish dark mode',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android UI engineer specializing in dark mode design and Material Design 3 color systems',
      task: 'Polish dark mode across the entire app to ensure it feels intentional and elegant.',
      context: { androidDir: args.androidDir, designBrief: args.designBrief },
      instructions: [
        `Read the theme files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/theme/`,
        `Read ALL screen files to check dark mode rendering`,
        '',
        'Ensure dark mode quality:',
        '1. Surface hierarchy: Use tonal elevation (surface -> surfaceContainer -> surfaceContainerHigh)',
        '2. No harsh pure black backgrounds unless deliberate — use dark gray surfaces (e.g., #1C1B1F)',
        '3. Cards and surfaces have proper elevation differentiation',
        '4. Text contrast: onSurface and onSurfaceVariant have sufficient contrast',
        '5. Accent colors adjusted for dark backgrounds (slightly lighter/brighter)',
        '6. Income/expense colors readable on dark surfaces',
        '7. Bottom nav, top bar properly styled in dark',
        '8. Dialogs and bottom sheets have proper dark surface colors',
        '9. Input fields have visible borders/outlines in dark mode',
        '10. Empty state illustrations/icons look good on dark',
        '11. Sign-in screen gradient works in dark mode',
        '',
        'Fix any muddy contrast, harsh transitions, or forgotten light-mode-only styling',
        'Return summary'
      ],
      outputFormat: 'JSON with summary (string), filesModified (array)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: { summary: { type: 'string' }, filesModified: { type: 'array', items: { type: 'string' } } }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['dark-mode', 'polish']
}));

export const premiumPolishPassTask = defineTask('premium-polish', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 8: Final premium polish pass',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android product designer performing a final quality pass on a premium finance app',
      task: 'Do a final consistency and polish pass across the entire app, fixing any remaining visual issues.',
      context: { androidDir: args.androidDir, designBrief: args.designBrief },
      instructions: [
        `Read ALL UI files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/`,
        '',
        'Final polish checklist — fix any issues found:',
        '1. Padding consistency: all screen content uses consistent horizontal padding',
        '2. Corner radii: every card, button, dialog, chip uses design system radius tokens',
        '3. Typography: every text uses the correct MaterialTheme.typography style (no hardcoded sizes)',
        '4. Colors: every color references MaterialTheme.colorScheme (no hardcoded colors)',
        '5. Icon sizes: consistent 24.dp for actions, 20.dp for badges, 48.dp for empty states',
        '6. Divider usage: consistent or removed if not needed',
        '7. Row heights: list items have consistent minimum height',
        '8. Touch targets: minimum 48.dp for all interactive elements',
        '9. Pressed/disabled states: all buttons and interactive elements have proper state styling',
        '10. Form validation: error states use consistent error color and message placement',
        '11. Snackbar/toast styling: consistent with design system',
        '12. Dialog styling: consistent corner radius, padding, button placement',
        '13. Remove any TODO comments or placeholder styling',
        '14. Ensure no screen looks obviously inconsistent with the others',
        'Return summary of fixes applied'
      ],
      outputFormat: 'JSON with summary (string), filesModified (array), issuesFixed (array)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: {
        summary: { type: 'string' },
        filesModified: { type: 'array', items: { type: 'string' } },
        issuesFixed: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['polish', 'consistency']
}));

export const designQualityReviewTask = defineTask('design-quality-review', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Phase 9: Design quality review',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior product design reviewer and Android UI quality assessor',
      task: 'Review the complete visual redesign for quality, consistency, and adherence to the design brief.',
      context: { androidDir: args.androidDir, designBrief: args.designBrief },
      instructions: [
        `Read ALL UI files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/ui/`,
        'Verify the redesign against the brief:',
        '1. Does the app feel trustworthy, modern, clean, premium, calm?',
        '2. Is the color palette cohesive and used consistently?',
        '3. Is typography hierarchy clear and consistent?',
        '4. Are spacing and corner radii consistent?',
        '5. Do all screens follow the same visual language?',
        '6. Is dark mode polished?',
        '7. Are there any remaining generic/template-looking elements?',
        '8. Are empty/loading/error states handled?',
        '9. Is motion subtle and meaningful?',
        '10. Are there any accessibility concerns?',
        'Return passesQuality and any issues found'
      ],
      outputFormat: 'JSON with passesQuality (boolean), issues (array), summary (string)'
    },
    outputSchema: {
      type: 'object', required: ['passesQuality', 'summary'],
      properties: {
        passesQuality: { type: 'boolean' },
        issues: { type: 'array', items: { type: 'object' } },
        summary: { type: 'string' }
      }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['quality', 'review']
}));

export const fixDesignQualityIssuesTask = defineTask('fix-design-quality', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Fix design quality issues',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android UI engineer',
      task: 'Fix the design quality issues identified in the review.',
      context: { androidDir: args.androidDir, qualityResult: args.qualityResult },
      instructions: [
        'Fix each issue identified in the quality review',
        'Read affected files, apply fixes, ensure visual consistency',
        'Return summary'
      ],
      outputFormat: 'JSON with summary (string), filesModified (array)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: { summary: { type: 'string' }, filesModified: { type: 'array', items: { type: 'string' } } }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['quality', 'fix']
}));

export const buildAndVerifyTask = defineTask('build-and-verify', (args, taskCtx) => ({
  kind: 'agent',
  title: `Build & verify (${args.scope}, iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android build engineer',
      task: `Build the Android project and check for compilation errors. Scope: ${args.scope}, iteration ${args.iteration}.`,
      context: { androidDir: args.androidDir, scope: args.scope, iteration: args.iteration },
      instructions: [
        `cd ${args.androidDir} && ./gradlew assembleDebug 2>&1`,
        'If build succeeds, return allPassing: true',
        'If build fails, capture errors with file paths and line numbers',
        'Return complete error details'
      ],
      outputFormat: 'JSON with allPassing (boolean), errors (array), buildOutput (string)'
    },
    outputSchema: {
      type: 'object', required: ['allPassing'],
      properties: { allPassing: { type: 'boolean' }, errors: { type: 'array', items: { type: 'object' } }, buildOutput: { type: 'string' } }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['build', 'verification', args.scope]
}));

export const fixBuildFailuresTask = defineTask('fix-build-failures', (args, taskCtx) => ({
  kind: 'agent',
  title: `Fix build failures (${args.scope}, iteration ${args.iteration})`,
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Android developer and debugger',
      task: `Fix all build failures for scope: ${args.scope}.`,
      context: { androidDir: args.androidDir, scope: args.scope, buildResult: args.buildResult, iteration: args.iteration },
      instructions: [
        'Analyze build errors from buildResult',
        `Read failing files under ${args.androidDir}/app/src/main/java/com/budgetmanager/app/`,
        'Fix each error: missing imports, type mismatches, unresolved references, etc.',
        'Return summary of fixes'
      ],
      outputFormat: 'JSON with summary (string), fixesApplied (array), filesModified (array)'
    },
    outputSchema: {
      type: 'object', required: ['summary'],
      properties: { summary: { type: 'string' }, fixesApplied: { type: 'array', items: { type: 'object' } }, filesModified: { type: 'array', items: { type: 'string' } } }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['fix', 'debugging', args.scope]
}));

export const buildApkTask = defineTask('build-apk', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Build debug APK',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Android build engineer',
      task: 'Build the debug APK.',
      context: { androidDir: args.androidDir },
      instructions: [
        `cd ${args.androidDir} && ./gradlew assembleDebug 2>&1`,
        'Find APK path and return status'
      ],
      outputFormat: 'JSON with apkBuilt (boolean), apkPath (string), buildOutput (string)'
    },
    outputSchema: {
      type: 'object', required: ['apkBuilt'],
      properties: { apkBuilt: { type: 'boolean' }, apkPath: { type: 'string' }, buildOutput: { type: 'string' } }
    }
  },
  io: { inputJsonPath: `tasks/${taskCtx.effectId}/input.json`, outputJsonPath: `tasks/${taskCtx.effectId}/result.json` },
  labels: ['build', 'apk']
}));
