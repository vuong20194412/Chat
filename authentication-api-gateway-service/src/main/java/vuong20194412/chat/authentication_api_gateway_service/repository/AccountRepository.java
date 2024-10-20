package vuong20194412.chat.authentication_api_gateway_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vuong20194412.chat.authentication_api_gateway_service.model.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByUserIdOrEmail(Long userId, String email);

    Account findByEmail(String email);

    @Query("SELECT a " +
            "FROM  vuong20194412.chat.authentication_api_gateway_service.model.Account a JOIN a.tokens t " +
            "WHERE a.email = :email" +
            " AND t.token = :token AND t.expirationTime >= :now")
    Account findByEmailAndValidToken(String email, String token, Long now);

    @Query("SELECT a " +
            "FROM  vuong20194412.chat.authentication_api_gateway_service.model.Account a LEFT JOIN FETCH a.tokens " +
            "WHERE a.email = :email" +
            " AND a.isEnabled = :isEnabled" +
            " AND a.isNonLocked = :isNonLocked")
    Account findByEmailAndIsEnabledAndIsNonLockedWithTokens(String email, boolean isEnabled, boolean isNonLocked);

}
