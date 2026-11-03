package my.baiti.tenancy;

public final class TenantResolutionException extends RuntimeException {
  public TenantResolutionException() {
    super("Authentication failed");
  }
}
