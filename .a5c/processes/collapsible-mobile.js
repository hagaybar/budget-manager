/**
 * @process collapsible-mobile
 * @description Make UI sections collapsible on mobile for easier usage
 * @inputs { appName: string }
 * @outputs { success: boolean, phases: object }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const {
    appName = 'budget_manager',
    maxTestIterations = 4
  } = inputs;

  // ============================================================================
  // PHASE 1: WRITE/UPDATE E2E TESTS (TDD Red)
  // ============================================================================

  const testsResult = await ctx.task(writeCollapsibleTestsTask, { appName });

  // ============================================================================
  // PHASE 2: IMPLEMENT COLLAPSIBLE SECTIONS
  // ============================================================================

  const implResult = await ctx.task(implementCollapsibleTask, {
    appName,
    tests: testsResult
  });

  // ============================================================================
  // PHASE 3: TDD CONVERGENCE - Run tests, fix, repeat
  // ============================================================================

  let iteration = 0;
  let allPassing = false;
  const testHistory = [];

  while (iteration < maxTestIterations && !allPassing) {
    iteration++;

    const runResult = await ctx.task(runTestsTask, { appName, iteration });
    testHistory.push({ iteration, result: runResult });
    allPassing = runResult.allPassing === true;

    if (!allPassing && iteration < maxTestIterations) {
      await ctx.task(fixIssuesTask, {
        appName,
        testResults: runResult,
        iteration
      });
    }
  }

  // ============================================================================
  // PHASE 4: VISUAL VERIFICATION + COMMIT
  // ============================================================================

  const finalResult = await ctx.task(verifyAndCommitTask, {
    appName,
    testsPassing: allPassing
  });

  return {
    success: allPassing && finalResult.committed === true,
    phases: {
      tests: testsResult,
      implementation: implResult,
      tddConvergence: { iterations: iteration, allPassing, history: testHistory },
      delivery: finalResult
    }
  };
}

// ============================================================================
// TASK DEFINITIONS
// ============================================================================

export const writeCollapsibleTestsTask = defineTask('write-collapsible-tests', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Write E2E tests for collapsible mobile sections',
  description: 'Add tests verifying collapsible behavior on mobile viewport',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior Playwright test engineer',
      task: 'Add E2E tests for collapsible card sections on mobile',
      context: {
        appName: args.appName,
        projectDir: '/home/ubuntu/projects/budget_manager',
        serverUrl: 'http://localhost:8018'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Read tests/test_ui_e2e.py to understand existing test patterns',
        'Read app/static/index.html to understand current UI structure',
        '',
        'IMPORTANT: Do NOT delete any existing data. Do NOT modify the database.',
        'IMPORTANT: Keep ALL existing tests working. Only ADD new tests.',
        '',
        'Add a new test class TestCollapsibleMobile to tests/test_ui_e2e.py with these tests:',
        '',
        '  test_sections_collapsible_on_mobile:',
        '    - Set viewport to 375x667 (mobile)',
        '    - Go to page',
        '    - Each card section has a clickable card-title that acts as a toggle',
        '    - Click the "Add Transaction" card-title header → the form should collapse/hide',
        '    - Click it again → the form should expand/show',
        '    - The card-title should have a visual chevron/arrow indicator (data-testid="collapse-toggle")',
        '',
        '  test_sections_expanded_on_desktop:',
        '    - Set viewport to 1280x900 (desktop)',
        '    - Go to page',
        '    - All sections should be fully visible (not collapsed)',
        '    - The collapse toggles should NOT be visible or sections should not be collapsible',
        '',
        '  test_collapse_does_not_delete_data:',
        '    - Set viewport to 375x667',
        '    - Add a transaction first at desktop size, then switch to mobile',
        '    - Collapse and expand the Transactions section',
        '    - Verify the transaction is still there after expanding',
        '',
        'Use data-testid attributes:',
        '  - data-testid="collapse-toggle" on the clickable header/chevron elements (one per card)',
        '  - The card body (form/content) gets a wrapper with data-testid="card-body-{section}" where section is: add-form, transactions, summary, recurring',
        '',
        'When testing collapse: check that the card body has display:none or is not visible',
        'When testing expand: check that the card body is visible',
        '',
        'CRITICAL: Ensure existing tests still pass. The existing tests use desktop viewport (1280x900) where everything should remain expanded.',
        '',
        'Actually write the updated test file to disk.',
        'Return summary of changes.'
      ],
      outputFormat: 'JSON with testFile (string), newTestCount (number), selectors (array)'
    },
    outputSchema: {
      type: 'object',
      required: ['testFile', 'newTestCount'],
      properties: {
        testFile: { type: 'string' },
        newTestCount: { type: 'number' },
        selectors: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['tdd', 'tests']
}));

export const implementCollapsibleTask = defineTask('implement-collapsible', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Implement collapsible mobile sections',
  description: 'Add CSS/JS for collapsible card sections on mobile',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior frontend developer specializing in mobile-first responsive UX',
      task: 'Implement collapsible card sections for mobile viewport in the Budget Manager UI',
      context: {
        appName: args.appName,
        tests: args.tests,
        projectDir: '/home/ubuntu/projects/budget_manager',
        serverUrl: 'http://localhost:8018'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Read tests/test_ui_e2e.py to understand the exact test expectations for collapsible behavior',
        'Read app/static/index.html to understand the current UI',
        '',
        'IMPORTANT: Do NOT delete any existing data. Do NOT modify any backend code.',
        'IMPORTANT: Only modify app/static/index.html',
        '',
        'Implement collapsible sections in app/static/index.html:',
        '',
        'BEHAVIOR:',
        '  - On MOBILE (viewport <= 480px): card sections are collapsible',
        '    * Each card-title becomes a clickable toggle',
        '    * A chevron/arrow icon (▼/▲ or CSS triangle) appears next to card titles',
        '    * Clicking the title toggles the card body visibility',
        '    * Use smooth CSS transition for collapse/expand (max-height or similar)',
        '    * By default on page load: "Transactions" and "Summary" sections START EXPANDED',
        '    * "Add Transaction" and "Recurring Transactions" sections can START COLLAPSED on mobile to save space',
        '    * Actually - to keep existing tests working, start ALL sections EXPANDED even on mobile',
        '  - On DESKTOP (viewport > 480px): sections are always expanded, chevrons hidden',
        '',
        'IMPLEMENTATION:',
        '  CSS:',
        '    - Add .card-title styles for clickable behavior on mobile (cursor: pointer)',
        '    - Add a .collapse-toggle element (chevron) inside each card-title, hidden on desktop',
        '    - Add .card-body wrapper class for the content of each card (after the title)',
        '    - Add .card-body.collapsed { max-height: 0; overflow: hidden; padding: 0; opacity: 0; }',
        '    - Add smooth transition: max-height 0.3s ease, opacity 0.2s ease, padding 0.3s ease',
        '    - On desktop @media: .card-body always visible, .collapse-toggle hidden',
        '',
        '  HTML:',
        '    - Wrap each card\'s content (everything after the card-title) in a div.card-body',
        '    - Add data-testid="card-body-add-form" / "card-body-transactions" / "card-body-summary" / "card-body-recurring"',
        '    - Add a span.collapse-toggle with data-testid="collapse-toggle" inside each card-title',
        '    - Use ▾ (expanded) and ▸ (collapsed) or ▼/▶ as the chevron',
        '',
        '  JavaScript:',
        '    - Add click handler on each .card-title (only active on mobile)',
        '    - Toggle .collapsed class on the adjacent .card-body',
        '    - Update chevron direction',
        '    - Check if viewport is mobile before toggling (respect desktop always-visible)',
        '    - Do NOT interfere with form submission or other click handlers',
        '',
        'CRITICAL:',
        '  - Do NOT break any existing functionality',
        '  - Do NOT modify any backend files',
        '  - Do NOT delete any data',
        '  - All existing E2E tests must continue to pass (they run at 1280x900 desktop)',
        '  - The data-testid attributes must match what the tests expect',
        '',
        'Actually write the updated file to disk.',
        'Return summary of changes.'
      ],
      outputFormat: 'JSON with filesModified (array), summary (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['filesModified'],
      properties: {
        filesModified: { type: 'array', items: { type: 'string' } },
        summary: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['implementation', 'ui']
}));

export const runTestsTask = defineTask('run-tests', (args, taskCtx) => ({
  kind: 'agent',
  title: `Run all tests (iteration ${args.iteration})`,
  description: 'Run unit + E2E tests and report results',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Test runner and diagnostician',
      task: 'Run all tests and report results',
      context: {
        appName: args.appName,
        iteration: args.iteration,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'The FastAPI server is running on http://localhost:8018 with --reload',
        '',
        'Run unit tests:',
        '  python -m pytest tests/test_transactions.py tests/test_summary.py tests/test_models.py tests/test_recurring.py -v --tb=short 2>&1',
        '',
        'Run E2E tests:',
        '  python -m pytest tests/test_ui_e2e.py -v --tb=short 2>&1',
        '',
        'Report allPassing: true only if ALL tests pass',
        'If failures, provide specific actionable feedback'
      ],
      outputFormat: 'JSON with allPassing (boolean), unitTests (object), e2eTests (object), feedback (string or null)'
    },
    outputSchema: {
      type: 'object',
      required: ['allPassing'],
      properties: {
        allPassing: { type: 'boolean' },
        unitTests: { type: 'object' },
        e2eTests: { type: 'object' },
        feedback: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['testing', `iteration-${args.iteration}`]
}));

export const fixIssuesTask = defineTask('fix-issues', (args, taskCtx) => ({
  kind: 'agent',
  title: `Fix issues (iteration ${args.iteration})`,
  description: 'Fix failing tests',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior frontend developer and debugger',
      task: 'Fix failing tests from the latest test run',
      context: {
        appName: args.appName,
        testResults: args.testResults,
        iteration: args.iteration,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Review the test failure output carefully',
        'Read the failing test code and the UI code',
        'IMPORTANT: Do NOT delete any existing data or modify backend',
        'IMPORTANT: Do NOT break existing tests when fixing new ones',
        'Fix the root cause - prefer fixing UI code to match tests (TDD principle)',
        'Only fix tests if they have genuinely wrong expectations',
        'Return what was fixed'
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
  labels: ['fix', `iteration-${args.iteration}`]
}));

export const verifyAndCommitTask = defineTask('verify-commit', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Visual verification + screenshots + commit',
  description: 'Take mobile screenshots showing collapsible sections and commit',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'QA engineer',
      task: 'Take screenshots of collapsible sections on mobile and commit',
      context: {
        appName: args.appName,
        testsPassing: args.testsPassing,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'The server is running on http://localhost:8018',
        '',
        'IMPORTANT: Do NOT delete any existing data in the database.',
        '',
        'Write a Playwright script to:',
        '  1. Open UI at http://localhost:8018 in mobile viewport (375x667)',
        '  2. Add 2 sample transactions if list is empty',
        '  3. Take screenshot showing expanded sections -> screenshots/mobile_expanded.png',
        '  4. Click the "Transactions" card title to collapse it',
        '  5. Click the "Recurring Transactions" card title to collapse it',
        '  6. Take screenshot showing collapsed sections -> screenshots/mobile_collapsed.png',
        '  7. Click "Transactions" again to expand',
        '  8. Take screenshot showing re-expanded -> screenshots/mobile_reexpanded.png',
        '  9. Switch to desktop viewport (1280x900)',
        '  10. Take screenshot showing desktop (always expanded) -> screenshots/desktop_full.png',
        '',
        'Run the script',
        'Stage all new/modified files and commit:',
        '  "feat: add collapsible sections on mobile for easier usage"',
        'Return summary with screenshot paths'
      ],
      outputFormat: 'JSON with screenshots (array), committed (boolean), commitHash (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['screenshots', 'committed'],
      properties: {
        screenshots: { type: 'array', items: { type: 'string' } },
        committed: { type: 'boolean' },
        commitHash: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['visual-verify', 'commit', 'final']
}));
