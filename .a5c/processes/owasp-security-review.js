/**
 * @process owasp-security-review
 * @description OWASP-aligned security review: SAST, SCA/SBOM, secrets scan, plus manual checks
 *              for auth, access control, input handling, XSS/CSRF/CORS. Produces a severity-rated
 *              findings report with actionable fixes.
 * @inputs { projectName: string, codebasePath: string, techStack: object }
 * @outputs { success: boolean, securityScore: number, findings: array, report: object }
 * @references
 * - OWASP Top 10 2021: https://owasp.org/www-project-top-ten/
 * - OWASP API Security Top 10: https://owasp.org/www-project-api-security/
 * - CWE Top 25: https://cwe.mitre.org/top25/
 */

import { defineTask } from '@a5c-ai/babysitter-sdk';

/* ================================================================
   PROCESS ENTRY POINT
   ================================================================ */
export async function process(inputs, ctx) {
  const { projectName, codebasePath, techStack } = inputs;
  const allFindings = [];

  // ── Phase 1: Parallel automated scans (SAST + SCA + Secrets) ──
  ctx.log('Phase 1: Running automated scans in parallel (SAST, SCA, Secrets)');

  const [sastResult, scaResult, secretsResult] = await ctx.parallel.all([
    () => ctx.task(sastScanTask, { projectName, codebasePath, techStack }),
    () => ctx.task(scaScanTask, { projectName, codebasePath, techStack }),
    () => ctx.task(secretsScanTask, { projectName, codebasePath }),
  ]);

  allFindings.push(...(sastResult.findings || []));
  allFindings.push(...(scaResult.findings || []));
  allFindings.push(...(secretsResult.findings || []));

  // ── Phase 2: Manual review — Auth & Access Control ──
  ctx.log('Phase 2: Manual review — Authentication & Access Control');

  const authResult = await ctx.task(authAccessControlReviewTask, {
    projectName, codebasePath, techStack,
  });
  allFindings.push(...(authResult.findings || []));

  // ── Phase 3: Manual review — Input Handling & Injection ──
  ctx.log('Phase 3: Manual review — Input Handling & Injection');

  const inputResult = await ctx.task(inputHandlingReviewTask, {
    projectName, codebasePath, techStack,
  });
  allFindings.push(...(inputResult.findings || []));

  // ── Phase 4: Manual review — XSS / CSRF / CORS ──
  ctx.log('Phase 4: Manual review — XSS, CSRF, CORS');

  const clientSideResult = await ctx.task(clientSideSecurityReviewTask, {
    projectName, codebasePath, techStack,
  });
  allFindings.push(...(clientSideResult.findings || []));

  // ── Phase 5: Consolidation — deduplicate, rate, prioritise ──
  ctx.log('Phase 5: Consolidating findings');

  const consolidatedReport = await ctx.task(consolidateFindingsTask, {
    projectName,
    allFindings,
    techStack,
  });

  // ── Phase 6: Human review breakpoint ──
  await ctx.breakpoint({
    question: [
      `Security review complete for ${projectName}.`,
      `Total findings: ${consolidatedReport.totalFindings}`,
      `Critical: ${consolidatedReport.bySeverity?.critical || 0}`,
      `High: ${consolidatedReport.bySeverity?.high || 0}`,
      `Medium: ${consolidatedReport.bySeverity?.medium || 0}`,
      `Low: ${consolidatedReport.bySeverity?.low || 0}`,
      `Info: ${consolidatedReport.bySeverity?.info || 0}`,
      '',
      'Review the report and approve to finalise.',
    ].join('\n'),
    title: 'Security Review — Final Approval',
    context: {
      runId: ctx.runId,
      files: [
        { path: 'artifacts/security-report.md', format: 'markdown', label: 'Full Report' },
      ],
    },
  });

  return {
    success: true,
    projectName,
    securityScore: consolidatedReport.securityScore,
    findings: consolidatedReport.findings,
    report: consolidatedReport,
    artifacts: ['artifacts/security-report.md'],
    metadata: { processId: 'owasp-security-review', timestamp: ctx.now() },
  };
}

/* ================================================================
   TASK DEFINITIONS
   ================================================================ */

// ── 1a. SAST Scan ──
export const sastScanTask = defineTask('sast-scan', (args, taskCtx) => ({
  kind: 'agent',
  title: 'SAST — Static Application Security Testing',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior application security engineer specialising in static analysis',
      task: [
        `Perform a thorough SAST review of the "${args.projectName}" codebase at ${args.codebasePath}.`,
        'Tech stack: ' + JSON.stringify(args.techStack),
        '',
        'You MUST read every source file in app/ and tests/ before reporting.',
        'Do NOT guess — only report findings you can cite with file path and line number.',
      ].join('\n'),
      context: { projectName: args.projectName, codebasePath: args.codebasePath, techStack: args.techStack },
      instructions: [
        'Read all Python source files under app/ (main.py, database.py, schemas.py, models.py, routers/*.py)',
        'Read app/static/index.html (the full SPA frontend)',
        'Check for OWASP Top 10 2021 issues: injection (A03), insecure design (A04), security misconfiguration (A05), vulnerable components (A06), identification/auth failures (A07), integrity failures (A08), logging failures (A09), SSRF (A10)',
        'Check CWE Top 25: SQL injection (CWE-89), OS command injection (CWE-78), XSS (CWE-79), path traversal (CWE-22), CSRF (CWE-352), hardcoded credentials (CWE-798), missing auth (CWE-306), broken access control (CWE-862)',
        'For each finding: provide id, title, severity (critical/high/medium/low/info), cwes, affected file + line, description, proof/evidence, and recommended fix',
        'Return a JSON object with "findings" array and "summary" string',
      ],
      outputFormat: 'JSON: { findings: [{ id, title, severity, cwes, file, line, description, evidence, fix }], summary: string }',
    },
    outputSchema: {
      type: 'object',
      required: ['findings', 'summary'],
      properties: {
        findings: {
          type: 'array',
          items: {
            type: 'object',
            required: ['id', 'title', 'severity', 'file', 'description', 'fix'],
            properties: {
              id: { type: 'string' },
              title: { type: 'string' },
              severity: { type: 'string', enum: ['critical', 'high', 'medium', 'low', 'info'] },
              cwes: { type: 'array', items: { type: 'string' } },
              file: { type: 'string' },
              line: { type: 'number' },
              description: { type: 'string' },
              evidence: { type: 'string' },
              fix: { type: 'string' },
            },
          },
        },
        summary: { type: 'string' },
      },
    },
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`,
  },
  labels: ['sast', 'owasp'],
}));

// ── 1b. SCA / Dependency / SBOM Risk Scan ──
export const scaScanTask = defineTask('sca-scan', (args, taskCtx) => ({
  kind: 'agent',
  title: 'SCA — Software Composition Analysis & SBOM Risk',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Software composition analyst and supply-chain security expert',
      task: [
        `Analyse dependencies and supply-chain risks for "${args.projectName}" at ${args.codebasePath}.`,
        'Tech stack: ' + JSON.stringify(args.techStack),
      ].join('\n'),
      context: { projectName: args.projectName, codebasePath: args.codebasePath, techStack: args.techStack },
      instructions: [
        'Read requirements.txt to enumerate all direct dependencies and their pinned versions',
        'Run "pip audit" or "pip-audit" if available, else check PyPI / safety DB for known CVEs against each dependency version via web search',
        'Run "npm audit" inside .a5c/ if applicable (babysitter SDK deps)',
        'For each dependency: check if version is latest, identify known CVEs, assess severity',
        'Check for dependency confusion risks (private package names, namespace squatting)',
        'Check for overly broad version pins (^, ~, >=) that could pull vulnerable versions',
        'Assess transitive dependency risk where possible',
        'Generate a mini SBOM: list each direct dependency, its version, license, and known vulnerabilities',
        'For each finding: provide id, title, severity, affected package + version, CVE IDs if any, description, fix',
        'Return JSON with "findings" array, "sbom" array, and "summary" string',
      ],
      outputFormat: 'JSON: { findings: [{ id, title, severity, package, version, cves, description, fix }], sbom: [{ package, version, license, knownVulnerabilities }], summary: string }',
    },
    outputSchema: {
      type: 'object',
      required: ['findings', 'sbom', 'summary'],
      properties: {
        findings: { type: 'array' },
        sbom: { type: 'array' },
        summary: { type: 'string' },
      },
    },
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`,
  },
  labels: ['sca', 'sbom', 'dependencies'],
}));

// ── 1c. Secrets Scan ──
export const secretsScanTask = defineTask('secrets-scan', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Secrets Scan — Hardcoded Credentials & Sensitive Data',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Secrets detection and sensitive-data-exposure specialist',
      task: `Scan the "${args.projectName}" codebase at ${args.codebasePath} for hardcoded secrets, API keys, tokens, passwords, and sensitive data exposure.`,
      context: { projectName: args.projectName, codebasePath: args.codebasePath },
      instructions: [
        'Read all source files: app/**/*.py, app/static/index.html, tests/**/*.py, .gitignore, *.md, requirements.txt, .a5c/processes/*.js, .a5c/processes/*.json',
        'Search for patterns: API keys, tokens, passwords, secrets, private keys, connection strings, AWS/GCP/Azure credentials',
        'Check for high-entropy strings that could be secrets',
        'Check .gitignore for proper exclusion of .env, *.db, credentials, key files',
        'Check if sensitive data (passwords, PII) could leak via error messages, logs, or API responses',
        'Verify no secrets in git history (check recent commits with git log -p)',
        'For each finding: id, title, severity, file, line, description, evidence (redacted), fix',
        'Return JSON with "findings" array and "summary" string',
      ],
      outputFormat: 'JSON: { findings: [{ id, title, severity, file, line, description, evidence, fix }], summary: string }',
    },
    outputSchema: {
      type: 'object',
      required: ['findings', 'summary'],
      properties: {
        findings: { type: 'array' },
        summary: { type: 'string' },
      },
    },
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`,
  },
  labels: ['secrets', 'sensitive-data'],
}));

// ── 2. Authentication & Access Control Review ──
export const authAccessControlReviewTask = defineTask('auth-access-control-review', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Manual Review — Authentication & Access Control',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Application security engineer specialising in authentication and authorisation',
      task: [
        `Review authentication and access control for "${args.projectName}" at ${args.codebasePath}.`,
        'This is a FastAPI + SQLite budget manager app. It currently has NO authentication.',
      ].join('\n'),
      context: { projectName: args.projectName, codebasePath: args.codebasePath, techStack: args.techStack },
      instructions: [
        'Read app/main.py, app/routers/transactions.py, app/routers/summary.py, app/routers/recurring.py, app/routers/backup.py',
        'Assess: Is there any authentication mechanism? (JWT, session, API key, basic auth)',
        'Assess: Is there any authorization / access control? Can any user access any data?',
        'Check for: missing authentication on sensitive endpoints (CRUD, backup/restore, export/import)',
        'Check for: IDOR vulnerabilities (can user A access user B data via ID enumeration)',
        'Check for: admin-only functionality accessible without privilege checks',
        'Check for: rate limiting on any endpoints',
        'Map OWASP categories: A01 Broken Access Control, A07 Identification and Authentication Failures',
        'For each finding: id, title, severity, cwes, file, line, description, evidence, fix',
        'Return JSON with "findings" array and "summary" string',
      ],
      outputFormat: 'JSON: { findings: [{ id, title, severity, cwes, file, line, description, evidence, fix }], summary: string }',
    },
    outputSchema: {
      type: 'object',
      required: ['findings', 'summary'],
      properties: {
        findings: { type: 'array' },
        summary: { type: 'string' },
      },
    },
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`,
  },
  labels: ['auth', 'access-control', 'owasp-a01', 'owasp-a07'],
}));

// ── 3. Input Handling & Injection Review ──
export const inputHandlingReviewTask = defineTask('input-handling-review', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Manual Review — Input Handling & Injection',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Application security engineer specialising in injection vulnerabilities and input validation',
      task: [
        `Review input handling and injection attack surface for "${args.projectName}" at ${args.codebasePath}.`,
        'This is a FastAPI app using Pydantic v2 for validation and raw SQLite (no ORM).',
      ].join('\n'),
      context: { projectName: args.projectName, codebasePath: args.codebasePath, techStack: args.techStack },
      instructions: [
        'Read app/schemas.py to understand all Pydantic validation models',
        'Read all router files: app/routers/transactions.py, summary.py, recurring.py, backup.py',
        'Read app/database.py for DB schema and connection handling',
        'Check every SQL query for parameterised queries vs string formatting/concatenation (SQL injection CWE-89)',
        'Check for OS command injection (CWE-78) in any subprocess or os.system calls',
        'Check for path traversal (CWE-22) especially in backup.py (filename parameters)',
        'Check for server-side request forgery (CWE-918)',
        'Check Pydantic validators: are all user inputs validated? Are there max_length constraints?',
        'Check for insecure deserialization in JSON import/restore endpoints',
        'Check for denial-of-service via unbounded queries (e.g. GET /api/transactions with no pagination)',
        'Map to OWASP A03 Injection',
        'For each finding: id, title, severity, cwes, file, line, description, evidence, fix',
        'Return JSON with "findings" array and "summary" string',
      ],
      outputFormat: 'JSON: { findings: [{ id, title, severity, cwes, file, line, description, evidence, fix }], summary: string }',
    },
    outputSchema: {
      type: 'object',
      required: ['findings', 'summary'],
      properties: {
        findings: { type: 'array' },
        summary: { type: 'string' },
      },
    },
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`,
  },
  labels: ['injection', 'input-validation', 'owasp-a03'],
}));

// ── 4. XSS / CSRF / CORS Review ──
export const clientSideSecurityReviewTask = defineTask('client-side-security-review', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Manual Review — XSS, CSRF, CORS',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Application security engineer specialising in client-side vulnerabilities, XSS, CSRF, and CORS',
      task: [
        `Review client-side security for "${args.projectName}" at ${args.codebasePath}.`,
        'The frontend is a single-page vanilla JS app in app/static/index.html.',
        'The backend is FastAPI with CORS middleware configured in app/main.py.',
      ].join('\n'),
      context: { projectName: args.projectName, codebasePath: args.codebasePath, techStack: args.techStack },
      instructions: [
        'Read app/static/index.html in full — it is the entire SPA (~1200 lines)',
        'Read app/main.py for CORS configuration',
        'XSS: Check all places where data from the API is rendered into the DOM. Is innerHTML used? Is data escaped? Check for DOM-based XSS in URL parameters, event handlers, dynamic HTML generation.',
        'CSRF: Check if any state-changing requests (POST, PUT, DELETE) have CSRF protection. Check for CSRF tokens, SameSite cookie attributes, custom headers.',
        'CORS: Check the CORSMiddleware config — is allow_origins=["*"]? Is allow_credentials=True with wildcard origins? What are the security implications?',
        'Check Content-Security-Policy, X-Frame-Options, X-Content-Type-Options headers',
        'Check for clickjacking protection',
        'Check for open redirect vulnerabilities',
        'Check for insecure use of postMessage, eval, or Function constructor',
        'Check cookie security: httpOnly, secure, SameSite attributes',
        'For each finding: id, title, severity, cwes, file, line, description, evidence, fix',
        'Return JSON with "findings" array and "summary" string',
      ],
      outputFormat: 'JSON: { findings: [{ id, title, severity, cwes, file, line, description, evidence, fix }], summary: string }',
    },
    outputSchema: {
      type: 'object',
      required: ['findings', 'summary'],
      properties: {
        findings: { type: 'array' },
        summary: { type: 'string' },
      },
    },
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`,
  },
  labels: ['xss', 'csrf', 'cors', 'client-side', 'owasp-a05'],
}));

// ── 5. Consolidation & Report Generation ──
export const consolidateFindingsTask = defineTask('consolidate-findings', (args, taskCtx) => ({
  kind: 'agent',
  title: 'Consolidate Findings — Deduplicate, Score, Report',
  agent: {
    name: 'general-purpose',
    prompt: {
      role: 'Senior security consultant producing an OWASP-aligned security audit report',
      task: [
        `Consolidate all security findings for "${args.projectName}" into a final report.`,
        'Deduplicate, assign final severities, calculate a security score (0-100), and produce a Markdown report.',
      ].join('\n'),
      context: {
        projectName: args.projectName,
        techStack: args.techStack,
        rawFindings: args.allFindings,
      },
      instructions: [
        'Review all provided findings and remove exact duplicates',
        'For each unique finding, validate the severity rating (critical/high/medium/low/info)',
        'Map each finding to OWASP Top 10 2021 and CWE categories',
        'Calculate a security score: start at 100, subtract: critical=-20, high=-10, medium=-5, low=-2, info=-0',
        'Minimum score is 0',
        'Group findings by: (a) severity, (b) OWASP category',
        'For each finding include: id, title, severity, owasp category, cwes, file, line, description, evidence, recommended fix',
        'Write a full Markdown report to artifacts/security-report.md with sections: Executive Summary, Findings by Severity, Findings by OWASP Category, SBOM Summary (if available), Recommendations, Score',
        'Return JSON with: securityScore, totalFindings, bySeverity counts, findings array, summary string',
      ],
      outputFormat: 'JSON: { securityScore: number, totalFindings: number, bySeverity: { critical, high, medium, low, info }, findings: array, summary: string }',
    },
    outputSchema: {
      type: 'object',
      required: ['securityScore', 'totalFindings', 'bySeverity', 'findings', 'summary'],
      properties: {
        securityScore: { type: 'number', minimum: 0, maximum: 100 },
        totalFindings: { type: 'number' },
        bySeverity: {
          type: 'object',
          properties: {
            critical: { type: 'number' },
            high: { type: 'number' },
            medium: { type: 'number' },
            low: { type: 'number' },
            info: { type: 'number' },
          },
        },
        findings: { type: 'array' },
        summary: { type: 'string' },
      },
    },
  },
  io: {
    inputJsonPath: `tasks/${taskCtx.effectId}/input.json`,
    outputJsonPath: `tasks/${taskCtx.effectId}/result.json`,
  },
  labels: ['consolidation', 'report', 'scoring'],
}));
