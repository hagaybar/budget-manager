/**
 * @process currency-and-recurring
 * @description Change currency to ILS (₪) and add recurring transactions feature with TDD
 * @inputs { appName: string }
 * @outputs { success: boolean, phases: object }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

export async function process(inputs, ctx) {
  const {
    appName = 'budget_manager',
    maxTestIterations = 5
  } = inputs;

  // ============================================================================
  // PHASE 1: PLAN - Analyze codebase & design schema/API changes
  // ============================================================================

  const planResult = await ctx.task(planChangesTask, {
    appName,
    features: [
      'Change all currency display from $ (USD) to ₪ (ILS / ש"ח)',
      'Add recurring transactions: weekly (specific day) or monthly (specific date)',
      'Recurring transactions have: type, amount, category, description, frequency, day_of_week/day_of_month, start_date, end_date (optional/nullable)',
      'Auto-generate transaction instances from recurring definitions',
      'UI for managing recurring transactions'
    ]
  });

  // ============================================================================
  // PHASE 2: WRITE TESTS FIRST (TDD - Red)
  // ============================================================================

  const testsResult = await ctx.task(writeTestsTask, {
    appName,
    plan: planResult
  });

  // ============================================================================
  // PHASE 3: IMPLEMENT CHANGES (TDD - Green)
  // ============================================================================

  const implResult = await ctx.task(implementChangesTask, {
    appName,
    plan: planResult,
    tests: testsResult
  });

  // ============================================================================
  // PHASE 4: TDD CONVERGENCE - Run tests, fix, repeat
  // ============================================================================

  let iteration = 0;
  let allPassing = false;
  const testHistory = [];

  while (iteration < maxTestIterations && !allPassing) {
    iteration++;

    const runResult = await ctx.task(runAllTestsTask, {
      appName,
      iteration
    });

    testHistory.push({
      iteration,
      result: runResult
    });

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
  // PHASE 5: VISUAL VERIFICATION + COMMIT
  // ============================================================================

  const finalResult = await ctx.task(visualVerifyAndCommitTask, {
    appName,
    testsPassing: allPassing,
    testIterations: iteration
  });

  return {
    success: allPassing && finalResult.committed === true,
    phases: {
      plan: planResult,
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

export const planChangesTask = defineTask('plan-changes', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Plan currency change and recurring transactions feature',
  description: 'Analyze existing code and design schema/API/UI changes',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior software architect specializing in Python/FastAPI applications',
      task: 'Plan the implementation of currency change ($ to ₪) and recurring transactions feature',
      context: {
        appName: args.appName,
        features: args.features,
        projectDir: '/home/ubuntu/projects/budget_manager',
        serverUrl: 'http://localhost:8018'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Read ALL existing source files to understand current architecture:',
        '  - app/database.py (DB schema)',
        '  - app/schemas.py (Pydantic models)',
        '  - app/routers/transactions.py (CRUD endpoints)',
        '  - app/routers/summary.py (monthly summary)',
        '  - app/main.py (app setup)',
        '  - app/static/index.html (UI)',
        '  - tests/test_ui_e2e.py (E2E tests)',
        '  - tests/test_transactions.py, tests/test_summary.py, tests/test_models.py (unit tests)',
        '',
        'Design these changes:',
        '',
        '1. CURRENCY CHANGE ($ → ₪):',
        '   - All UI display of currency must change from $ to ₪',
        '   - The locale formatting should use he-IL for number formatting',
        '   - Backend stores amounts as numbers - no backend currency change needed',
        '   - Update all test assertions that check for $ symbol',
        '',
        '2. RECURRING TRANSACTIONS:',
        '   Database: New table `recurring_transactions` with columns:',
        '     - id INTEGER PRIMARY KEY AUTOINCREMENT',
        '     - type TEXT NOT NULL (income/expense)',
        '     - amount REAL NOT NULL > 0',
        '     - category TEXT NOT NULL',
        '     - description TEXT DEFAULT empty',
        '     - frequency TEXT NOT NULL (weekly/monthly)',
        '     - day_of_week INTEGER (0=Monday..6=Sunday, for weekly)',
        '     - day_of_month INTEGER (1-31, for monthly)',
        '     - start_date TEXT (YYYY-MM-DD, optional - defaults to today)',
        '     - end_date TEXT (YYYY-MM-DD, nullable/optional - null means no end)',
        '     - is_active INTEGER DEFAULT 1',
        '     - created_at TEXT DEFAULT datetime(now)',
        '',
        '   API Endpoints:',
        '     - POST /api/recurring - create recurring transaction definition',
        '     - GET /api/recurring - list all recurring definitions',
        '     - GET /api/recurring/{id} - get single recurring definition',
        '     - PUT /api/recurring/{id} - update recurring definition',
        '     - DELETE /api/recurring/{id} - delete recurring definition',
        '     - POST /api/recurring/{id}/generate - manually generate transactions from a recurring def for a date range',
        '',
        '   Pydantic Schemas:',
        '     - RecurringTransactionCreate, RecurringTransactionUpdate, RecurringTransactionResponse',
        '',
        '   Generation Logic:',
        '     - When monthly summary is requested, auto-include generated occurrences from active recurring definitions',
        '     - OR: a generate endpoint that materializes transactions into the transactions table',
        '     - Prefer materialization approach: generate actual transaction records so they show in existing list/summary',
        '     - Track which transactions came from recurring (add recurring_id column to transactions table)',
        '',
        '   UI Changes:',
        '     - Add a "Recurring Transactions" section with its own form and list',
        '     - Form: type, amount, category, description, frequency (weekly/monthly), day selector, start date, end date (optional)',
        '     - List shows all recurring definitions with edit/delete/generate buttons',
        '     - Visual indicator on generated transactions showing they came from a recurring rule',
        '',
        'Write the plan to ENHANCEMENT_PLAN.md in the project root',
        'Return a summary of the planned changes'
      ],
      outputFormat: 'JSON with planFile (string), dbChanges (array), apiChanges (array), uiChanges (array), testChanges (array)'
    },
    outputSchema: {
      type: 'object',
      required: ['planFile'],
      properties: {
        planFile: { type: 'string' },
        dbChanges: { type: 'array', items: { type: 'string' } },
        apiChanges: { type: 'array', items: { type: 'string' } },
        uiChanges: { type: 'array', items: { type: 'string' } },
        testChanges: { type: 'array', items: { type: 'string' } }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['planning', 'architecture']
}));

export const writeTestsTask = defineTask('write-tests', (args, taskCtx) => ({
  kind: 'agent',
  title: 'TDD: Write tests for currency change and recurring transactions',
  description: 'Write failing tests first that define expected behavior',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior test engineer specializing in Python/FastAPI and Playwright',
      task: 'Write comprehensive tests for the currency change and recurring transactions feature',
      context: {
        appName: args.appName,
        plan: args.plan,
        projectDir: '/home/ubuntu/projects/budget_manager',
        serverUrl: 'http://localhost:8018'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Read the ENHANCEMENT_PLAN.md to understand the full plan',
        'Read ALL existing test files to understand current test patterns and fixtures',
        'Read ALL existing source files to understand the API contract',
        '',
        'UPDATE existing tests for currency change:',
        '  - tests/test_ui_e2e.py: Change all $ references to ₪ in assertions',
        '  - Any test that checks currency display format',
        '',
        'CREATE new test file tests/test_recurring.py with these test cases:',
        '  API Tests:',
        '  - test_create_weekly_recurring: POST /api/recurring with weekly frequency',
        '  - test_create_monthly_recurring: POST /api/recurring with monthly frequency',
        '  - test_create_recurring_no_end_date: end_date is null/omitted',
        '  - test_create_recurring_with_end_date: end_date is provided',
        '  - test_create_recurring_with_start_date: start_date is provided',
        '  - test_create_recurring_defaults_start_to_today: start_date omitted defaults to today',
        '  - test_list_recurring: GET /api/recurring returns all definitions',
        '  - test_get_recurring_by_id: GET /api/recurring/{id}',
        '  - test_update_recurring: PUT /api/recurring/{id}',
        '  - test_delete_recurring: DELETE /api/recurring/{id}',
        '  - test_generate_transactions_from_weekly: generate transactions for a date range from weekly recurring',
        '  - test_generate_transactions_from_monthly: generate transactions for a date range from monthly recurring',
        '  - test_generate_respects_end_date: no transactions generated after end_date',
        '  - test_generate_respects_start_date: no transactions generated before start_date',
        '  - test_invalid_frequency: reject invalid frequency value',
        '  - test_invalid_day_of_week: day_of_week out of 0-6 range',
        '  - test_invalid_day_of_month: day_of_month out of 1-31 range',
        '  - test_recurring_not_found: 404 for non-existent recurring',
        '',
        'UPDATE tests/test_ui_e2e.py to add recurring UI tests:',
        '  - test_recurring_section_visible: recurring section exists on page',
        '  - test_add_weekly_recurring_via_ui: fill recurring form, submit, verify in list',
        '  - test_add_monthly_recurring_via_ui: fill monthly recurring form, submit, verify',
        '  - test_delete_recurring_via_ui: delete a recurring definition',
        '  - test_currency_displays_shekel: verify ₪ symbol appears instead of $',
        '',
        'Use existing conftest.py patterns (temp DB, TestClient) for API tests',
        'Use existing Playwright patterns for E2E tests',
        'Use data-testid attributes for new UI elements:',
        '  - data-testid="recurring-section"',
        '  - data-testid="recurring-form"',
        '  - data-testid="recurring-frequency"',
        '  - data-testid="recurring-day-of-week"',
        '  - data-testid="recurring-day-of-month"',
        '  - data-testid="recurring-start-date"',
        '  - data-testid="recurring-end-date"',
        '  - data-testid="recurring-submit-btn"',
        '  - data-testid="recurring-list"',
        '  - data-testid="recurring-row"',
        '  - data-testid="recurring-delete-btn"',
        '',
        'Actually write all test files to disk',
        'Return summary of tests written'
      ],
      outputFormat: 'JSON with testFiles (array), newTestCount (number), updatedTestCount (number), selectors (array)'
    },
    outputSchema: {
      type: 'object',
      required: ['testFiles', 'newTestCount'],
      properties: {
        testFiles: { type: 'array', items: { type: 'string' } },
        newTestCount: { type: 'number' },
        updatedTestCount: { type: 'number' },
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

export const implementChangesTask = defineTask('implement-changes', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Implement currency change and recurring transactions',
  description: 'Implement all backend and frontend changes to pass the tests',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior full-stack developer specializing in Python/FastAPI and mobile-first web design',
      task: 'Implement currency change ($ → ₪) and recurring transactions feature to pass all tests',
      context: {
        appName: args.appName,
        plan: args.plan,
        tests: args.tests,
        projectDir: '/home/ubuntu/projects/budget_manager',
        serverUrl: 'http://localhost:8018'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Read ENHANCEMENT_PLAN.md for the full implementation plan',
        'Read ALL test files (especially new/updated ones) to understand exact expectations',
        'Read ALL existing source files to understand current implementation',
        '',
        'IMPLEMENT these changes:',
        '',
        '1. DATABASE (app/database.py):',
        '   - Add recurring_transactions table to create_tables()',
        '   - Add recurring_id column to transactions table (nullable, references recurring_transactions.id)',
        '   - Ensure backward compatibility - existing transactions without recurring_id stay valid',
        '',
        '2. SCHEMAS (app/schemas.py):',
        '   - Add RecurringTransactionCreate schema:',
        '     * type: Literal["income", "expense"]',
        '     * amount: float > 0',
        '     * category: str (1-100 chars)',
        '     * description: str (optional, max 500)',
        '     * frequency: Literal["weekly", "monthly"]',
        '     * day_of_week: Optional[int] (0-6, required if frequency=weekly)',
        '     * day_of_month: Optional[int] (1-31, required if frequency=monthly)',
        '     * start_date: Optional[str] (YYYY-MM-DD, defaults to today)',
        '     * end_date: Optional[str] (YYYY-MM-DD, nullable = no end)',
        '   - Add RecurringTransactionUpdate schema (all optional)',
        '   - Add RecurringTransactionResponse schema',
        '   - Add validators: day_of_week required when frequency=weekly, day_of_month required when frequency=monthly',
        '',
        '3. ROUTER (app/routers/recurring.py - NEW FILE):',
        '   - POST /api/recurring - create recurring definition',
        '   - GET /api/recurring - list all recurring definitions',
        '   - GET /api/recurring/{id} - get one',
        '   - PUT /api/recurring/{id} - update',
        '   - DELETE /api/recurring/{id} - delete (also delete generated transactions)',
        '   - POST /api/recurring/{id}/generate - generate transactions for given date range',
        '     * Query params: start_date, end_date',
        '     * Calculates all occurrences between start_date and end_date',
        '     * For weekly: every occurrence of day_of_week in range',
        '     * For monthly: every occurrence of day_of_month in range',
        '     * Creates actual transaction records with recurring_id set',
        '     * Avoids duplicates (skip dates that already have a transaction from this recurring_id)',
        '',
        '4. UPDATE app/main.py:',
        '   - Import and include the recurring router',
        '',
        '5. UPDATE app/routers/transactions.py:',
        '   - Add recurring_id to transaction response if present',
        '',
        '6. UI (app/static/index.html):',
        '   Currency Change:',
        '   - Replace ALL "$" with "₪" in currency display',
        '   - Change locale from en-US to he-IL for number formatting (or keep comma formatting with ₪ prefix)',
        '   - Update formatAmount() function',
        '',
        '   Recurring Transactions Section:',
        '   - Add a "Recurring Transactions" section (data-testid="recurring-section")',
        '   - Add form (data-testid="recurring-form") with fields:',
        '     * Type select (reuse pattern from add-form)',
        '     * Amount input',
        '     * Category input',
        '     * Description input',
        '     * Frequency select (data-testid="recurring-frequency"): weekly/monthly',
        '     * Day of week select (data-testid="recurring-day-of-week"): Mon-Sun (show/hide based on frequency)',
        '     * Day of month input (data-testid="recurring-day-of-month"): 1-31 (show/hide based on frequency)',
        '     * Start date input (data-testid="recurring-start-date")',
        '     * End date input (data-testid="recurring-end-date") - can be empty',
        '     * Submit button (data-testid="recurring-submit-btn")',
        '   - Recurring list (data-testid="recurring-list") showing all definitions',
        '     * Each row with data-testid="recurring-row"',
        '     * Delete button with data-testid="recurring-delete-btn"',
        '     * Show frequency, day, amount, category, date range',
        '   - Style consistently with existing UI (same color scheme, responsive)',
        '   - Mobile-friendly layout',
        '',
        'Actually write all files to disk',
        'Make sure the FastAPI server can restart cleanly with the changes',
        'Return summary of changes'
      ],
      outputFormat: 'JSON with filesCreated (array), filesModified (array), summary (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['filesCreated', 'filesModified'],
      properties: {
        filesCreated: { type: 'array', items: { type: 'string' } },
        filesModified: { type: 'array', items: { type: 'string' } },
        summary: { type: 'string' }
      }
    }
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },
  labels: ['implementation', 'backend', 'frontend']
}));

export const runAllTestsTask = defineTask('run-all-tests', (args, taskCtx) => ({
  kind: 'agent',
  title: `Run all tests (iteration ${args.iteration})`,
  description: 'Run both unit tests and Playwright E2E tests',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Test runner and diagnostician',
      task: 'Run all tests (unit + E2E) and report detailed results',
      context: {
        appName: args.appName,
        iteration: args.iteration,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'The FastAPI server is already running on http://localhost:8018',
        '',
        'Step 1: Run unit tests:',
        '  python -m pytest tests/test_transactions.py tests/test_summary.py tests/test_models.py tests/test_recurring.py -v --tb=short 2>&1',
        '',
        'Step 2: Run E2E tests:',
        '  python -m pytest tests/test_ui_e2e.py -v --tb=short 2>&1',
        '',
        'Capture complete output from both runs',
        'Parse results: total tests, passed, failed, errors for each',
        'If tests fail, provide specific actionable feedback about what is wrong',
        'Check if the server needs to be restarted (if code changes require reload)',
        'Report allPassing: true only if ALL tests pass (both unit and E2E)',
        'Return structured results'
      ],
      outputFormat: 'JSON with allPassing (boolean), unitTests (object with total/passed/failed), e2eTests (object with total/passed/failed), feedback (string or null), rawOutput (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['allPassing'],
      properties: {
        allPassing: { type: 'boolean' },
        unitTests: { type: 'object' },
        e2eTests: { type: 'object' },
        feedback: { type: 'string' },
        rawOutput: { type: 'string' }
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
  description: 'Fix failing tests by updating implementation or test expectations',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior full-stack developer and debugger',
      task: 'Fix all failing tests from the latest test run',
      context: {
        appName: args.appName,
        testResults: args.testResults,
        iteration: args.iteration,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Review the test failure output carefully',
        'Read the failing test code',
        'Read the relevant source code (routers, schemas, database, UI)',
        '',
        'Determine root cause for each failure:',
        '  - Is it a bug in the implementation? Fix the implementation.',
        '  - Is it a wrong test expectation? Fix the test (only if genuinely wrong).',
        '  - Is it a missing feature? Implement the missing piece.',
        '  - Is it a server restart needed? Note this.',
        '',
        'TDD principle: prefer fixing implementation to match tests',
        'Only fix tests if they have genuinely wrong expectations',
        '',
        'After fixing, verify the fix makes sense logically',
        'Return what was fixed and why'
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

export const visualVerifyAndCommitTask = defineTask('visual-verify-commit', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Visual verification + screenshots + commit',
  description: 'Take screenshots of updated UI and commit all changes',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'QA engineer doing final visual verification',
      task: 'Visually verify the updated budget manager UI (₪ currency, recurring transactions) and commit',
      context: {
        appName: args.appName,
        testsPassing: args.testsPassing,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'The server is running on http://localhost:8018',
        '',
        'Write a Python script using Playwright to:',
        '  1. Open the UI at http://localhost:8018 in desktop viewport (1280x900)',
        '  2. Add 2 sample transactions via the form (verify ₪ currency display)',
        '  3. Take screenshot -> screenshots/currency_desktop.png',
        '  4. Add a weekly recurring transaction via the recurring form',
        '  5. Add a monthly recurring transaction via the recurring form',
        '  6. Take screenshot -> screenshots/recurring_desktop.png',
        '  7. Switch to mobile viewport (375x667)',
        '  8. Take screenshot -> screenshots/recurring_mobile.png',
        '',
        'Run the script to generate screenshots',
        'Stage all new/modified files and commit with message:',
        '  "feat: change currency to ILS (₪) and add recurring transactions"',
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
