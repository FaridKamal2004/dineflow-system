package dineflowsystem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Embedded database; no database server or embedded production credentials. */
public final class DatabaseConnection {
  private DatabaseConnection() {}

  public static Connection getConnection() throws SQLException {
    String location = System.getProperty("dineflow.db", "./data/dineflow");
    return DriverManager.getConnection("jdbc:h2:" + location, "sa", "");
  }

  public static void initialize() throws SQLException {
    try (Connection connection = getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          "CREATE TABLE IF NOT EXISTS customers (id INT PRIMARY KEY CHECK(id>0), name VARCHAR(80)"
              + " NOT NULL, contact VARCHAR(30) NOT NULL)");
      statement.execute(
          "CREATE TABLE IF NOT EXISTS employees (id VARCHAR(10) PRIMARY KEY, name VARCHAR(80) NOT"
              + " NULL, contact VARCHAR(30) NOT NULL, role VARCHAR(12) NOT NULL)");
      statement.execute(
          "CREATE TABLE IF NOT EXISTS salary (staff_id VARCHAR(10) PRIMARY KEY REFERENCES"
              + " employees(id), hours DECIMAL(7,2) NOT NULL CHECK(hours BETWEEN 0 AND 744), rate"
              + " DECIMAL(8,2) NOT NULL, total DECIMAL(12,2) NOT NULL)");
      statement.execute(
          "CREATE TABLE IF NOT EXISTS orders (id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
              + " customer_id INT NOT NULL REFERENCES customers(id), customer_name VARCHAR(80) NOT"
              + " NULL, total DECIMAL(12,2) NOT NULL CHECK(total>=0), status VARCHAR(12) DEFAULT"
              + " 'QUEUED' NOT NULL CHECK(status IN"
              + " ('QUEUED','PREPARING','READY','SERVED','CANCELLED')), created_at TIMESTAMP"
              + " DEFAULT CURRENT_TIMESTAMP NOT NULL)");
      statement.execute(
          "CREATE TABLE IF NOT EXISTS order_items (id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
              + " order_id INT NOT NULL REFERENCES orders(id), meal_id INT NOT NULL, name"
              + " VARCHAR(80) NOT NULL, price DECIMAL(8,2) NOT NULL CHECK(price>=0))");
    }
  }
}
