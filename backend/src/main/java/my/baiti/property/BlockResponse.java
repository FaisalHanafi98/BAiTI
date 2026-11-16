package my.baiti.property;

import java.util.UUID;

public record BlockResponse(UUID id, String name, String description) {
  static BlockResponse from(Block block) {
    return new BlockResponse(block.getId(), block.getName(), block.getDescription());
  }
}
