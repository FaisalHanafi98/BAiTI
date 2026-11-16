package my.baiti.property;

import java.util.Map;
import java.util.UUID;
import my.baiti.common.ResourceNotFoundException;
import my.baiti.security.TenantPrincipal;
import my.baiti.system.PropertyActivity.Action;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UnitService {
  private static final Map<String, String> SORT_FIELDS =
      Map.of(
          "blockName",
          "block.name",
          "unitNo",
          "unitNo",
          "status",
          "status",
          "floorNo",
          "floorNo",
          "type",
          "type");
  private static final Sort DEFAULT_SORT = Sort.by("block.name", "unitNo");
  private static final Sort GUARD_SORT = Sort.by("unitNo");

  private final UnitRepository units;
  private final BlockService blocks;
  private final PropertyAudit audit;

  public UnitService(UnitRepository units, BlockService blocks, PropertyAudit audit) {
    this.units = units;
    this.blocks = blocks;
    this.audit = audit;
  }

  public PropertyPage<UnitView> list(
      TenantPrincipal actor, UUID blockId, String status, Pageable page) {
    boolean guard = actor.roles().contains("Guard");
    if (guard && (blockId != null || status != null)) {
      throw new AccessDeniedException("Unit filters require access to unit details");
    }
    var query =
        PropertyPage.bounded(
            page,
            guard ? GUARD_SORT : DEFAULT_SORT,
            guard ? Map.of("unitNo", "unitNo") : SORT_FIELDS);
    var filter = Specification.<Unit>unrestricted();
    if (blockId != null) {
      blocks.require(blockId);
      filter =
          filter.and(
              (unit, criteria, builder) -> builder.equal(unit.get("block").get("id"), blockId));
    }
    if (status != null) {
      var occupancy = occupancy(status);
      filter =
          filter.and((unit, criteria, builder) -> builder.equal(unit.get("status"), occupancy));
    }
    return PropertyPage.from(units.findAll(filter, query).map(unit -> view(unit, guard)));
  }

  public UnitView get(TenantPrincipal actor, UUID id) {
    return view(require(id), actor.roles().contains("Guard"));
  }

  @Transactional
  public UnitResponse create(CreateUnitRequest request) {
    var unit = new Unit();
    apply(
        unit,
        request.blockId(),
        request.unitNo(),
        request.type(),
        request.floorNo(),
        request.status() == null ? Unit.Status.VACANT : request.status());
    units.saveAndFlush(unit);
    audit.record(Action.UNIT_CREATED, unit.getId());
    return UnitResponse.from(unit);
  }

  @Transactional
  public UnitResponse update(UUID id, UpdateUnitRequest request) {
    var unit = require(id);
    apply(
        unit,
        request.blockId(),
        request.unitNo(),
        request.type(),
        request.floorNo(),
        request.status());
    units.flush();
    audit.record(Action.UNIT_UPDATED, id);
    return UnitResponse.from(unit);
  }

  @Transactional
  public void delete(UUID id) {
    units.delete(require(id));
    units.flush();
    audit.record(Action.UNIT_DELETED, id);
  }

  private void apply(
      Unit unit, UUID blockId, String unitNo, String type, String floorNo, Unit.Status status) {
    unit.setBlock(blocks.require(blockId));
    unit.setUnitNo(unitNo);
    unit.setType(type);
    unit.setFloorNo(floorNo);
    unit.setStatus(status);
  }

  private Unit require(UUID id) {
    return units.findById(id).orElseThrow(ResourceNotFoundException::new);
  }

  private static UnitView view(Unit unit, boolean guard) {
    return guard ? new GuardUnitResponse(unit.getUnitNo()) : UnitResponse.from(unit);
  }

  private static Unit.Status occupancy(String status) {
    try {
      return Unit.Status.valueOf(status);
    } catch (IllegalArgumentException failure) {
      throw new PropertyQueryException("status", "Use OCCUPIED or VACANT");
    }
  }
}
