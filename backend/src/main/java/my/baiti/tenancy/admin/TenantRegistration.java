package my.baiti.tenancy.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

public record TenantRegistration(
    @NotBlank @Pattern(regexp = "[a-z0-9-]{3,50}") String slug,
    @NotBlank @Size(max = 160) String name,
    @NotBlank @Email @Size(max = 320) String adminEmail,
    @NotBlank @Size(max = 160) String adminName,
    @NotBlank @Size(min = 12, max = 72) String initialPassword) {

  @JsonIgnore
  @AssertTrue(message = "Initial password must fit within 72 UTF-8 bytes")
  public boolean isInitialPasswordByteLengthValid() {
    return initialPassword == null || initialPassword.getBytes(StandardCharsets.UTF_8).length <= 72;
  }

  @Override
  public String toString() {
    return "TenantRegistration[redacted]";
  }
}
