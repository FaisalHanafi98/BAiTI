package my.baiti.people;

public enum PermissionCatalog {
  TENANT_SETTINGS_MANAGE("tenancy"),
  USER_MANAGE("people"),
  ROLE_MANAGE("people"),
  BLOCK_MANAGE("property"),
  UNIT_MANAGE("property"),
  UNIT_READ_ALL("property"),
  RESIDENT_MANAGE("people"),
  RESIDENT_READ_ALL("people"),
  FAMILY_MANAGE_OWN("people"),
  BILL_MANAGE("finance"),
  BILL_READ_ALL("finance"),
  BILL_READ_OWN("finance"),
  PAYMENT_READ_OWN("finance"),
  PAYMENT_RECORD("finance"),
  PAYMENT_METHOD_MANAGE("finance"),
  COMPLAINT_CREATE_OWN("complaints"),
  COMPLAINT_READ_OWN("complaints"),
  COMPLAINT_READ_ALL("complaints"),
  COMPLAINT_PROGRESS("complaints"),
  COMPLAINT_CATEGORY_MANAGE("complaints"),
  MAINTENANCE_CREATE_OWN("maintenance"),
  MAINTENANCE_READ_OWN("maintenance"),
  MAINTENANCE_READ_ALL("maintenance"),
  MAINTENANCE_PROGRESS("maintenance"),
  FACILITY_MANAGE("facilities"),
  FACILITY_READ("facilities"),
  BOOKING_CREATE_OWN("facilities"),
  BOOKING_READ_OWN("facilities"),
  BOOKING_CANCEL_OWN("facilities"),
  BOOKING_READ_ALL("facilities"),
  BOOKING_APPROVE("facilities"),
  ANNOUNCEMENT_MANAGE("community"),
  EVENT_MANAGE("community"),
  ANNOUNCEMENT_READ("community"),
  EVENT_READ("community"),
  VISITOR_PREREG_CREATE_OWN("visitors"),
  VISITOR_PREREG_READ_OWN("visitors"),
  VISITOR_CHECKIN("visitors"),
  VISITOR_CHECKOUT("visitors"),
  VISITOR_READ_ONSITE("visitors"),
  NOTIFICATION_READ_OWN("system"),
  ACTIVITY_LOG_READ("system");

  private final String module;

  PermissionCatalog(String module) {
    this.module = module;
  }

  public String module() {
    return module;
  }
}
