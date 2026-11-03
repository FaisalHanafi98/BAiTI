package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import my.baiti.common.TenantAwareBaseEntity;
import my.baiti.community.Announcement;
import my.baiti.community.Event;
import my.baiti.complaints.Complaint;
import my.baiti.complaints.ComplaintCategory;
import my.baiti.complaints.ComplaintUpdate;
import my.baiti.config.DataSourceConfig;
import my.baiti.config.JpaConfig;
import my.baiti.facilities.Facility;
import my.baiti.facilities.FacilityBooking;
import my.baiti.finance.Bill;
import my.baiti.finance.Payment;
import my.baiti.finance.PaymentMethod;
import my.baiti.maintenance.MaintenanceRequest;
import my.baiti.people.FamilyMember;
import my.baiti.people.Permission;
import my.baiti.people.Resident;
import my.baiti.people.Role;
import my.baiti.people.User;
import my.baiti.property.Block;
import my.baiti.property.Unit;
import my.baiti.security.RefreshToken;
import my.baiti.visitors.Visitor;
import my.baiti.visitors.VisitorPreRegistration;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({DataSourceConfig.class, JpaConfig.class, FoundationTenantConfiguration.class})
class PersistenceConstraintIT extends NativePostgresTestSupport {

  private static final List<KeyCase> KEYS =
      List.of(
          new KeyCase("roles", "uq_roles_tenant_name"),
          new KeyCase("permissions", "uq_permissions_tenant_code"),
          new KeyCase("users", "uq_users_tenant_email"),
          new KeyCase("refresh_tokens", "refresh_tokens_token_hash_key"),
          new KeyCase("blocks", "uq_blocks_tenant_name"),
          new KeyCase("units", "uq_units_tenant_block_unit"),
          new KeyCase("residents", "uq_residents_active_portal_account"),
          new KeyCase("payment_methods", "uq_payment_methods_tenant_name"),
          new KeyCase("bills", "uq_bills_unit_month_type"),
          new KeyCase("complaint_categories", "uq_complaint_categories_tenant_name"),
          new KeyCase("facilities", "uq_facilities_tenant_name"),
          new KeyCase("facility_bookings", "uq_facility_bookings_slot"),
          new KeyCase("visitor_pre_registrations", "uq_visitor_pre_reg_approval_code"));

  private static final List<EnumCase> ENUMS =
      List.of(
          new EnumCase("users", User.class, "status"),
          new EnumCase("units", Unit.class, "status"),
          new EnumCase("residents", Resident.class, "residentType"),
          new EnumCase("residents", Resident.class, "status"),
          new EnumCase("bills", Bill.class, "status"),
          new EnumCase("complaints", Complaint.class, "status"),
          new EnumCase("complaint_updates", ComplaintUpdate.class, "status"),
          new EnumCase("maintenance_requests", MaintenanceRequest.class, "priority"),
          new EnumCase("maintenance_requests", MaintenanceRequest.class, "status"),
          new EnumCase("facilities", Facility.class, "status"),
          new EnumCase("facility_bookings", FacilityBooking.class, "status"),
          new EnumCase("announcements", Announcement.class, "targetAudience"),
          new EnumCase("visitor_pre_registrations", VisitorPreRegistration.class, "status"),
          new EnumCase("visitors", Visitor.class, "status"));

  private final EntityManager entities;
  private Role role;
  private User user;
  private Block block;
  private Unit unit;
  private Resident resident;

  @Autowired
  PersistenceConstraintIT(EntityManager entities) {
    this.entities = entities;
  }

  @BeforeEach
  void createApplicationRoleFixture() {
    assertThat(entities.createNativeQuery("SELECT current_user", String.class).getSingleResult())
        .isEqualTo("baiti_app");
    entities
        .unwrap(Session.class)
        .doWork(
            connection -> {
              try (var statement =
                  connection.prepareStatement("SELECT set_config('app.current_tenant', ?, true)")) {
                statement.setString(1, FoundationTenantConfiguration.TENANT.toString());
                statement.execute();
              }
            });
    role = new Role();
    role.setName(key());
    store(role);
    user = new User();
    user.setRole(role);
    user.setName("Synthetic constraint user");
    user.setEmail(key() + "@example.invalid");
    user.setPasswordHash("synthetic-not-a-login-hash");
    store(user);
    block = new Block();
    block.setName(key());
    store(block);
    unit = new Unit();
    unit.setBlock(block);
    unit.setUnitNo(key());
    store(unit);
    resident = new Resident();
    resident.setUser(user);
    resident.setUnit(unit);
    resident.setName("Synthetic constraint resident");
    resident.setResidentType(Resident.ResidentType.OWNER);
    store(resident);
    entities.flush();
  }

  static Stream<Arguments> businessKeys() {
    return KEYS.stream().map(value -> Arguments.of(value.table(), value.constraint()));
  }

  @ParameterizedTest
  @MethodSource("businessKeys")
  void duplicateBusinessKeysFailTheirNamedDatabaseConstraint(String table, String constraint) {
    var row = fixture(table);
    entities.flush();
    assertThat(visibleCount(table, row.getId())).isEqualTo(1);
    assertDatabaseFailure(() -> copy(row, null, null), "23505", constraint);
  }

  @Test
  void everyBusinessUniqueIndexHasAnExplicitBehaviourCase() {
    var expected = new ArrayList<>(KEYS.stream().map(KeyCase::constraint).toList());
    expected.add("tenants_slug_key");
    expected.add("super_admins_email_key");
    var actual =
        entities
            .unwrap(Session.class)
            .createNativeQuery(
                """
        SELECT i.relname FROM pg_index x
        JOIN pg_class i ON i.oid = x.indexrelid
        JOIN pg_class t ON t.oid = x.indrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'public' AND t.relname <> 'flyway_schema_history'
          AND x.indisunique AND NOT x.indisprimary
        """,
                String.class)
            .getResultList();
    assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
  }

  static Stream<Arguments> enumFields() {
    return ENUMS.stream().map(value -> Arguments.of(value.table(), value.type(), value.field()));
  }

  @ParameterizedTest
  @MethodSource("enumFields")
  void everyDeclaredEnumValueRoundTripsAsDatabaseText(String table, Class<?> type, String fieldName)
      throws Exception {
    var row = fixture(table);
    entities.flush();
    assertThat(row.getClass()).isEqualTo(type);
    var field = type.getDeclaredField(fieldName);
    field.setAccessible(true);
    String column = field.getAnnotation(Column.class).name();
    assertThat(column).matches("[a-z_]+");
    for (Object value : field.getType().getEnumConstants()) {
      field.set(row, value);
      entities.flush();
      entities.refresh(row);
      assertThat(field.get(row)).isEqualTo(value);
      assertThat(
              entities
                  .createNativeQuery(
                      "SELECT " + column + " FROM " + table + " WHERE id = :id", String.class)
                  .setParameter("id", row.getId())
                  .getSingleResult())
          .isEqualTo(((Enum<?>) value).name());
    }
  }

  @Test
  void everyEntityEnumFieldHasAnExplicitBehaviourCase() {
    var actual =
        entities.getMetamodel().getEntities().stream()
            .flatMap(type -> Arrays.stream(type.getJavaType().getDeclaredFields()))
            .filter(field -> field.isAnnotationPresent(Enumerated.class))
            .map(field -> field.getDeclaringClass().getSimpleName() + "." + field.getName())
            .toList();
    var expected =
        new ArrayList<>(
            ENUMS.stream()
                .map(value -> value.type().getSimpleName() + "." + value.field())
                .toList());
    expected.add("Tenant.status");
    expected.add("SuperAdmin.status");
    assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
  }

  @Test
  void movedOutResidencyDoesNotReserveTheActivePortalAccountKey() {
    UUID copyId = copy(resident, "status", "MOVED_OUT");
    assertThat(visibleCount("residents", copyId)).isEqualTo(1);
  }

  @Test
  void residentsWithoutPortalAccountsDoNotConflictWithEachOther() {
    assertThat(visibleCount("residents", copy(resident, "user_id", null))).isEqualTo(1);
    assertThat(visibleCount("residents", copy(resident, "user_id", null))).isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"CANCELLED", "REJECTED"})
  void inactiveBookingsDoNotReserveAnActiveSlot(String status) {
    var booking = fixture("facility_bookings");
    entities.flush();
    assertThat(visibleCount("facility_bookings", copy(booking, "status", status))).isEqualTo(1);
  }

  @Test
  void duplicateCompositeRoleGrantsAreRejectedByTheDatabase() {
    var permission = (Permission) fixture("permissions");
    entities.flush();
    role.grantPermission(permission);
    entities.merge(role);
    entities.flush();
    assertDatabaseFailure(
        () ->
            entities
                .createNativeQuery(
                    """
            INSERT INTO roles_permissions
            SELECT * FROM roles_permissions WHERE role_id = :role AND permission_id = :permission
            """)
                .setParameter("role", role.getId())
                .setParameter("permission", permission.getId())
                .executeUpdate(),
        "23505",
        "roles_permissions_pkey");
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 151})
  void familyAgeOutsideTheAuthoredRangeIsRejected(int age) {
    var member = (FamilyMember) fixture("family_members");
    entities.flush();
    assertDatabaseFailure(
        () -> {
          member.setAge((short) age);
          entities.flush();
        },
        "23514",
        null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.00", "-0.01"})
  void billsMustHavePositiveExactAmounts(String value) {
    var bill = (Bill) fixture("bills");
    entities.flush();
    assertDatabaseFailure(
        () -> {
          bill.setAmount(new BigDecimal(value));
          entities.flush();
        },
        "23514",
        null);
  }

  @Test
  void billingMonthMustBeTheFirstCalendarDay() {
    var bill = (Bill) fixture("bills");
    entities.flush();
    assertDatabaseFailure(
        () -> {
          bill.setBillingMonth(LocalDate.of(2026, 10, 2));
          entities.flush();
        },
        "23514",
        null);
  }

  @Test
  void paymentsMustHavePositiveAmountsAtInsertion() {
    var payment = fixture("payments");
    entities.flush();
    assertDatabaseFailure(() -> copy(payment, "amount_paid", BigDecimal.ZERO), "23514", null);
  }

  @Test
  void eventEndCannotPrecedeItsStart() {
    var event = (Event) fixture("events");
    entities.flush();
    assertDatabaseFailure(
        () -> {
          event.setEndDate(event.getStartDate().minusSeconds(1));
          entities.flush();
        },
        "23514",
        null);
  }

  @Test
  void visitorExitCannotPrecedeItsEntry() {
    var visitor = (Visitor) fixture("visitors");
    entities.flush();
    assertDatabaseFailure(
        () -> {
          visitor.setExitTime(visitor.getEntryTime().minusSeconds(1));
          entities.flush();
        },
        "23514",
        null);
  }

  private TenantAwareBaseEntity fixture(String table) {
    return switch (table) {
      case "roles" -> role;
      case "users" -> user;
      case "blocks" -> block;
      case "units" -> unit;
      case "residents" -> resident;
      case "permissions" -> {
        var value = new Permission();
        value.setCode(key());
        value.setModule("FOUNDATION");
        yield store(value);
      }
      case "refresh_tokens" -> {
        var value = new RefreshToken();
        value.setUser(user);
        value.setTokenHash(UUID.randomUUID().toString().replace("-", "").repeat(2));
        value.setFamilyId(UUID.randomUUID());
        value.setExpiresAt(Instant.parse("2030-01-01T00:00:00Z"));
        yield store(value);
      }
      case "payment_methods" -> {
        var value = new PaymentMethod();
        value.setName(key());
        yield store(value);
      }
      case "bills" -> {
        var value = new Bill();
        value.setUnit(unit);
        value.setBillingMonth(LocalDate.of(2026, 10, 1));
        value.setDueDate(LocalDate.of(2026, 10, 31));
        value.setAmount(new BigDecimal("100.25"));
        yield store(value);
      }
      case "payments" -> {
        var value = new Payment();
        value.setBill((Bill) fixture("bills"));
        value.setResident(resident);
        value.setPaymentMethod((PaymentMethod) fixture("payment_methods"));
        value.setAmountPaid(new BigDecimal("40.25"));
        yield store(value);
      }
      case "complaint_categories" -> {
        var value = new ComplaintCategory();
        value.setName(key());
        yield store(value);
      }
      case "complaints" -> {
        var value = new Complaint();
        value.setCategory((ComplaintCategory) fixture("complaint_categories"));
        value.setUnit(unit);
        value.setResident(resident);
        value.setTitle("Synthetic constraint complaint");
        value.setDescription("Synthetic persistence fixture");
        yield store(value);
      }
      case "complaint_updates" -> {
        var value = new ComplaintUpdate();
        value.setComplaint((Complaint) fixture("complaints"));
        value.setUser(user);
        value.setStatus(Complaint.Status.OPEN);
        value.setMessage("Synthetic constraint history");
        yield store(value);
      }
      case "maintenance_requests" -> {
        var value = new MaintenanceRequest();
        value.setUnit(unit);
        value.setRequestedBy(user);
        value.setDescription("Synthetic maintenance fixture");
        yield store(value);
      }
      case "facilities" -> {
        var value = new Facility();
        value.setName(key());
        value.setCapacity(10);
        yield store(value);
      }
      case "facility_bookings" -> {
        var value = new FacilityBooking();
        value.setFacility((Facility) fixture("facilities"));
        value.setResident(resident);
        value.setBookingDate(LocalDate.of(2026, 10, 29));
        value.setTimeSlot("09:00-10:00");
        yield store(value);
      }
      case "visitor_pre_registrations" -> {
        var value = new VisitorPreRegistration();
        value.setResident(resident);
        value.setVisitorName("Synthetic constraint visitor");
        value.setVisitDate(LocalDate.of(2026, 10, 29));
        value.setApprovalCode(key());
        yield store(value);
      }
      case "visitors" -> {
        var value = new Visitor();
        value.setResident(resident);
        value.setName("Synthetic constraint visitor");
        value.setEntryTime(Instant.parse("2026-10-29T01:00:00Z"));
        yield store(value);
      }
      case "announcements" -> {
        var value = new Announcement();
        value.setPostedBy(user);
        value.setTitle("Synthetic constraint announcement");
        value.setContent("Synthetic fixture content");
        yield store(value);
      }
      case "family_members" -> {
        var value = new FamilyMember();
        value.setResident(resident);
        value.setName("Synthetic constraint family member");
        value.setRelationship("SIBLING");
        value.setAge((short) 30);
        yield store(value);
      }
      case "events" -> {
        var value = new Event();
        value.setTitle("Synthetic constraint event");
        value.setStartDate(Instant.parse("2026-10-29T01:00:00Z"));
        value.setEndDate(Instant.parse("2026-10-29T02:00:00Z"));
        value.setCreatedBy(user.getId());
        yield store(value);
      }
      default -> throw new IllegalArgumentException("Unknown synthetic fixture table");
    };
  }

  private UUID copy(TenantAwareBaseEntity row, String overrideColumn, Object overrideValue) {
    String table = row.getClass().getAnnotation(Table.class).name();
    assertThat(table).matches("[a-z_]+");
    String extra = "";
    if (overrideColumn != null) {
      assertThat(overrideColumn).matches("[a-z_]+");
      extra =
          ", '"
              + overrideColumn
              + "', "
              + (overrideValue == null ? "CAST(NULL AS text)" : ":copyValue");
    }
    UUID id = UUID.randomUUID();
    String sql =
            """
        INSERT INTO %1$s
        SELECT (jsonb_populate_record(CAST(NULL AS %1$s), to_jsonb(record_source)
          || jsonb_build_object('id', CAST(:copyId AS uuid)%2$s))).*
        FROM %1$s record_source WHERE id = :sourceId
        """
            .formatted(table, extra);
    var query =
        entities
            .createNativeQuery(sql)
            .setParameter("copyId", id)
            .setParameter("sourceId", row.getId());
    if (overrideColumn != null && overrideValue != null) {
      query.setParameter("copyValue", overrideValue);
    }
    assertThat(query.executeUpdate()).isEqualTo(1);
    return id;
  }

  private long visibleCount(String table, UUID id) {
    return ((Number)
            entities
                .createNativeQuery("SELECT count(*) FROM " + table + " WHERE id = :id")
                .setParameter("id", id)
                .getSingleResult())
        .longValue();
  }

  private static void assertDatabaseFailure(Runnable action, String state, String constraint) {
    Throwable failure = catchThrowable(action::run);
    assertThat(failure).isInstanceOf(PersistenceException.class);
    while (failure.getCause() != null) {
      failure = failure.getCause();
    }
    assertThat(failure).isInstanceOf(SQLException.class);
    assertThat(((SQLException) failure).getSQLState()).isEqualTo(state);
    if (constraint != null) {
      assertThat(failure.getMessage()).contains(constraint);
    }
  }

  private <T extends TenantAwareBaseEntity> T store(T row) {
    entities.persist(row);
    return row;
  }

  private static String key() {
    return "F0_" + UUID.randomUUID();
  }

  private record KeyCase(String table, String constraint) {}

  private record EnumCase(String table, Class<?> type, String field) {}
}
