package my.baiti.finance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
  Optional<PaymentMethod> findByName(String name);

  List<PaymentMethod> findByIsActiveTrue();
}
