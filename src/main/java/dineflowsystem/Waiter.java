package dineflowsystem;

public final class Waiter extends Staff {
  public Waiter(String id, String name, String contact) {
    super(id, name, contact);
    if (!id.startsWith("W")) throw new IllegalArgumentException("Waiter ID must start with W.");
    ratePerHour = 10;
  }

  @Override
  public String role() {
    return "Waiter";
  }
}
