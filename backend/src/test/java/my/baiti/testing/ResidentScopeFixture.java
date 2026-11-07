package my.baiti.testing;

import java.util.UUID;
import javax.sql.DataSource;

public final class ResidentScopeFixture implements AutoCloseable {

  private final AuthenticationFixture first;
  private final AuthenticationFixture second;
  private final UUID block = UUID.randomUUID();
  private final UUID firstUnit = UUID.randomUUID();
  private final UUID secondUnit = UUID.randomUUID();
  private final UUID firstResident = UUID.randomUUID();
  private final UUID secondResident = UUID.randomUUID();

  public static ResidentScopeFixture create(DataSource source, UUID tenant) {
    var first = new AuthenticationFixture(source, tenant, "Synthetic scope password");
    AuthenticationFixture second = null;
    try {
      second = new AuthenticationFixture(source, tenant, "Synthetic scope password");
      return new ResidentScopeFixture(tenant, first, second);
    } catch (RuntimeException | Error failure) {
      try {
        if (second != null) {
          second.close();
        }
      } finally {
        first.close();
      }
      throw failure;
    }
  }

  private ResidentScopeFixture(
      UUID tenant, AuthenticationFixture first, AuthenticationFixture second) {
    this.first = first;
    this.second = second;
    first.scoped(
        jdbc -> {
          jdbc.update(
              "INSERT INTO blocks (id, tenant_id, name) VALUES (?, ?, ?)",
              block,
              tenant,
              "Synthetic scope " + block);
          for (var unit : new UUID[] {firstUnit, secondUnit}) {
            jdbc.update(
                "INSERT INTO units (id, tenant_id, block_id, unit_no) VALUES (?, ?, ?, ?)",
                unit,
                tenant,
                block,
                "Synthetic " + unit);
          }
          jdbc.update(
              "INSERT INTO residents (id, tenant_id, user_id, unit_id, name, resident_type)"
                  + " VALUES (?, ?, ?, ?, ?, 'OWNER')",
              firstResident,
              tenant,
              first.userId(),
              firstUnit,
              "Synthetic first resident");
          jdbc.update(
              "INSERT INTO residents (id, tenant_id, user_id, unit_id, name, resident_type)"
                  + " VALUES (?, ?, ?, ?, ?, 'OWNER')",
              secondResident,
              tenant,
              second.userId(),
              secondUnit,
              "Synthetic second resident");
        });
  }

  public UUID firstUser() {
    return first.userId();
  }

  public UUID firstResident() {
    return firstResident;
  }

  public UUID secondResident() {
    return secondResident;
  }

  public UUID firstUnit() {
    return firstUnit;
  }

  public UUID secondUnit() {
    return secondUnit;
  }

  public void moveFirstOut() {
    first.scoped(
        jdbc ->
            jdbc.update("UPDATE residents SET status = 'MOVED_OUT' WHERE id = ?", firstResident));
  }

  public void unlinkFirstUser() {
    first.scoped(
        jdbc -> jdbc.update("UPDATE residents SET user_id = NULL WHERE id = ?", firstResident));
  }

  @Override
  public void close() {
    first.scoped(
        jdbc -> {
          jdbc.update("DELETE FROM residents WHERE id IN (?, ?)", firstResident, secondResident);
          jdbc.update("DELETE FROM units WHERE id IN (?, ?)", firstUnit, secondUnit);
          jdbc.update("DELETE FROM blocks WHERE id = ?", block);
        });
    try {
      second.close();
    } finally {
      first.close();
    }
  }
}
