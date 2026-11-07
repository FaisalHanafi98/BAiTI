package my.baiti.tenancy.admin;

import jakarta.validation.Valid;
import my.baiti.security.AuthController.LoginRequest;
import my.baiti.security.JwtService;
import my.baiti.security.TokenLifetimes;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlatformAuthController {
  private final PlatformAuthentication authentication;
  private final JwtService jwt;
  private final TokenLifetimes lifetimes;

  public PlatformAuthController(
      PlatformAuthentication authentication, JwtService jwt, TokenLifetimes lifetimes) {
    this.authentication = authentication;
    this.jwt = jwt;
    this.lifetimes = lifetimes;
  }

  @PostMapping(value = "/api/admin/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PlatformAccess> login(@Valid @RequestBody LoginRequest request) {
    var principal = authentication.login(request.email(), request.password());
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(new PlatformAccess(jwt.issuePlatform(principal), lifetimes.access().toSeconds()));
  }

  public record PlatformAccess(String accessToken, long expiresIn) {
    @Override
    public String toString() {
      return "PlatformAccess[redacted]";
    }
  }
}
