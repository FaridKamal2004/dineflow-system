package dineflowsystem;

public enum OrderStatus {
  QUEUED,
  PREPARING,
  READY,
  SERVED,
  CANCELLED;

  public boolean canMoveTo(OrderStatus target) {
    return target != null
        && switch (this) {
          case QUEUED -> target == PREPARING || target == CANCELLED;
          case PREPARING -> target == READY || target == CANCELLED;
          case READY -> target == SERVED;
          case SERVED, CANCELLED -> false;
        };
  }

  public OrderStatus next() {
    return switch (this) {
      case QUEUED -> PREPARING;
      case PREPARING -> READY;
      case READY -> SERVED;
      default -> null;
    };
  }
}
