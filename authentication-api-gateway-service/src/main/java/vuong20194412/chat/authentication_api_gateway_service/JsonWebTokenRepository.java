package vuong20194412.chat.authentication_api_gateway_service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface JsonWebTokenRepository extends JpaRepository<JsonWebToken, UUID> {

    boolean existsByTokenAndExpirationTimeGreaterThanEqual(String token, Long expirationTime);

    void deleteByExpirationTimeLessThan(Long expirationTime);

    JsonWebToken findByTokenAndExpirationTimeGreaterThanEqual(String token, Long expirationTime);

}
