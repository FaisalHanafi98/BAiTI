package my.baiti.property;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PropertyPage<T>(
    List<T> content, long totalElements, int totalPages, int page, int size) {
  static <T> PropertyPage<T> from(Page<T> result) {
    return new PropertyPage<>(
        result.getContent(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.getNumber(),
        result.getSize());
  }

  static Pageable bounded(Pageable page, Sort defaultSort, Map<String, String> fields) {
    var sort = defaultSort;
    if (page.getSort().isSorted()) {
      sort =
          Sort.by(
              page.getSort().stream()
                  .map(
                      order -> {
                        var property = fields.get(order.getProperty());
                        if (property == null) {
                          throw new PropertyQueryException("sort", "Unsupported sort field");
                        }
                        return order.withProperty(property);
                      })
                  .toList());
    }
    return PageRequest.of(
        page.getPageNumber(), Math.min(page.getPageSize(), 100), sort.and(Sort.by("id")));
  }
}
