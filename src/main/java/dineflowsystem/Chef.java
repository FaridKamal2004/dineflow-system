package dineflowsystem;

public final class Chef extends Staff {
  public Chef(String id, String name, String contact) {
    super(id, name, contact);
    if (!id.startsWith("F")) throw new IllegalArgumentException("Chef ID must start with F.");
    ratePerHour = 20;
  }

  @Override
  public String role() {
    return "Chef";
  }
}
