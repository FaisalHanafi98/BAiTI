package my.baiti.people;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PermissionCatalogTest {

  @Test
  void eachDefaultRoleReceivesExactlyTheGrantsInTheAcceptedMatrix() throws Exception {
    try (var reader =
        new BufferedReader(
            new InputStreamReader(
                getClass().getResourceAsStream("/fixtures/permission-matrix.csv"),
                StandardCharsets.UTF_8))) {
      var header = reader.readLine().split(",");
      var rows = reader.lines().map(line -> line.split(",")).toList();
      Map<String, Set<PermissionCatalog>> expected = new LinkedHashMap<>();
      for (int column = 2; column < header.length; column++) {
        int roleColumn = column;
        expected.put(
            header[column],
            rows.stream()
                .filter(row -> Boolean.parseBoolean(row[roleColumn]))
                .map(row -> PermissionCatalog.valueOf(row[0]))
                .collect(Collectors.toSet()));
      }
      assertThat(DefaultRoleGrants.all()).containsExactlyInAnyOrderEntriesOf(expected);
      assertThat(DefaultRoleGrants.forRole("SuperAdmin")).isEmpty();
      assertThat(DefaultRoleGrants.forRole("Unknown")).isEmpty();
    }
  }

  @Test
  void tenantCatalogueMatchesEveryPermissionAndModuleInTheAcceptedMatrix() throws Exception {
    try (var reader =
        new BufferedReader(
            new InputStreamReader(
                getClass().getResourceAsStream("/fixtures/permission-matrix.csv"),
                StandardCharsets.UTF_8))) {
      var expected =
          reader
              .lines()
              .skip(1)
              .map(line -> line.split(","))
              .collect(Collectors.toMap(columns -> columns[0], columns -> columns[1]));
      var actual =
          Arrays.stream(PermissionCatalog.values())
              .collect(Collectors.toMap(Enum::name, PermissionCatalog::module));
      assertThat(actual).containsExactlyInAnyOrderEntriesOf(expected);
      assertThat(actual).doesNotContainKeys("TENANT_CREATE", "TENANT_SUSPEND", "PLATFORM_LOG_READ");
    }
  }
}
