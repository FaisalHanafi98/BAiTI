package my.baiti.finance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, UUID> {
  Optional<Bill> findByUnitIdAndBillingMonthAndBillType(
      UUID unitId, LocalDate billingMonth, String billType);

  List<Bill> findByUnitId(UUID unitId);

  List<Bill> findByStatus(Bill.Status status);
}
