/**
 * @process ui-tdd
 * @description Build mobile-friendly web UI with TDD using Playwright for verification
 * @inputs { appName: string, tunnelUrl: string }
 * @outputs { success: boolean, phases: object }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const {
    appName = 'budget_manager',
    maxTestIterations = 4
  } = inputs;

  // ============================================================================
  // PHASE 1: WRITE E2E TESTS FIRST (TDD - Red)
  // ============================================================================

  const testsResult = await ctx.task(writeE2ETestsTask, {
    appName,
    apiEndpoints: [
      'POST /api/transactions',
      'GET /api/transactions',
      'GET /api/transactions/{id}',
      'PUT /api/transactions/{id}',
      'DELETE /api/transactions/{id}',
      'GET /api/summary/monthly'
    ]
  });

  // ============================================================================
  // PHASE 2: IMPLEMENT UI (TDD - Green)
  // ============================================================================

  const uiResult = await ctx.task(implementUITask, {
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

    const runResult = await ctx.task(runPlaywrightTestsTask, {
      appName,
      iteration
    });

    testHistory.push({
      iteration,
      result: runResult
    });

    allPassing = runResult.allPassing === true;

    if (!allPassing && iteration < maxTestIterations) {
      const fixResult = await ctx.task(fixUIIssuesTask, {
        appName,
        testResults: runResult,
        iteration
      });
    }
  }

  // ============================================================================
  // PHASE 4: VISUAL VERIFICATION + COMMIT
  // ============================================================================

  const finalResult = await ctx.task(visualVerifyAndCommitTask, {
    appName,
    testsPassing: allPassing,
    testIterations: iteration
  });

  return {
    success: allPassing && finalResult.committed === true,
    phases: {
      tests: testsResult,
      ui: uiResult,
      tddConvergence: { iterations: iteration, allPassing, history: testHistory },
      delivery: finalResult
    }
  };
}

// ============================================================================
// TASK DEFINITIONS
// ============================================================================

export const writeE2ETestsTask = defineTask('write-e2e-tests', (args, taskCtx) => ({
  kind: 'agent',
  title: 'TDD: Write Playwright E2E tests for UI',
  description: 'Write failing tests first that define expected UI behavior',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'senior frontend test engineer specializing in Playwright',
      task: 'Write Playwright E2E tests that define the expected behavior of a mobile-friendly budget manager UI',
      context: {
        appName: args.appName,
        apiEndpoints: args.apiEndpoints,
        projectDir: '/home/ubuntu/projects/budget_manager',
        serverUrl: 'http://localhost:8018'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Read the existing app source code in app/ to understand the API contract (schemas, routes, response formats)',
        'Create tests/test_ui_e2e.py using pytest and playwright (playwright.sync_api)',
        'The UI will be served at GET / on the same FastAPI server (http://localhost:8018)',
        'Write tests for these UI behaviors:',
        '  - test_homepage_loads: page loads with title "Budget Manager", has navigation/header',
        '  - test_add_income_transaction: fill form (type=income, amount=1000, category=Salary, description=Test salary, date=2026-03-01), submit, verify it appears in transaction list',
        '  - test_add_expense_transaction: fill form (type=expense, amount=50, category=Food, description=Lunch, date=2026-03-01), submit, verify it appears',
        '  - test_transaction_list_displays: after adding transactions, verify the list shows them with correct amounts and categories',
        '  - test_monthly_summary_displays: verify summary section shows total income, total expenses, net balance, and category breakdowns',
        '  - test_delete_transaction: click delete on a transaction, verify it is removed from the list',
        '  - test_mobile_responsive: set viewport to 375x667 (iPhone SE), verify page is usable and not overflowing',
        'Use a conftest.py fixture that: starts with a clean DB (delete all transactions via API before each test), creates a Playwright browser page',
        'Use data-testid attributes for reliable selectors (e.g., data-testid="add-form", data-testid="transaction-list", data-testid="summary-section")',
        'Actually write the test files to disk',
        'Return summary of tests written'
      ],
      outputFormat: 'JSON with testFiles (array), testCount (number), selectors (array of data-testid values used)'
    },
    outputSchema: {
      type: 'object',
      required: ['testFiles', 'testCount'],
      properties: {
        testFiles: { type: 'array', items: { type: 'string' } },
        testCount: { type: 'number' },
        selectors: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['tdd', 'tests', 'playwright']
}));

export const implementUITask = defineTask('implement-ui', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Implement mobile-friendly UI',
  description: 'Build the HTML/CSS/JS frontend served by FastAPI',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'senior full-stack developer specializing in mobile-first responsive web design',
      task: 'Build a beautiful, mobile-friendly single-page UI for the budget manager app',
      context: {
        appName: args.appName,
        tests: args.tests,
        projectDir: '/home/ubuntu/projects/budget_manager',
        serverUrl: 'http://localhost:8018'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Read the E2E test file to understand the exact data-testid selectors and UI expectations',
        'Read app/schemas.py and app/routers/ to understand the exact API contract',
        'Create app/static/index.html - a single-page app with:',
        '  - Mobile-first responsive design (works great on 375px iPhone up to desktop)',
        '  - Clean, modern look with a pleasant color scheme (think budget/finance - blues, greens)',
        '  - A header with app title "Budget Manager"',
        '  - An "Add Transaction" form (data-testid="add-form") with fields:',
        '    * Type selector (income/expense) with data-testid="type-select"',
        '    * Amount input with data-testid="amount-input"',
        '    * Category input with data-testid="category-input"',
        '    * Description input with data-testid="description-input"',
        '    * Date input with data-testid="date-input"',
        '    * Submit button with data-testid="submit-btn"',
        '  - A transactions list section (data-testid="transaction-list") showing all transactions',
        '    * Each transaction row with data-testid="transaction-row"',
        '    * Delete button on each row with data-testid="delete-btn"',
        '    * Show type icon/badge, amount (green for income, red for expense), category, description, date',
        '  - A monthly summary section (data-testid="summary-section") showing:',
        '    * data-testid="total-income", data-testid="total-expenses", data-testid="net-balance"',
        '    * Category breakdown list',
        '    * Month/year selector',
        '  - All API calls use fetch() to /api/transactions and /api/summary/monthly',
        '  - Auto-refresh transaction list and summary after add/delete',
        '  - Use vanilla HTML/CSS/JS (no frameworks needed)',
        '  - CSS should use flexbox/grid for layout, system fonts, rounded corners, subtle shadows',
        'Update app/main.py to serve the static HTML:',
        '  - Add: from fastapi.staticfiles import StaticFiles',
        '  - Add: from fastapi.responses import FileResponse',
        '  - Mount static files: app.mount("/static", StaticFiles(directory="app/static"), name="static")',
        '  - Change root endpoint to serve the HTML: return FileResponse("app/static/index.html")',
        'Create the app/static/ directory',
        'Actually write all files to disk',
        'Return summary of files created'
      ],
      outputFormat: 'JSON with filesCreated (array), filesModified (array), selectorsImplemented (array)'
    },
    outputSchema: {
      type: 'object',
      required: ['filesCreated'],
      properties: {
        filesCreated: { type: 'array', items: { type: 'string' } },
        filesModified: { type: 'array', items: { type: 'string' } },
        selectorsImplemented: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['implementation', 'ui', 'frontend']
}));

export const runPlaywrightTestsTask = defineTask('run-playwright-tests', (args, taskCtx) => ({
  kind: 'agent',
  title: `Run Playwright E2E tests (iteration ${args.iteration})`,
  description: 'Execute Playwright tests against the live UI',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Test runner and diagnostician',
      task: 'Run the Playwright E2E tests and report detailed results',
      context: {
        appName: args.appName,
        iteration: args.iteration,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'The FastAPI server is already running on http://localhost:8018',
        'Run: python -m pytest tests/test_ui_e2e.py -v --tb=short 2>&1',
        'Capture the complete output',
        'Parse results: total tests, passed, failed, errors',
        'If tests fail, analyze failure messages and provide specific, actionable feedback',
        'Report allPassing: true/false',
        'Return structured results'
      ],
      outputFormat: 'JSON with allPassing (boolean), totalTests (number), passed (number), failed (number), feedback (string or null), rawOutput (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['allPassing', 'totalTests', 'passed', 'failed'],
      properties: {
        allPassing: { type: 'boolean' },
        totalTests: { type: 'number' },
        passed: { type: 'number' },
        failed: { type: 'number' },
        feedback: { type: 'string' },
        rawOutput: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['testing', 'playwright', `iteration-${args.iteration}`]
}));

export const fixUIIssuesTask = defineTask('fix-ui-issues', (args, taskCtx) => ({
  kind: 'agent',
  title: `Fix UI issues (iteration ${args.iteration})`,
  description: 'Fix failing E2E tests by updating UI or test code',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'senior full-stack developer and debugger',
      task: 'Fix the failing Playwright E2E tests',
      context: {
        appName: args.appName,
        testResults: args.testResults,
        iteration: args.iteration,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Review the test failure output carefully',
        'Read the failing test code in tests/test_ui_e2e.py',
        'Read the UI code in app/static/index.html',
        'Read app/main.py for the static file serving setup',
        'Determine root cause: is the bug in the HTML/JS, in the test expectations, or in the app routing?',
        'Fix the root cause - prefer fixing the UI code to match the tests (TDD principle)',
        'Only fix tests if they have genuinely wrong expectations (wrong selectors, wrong API contract)',
        'After fixing, do a quick sanity check if possible',
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
  labels: ['fix', 'ui', `iteration-${args.iteration}`]
}));

export const visualVerifyAndCommitTask = defineTask('visual-verify-commit', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Visual verification + screenshots + commit',
  description: 'Take screenshots of final UI on mobile and desktop viewports, then commit',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'QA engineer doing final visual verification',
      task: 'Visually verify the budget manager UI and commit the code',
      context: {
        appName: args.appName,
        testsPassing: args.testsPassing,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'The server is running on http://localhost:8018',
        'Write a quick Python script using Playwright to:',
        '  1. Open the UI at http://localhost:8018 in desktop viewport (1280x900)',
        '  2. Add 3 sample transactions via the UI form (use page interactions, not API)',
        '  3. Take a screenshot -> screenshots/ui_desktop.png',
        '  4. Switch to mobile viewport (375x667)',
        '  5. Take a screenshot -> screenshots/ui_mobile.png',
        '  6. Navigate to show summary section',
        '  7. Take a screenshot -> screenshots/ui_summary.png',
        'Run the script to generate screenshots',
        'Stage all new/modified files and commit: "feat: add mobile-friendly web UI with E2E tests"',
        'Return summary with screenshot paths'
      ],
      outputFormat: 'JSON with screenshots (array of paths), committed (boolean), commitHash (string)'
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
