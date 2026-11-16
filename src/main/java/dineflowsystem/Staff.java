package dineflowsystem;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Original abstract hierarchy with a shared validated demonstration payroll policy. */
public abstract class Staff {
  protected final String staffID, staffName, staffContact;
  protected double hourWorkMonth, ratePerHour;

  protected Staff(String id, String name, String contact) {
    if (id == null || !id.matches("[WRF][1-9][0-9]{0,7}"))
      throw new IllegalArgumentException(
          "Use W, R, or F followed by 1–8 digits, starting with 1–9.");
    staffID = id;
    staffName = Validation.text(name, "Staff name", 80);
    staffContact = Validation.contact(contact);
  }

  public void setHourWorkMonth(double hours) {
    if (!Double.isFinite(hours)
        || hours < 0
        || hours > 744
        || BigDecimal.valueOf(hours).stripTrailingZeros().scale() > 2)
      throw new IllegalArgumentException(
          "Monthly hours must be 0–744, with at most two decimal places.");
    hourWorkMonth = hours;
  }

  public BigDecimal salary() {
    BigDecimal hours = BigDecimal.valueOf(hourWorkMonth);
    BigDecimal regular = hours.min(new BigDecimal("160"));
    return regular
        .add(hours.subtract(regular).multiply(new BigDecimal("1.5")))
        .multiply(BigDecimal.valueOf(ratePerHour))
        .setScale(2, RoundingMode.HALF_UP);
  }

  public double calculateSalary() {
    return salary().doubleValue();
  }

  public abstract String role();

  public String getStaffID() {
    return staffID;
  }

  public String getStaffName() {
    return staffName;
  }

  public String getStaffContact() {
    return staffContact;
  }

  public double getHourWorkMonth() {
    return hourWorkMonth;
  }

  public double getRatePerHour() {
    return ratePerHour;
  }
}
