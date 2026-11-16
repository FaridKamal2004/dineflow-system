# Architecture

```text
Swing / FlatLaf
MainDashboard
  ├─ CustomerDashboard → Order + Customer + Meal
  ├─ OrderBoard        → OrderStatus
  └─ StaffDashboard    → StaffDeployPattern → Staff
                                            ├─ Waiter
                                            ├─ Cashier
                                            └─ Chef
              ↓ SwingWorker (JDBC off the event thread)
DatabaseOperations → DatabaseConnection → embedded H2 file
```

## Decisions

The application stays in Java to preserve the project's OOP foundation. The inherited staff types expose roles; the factory chooses the type from a validated W/R/F ID. Shared payroll arithmetic is centralized in the abstract class instead of copied into each subclass. `Restaurant` supplies the original menu and does not duplicate persisted operational state.

`Meal` is immutable. `Order` owns a private list and exposes immutable snapshots. Its customer ID is final and an order freezes after successful submission. The basket table is rendered from that list, so it is not a second source of truth. Customer identity cannot change while a draft contains items. At most 100 items are accepted; capacity violations are explicit.

Money uses `BigDecimal`. Prices must have at most two fractional digits, and order totals remain exact. Payroll rounds to two decimals using HALF_UP after applying the demonstration policy. Display/compatibility getters may return doubles, but totals are not accumulated as doubles.

## Transactions and data model

Customers, employees, salary, orders, and order_items have keys and database constraints. Employee IDs preserve their role prefixes. Order IDs use a database identity sequence; gaps after rollback are expected. Repeated customer IDs update the current contact record, while each order keeps its historical customer-name snapshot. Order lines snapshot names and prices independently of later menu changes.

`submitOrder` validates the draft, then writes the customer, order, and lines on the **same connection** before committing. The Java draft receives its persisted ID only after commit. Staff and pay also save in one transaction. An exception rolls back all dependent writes. Integration tests force a late constraint failure and verify that earlier inserts were rolled back.

Status updates use `WHERE id=? AND status=?`. Exactly one row must change, protecting against stale UI updates. The state machine allows QUEUED → PREPARING → READY → SERVED, plus QUEUED/PREPARING → CANCELLED. Served and cancelled states are terminal. READY cannot be cancelled in this demo. Cancellation preserves the order and its lines.

## UI and lifecycle

JDBC reads/writes run through `SwingWorker`; the event dispatch thread handles UI updates. Submitting disables repeated submission and protects the draft during the operation. Database failures preserve the draft. A receipt-read failure after a committed submission does not retain the submitted draft as an editable order: it can be retrieved from the board.

The board retrieves changes on tab selection and Refresh; it is not a live network feed. Queries are parameterized. Names resembling markup or SQL remain data. Native dialogs use text areas for user-derived error messages rather than Swing HTML labels.

## Limits and next steps

H2 is an embedded local dependency bundled into the JAR. The default data path is relative to the working directory. The app intentionally does not expose an HTTP server or database management console. Data is unencrypted and there is no authorization; do not use real records. Backup and migration tooling, repository injection, schema versioning, pagination, audit trails, a proper authentication model, and operational monitoring are future work.

Production multi-terminal operation would require a trusted backend and shared database with server-side access checks, rather than sharing an embedded file. Real payroll, tax, payment, and privacy requirements need separate design and specialist validation.
