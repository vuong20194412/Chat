package vuong20194412.chat.authentication_api_gateway_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vuong20194412.chat.authentication_api_gateway_service.entity.TransientPassword;

public interface TransientPasswordRepository extends JpaRepository<TransientPassword, Long> {

    TransientPassword findByIdAndRandomCodeAndErasableTimeGreaterThan(Long id, String randomCode, Long erasableTime);

}
