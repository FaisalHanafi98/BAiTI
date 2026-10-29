package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import my.baiti.people.Resident;
import my.baiti.people.Role;
import my.baiti.people.User;
import my.baiti.property.Block;
import my.baiti.property.Unit;
import my.baiti.security.RefreshToken;
import my.baiti.system.ActivityLog;
import my.baiti.system.Notification;
import my.baiti.visitors.Visitor;
import my.baiti.visitors.VisitorPreRegistration;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({DataSourceConfig.class, JpaConfig.class, FoundationTenantConfiguration.class})
class AggregateRepositoryIT extends NativePostgresTestSupport {

  private final EntityManager entities;
  private Graph graph;

  @Autowired
  AggregateRepositoryIT(EntityManager entities) {
    this.entities = entities;
  }

  @BeforeEach
  void createScopedSyntheticHousehold() {
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
    var role = new Role();
    role.setName("F0_" + UUID.randomUUID());
    store(role);
    var user = new User();
    user.setName("Synthetic foundation user");
    user.setEmail("f0-" + UUID.randomUUID() + "@example.invalid");
    user.setPasswordHash("synthetic-not-a-login-hash");
    user.setRole(role);
    store(user);
    var block = new Block();
    block.setName("F0_" + UUID.randomUUID());
    store(block);
    var unit = new Unit();
    unit.setBlock(block);
    unit.setUnitNo("F0_" + UUID.randomUUID());
    store(unit);
    var resident = new Resident();
    resident.setUser(user);
    resident.setUnit(unit);
    resident.setName("Synthetic foundation resident");
    resident.setResidentType(Resident.ResidentType.OWNER);
    store(resident);
    graph = new Graph(role, user, block, unit, resident);
    entities.flush();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "family",
        "finance",
        "complaints",
        "maintenance",
        "facilities",
        "community",
        "visitors",
        "system",
        "refresh"
      })
  void authoredAggregateRowsRoundTripUnderTheApplicationRole(String scenario) throws Exception {
    var rows =
        new ArrayList<TenantAwareBaseEntity>(
            List.of(graph.role, graph.user, graph.block, graph.unit, graph.resident));
    switch (scenario) {
      case "family" -> {
        var member = new FamilyMember();
        member.setResident(graph.resident);
        member.setName("Synthetic adult family member");
        member.setRelationship("SIBLING");
        member.setAge((short) 30);
        rows.add(store(member));
      }
      case "finance" -> {
        var method = new PaymentMethod();
        method.setName("F0_" + UUID.randomUUID());
        rows.add(store(method));
        var bill = new Bill();
        bill.setUnit(graph.unit);
        bill.setBillingMonth(LocalDate.of(2026, 10, 1));
        bill.setDueDate(LocalDate.of(2026, 10, 31));
        bill.setAmount(new BigDecimal("100.00"));
        rows.add(store(bill));
        var payment = new Payment();
        payment.setBill(bill);
        payment.setResident(graph.resident);
        payment.setPaymentMethod(method);
        payment.setAmountPaid(new BigDecimal("40.25"));
        rows.add(store(payment));
      }
      case "complaints" -> {
        var category = new ComplaintCategory();
        category.setName("F0_" + UUID.randomUUID());
        rows.add(store(category));
        var complaint = new Complaint();
        complaint.setResident(graph.resident);
        complaint.setUnit(graph.unit);
        complaint.setCategory(category);
        complaint.setTitle("Synthetic complaint");
        complaint.setDescription("Foundation persistence fixture");
        rows.add(store(complaint));
        var update = new ComplaintUpdate();
        update.setComplaint(complaint);
        update.setUser(graph.user);
        update.setMessage("Synthetic history entry");
        update.setStatus(Complaint.Status.OPEN);
        rows.add(store(update));
      }
      case "maintenance" -> {
        var request = new MaintenanceRequest();
        request.setUnit(graph.unit);
        request.setRequestedBy(graph.user);
        request.setDescription("Synthetic maintenance request");
        rows.add(store(request));
      }
      case "facilities" -> {
        var facility = new Facility();
        facility.setName("F0_" + UUID.randomUUID());
        facility.setCapacity(10);
        rows.add(store(facility));
        var booking = new FacilityBooking();
        booking.setFacility(facility);
        booking.setResident(graph.resident);
        booking.setBookingDate(LocalDate.of(2026, 10, 29));
        booking.setTimeSlot("09:00-10:00");
        rows.add(store(booking));
      }
      case "community" -> {
        var announcement = new Announcement();
        announcement.setTitle("Synthetic announcement");
        announcement.setContent("Foundation persistence fixture");
        announcement.setPostedBy(graph.user);
        rows.add(store(announcement));
        var event = event();
        event.setCreatedBy(graph.user.getId());
        rows.add(store(event));
      }
      case "visitors" -> {
        var invitation = new VisitorPreRegistration();
        invitation.setResident(graph.resident);
        invitation.setVisitorName("Synthetic invited visitor");
        invitation.setVisitDate(LocalDate.of(2026, 10, 29));
        invitation.setApprovalCode("135790");
        rows.add(store(invitation));
        var visit = new Visitor();
        visit.setResident(graph.resident);
        visit.setPreRegistration(invitation);
        visit.setName("Synthetic checked-in visitor");
        rows.add(store(visit));
      }
      case "system" -> {
        var notification = new Notification();
        notification.setUser(graph.user);
        notification.setTitle("Synthetic notification");
        notification.setMessage("Foundation persistence fixture");
        rows.add(store(notification));
        var audit = new ActivityLog();
        audit.setUserId(graph.user.getId());
        audit.setAction("FOUNDATION_TEST");
        audit.setDetail("{\"fixture\":true}");
        audit.setIpAddress(InetAddress.getByName("192.0.2.17"));
        rows.add(store(audit));
      }
      case "refresh" -> {
        var token = new RefreshToken();
        token.setUser(graph.user);
        token.setTokenHash(UUID.randomUUID().toString().replace("-", "").repeat(2));
        token.setFamilyId(UUID.randomUUID());
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        rows.add(store(token));
      }
      default -> throw new IllegalArgumentException("Unknown fixture scenario");
    }
    entities.flush();
    entities.clear();

    for (var original : rows) {
      var restored = entities.find(original.getClass(), original.getId());
      assertThat(restored).isNotNull();
      assertThat(restored.getTenantId()).isEqualTo(FoundationTenantConfiguration.TENANT);
      assertThat(restored.getCreatedAt()).isNotNull();
      if (restored instanceof Bill bill) {
        assertThat(bill.getAmount()).isEqualByComparingTo("100.00");
        assertThat(bill.getCurrency()).isEqualTo("MYR");
        assertThat(bill.getBillingMonth()).isEqualTo(LocalDate.of(2026, 10, 1));
      } else if (restored instanceof Payment payment) {
        assertThat(payment.getAmountPaid()).isEqualByComparingTo("40.25");
        assertThat(payment.getResident().getId()).isEqualTo(graph.resident.getId());
      } else if (restored instanceof Event event) {
        assertThat(event.getCreatedByUser().getId()).isEqualTo(graph.user.getId());
      } else if (restored instanceof ComplaintUpdate update) {
        assertThat(update.getUser().getId()).isEqualTo(graph.user.getId());
      } else if (restored instanceof ActivityLog audit) {
        assertThat(new ObjectMapper().readTree(audit.getDetail()).path("fixture").asBoolean())
            .isTrue();
        assertThat(audit.getIpAddress()).isEqualTo(InetAddress.getByName("192.0.2.17"));
      }
    }
  }

  @Test
  void duplicateUnitNumberWithinTheSameBlockIsRejected() {
    var duplicate = new Unit();
    duplicate.setBlock(graph.block);
    duplicate.setUnitNo(graph.unit.getUnitNo());

    assertDatabaseFailure(
        () -> {
          store(duplicate);
          entities.flush();
        },
        "23505");
  }

  @Test
  void userEmailUniquenessIsCaseInsensitive() {
    var duplicate = new User();
    duplicate.setRole(graph.role);
    duplicate.setName("Synthetic duplicate");
    duplicate.setEmail(graph.user.getEmail().toUpperCase(java.util.Locale.ROOT));
    duplicate.setPasswordHash("synthetic-not-a-login-hash");

    assertDatabaseFailure(
        () -> {
          store(duplicate);
          entities.flush();
        },
        "23505");
  }

  @Test
  void complaintWithHistoryCannotBeDeletedThroughJpaCascade() {
    var complaint = complaintWithHistory();
    UUID complaintId = complaint.getId();
    entities.clear();

    assertDatabaseFailure(
        () -> {
          entities.remove(entities.find(Complaint.class, complaintId));
          entities.flush();
        },
        "23503");
  }

  @Test
  void removingAHistoryEntryFromTheCollectionDoesNotDeleteItsRecord() {
    var complaint = complaintWithHistory();
    UUID complaintId = complaint.getId();
    UUID updateId = complaint.getUpdates().get(0).getId();
    entities.clear();

    entities.find(Complaint.class, complaintId).getUpdates().clear();
    entities.flush();
    entities.clear();

    var retained = entities.find(ComplaintUpdate.class, updateId);
    assertThat(retained).isNotNull();
    assertThat(retained.getComplaint().getId()).isEqualTo(complaintId);
  }

  @Test
  void aNewComplaintCanPersistItsHistoryEntry() {
    var complaint = complaintWithHistory();
    UUID updateId = complaint.getUpdates().get(0).getId();
    entities.clear();

    var restored = entities.find(ComplaintUpdate.class, updateId);
    assertThat(restored).isNotNull();
    assertThat(restored.getUser().getId()).isEqualTo(graph.user.getId());
    assertThat(restored.getMessage()).isEqualTo("Synthetic retained history");
  }

  @Test
  void eventWithoutAnExplicitCreatorIsRejectedBeforeProductionAuditingExists() {
    assertDatabaseFailure(
        () -> {
          store(event());
          entities.flush();
        },
        "23502");
  }

  private static void assertDatabaseFailure(Runnable action, String sqlState) {
    Throwable failure = catchThrowable(action::run);
    assertThat(failure).isInstanceOf(PersistenceException.class);
    while (failure.getCause() != null) {
      failure = failure.getCause();
    }
    assertThat(failure).isInstanceOf(SQLException.class);
    assertThat(((SQLException) failure).getSQLState()).isEqualTo(sqlState);
  }

  private Event event() {
    var event = new Event();
    event.setTitle("Synthetic event");
    event.setStartDate(Instant.parse("2026-10-29T01:00:00Z"));
    event.setEndDate(Instant.parse("2026-10-29T02:00:00Z"));
    return event;
  }

  private Complaint complaintWithHistory() {
    var category = new ComplaintCategory();
    category.setName("F0_" + UUID.randomUUID());
    store(category);
    var complaint = new Complaint();
    complaint.setResident(graph.resident);
    complaint.setUnit(graph.unit);
    complaint.setCategory(category);
    complaint.setTitle("Synthetic history preservation complaint");
    complaint.setDescription("Foundation deletion regression fixture");
    var update = new ComplaintUpdate();
    update.setComplaint(complaint);
    update.setUser(graph.user);
    update.setStatus(Complaint.Status.OPEN);
    update.setMessage("Synthetic retained history");
    complaint.getUpdates().add(update);
    store(complaint);
    entities.flush();
    return complaint;
  }

  private <T extends TenantAwareBaseEntity> T store(T entity) {
    entities.persist(entity);
    return entity;
  }

  private record Graph(Role role, User user, Block block, Unit unit, Resident resident) {}
}
