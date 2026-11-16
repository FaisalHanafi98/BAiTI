package my.baiti.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateUnitRequest(
    @NotNull UUID blockId,
    @NotBlank String unitNo,
    String type,
    String floorNo,
    Unit.Status status) {}
