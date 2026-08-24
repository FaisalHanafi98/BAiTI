package my.baiti.facilities;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityBookingRepository extends JpaRepository<FacilityBooking, UUID> {
  List<FacilityBooking> findByFacilityIdAndBookingDate(UUID facilityId, LocalDate bookingDate);

  List<FacilityBooking> findByResidentId(UUID residentId);
}
