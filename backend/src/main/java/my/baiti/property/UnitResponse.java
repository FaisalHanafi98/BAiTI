package my.baiti.property;

import java.util.UUID;

public record UnitResponse(
    UUID id,
    UUID blockId,
    String blockName,
    String unitNo,
    String type,
    String floorNo,
    Unit.Status status)
    implements UnitView {
  static UnitResponse from(Unit unit) {
    return new UnitResponse(
        unit.getId(),
        unit.getBlock().getId(),
        unit.getBlock().getName(),
        unit.getUnitNo(),
        unit.getType(),
        unit.getFloorNo(),
        unit.getStatus());
  }
}
