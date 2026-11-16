package my.baiti.property;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blocks")
@PreAuthorize("hasAuthority('BLOCK_MANAGE')")
public class BlockController {
  private final BlockService blocks;

  public BlockController(BlockService blocks) {
    this.blocks = blocks;
  }

  @GetMapping
  public PropertyPage<BlockResponse> list(
      @ParameterObject @PageableDefault(size = 20) Pageable page) {
    return blocks.list(page);
  }

  @GetMapping("/{id}")
  public BlockResponse get(@PathVariable UUID id) {
    return blocks.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<BlockResponse> create(@Valid @RequestBody CreateBlockRequest request) {
    var block = blocks.create(request);
    return ResponseEntity.created(URI.create("/api/blocks/" + block.id())).body(block);
  }

  @PutMapping("/{id}")
  public BlockResponse update(
      @PathVariable UUID id, @Valid @RequestBody UpdateBlockRequest request) {
    return blocks.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    blocks.delete(id);
  }
}
