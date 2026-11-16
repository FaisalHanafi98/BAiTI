package my.baiti.property;

final class PropertyQueryException extends RuntimeException {
  private final String field;

  PropertyQueryException(String field, String message) {
    super(message);
    this.field = field;
  }

  String field() {
    return field;
  }
}
