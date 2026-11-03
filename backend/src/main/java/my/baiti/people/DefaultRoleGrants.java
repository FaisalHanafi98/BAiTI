package my.baiti.people;

import java.util.Map;
import java.util.Set;

public final class DefaultRoleGrants {

  private static final Map<String, Set<PermissionCatalog>> GRANTS =
      Map.of(
          "Admin",
          Set.of(
              PermissionCatalog.TENANT_SETTINGS_MANAGE,
              PermissionCatalog.USER_MANAGE,
              PermissionCatalog.ROLE_MANAGE,
              PermissionCatalog.BLOCK_MANAGE,
              PermissionCatalog.UNIT_MANAGE,
              PermissionCatalog.UNIT_READ_ALL,
              PermissionCatalog.RESIDENT_MANAGE,
              PermissionCatalog.RESIDENT_READ_ALL,
              PermissionCatalog.BILL_MANAGE,
              PermissionCatalog.BILL_READ_ALL,
              PermissionCatalog.PAYMENT_RECORD,
              PermissionCatalog.PAYMENT_METHOD_MANAGE,
              PermissionCatalog.COMPLAINT_READ_ALL,
              PermissionCatalog.COMPLAINT_PROGRESS,
              PermissionCatalog.COMPLAINT_CATEGORY_MANAGE,
              PermissionCatalog.MAINTENANCE_READ_ALL,
              PermissionCatalog.MAINTENANCE_PROGRESS,
              PermissionCatalog.FACILITY_MANAGE,
              PermissionCatalog.FACILITY_READ,
              PermissionCatalog.BOOKING_READ_ALL,
              PermissionCatalog.BOOKING_APPROVE,
              PermissionCatalog.ANNOUNCEMENT_MANAGE,
              PermissionCatalog.EVENT_MANAGE,
              PermissionCatalog.ANNOUNCEMENT_READ,
              PermissionCatalog.EVENT_READ,
              PermissionCatalog.VISITOR_CHECKIN,
              PermissionCatalog.VISITOR_CHECKOUT,
              PermissionCatalog.VISITOR_READ_ONSITE,
              PermissionCatalog.NOTIFICATION_READ_OWN,
              PermissionCatalog.ACTIVITY_LOG_READ),
          "Manager",
          Set.of(
              PermissionCatalog.UNIT_READ_ALL,
              PermissionCatalog.RESIDENT_MANAGE,
              PermissionCatalog.RESIDENT_READ_ALL,
              PermissionCatalog.BILL_MANAGE,
              PermissionCatalog.BILL_READ_ALL,
              PermissionCatalog.PAYMENT_RECORD,
              PermissionCatalog.COMPLAINT_READ_ALL,
              PermissionCatalog.COMPLAINT_PROGRESS,
              PermissionCatalog.MAINTENANCE_READ_ALL,
              PermissionCatalog.MAINTENANCE_PROGRESS,
              PermissionCatalog.FACILITY_READ,
              PermissionCatalog.BOOKING_READ_ALL,
              PermissionCatalog.BOOKING_APPROVE,
              PermissionCatalog.ANNOUNCEMENT_MANAGE,
              PermissionCatalog.EVENT_MANAGE,
              PermissionCatalog.ANNOUNCEMENT_READ,
              PermissionCatalog.EVENT_READ,
              PermissionCatalog.VISITOR_CHECKIN,
              PermissionCatalog.VISITOR_CHECKOUT,
              PermissionCatalog.VISITOR_READ_ONSITE,
              PermissionCatalog.NOTIFICATION_READ_OWN),
          "Resident",
          Set.of(
              PermissionCatalog.FAMILY_MANAGE_OWN,
              PermissionCatalog.BILL_READ_OWN,
              PermissionCatalog.PAYMENT_READ_OWN,
              PermissionCatalog.COMPLAINT_CREATE_OWN,
              PermissionCatalog.COMPLAINT_READ_OWN,
              PermissionCatalog.MAINTENANCE_CREATE_OWN,
              PermissionCatalog.MAINTENANCE_READ_OWN,
              PermissionCatalog.FACILITY_READ,
              PermissionCatalog.BOOKING_CREATE_OWN,
              PermissionCatalog.BOOKING_READ_OWN,
              PermissionCatalog.BOOKING_CANCEL_OWN,
              PermissionCatalog.ANNOUNCEMENT_READ,
              PermissionCatalog.EVENT_READ,
              PermissionCatalog.VISITOR_PREREG_CREATE_OWN,
              PermissionCatalog.VISITOR_PREREG_READ_OWN,
              PermissionCatalog.NOTIFICATION_READ_OWN),
          "Guard",
          Set.of(
              PermissionCatalog.UNIT_READ_ALL,
              PermissionCatalog.RESIDENT_READ_ALL,
              PermissionCatalog.FACILITY_READ,
              PermissionCatalog.ANNOUNCEMENT_READ,
              PermissionCatalog.EVENT_READ,
              PermissionCatalog.VISITOR_CHECKIN,
              PermissionCatalog.VISITOR_CHECKOUT,
              PermissionCatalog.VISITOR_READ_ONSITE,
              PermissionCatalog.NOTIFICATION_READ_OWN));

  private DefaultRoleGrants() {}

  public static Map<String, Set<PermissionCatalog>> all() {
    return GRANTS;
  }

  public static Set<PermissionCatalog> forRole(String role) {
    return GRANTS.getOrDefault(role, Set.of());
  }
}
