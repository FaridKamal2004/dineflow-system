package dineflowsystem;

public final class Customer {
  private final int custID;
  private final String custName, custContact;

  public Customer(int id, String name, String contact) {
    if (id <= 0) throw new IllegalArgumentException("Customer ID must be a positive whole number.");
    custID = id;
    custName = Validation.text(name, "Customer name", 80);
    custContact = Validation.contact(contact);
  }

  public int getCustID() {
    return custID;
  }

  public String getCustName() {
    return custName;
  }

  public String getCustContact() {
    return custContact;
  }
}
