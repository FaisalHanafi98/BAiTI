package my.baiti.people;

public record CreatedUserResponse(UserResponse user, String temporaryPassword) {
  @Override
  public String toString() {
    return "CreatedUserResponse[redacted]";
  }
}
