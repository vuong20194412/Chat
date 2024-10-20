package vuong20194412.chat.authentication_api_gateway_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vuong20194412.chat.authentication_api_gateway_service.model.JsonWebToken;

import java.util.UUID;

public interface JsonWebTokenRepository extends JpaRepository<JsonWebToken, UUID> {

    boolean existsByTokenAndExpirationTimeGreaterThanEqual(String token, Long expirationTime);

    void deleteByExpirationTimeLessThan(Long expirationTime);

    JsonWebToken findByTokenAndExpirationTimeGreaterThanEqual(String token, Long expirationTime);

}
