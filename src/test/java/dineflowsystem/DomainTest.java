package dineflowsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DomainTest {
  @Test
  void orderPreservesCustomerIdentity() {
    assertEquals(42, new Order(42).getCustID());
  }

  @Test
  void moneyAddsExactly() {
    Order order = new Order(1);
    order.addItem(new Meal(1, "Small", .1));
    order.addItem(new Meal(2, "Large", .2));
    assertEquals(new BigDecimal("0.30"), order.total());
  }

  @Test
  void removingItemsRecalculatesTotal() {
    Order order = new Order(1);
    order.addItem(new Meal(1, "Ayam", 6));
    order.addItem(new Meal(2, "Sotong", 8));
    order.removeItem(0);
    assertEquals(new BigDecimal("8.00"), order.total());
  }

  @Test
  void moreThanTenItemsStayConsistent() {
    Order order = new Order(1);
    for (int i = 0; i < 12; i++) order.addItem(new Meal(1, "Ayam", 6));
    assertEquals(12, order.getItemCount());
    assertEquals(new BigDecimal("72.00"), order.total());
  }

  @Test
  void orderCapacityFailsExplicitly() {
    Order order = new Order(1);
    for (int i = 0; i < Order.MAX_ITEMS; i++) order.addItem(new Meal(1, "Ayam", 6));
    assertThrows(IllegalArgumentException.class, () -> order.addItem(new Meal(1, "Ayam", 6)));
    assertEquals(Order.MAX_ITEMS, order.getItemCount());
  }

  @Test
  void itemSnapshotIsImmutable() {
    Order order = new Order(1);
    order.addItem(new Meal(1, "Ayam", 6));
    assertThrows(UnsupportedOperationException.class, () -> order.items().clear());
  }

  @Test
  void submittedOrderCannotBeChanged() {
    Order order = new Order(1);
    order.addItem(new Meal(1, "Ayam", 6));
    order.markSubmitted(9);
    assertThrows(IllegalStateException.class, () -> order.removeItem(0));
    assertThrows(IllegalStateException.class, () -> order.addItem(new Meal(2, "Tea", 2)));
  }

  @Test
  void invalidInputsFailBeforeMutation() {
    assertThrows(IllegalArgumentException.class, () -> new Order(0));
    assertThrows(IllegalArgumentException.class, () -> new Meal(-1, "Bad", 3));
    assertThrows(IllegalArgumentException.class, () -> new Meal(1, "Bad", -1));
    assertThrows(IllegalArgumentException.class, () -> new Order(1).removeItem(0));
  }

  @Test
  void menuRejectsDuplicateIdsAndDoesNotExposeItsArray() {
    Restaurant restaurant = new Restaurant();
    assertThrows(IllegalArgumentException.class, () -> restaurant.addMeal(new Meal(1, "Other", 5)));
    Meal[] array = restaurant.getMeal();
    array[0] = null;
    assertNotNull(restaurant.getMeal()[0]);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "X1", "W0", "W-1", "1", "F100000000", "R1x"})
  void factoryRejectsInvalidIds(String id) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new StaffDeployPattern().createStaff(id, "Demo", "0123456789"));
  }

  @Test
  void factoryNormalizesIdsAndPreservesRolePrefixes() {
    StaffDeployPattern factory = new StaffDeployPattern();
    assertInstanceOf(Waiter.class, factory.createStaff(" w1 ", "Demo", "0123456789"));
    assertInstanceOf(Cashier.class, factory.createStaff("R1", "Demo", "0123456789"));
    assertInstanceOf(Chef.class, factory.createStaff("F1", "Demo", "0123456789"));
    assertThrows(IllegalArgumentException.class, () -> new Waiter("F1", "Demo", "0123456789"));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-1, 744.01, Double.NaN, Double.POSITIVE_INFINITY, 1.001})
  void rejectsInvalidHours(double hours) {
    Staff staff = new Waiter("W1", "Demo", "0123456789");
    assertThrows(IllegalArgumentException.class, () -> staff.setHourWorkMonth(hours));
    assertEquals(0, staff.getHourWorkMonth());
  }

  @Test
  void salaryHandlesBoundaryOvertimeAndFractionalHours() {
    Staff staff = new Waiter("W1", "Demo", "0123456789");
    staff.setHourWorkMonth(160);
    assertEquals(new BigDecimal("1600.00"), staff.salary());
    staff.setHourWorkMonth(170);
    assertEquals(new BigDecimal("1750.00"), staff.salary());
    staff.setHourWorkMonth(160.25);
    assertEquals(new BigDecimal("1603.75"), staff.salary());
  }

  @Test
  void roleRatesMatchOriginalCoursework() {
    Staff chef = new Chef("F1", "Demo", "0123456789"),
        cashier = new Cashier("R1", "Demo", "0123456789");
    chef.setHourWorkMonth(170);
    cashier.setHourWorkMonth(170);
    assertEquals(new BigDecimal("3500.00"), chef.salary());
    assertEquals(new BigDecimal("2625.00"), cashier.salary());
  }

  @Test
  void customerDetailsAreValidated() {
    assertThrows(IllegalArgumentException.class, () -> new Customer(-1, "Demo", "0123456789"));
    assertThrows(IllegalArgumentException.class, () -> new Customer(1, " ", "0123456789"));
    assertThrows(IllegalArgumentException.class, () -> new Customer(1, "Demo", "abc"));
    assertThrows(IllegalArgumentException.class, () -> new Customer(1, "Bad\nName", "0123456789"));
  }

  @Test
  void stateMachineAllowsOnlyIntendedTransitions() {
    assertTrue(OrderStatus.QUEUED.canMoveTo(OrderStatus.PREPARING));
    assertTrue(OrderStatus.PREPARING.canMoveTo(OrderStatus.CANCELLED));
    assertFalse(OrderStatus.QUEUED.canMoveTo(OrderStatus.SERVED));
    assertFalse(OrderStatus.READY.canMoveTo(OrderStatus.CANCELLED));
    for (OrderStatus target : OrderStatus.values()) {
      assertFalse(OrderStatus.SERVED.canMoveTo(target));
      assertFalse(OrderStatus.CANCELLED.canMoveTo(target));
    }
  }
}
