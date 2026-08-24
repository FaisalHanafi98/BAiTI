package my.baiti.finance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
  List<Payment> findByBillId(UUID billId);

  List<Payment> findByResidentId(UUID residentId);
}
