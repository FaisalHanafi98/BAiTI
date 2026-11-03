package my.baiti.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import my.baiti.people.UserIdentity;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService auth;
  private final OriginPolicy origins;
  private final TokenLifetimes lifetimes;

  public AuthController(AuthService auth, OriginPolicy origins, TokenLifetimes lifetimes) {
    this.auth = auth;
    this.origins = origins;
    this.lifetimes = lifetimes;
  }

  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return issued(auth.login(request.email(), request.password()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      @Valid @RequestBody(required = false) RefreshRequest body, HttpServletRequest request) {
    List<jakarta.servlet.http.Cookie> cookies =
        request.getCookies() == null
            ? List.of()
            : Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals("baiti_refresh"))
                .toList();
    if (cookies.size() > 1 || (!cookies.isEmpty() && body != null && body.token() != null)) {
      throw new BadCredentialsException("Authentication failed");
    }
    String token;
    if (!cookies.isEmpty()) {
      if (request.getHeader("Origin") == null
          || !origins.origins().contains(request.getHeader("Origin"))
          || !"true".equals(request.getHeader("X-BAiTI-Refresh"))) {
        throw new AccessDeniedException("Access denied");
      }
      token = cookies.get(0).getValue();
    } else {
      token = body == null ? null : body.token();
    }
    return issued(auth.refresh(token));
  }

  @GetMapping("/me")
  public ResponseEntity<UserIdentity> me(@AuthenticationPrincipal TenantPrincipal principal) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.me(principal));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@AuthenticationPrincipal TenantPrincipal principal) {
    auth.logout(principal);
    return ResponseEntity.noContent()
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.SET_COOKIE, refreshCookie("", java.time.Duration.ZERO).toString())
        .build();
  }

  private ResponseEntity<AuthResponse> issued(AuthService.LoginResult result) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.PRAGMA, "no-cache")
        .header(
            HttpHeaders.SET_COOKIE,
            refreshCookie(result.refreshToken(), lifetimes.refresh()).toString())
        .body(
            new AuthResponse(result.accessToken(), lifetimes.access().toSeconds(), result.user()));
  }

  private static ResponseCookie refreshCookie(String value, java.time.Duration age) {
    return ResponseCookie.from("baiti_refresh", value)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/api/auth/refresh")
        .maxAge(age)
        .build();
  }

  public record LoginRequest(
      @NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(max = 72) String password) {
    @Override
    public String toString() {
      return "LoginRequest[redacted]";
    }
  }

  public record RefreshRequest(
      @Size(min = 43, max = 43) @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token) {
    @Override
    public String toString() {
      return "RefreshRequest[redacted]";
    }
  }

  public record AuthResponse(String accessToken, long expiresIn, UserIdentity user) {
    @Override
    public String toString() {
      return "AuthResponse[redacted]";
    }
  }
}
