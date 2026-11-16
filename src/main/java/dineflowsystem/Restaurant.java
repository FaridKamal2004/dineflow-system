package dineflowsystem;

import java.util.ArrayList;
import java.util.List;

public final class Restaurant {
  private final List<Meal> meals = new ArrayList<>();

  public Restaurant() {
    addMeal(new Meal(1, "Nasi Lemak Ayam", 6));
    addMeal(new Meal(2, "Nasi Lemak Sotong", 8));
    addMeal(new Meal(3, "Nasi Lemak Siakap", 17));
  }

  public void addMeal(Meal meal) {
    if (meal == null || meals.stream().anyMatch(m -> m.getMealID() == meal.getMealID()))
      throw new IllegalArgumentException("Meal ID must be unique.");
    meals.add(meal);
  }

  public Meal[] getMeal() {
    return meals.toArray(Meal[]::new);
  }

  public int getMealCount() {
    return meals.size();
  }
}
