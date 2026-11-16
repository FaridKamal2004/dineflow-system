package dineflowsystem;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Immutable menu entry; amounts are represented with exact decimal arithmetic. */
public final class Meal {
  private final int mealID;
  private final String mealName;
  private final BigDecimal price;

  public Meal(int id, String name, double price) {
    this(id, name, BigDecimal.valueOf(price));
  }

  public Meal(int id, String name, BigDecimal price) {
    if (id <= 0) throw new IllegalArgumentException("Meal ID must be positive.");
    if (price == null || price.signum() < 0 || price.compareTo(new BigDecimal("10000")) > 0)
      throw new IllegalArgumentException("Price must be between RM 0 and RM 10,000.");
    this.mealID = id;
    this.mealName = Validation.text(name, "Meal name", 80);
    this.price = price.setScale(2, RoundingMode.UNNECESSARY);
  }

  public int getMealID() {
    return mealID;
  }

  public String getMealName() {
    return mealName;
  }

  public double getMealPrice() {
    return price.doubleValue();
  }

  public BigDecimal price() {
    return price;
  }

  @Override
  public String toString() {
    return mealName;
  }
}
