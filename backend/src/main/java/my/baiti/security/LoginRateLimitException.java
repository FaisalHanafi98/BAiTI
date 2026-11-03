package my.baiti.security;

public final class LoginRateLimitException extends RuntimeException {
  public LoginRateLimitException() {
    super("Too many requests");
  }
}
