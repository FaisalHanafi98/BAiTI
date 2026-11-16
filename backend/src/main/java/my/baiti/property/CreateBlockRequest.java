package my.baiti.property;

import jakarta.validation.constraints.NotBlank;

public record CreateBlockRequest(@NotBlank String name, String description) {}
