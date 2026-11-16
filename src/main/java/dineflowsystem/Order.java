package dineflowsystem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A draft belongs to one customer and freezes after it is committed to storage. */
public final class Order {
  public static final int MAX_ITEMS = 100;
  private int orderID = -1;
  private final int custID;
  private final List<Meal> items = new ArrayList<>();

  public Order(int customerID) {
    if (customerID <= 0)
      throw new IllegalArgumentException("Customer ID must be a positive whole number.");
    this.custID = customerID;
  }

  public void addItem(Meal meal) {
    mutable();
    if (items.size() >= MAX_ITEMS)
      throw new IllegalArgumentException("An order can contain up to " + MAX_ITEMS + " items.");
    items.add(Objects.requireNonNull(meal, "Select a meal."));
  }

  public void removeItem(int index) {
    mutable();
    if (index < 0 || index >= items.size())
      throw new IllegalArgumentException("Select an item to remove.");
    items.remove(index);
  }

  private void mutable() {
    if (orderID > 0) throw new IllegalStateException("A submitted order cannot be edited.");
  }

  void markSubmitted(int id) {
    mutable();
    if (id <= 0) throw new IllegalArgumentException("Invalid order ID.");
    orderID = id;
  }

  public int getOrderID() {
    return orderID;
  }

  public int getCustID() {
    return custID;
  }

  public int getItemCount() {
    return items.size();
  }

  public List<Meal> items() {
    return List.copyOf(items);
  }

  public BigDecimal total() {
    return items.stream().map(Meal::price).reduce(new BigDecimal("0.00"), BigDecimal::add);
  }

  public double getTotalAmount() {
    return total().doubleValue();
  }
}
