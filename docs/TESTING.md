# Test report

Validation date: **16 November 2026**, Asia/Kuala_Lumpur.

| Suite | Result | Purpose |
| --- | --- | --- |
| DomainTest | 27 passed | Identity regression, exact totals, removal, 12-item basket, explicit 100-item capacity, immutable snapshots, frozen submissions, validation, factory roles, pay boundaries and state transitions |
| RepositoryTest | 12 passed | Real H2 SQL integration: order/line snapshots, repeat customers, duplicate submission rejection, forced rollback, atomic staff/pay writes, full staff IDs, stale updates, cancellation history, parameterized SQL, missing records and disk reopen |
| SwingPanelTest | 3 passed | Headless construction, empty-submit disabling, and button-driven basket/model consistency beyond ten items |
| Maven verify / shaded JAR | Passed locally | Java 17 compilation, tests, and runnable dependency-inclusive packaging |

Total: **42 tests, zero failures, zero errors, zero skipped** in the local verification run. Tests use isolated temporary databases and synthetic data; they do not touch application records. The GitHub workflow repeats verification on Windows and Ubuntu.

## Manual packaged-application smoke check

On Windows with Java 17, the built JAR was opened in the real desktop UI using a separate synthetic QA database. A demo customer was entered, a meal added, an RM 8.00 order submitted, and the persisted receipt opened. The order appeared on the kitchen board as QUEUED and was advanced to PREPARING. The draft cleared after successful submission. No real customer or employee data was used.

The ordering panel also has a reproducible offscreen Swing render for documentation. This render is labeled as a component render, rather than a screenshot of an independently deployed browser application.

The static showcase was checked in Chromium at 1440px and 390px widths: HTTP 200, no page runtime errors, no horizontal overflow, and zero automatically detected WCAG 2/2.1 A/AA violations after text-contrast corrections. This web-page scan does not establish accessibility conformance of the Java desktop app.

## Reproduce

```sh
mvn -B -ntp verify
```

XML and text reports are written to `target/surefire-reports/`. CI uploads them for each operating system. To inspect persistence manually, run the JAR from a writable folder, save an order, close, and reopen from the same folder.

## Boundaries

The headless tests do not automate native print dialogs or certify assistive-technology support. The manual check does not exhaust every desktop/OS combination. macOS and physical Linux desktop rendering, concurrency under real load, real payroll correctness, production security, accessibility conformance, database upgrades, recovery from storage hardware failure, and migration from the original MySQL system require further work. No authentication or payment workflow exists to test.
