# OrderFlow → QualityHub integration

This exercise runs actual Chromium tests against the packaged Java OrderFlow application. A private temporary QualityHub instance receives the results through its real HTTP API. No hosted account, database subscription, integration secret or awake public demo is required.

## Execute locally

Requirements: Java 21, Node.js 24, built OrderFlow JAR and Chromium. Obtain the pinned QualityHub revision listed in `.github/workflows/ci.yml`, install its dependencies with `npm ci`, then:

```sh
./scripts/build.sh -B
cd frontend
npx playwright install chromium
cd ..
QUALITYHUB_CHECKOUT=/absolute/path/to/QualityHub node scripts/qualityhub-ci.mjs
```

The local default is a sibling `../qualityhub` checkout. The vendored reporter must be byte-identical to `integrations/playwright-reporter.ts` in that checkout; the runner checks this before starting. Updating the target is deliberate: update the pinned revision, refresh `frontend/integrations/qualityhub-reporter.ts`, and rerun the exercise.

## What the exercise proves

1. Start an isolated QualityHub API on loopback with a temporary SQLite database.
2. Generate a temporary owner password and project-scoped upload token in memory.
3. Run the real OrderFlow browser workflow while Playwright deliberately replaces **POST /api/orders** with HTTP 503. Assert that injection actually occurred and that the browser run failed.
4. Import the failed run through the reporter, preserving attempts, actual screenshots, workflow log, error context and Playwright trace.
5. Download every retained file using an authorized session and check SHA-256 against stored metadata. Replay the same report/files and require replay responses rather than duplicates.
6. Create a defect linked to the failed result, explicitly labelled as a controlled experiment.
7. Restart the normal test service and execute the same browser test without the injected outage. Verify a successful run and its evidence.
8. Compare the two completed runs and require exactly one fixed test.
9. Save portable reports, export and comparison; stop the temporary QualityHub process and remove its database.

The failed first browser invocation is **expected only inside this explicit experiment**. Unexpected success, failure to inject, reporter errors, missing files, hash mismatches, duplicate delivery or failed recovery cause the integration runner to fail. Ordinary `npm run test:e2e` does not inject faults.

## CI evidence

GitHub Actions checks out QualityHub at an exact revision. Its project identity/token are created for that job and never written into reports. Source records include the OrderFlow repository, tested commit, Actions run link and scenario. Local runs record whether tracked files had uncommitted changes. Metadata is supplied by the integration, not a signed build attestation.

The `orderflow-test-evidence` Actions artifact retains `.qualityhub-evidence/` for seven days:

- `controlled-failure.json` / `recovery.json`: reporter payloads including retained file bytes.
- `comparison.json`: stable-identity before/after comparison.
- `workspace-export.json`: runs, linked defect and retained attachment metadata; no credentials.
- `summary.json`: revisions, verified file count and replay result.

The QualityHub API/database are temporary. Reports are not automatically published into the shared public demo and do not survive as a hosted workspace. Use the saved report JSON in an owned QualityHub Automation panel when a browsable retained copy is needed.

Only synthetic OrderFlow records and local test credentials are used. Payment and refund actions remain simulations. The outage is deliberately injected in the browser; this exercise does not demonstrate a real production outage or a deployed code fix.
