package dineflowsystem;

public final class Cashier extends Staff {
  public Cashier(String id, String name, String contact) {
    super(id, name, contact);
    if (!id.startsWith("R")) throw new IllegalArgumentException("Cashier ID must start with R.");
    ratePerHour = 15;
  }

  @Override
  public String role() {
    return "Cashier";
  }
}
