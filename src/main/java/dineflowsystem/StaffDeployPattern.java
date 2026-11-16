package dineflowsystem;

import java.util.Locale;

/** Factory retained from the coursework design, with explicit input validation. */
public final class StaffDeployPattern {
  public Staff createStaff(String id, String name, String contact) {
    String normalized = id == null ? "" : id.trim().toUpperCase(Locale.ROOT);
    if (!normalized.matches("[WRF][1-9][0-9]{0,7}"))
      throw new IllegalArgumentException("Staff ID example: W1 (waiter), R1 (cashier), F1 (chef).");
    return switch (normalized.charAt(0)) {
      case 'W' -> new Waiter(normalized, name, contact);
      case 'R' -> new Cashier(normalized, name, contact);
      case 'F' -> new Chef(normalized, name, contact);
      default -> throw new IllegalArgumentException("Unknown staff role.");
    };
  }
}
