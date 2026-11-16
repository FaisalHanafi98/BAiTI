package my.baiti.property;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(oneOf = {UnitResponse.class, GuardUnitResponse.class})
public sealed interface UnitView permits UnitResponse, GuardUnitResponse {}
