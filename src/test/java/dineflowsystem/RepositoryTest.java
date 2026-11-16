package dineflowsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class RepositoryTest {
  @BeforeEach
  void createDatabase() throws Exception {
    System.setProperty("dineflow.db", "mem:test_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    DatabaseConnection.initialize();
  }

  @AfterEach
  void closeDatabase() throws Exception {
    try (Connection c = DatabaseConnection.getConnection();
        Statement s = c.createStatement()) {
      s.execute("SHUTDOWN");
    }
    System.clearProperty("dineflow.db");
  }

  private Customer customer() {
    return new Customer(7, "Demo Guest", "0123456789");
  }

  private Order draft() {
    Order order = new Order(7);
    order.addItem(new Meal(1, "Nasi Lemak Ayam", 6));
    order.addItem(new Meal(2, "Nasi Lemak Sotong", 8));
    return order;
  }

  private int count(String table) throws Exception {
    try (Connection c = DatabaseConnection.getConnection();
        Statement s = c.createStatement();
        ResultSet rows = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
      rows.next();
      return rows.getInt(1);
    }
  }

  @Test
  void submissionPersistsIdentityItemsTotalAndReceipt() throws Exception {
    Order order = draft();
    int id = DatabaseOperations.submitOrder(customer(), order);
    assertTrue(id > 0);
    assertEquals(id, order.getOrderID());
    assertEquals(1, count("customers"));
    assertEquals(2, count("order_items"));
    var ticket = DatabaseOperations.listOrders().get(0);
    assertEquals(7, ticket.customerId());
    assertEquals(new BigDecimal("14.00"), ticket.total());
    assertTrue(DatabaseOperations.receipt(id).contains("Nasi Lemak Sotong"));
    assertTrue(DatabaseOperations.receipt(id).contains("TOTAL: RM 14.00"));
  }

  @Test
  void repeatCustomersCanPlaceNewOrdersWithoutDuplicateKeyFailure() throws Exception {
    DatabaseOperations.submitOrder(customer(), draft());
    int second =
        DatabaseOperations.submitOrder(new Customer(7, "Updated Guest", "0123456789"), draft());
    assertTrue(second > 1);
    assertEquals(1, count("customers"));
    assertEquals(2, count("orders"));
    assertEquals("Updated Guest", DatabaseOperations.getCustomerById(7).getCustName());
    assertEquals("Demo Guest", DatabaseOperations.listOrders().get(1).customerName());
  }

  @Test
  void orderSubmissionIsNotDuplicated() throws Exception {
    Order order = draft();
    DatabaseOperations.submitOrder(customer(), order);
    assertThrows(
        IllegalStateException.class, () -> DatabaseOperations.submitOrder(customer(), order));
    assertEquals(1, count("orders"));
  }

  @Test
  void rejectsEmptyAndMismatchedOrdersWithoutWriting() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> DatabaseOperations.submitOrder(customer(), new Order(7)));
    assertThrows(
        IllegalArgumentException.class,
        () -> DatabaseOperations.submitOrder(new Customer(9, "Other", "0123456789"), draft()));
    assertEquals(0, count("customers"));
  }

  @Test
  void itemFailureRollsBackCustomerOrderAndLines() throws Exception {
    try (Connection c = DatabaseConnection.getConnection();
        Statement s = c.createStatement()) {
      s.execute("ALTER TABLE order_items ADD CONSTRAINT force_failure CHECK(price<0)");
    }
    Order order = draft();
    assertThrows(SQLException.class, () -> DatabaseOperations.submitOrder(customer(), order));
    assertEquals(-1, order.getOrderID());
    assertEquals(0, count("customers"));
    assertEquals(0, count("orders"));
    assertEquals(0, count("order_items"));
  }

  @Test
  void staffAndSalaryRollbackTogether() throws Exception {
    try (Connection c = DatabaseConnection.getConnection();
        Statement s = c.createStatement()) {
      s.execute("ALTER TABLE salary ADD CONSTRAINT force_failure CHECK(total<0)");
    }
    assertThrows(
        SQLException.class,
        () -> DatabaseOperations.saveStaff(new Chef("F1", "Demo Chef", "0123456789")));
    assertEquals(0, count("employees"));
    assertEquals(0, count("salary"));
  }

  @Test
  void rolePrefixesAvoidCollidingStaffIdsAndUpdatesAreIdempotent() throws Exception {
    Staff w = new Waiter("W1", "Demo Waiter", "0123456789"),
        f = new Chef("F1", "Demo Chef", "0123456789");
    DatabaseOperations.saveStaff(w);
    DatabaseOperations.saveStaff(f);
    w.setHourWorkMonth(170);
    DatabaseOperations.saveStaff(w);
    assertEquals(2, count("employees"));
    assertEquals(2, count("salary"));
    assertEquals(
        new BigDecimal("1750.00"),
        DatabaseOperations.listStaff().stream()
            .filter(s -> s.getStaffID().equals("W1"))
            .findFirst()
            .orElseThrow()
            .salary());
  }

  @Test
  void statusLifecycleIsStoredAndStaleUpdatesAreRejected() throws Exception {
    int id = DatabaseOperations.submitOrder(customer(), draft());
    DatabaseOperations.changeStatus(id, OrderStatus.QUEUED, OrderStatus.PREPARING);
    assertThrows(
        IllegalStateException.class,
        () -> DatabaseOperations.changeStatus(id, OrderStatus.QUEUED, OrderStatus.PREPARING));
    assertThrows(
        IllegalArgumentException.class,
        () -> DatabaseOperations.changeStatus(id, OrderStatus.PREPARING, OrderStatus.SERVED));
    DatabaseOperations.changeStatus(id, OrderStatus.PREPARING, OrderStatus.READY);
    DatabaseOperations.changeStatus(id, OrderStatus.READY, OrderStatus.SERVED);
    assertEquals(OrderStatus.SERVED, DatabaseOperations.listOrders().get(0).status());
  }

  @Test
  void cancellationPreservesHistory() throws Exception {
    int id = DatabaseOperations.submitOrder(customer(), draft());
    DatabaseOperations.changeStatus(id, OrderStatus.QUEUED, OrderStatus.CANCELLED);
    assertEquals(1, count("orders"));
    assertEquals(2, count("order_items"));
    assertEquals(OrderStatus.CANCELLED, DatabaseOperations.listOrders().get(0).status());
  }

  @Test
  void customerNameIsBoundAsDataNotSql() throws Exception {
    DatabaseOperations.submitOrder(
        new Customer(7, "Robert'); DROP TABLE customers;--", "0123456789"), draft());
    assertEquals(1, count("customers"));
    assertTrue(DatabaseOperations.receipt(1).contains("DROP TABLE"));
  }

  @Test
  void missingRecordsAreExplicit() throws Exception {
    assertNull(DatabaseOperations.getCustomerById(987));
    assertThrows(IllegalArgumentException.class, () -> DatabaseOperations.receipt(987));
    assertThrows(
        IllegalStateException.class,
        () -> DatabaseOperations.changeStatus(987, OrderStatus.QUEUED, OrderStatus.PREPARING));
  }

  @Test
  void fileDatabaseSurvivesClosingAndReopening(@TempDir Path directory) throws Exception {
    try (Connection c = DatabaseConnection.getConnection();
        Statement s = c.createStatement()) {
      s.execute("SHUTDOWN");
    }
    System.setProperty(
        "dineflow.db",
        directory.resolve("restaurant").toAbsolutePath().toString().replace('\\', '/'));
    DatabaseConnection.initialize();
    DatabaseOperations.submitOrder(customer(), draft());
    try (Connection c = DatabaseConnection.getConnection();
        Statement s = c.createStatement()) {
      s.execute("SHUTDOWN");
    }
    DatabaseConnection.initialize();
    assertEquals(1, DatabaseOperations.listOrders().size());
    assertTrue(DatabaseOperations.receipt(1).contains("14.00"));
  }
}
