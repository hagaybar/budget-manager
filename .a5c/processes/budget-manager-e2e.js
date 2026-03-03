/**
 * @process budget-manager-e2e
 * @description End-to-end budget manager app: Plan → Implement → Test (convergence) → Deliver
 * @inputs { appName: string, stack: string, requirements: array }
 * @outputs { success: boolean, phases: object }
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

/**
 * Budget Manager E2E Process
 *
 * Phase 1: Architecture & Planning → TASK_PLAN.md
 * Phase 2: Scaffold + Implement all code
 * Phase 3: Test with convergence loop (write tests → run → fix → re-run)
 * Phase 4: Delivery (README + final_delivery commit)
 */
export async function process(inputs, ctx) {
  const {
    appName = 'budget_manager',
    stack = 'Python/FastAPI/SQLite',
    requirements = [],
    targetTestPasses = true,
    maxTestIterations = 4
  } = inputs;

  // ============================================================================
  // PHASE 1: ARCHITECTURE & PLANNING
  // ============================================================================

  const planResult = await ctx.task(architecturePlanningTask, {
    appName,
    stack,
    requirements
  });

  // ============================================================================
  // PHASE 2: PROJECT SCAFFOLDING & IMPLEMENTATION
  // ============================================================================

  const scaffoldResult = await ctx.task(scaffoldProjectTask, {
    appName,
    stack,
    plan: planResult
  });

  const implementResult = await ctx.task(implementAppTask, {
    appName,
    stack,
    plan: planResult,
    scaffold: scaffoldResult
  });

  // ============================================================================
  // PHASE 3: TESTING WITH CONVERGENCE LOOP
  // ============================================================================

  let testIteration = 0;
  let allTestsPassing = false;
  const testHistory = [];

  while (testIteration < maxTestIterations && !allTestsPassing) {
    testIteration++;

    // Write or update tests
    const writeTestsResult = await ctx.task(writeTestsTask, {
      appName,
      stack,
      plan: planResult,
      iteration: testIteration,
      previousFeedback: testIteration > 1 ? testHistory[testHistory.length - 1].feedback : null
    });

    // Run tests and capture results
    const runTestsResult = await ctx.task(runTestsTask, {
      appName,
      iteration: testIteration
    });

    testHistory.push({
      iteration: testIteration,
      testsWritten: writeTestsResult,
      testResults: runTestsResult,
      feedback: runTestsResult.feedback || null
    });

    allTestsPassing = runTestsResult.allPassing === true;

    // If tests are failing, fix the code
    if (!allTestsPassing && testIteration < maxTestIterations) {
      const fixResult = await ctx.task(fixFailuresTask, {
        appName,
        stack,
        plan: planResult,
        testResults: runTestsResult,
        iteration: testIteration
      });
    }
  }

  // ============================================================================
  // PHASE 4: DELIVERY
  // ============================================================================

  const deliveryResult = await ctx.task(deliverProjectTask, {
    appName,
    stack,
    plan: planResult,
    testsPassing: allTestsPassing,
    testIterations: testIteration
  });

  return {
    success: allTestsPassing && deliveryResult.committed === true,
    appName,
    stack,
    phases: {
      planning: { completed: true, plan: planResult },
      implementation: { completed: true, scaffold: scaffoldResult, implementation: implementResult },
      testing: { completed: true, iterations: testIteration, allPassing: allTestsPassing, history: testHistory },
      delivery: { completed: true, result: deliveryResult }
    },
    metadata: {
      processId: 'budget-manager-e2e',
      timestamp: ctx.now()
    }
  };
}

// ============================================================================
// TASK DEFINITIONS
// ============================================================================

/**
 * Phase 1: Architecture & Planning
 * Creates TASK_PLAN.md with architecture, schema, and API endpoints
 */
export const architecturePlanningTask = defineTask('architecture-planning', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Architecture & Planning → TASK_PLAN.md',
  description: 'Design architecture, database schema, and API endpoints for the budget manager app',

  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'senior Python backend architect',
      task: `Design the complete architecture for a "${args.appName}" application using ${args.stack}. Create a TASK_PLAN.md file in the project root with the full plan.`,
      context: {
        appName: args.appName,
        stack: args.stack,
        requirements: args.requirements,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Create a comprehensive TASK_PLAN.md file at /home/ubuntu/projects/budget_manager/TASK_PLAN.md',
        'Include: Project overview, tech stack decisions (FastAPI + SQLite + Pydantic)',
        'Define SQLite database schema with tables: transactions (id, type[income/expense], amount, category, description, date, created_at)',
        'Define all REST API endpoints: POST/GET/PUT/DELETE /api/transactions, GET /api/summary/monthly',
        'Define Pydantic models for request/response schemas',
        'Define project directory structure',
        'Include monthly summary logic specification: filter by year/month, aggregate income vs expenses, calculate net balance, category breakdowns',
        'The plan must be complete and actionable - another developer should be able to implement from it alone',
        'Actually write the file to disk using your tools - do not just describe it',
        'Return a JSON summary of the plan including the list of API endpoints, database tables, and key files to create'
      ],
      outputFormat: 'JSON with endpoints (array), tables (array), models (array), fileStructure (array), summary (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['endpoints', 'tables', 'summary'],
      properties: {
        endpoints: { type: 'array', items: { type: 'string' } },
        tables: { type: 'array', items: { type: 'string' } },
        models: { type: 'array', items: { type: 'string' } },
        fileStructure: { type: 'array', items: { type: 'string' } },
        summary: { type: 'string' }
      }
    }
  },

  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },

  labels: ['planning', 'architecture']
}));

/**
 * Phase 2a: Scaffold the project
 */
export const scaffoldProjectTask = defineTask('scaffold-project', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Scaffold project structure',
  description: 'Initialize git repo, create directory structure, requirements.txt, pyproject.toml',

  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'senior Python developer',
      task: `Scaffold the "${args.appName}" project at /home/ubuntu/projects/budget_manager`,
      context: {
        appName: args.appName,
        stack: args.stack,
        plan: args.plan,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Initialize a git repository with git init',
        'Create a .gitignore for Python projects (include __pycache__, *.pyc, .venv, *.db, .a5c/)',
        'Create requirements.txt with: fastapi, uvicorn[standard], pydantic, pytest, pytest-cov, httpx (for test client)',
        'Create the directory structure: app/ (with __init__.py, main.py, models.py, schemas.py, database.py, routers/), tests/ (with __init__.py, conftest.py)',
        'Create a minimal app/__init__.py and tests/__init__.py',
        'Install dependencies using pip install -r requirements.txt',
        'Create the initial commit',
        'Actually create all files on disk - do not just describe them',
        'Return a summary of files created and commands run'
      ],
      outputFormat: 'JSON with filesCreated (array of paths), gitInitialized (boolean), dependenciesInstalled (boolean)'
    },
    outputSchema: {
      type: 'object',
      required: ['filesCreated', 'gitInitialized'],
      properties: {
        filesCreated: { type: 'array', items: { type: 'string' } },
        gitInitialized: { type: 'boolean' },
        dependenciesInstalled: { type: 'boolean' }
      }
    }
  },

  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },

  labels: ['scaffold', 'setup']
}));

/**
 * Phase 2b: Implement the full application
 */
export const implementAppTask = defineTask('implement-app', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Implement full budget manager application',
  description: 'Write all application code: models, schemas, routes, database, main app',

  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'senior Python/FastAPI developer',
      task: `Implement the complete "${args.appName}" application based on the architecture plan`,
      context: {
        appName: args.appName,
        stack: args.stack,
        plan: args.plan,
        scaffold: args.scaffold,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Read the TASK_PLAN.md for the complete architecture specification',
        'Implement app/database.py: SQLite connection using sqlite3 stdlib, create_tables() function, get_db() dependency',
        'Implement app/models.py: Transaction dataclass/model with fields: id, type (income/expense), amount (float), category (str), description (str), date (str YYYY-MM-DD), created_at (auto)',
        'Implement app/schemas.py: Pydantic models - TransactionCreate, TransactionUpdate, TransactionResponse, MonthlySummary, CategoryBreakdown',
        'Implement app/routers/transactions.py: Full CRUD router - POST /api/transactions, GET /api/transactions (with optional filters), GET /api/transactions/{id}, PUT /api/transactions/{id}, DELETE /api/transactions/{id}',
        'Implement app/routers/summary.py: GET /api/summary/monthly?year=YYYY&month=MM - returns total income, total expenses, net balance, category breakdowns',
        'Implement app/main.py: FastAPI app instance, include routers, startup event to create tables, CORS middleware',
        'Use proper error handling with HTTPException for 404s and validation errors',
        'Ensure all database operations use parameterized queries (no SQL injection)',
        'Actually write all code to disk using your tools',
        'Commit the implementation with message "feat: implement budget manager core"',
        'Return a summary of all files implemented'
      ],
      outputFormat: 'JSON with filesCreated (array), filesModified (array), endpointsImplemented (array), committed (boolean)'
    },
    outputSchema: {
      type: 'object',
      required: ['filesCreated', 'endpointsImplemented'],
      properties: {
        filesCreated: { type: 'array', items: { type: 'string' } },
        filesModified: { type: 'array', items: { type: 'string' } },
        endpointsImplemented: { type: 'array', items: { type: 'string' } },
        committed: { type: 'boolean' }
      }
    }
  },

  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },

  labels: ['implementation', 'core']
}));

/**
 * Phase 3a: Write tests
 */
export const writeTestsTask = defineTask('write-tests', (args, taskCtx) => ({
  kind: 'agent',
  title: `Write tests (iteration ${args.iteration})`,
  description: 'Write comprehensive unit and integration tests for the budget manager',

  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'senior Python test engineer',
      task: `Write comprehensive tests for the "${args.appName}" application (iteration ${args.iteration})`,
      context: {
        appName: args.appName,
        stack: args.stack,
        plan: args.plan,
        iteration: args.iteration,
        previousFeedback: args.previousFeedback,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Read the existing source code in app/ to understand the implementation',
        'Create tests/conftest.py with pytest fixtures: test database (in-memory or temp file), FastAPI TestClient using httpx',
        'Create tests/test_transactions.py: Test all CRUD operations - create income, create expense, get all, get by id, update, delete, get nonexistent (404)',
        'Create tests/test_summary.py: Test monthly summary - add multiple transactions across months, verify monthly aggregation, verify category breakdowns, verify net balance calculation',
        'Create tests/test_models.py: Test Pydantic schema validation - valid inputs, invalid inputs, missing required fields',
        'Target 100% coverage of core business logic (models, schemas, routes, database)',
        'If this is iteration > 1, review the previous feedback and fix/add tests accordingly',
        'Actually write all test files to disk',
        'Return a summary of test files created and test count'
      ],
      outputFormat: 'JSON with testFiles (array of paths), testCount (number), coverageTargets (array)'
    },
    outputSchema: {
      type: 'object',
      required: ['testFiles', 'testCount'],
      properties: {
        testFiles: { type: 'array', items: { type: 'string' } },
        testCount: { type: 'number' },
        coverageTargets: { type: 'array', items: { type: 'string' } }
      }
    }
  },

  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },

  labels: ['testing', 'write-tests', `iteration-${args.iteration}`]
}));

/**
 * Phase 3b: Run tests
 */
export const runTestsTask = defineTask('run-tests', (args, taskCtx) => ({
  kind: 'agent',
  title: `Run tests (iteration ${args.iteration})`,
  description: 'Execute pytest with coverage and report results',

  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Python test runner and diagnostician',
      task: `Run the test suite for "${args.appName}" and report results`,
      context: {
        appName: args.appName,
        iteration: args.iteration,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Run: python -m pytest tests/ -v --tb=short --cov=app --cov-report=term-missing 2>&1',
        'Capture the full output',
        'Parse the results: total tests, passed, failed, errors, coverage percentage',
        'If tests fail, analyze the failure messages and provide specific feedback on what needs fixing',
        'Report whether ALL tests pass (allPassing: true/false)',
        'If there are import errors or missing dependencies, note them in the feedback',
        'Return structured results'
      ],
      outputFormat: 'JSON with allPassing (boolean), totalTests (number), passed (number), failed (number), errors (number), coveragePercent (number), feedback (string or null), rawOutput (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['allPassing', 'totalTests', 'passed', 'failed'],
      properties: {
        allPassing: { type: 'boolean' },
        totalTests: { type: 'number' },
        passed: { type: 'number' },
        failed: { type: 'number' },
        errors: { type: 'number' },
        coveragePercent: { type: 'number' },
        feedback: { type: 'string' },
        rawOutput: { type: 'string' }
      }
    }
  },

  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },

  labels: ['testing', 'run-tests', `iteration-${args.iteration}`]
}));

/**
 * Phase 3c: Fix test failures
 */
export const fixFailuresTask = defineTask('fix-failures', (args, taskCtx) => ({
  kind: 'agent',
  title: `Fix test failures (iteration ${args.iteration})`,
  description: 'Diagnose and fix failing tests or application bugs',

  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'senior Python debugger and developer',
      task: `Fix the failing tests in "${args.appName}" - iteration ${args.iteration}`,
      context: {
        appName: args.appName,
        stack: args.stack,
        plan: args.plan,
        testResults: args.testResults,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Review the test failure output carefully',
        'Read the failing test files and the corresponding source code',
        'Determine if the bug is in the application code or in the test code',
        'Fix the root cause - prefer fixing application code to match the spec, but fix test code if tests have wrong expectations',
        'Do NOT lower test expectations to make tests pass - fix the actual code',
        'Make targeted fixes - do not rewrite entire files unless necessary',
        'After fixing, do a quick sanity check by running the failing test(s) individually',
        'Return a summary of what was fixed'
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

  labels: ['testing', 'fix', `iteration-${args.iteration}`]
}));

/**
 * Phase 4: Delivery
 * Create README, make final_delivery commit
 */
export const deliverProjectTask = defineTask('deliver-project', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Deliver: README + final_delivery commit',
  description: 'Finalize README with setup instructions, create final_delivery git commit',

  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'senior developer finalizing a project for delivery',
      task: `Finalize the "${args.appName}" project for delivery`,
      context: {
        appName: args.appName,
        stack: args.stack,
        plan: args.plan,
        testsPassing: args.testsPassing,
        testIterations: args.testIterations,
        projectDir: '/home/ubuntu/projects/budget_manager'
      },
      instructions: [
        'Working directory is /home/ubuntu/projects/budget_manager',
        'Create a comprehensive README.md with:',
        '  - Project title and description',
        '  - Tech stack (Python, FastAPI, SQLite, Pydantic)',
        '  - Setup instructions: clone, create venv, pip install -r requirements.txt',
        '  - How to run: uvicorn app.main:app --reload',
        '  - API endpoints documentation with example curl commands',
        '  - How to run tests: pytest tests/ -v --cov=app',
        '  - Project structure overview',
        'Run the full test suite one final time to confirm all tests pass: python -m pytest tests/ -v --cov=app --cov-report=term-missing',
        'Stage all changes with git add',
        'Create a final commit with message "final_delivery: budget manager app complete with tests"',
        'Verify the commit was created with git log --oneline -3',
        'Return the delivery summary'
      ],
      outputFormat: 'JSON with readmeCreated (boolean), committed (boolean), commitHash (string), finalTestsPassing (boolean), summary (string)'
    },
    outputSchema: {
      type: 'object',
      required: ['readmeCreated', 'committed'],
      properties: {
        readmeCreated: { type: 'boolean' },
        committed: { type: 'boolean' },
        commitHash: { type: 'string' },
        finalTestsPassing: { type: 'boolean' },
        summary: { type: 'string' }
      }
    }
  },

  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`
  },

  labels: ['delivery', 'final']
}));
