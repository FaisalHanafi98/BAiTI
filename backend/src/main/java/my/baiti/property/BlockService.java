package my.baiti.property;

import java.util.Map;
import java.util.UUID;
import my.baiti.common.ResourceNotFoundException;
import my.baiti.system.PropertyActivity.Action;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BlockService {
  private final BlockRepository blocks;
  private final PropertyAudit audit;

  public BlockService(BlockRepository blocks, PropertyAudit audit) {
    this.blocks = blocks;
    this.audit = audit;
  }

  public PropertyPage<BlockResponse> list(Pageable page) {
    var query = PropertyPage.bounded(page, Sort.by("name"), Map.of("name", "name"));
    return PropertyPage.from(blocks.findAll(query).map(BlockResponse::from));
  }

  public BlockResponse get(UUID id) {
    return BlockResponse.from(require(id));
  }

  @Transactional
  public BlockResponse create(CreateBlockRequest request) {
    var block = new Block();
    block.setName(request.name());
    block.setDescription(request.description());
    blocks.saveAndFlush(block);
    audit.record(Action.BLOCK_CREATED, block.getId());
    return BlockResponse.from(block);
  }

  @Transactional
  public BlockResponse update(UUID id, UpdateBlockRequest request) {
    var block = require(id);
    block.setName(request.name());
    block.setDescription(request.description());
    blocks.flush();
    audit.record(Action.BLOCK_UPDATED, id);
    return BlockResponse.from(block);
  }

  @Transactional
  public void delete(UUID id) {
    blocks.delete(require(id));
    blocks.flush();
    audit.record(Action.BLOCK_DELETED, id);
  }

  Block require(UUID id) {
    return blocks.findById(id).orElseThrow(ResourceNotFoundException::new);
  }
}
