package dineflowsystem;

final class Validation {
  private Validation() {}

  static String text(String value, String field, int limit) {
    if (value == null
        || value.isBlank()
        || value.trim().length() > limit
        || value.codePoints().anyMatch(Character::isISOControl))
      throw new IllegalArgumentException(
          field + " is required, must be plain text, and cannot exceed " + limit + " characters.");
    return value.trim();
  }

  static String contact(String value) {
    String contact = text(value, "Contact", 30);
    if (!contact.matches("[+0-9 ()-]{6,30}") || contact.replaceAll("\\D", "").length() < 6)
      throw new IllegalArgumentException("Enter a contact number containing at least six digits.");
    return contact;
  }
}
