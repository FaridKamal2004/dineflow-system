package dineflowsystem;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Owns connection boundaries: all dependent writes use one transaction. */
public final class DatabaseOperations {
  private DatabaseOperations() {}

  public record Ticket(
      int id,
      int customerId,
      String customerName,
      BigDecimal total,
      OrderStatus status,
      LocalDateTime createdAt) {}

  @FunctionalInterface
  private interface Work<T> {
    T run(Connection connection) throws SQLException;
  }

  private static <T> T transaction(Work<T> work) throws SQLException {
    try (Connection connection = DatabaseConnection.getConnection()) {
      connection.setAutoCommit(false);
      try {
        T result = work.run(connection);
        connection.commit();
        return result;
      } catch (SQLException | RuntimeException error) {
        try {
          connection.rollback();
        } catch (SQLException rollback) {
          error.addSuppressed(rollback);
        }
        throw error;
      }
    }
  }

  public static synchronized int submitOrder(Customer customer, Order order) throws SQLException {
    if (customer == null || order == null || customer.getCustID() != order.getCustID())
      throw new IllegalArgumentException("Order and customer must match.");
    if (order.getItemCount() == 0)
      throw new IllegalArgumentException("Add at least one item before submitting.");
    if (order.getOrderID() > 0)
      throw new IllegalStateException("This order has already been submitted.");
    int id =
        transaction(
            connection -> {
              saveCustomer(connection, customer);
              int generated;
              try (PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO orders(customer_id,customer_name,total) VALUES(?,?,?)",
                      Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, customer.getCustID());
                statement.setString(2, customer.getCustName());
                statement.setBigDecimal(3, order.total());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                  if (!keys.next()) throw new SQLException("Order ID was not generated.");
                  generated = keys.getInt(1);
                }
              }
              try (PreparedStatement statement =
                  connection.prepareStatement(
                      "INSERT INTO order_items(order_id,meal_id,name,price) VALUES(?,?,?,?)")) {
                for (Meal item : order.items()) {
                  statement.setInt(1, generated);
                  statement.setInt(2, item.getMealID());
                  statement.setString(3, item.getMealName());
                  statement.setBigDecimal(4, item.price());
                  statement.addBatch();
                }
                statement.executeBatch();
              }
              return generated;
            });
    order.markSubmitted(id);
    return id;
  }

  private static void saveCustomer(Connection connection, Customer customer) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "MERGE INTO customers(id,name,contact) KEY(id) VALUES(?,?,?)")) {
      statement.setInt(1, customer.getCustID());
      statement.setString(2, customer.getCustName());
      statement.setString(3, customer.getCustContact());
      statement.executeUpdate();
    }
  }

  public static void saveStaff(Staff staff) throws SQLException {
    if (staff == null) throw new IllegalArgumentException("Staff is required.");
    transaction(
        connection -> {
          try (PreparedStatement statement =
              connection.prepareStatement(
                  "MERGE INTO employees(id,name,contact,role) KEY(id) VALUES(?,?,?,?)")) {
            statement.setString(1, staff.getStaffID());
            statement.setString(2, staff.getStaffName());
            statement.setString(3, staff.getStaffContact());
            statement.setString(4, staff.role());
            statement.executeUpdate();
          }
          try (PreparedStatement statement =
              connection.prepareStatement(
                  "MERGE INTO salary(staff_id,hours,rate,total) KEY(staff_id) VALUES(?,?,?,?)")) {
            statement.setString(1, staff.getStaffID());
            statement.setBigDecimal(2, BigDecimal.valueOf(staff.getHourWorkMonth()));
            statement.setBigDecimal(3, BigDecimal.valueOf(staff.getRatePerHour()));
            statement.setBigDecimal(4, staff.salary());
            statement.executeUpdate();
          }
          return null;
        });
  }

  public static List<Staff> listStaff() throws SQLException {
    List<Staff> result = new ArrayList<>();
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT e.*,s.hours FROM employees e JOIN salary s ON s.staff_id=e.id ORDER BY"
                    + " e.id");
        ResultSet rows = statement.executeQuery()) {
      while (rows.next()) {
        Staff staff =
            new StaffDeployPattern()
                .createStaff(
                    rows.getString("id"), rows.getString("name"), rows.getString("contact"));
        staff.setHourWorkMonth(rows.getDouble("hours"));
        result.add(staff);
      }
    }
    return List.copyOf(result);
  }

  public static Customer getCustomerById(int id) throws SQLException {
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT * FROM customers WHERE id=?")) {
      statement.setInt(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        return rows.next()
            ? new Customer(rows.getInt("id"), rows.getString("name"), rows.getString("contact"))
            : null;
      }
    }
  }

  public static List<Ticket> listOrders() throws SQLException {
    List<Ticket> result = new ArrayList<>();
    try (Connection connection = DatabaseConnection.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rows = statement.executeQuery("SELECT * FROM orders ORDER BY id DESC")) {
      while (rows.next())
        result.add(
            new Ticket(
                rows.getInt("id"),
                rows.getInt("customer_id"),
                rows.getString("customer_name"),
                rows.getBigDecimal("total"),
                OrderStatus.valueOf(rows.getString("status")),
                rows.getTimestamp("created_at").toLocalDateTime()));
    }
    return List.copyOf(result);
  }

  public static void changeStatus(int id, OrderStatus expected, OrderStatus target)
      throws SQLException {
    if (expected == null || !expected.canMoveTo(target))
      throw new IllegalArgumentException("That order status change is not allowed.");
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("UPDATE orders SET status=? WHERE id=? AND status=?")) {
      statement.setString(1, target.name());
      statement.setInt(2, id);
      statement.setString(3, expected.name());
      if (statement.executeUpdate() != 1)
        throw new IllegalStateException("Order changed or no longer exists. Refresh the board.");
    }
  }

  public static String receipt(int id) throws SQLException {
    StringBuilder text =
        new StringBuilder("DINEFLOW / ORDER RECEIPT\n================================\n");
    try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT * FROM orders WHERE id=?")) {
      statement.setInt(1, id);
      try (ResultSet rows = statement.executeQuery()) {
        if (!rows.next()) throw new IllegalArgumentException("Order not found.");
        text.append("Order #")
            .append(id)
            .append("\nCustomer: ")
            .append(rows.getString("customer_name"))
            .append(" (#")
            .append(rows.getInt("customer_id"))
            .append(")\nStatus: ")
            .append(rows.getString("status"))
            .append("\n")
            .append(rows.getTimestamp("created_at"))
            .append("\n--------------------------------\n");
        try (PreparedStatement items =
            connection.prepareStatement(
                "SELECT name,price FROM order_items WHERE order_id=? ORDER BY id")) {
          items.setInt(1, id);
          try (ResultSet lines = items.executeQuery()) {
            while (lines.next())
              text.append(
                  String.format(
                      "%-22s RM %s%n", lines.getString(1), lines.getBigDecimal(2).toPlainString()));
          }
        }
        text.append("--------------------------------\nTOTAL: RM ")
            .append(rows.getBigDecimal("total").toPlainString())
            .append(
                "\nNo tax / service charge configured.\nPayment is not processed by this demo.\n");
      }
    }
    return text.toString();
  }
}
