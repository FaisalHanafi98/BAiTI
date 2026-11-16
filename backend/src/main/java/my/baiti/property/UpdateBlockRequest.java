package my.baiti.property;

import jakarta.validation.constraints.NotBlank;

public record UpdateBlockRequest(@NotBlank String name, String description) {}
