package my.baiti.people;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PeoplePage<T>(
    List<T> content, long totalElements, int totalPages, int page, int size) {
  static <T> PeoplePage<T> from(Page<T> result) {
    return new PeoplePage<>(
        result.getContent(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.getNumber(),
        result.getSize());
  }

  static Pageable bounded(Pageable page, Set<String> fields) {
    var sort = page.getSort().isSorted() ? page.getSort() : Sort.by("name");
    if (sort.stream().anyMatch(order -> !fields.contains(order.getProperty()))) {
      throw new PeopleQueryException("Unsupported sort field");
    }
    return PageRequest.of(
        page.getPageNumber(), Math.min(page.getPageSize(), 100), sort.and(Sort.by("id")));
  }
}
