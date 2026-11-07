package my.baiti.tenancy.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import my.baiti.common.ApiError;
import my.baiti.security.PlatformPrincipal;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tenants")
@PreAuthorize("hasAuthority('" + PlatformPrincipal.AUTHORITY + "')")
public class TenantAdminController {
  private final TenantOnboardingService tenants;

  public TenantAdminController(TenantOnboardingService tenants) {
    this.tenants = tenants;
  }

  @PostMapping
  public ResponseEntity<TenantOnboardingService.CreatedTenant> create(
      @AuthenticationPrincipal PlatformPrincipal actor,
      @Valid @RequestBody TenantRegistration registration,
      HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .cacheControl(CacheControl.noStore())
        .body(tenants.create(actor, registration, request.getRemoteAddr()));
  }

  @GetMapping
  public ResponseEntity<TenantOnboardingService.TenantPage> list(
      @AuthenticationPrincipal PlatformPrincipal actor,
      @ParameterObject @PageableDefault(size = 20, sort = "slug") Pageable pageable) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(tenants.list(actor, pageable));
  }

  @ExceptionHandler(TenantOnboardingService.SlugConflict.class)
  public ResponseEntity<ApiError> conflict(HttpServletRequest request) {
    return ApiError.response(request, HttpStatus.CONFLICT.value(), "Community slug already exists");
  }
}
