package vuong20194412.chat.authentication_api_gateway_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vuong20194412.chat.authentication_api_gateway_service.entity.BlackJsonWebTokenHash;

public interface BlackJsonWebTokenHashRepository extends JpaRepository<BlackJsonWebTokenHash, String> {

    boolean existsByEncodedHashAndIssuedAtTimeAndExpirationTime(String encodedHash, Long issuedAtTime, Long expirationTime);

    void deleteAllByExpirationTimeLessThanEqual(Long expirationTime);

}
