package my.baiti.property;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import my.baiti.security.TenantPrincipal;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/units")
public class UnitController {
  private static final String READ = "hasAuthority('UNIT_READ_ALL')";
  private static final String MANAGE = "hasAuthority('UNIT_MANAGE')";
  private final UnitService units;

  public UnitController(UnitService units) {
    this.units = units;
  }

  @GetMapping
  @Operation(
      description =
          "Guard responses include only unit numbers. Guards can sort by unit number. Block and occupancy filters require access to unit details.")
  @PreAuthorize(READ)
  public PropertyPage<UnitView> list(
      @AuthenticationPrincipal TenantPrincipal actor,
      @RequestParam(required = false) UUID blockId,
      @RequestParam(required = false) String status,
      @ParameterObject @PageableDefault(size = 20) Pageable page) {
    return units.list(actor, blockId, status, page);
  }

  @GetMapping("/{id}")
  @Operation(description = "Guard responses include only the unit number.")
  @PreAuthorize(READ)
  public UnitView get(@AuthenticationPrincipal TenantPrincipal actor, @PathVariable UUID id) {
    return units.get(actor, id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MANAGE)
  public ResponseEntity<UnitResponse> create(@Valid @RequestBody CreateUnitRequest request) {
    var unit = units.create(request);
    return ResponseEntity.created(URI.create("/api/units/" + unit.id())).body(unit);
  }

  @PutMapping("/{id}")
  @PreAuthorize(MANAGE)
  public UnitResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUnitRequest request) {
    return units.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(MANAGE)
  public void delete(@PathVariable UUID id) {
    units.delete(id);
  }
}
