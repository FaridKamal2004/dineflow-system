# Interview guide

## Introduce it accurately

“DineFlow began as a group Java OOP coursework application using Swing and MySQL. I enhanced the concept into a self-contained desktop demo with validated order models, transactional order persistence, a kitchen workflow, staff updates, automated tests, and reproducible GitHub releases.”

The enhancement was developed with AI assistance. Review the source, run it yourself, and explain the implementation in your own words. Credit the original group project and do not claim production usage or independently measured efficiency gains.

## Three-minute demonstration

1. Enter a synthetic customer and add two meals. Explain why the basket uses the domain list instead of maintaining a separate total.
2. Submit the order and show its stored receipt. Explain the one-connection transaction and the regression test that forces an item-write failure.
3. On Order board, advance the order, filter by status, and explain why a stale status cannot overwrite a newer state.
4. Create a waiter with 170 hours. Explain the factory and base/overtime arithmetic. Save, select the row, and update the hours.
5. Reopen the application from the same directory to demonstrate persistence. Open the green CI run and tests.

## Questions to practice

**What concrete defect did you fix?** The original `Order(int customerID)` assigned `custID` from itself. Orders therefore lost the supplied customer ID. A regression test now verifies identity. Another original issue was an order array that silently stopped accepting items while the table continued adding rows.

**Why H2 rather than MySQL?** An embedded file database removes server setup for an interview demo while retaining JDBC, SQL constraints, transactions, and persistence. It is not a claim that H2 is the right production multi-user deployment.

**What makes a transaction valid?** All dependent writes must use the same connection with auto-commit disabled. Opening an unrelated connection for each insert defeats rollback of the outer connection. The tests prove atomicity by forcing a late write to fail.

**How do you handle money?** `BigDecimal` represents prices and totals. Binary floating-point arithmetic can produce unexpected decimal results. Payroll rounding is explicit and centralized.

**Where is OOP visible?** Staff is abstract; Waiter, Cashier, and Chef are subtypes with roles/rates; StaffDeployPattern constructs the type. Order encapsulates its meal list and is associated with one customer. The UI delegates storage to a repository class.

**Why is the UI responsive during saves?** SwingWorker executes JDBC operations away from Swing's event dispatch thread. Completion callbacks update the UI on that thread.

**How does the board prevent invalid transitions?** An enum defines permitted changes, and SQL checks the expected previous status. A zero-row update signals stale state rather than silently overwriting it.

**What are its limits?** No authentication, encrypted data, payments, cloud sync, multi-terminal live updates, production payroll compliance, or automated MySQL migration. Tests do not establish commercial readiness.

## Before the interview

- Install Java 17+, download the release, and run it from a folder you can find again.
- Use synthetic customer/staff details.
- Practice following one click from UI → domain validation → JDBC transaction → refreshed UI.
- Read both rollback integration tests and explain what would have gone wrong before the fix.
- Keep a copy of the runnable JAR and this guide offline.
