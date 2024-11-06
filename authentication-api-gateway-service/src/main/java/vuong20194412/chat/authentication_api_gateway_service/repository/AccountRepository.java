package vuong20194412.chat.authentication_api_gateway_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vuong20194412.chat.authentication_api_gateway_service.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailOrGmailOrOutlook(String email, String gmail, String outlook);

    boolean existsByUserId(Long userId);

    Account findByEmail(String email);

    Account findByEmailAndIsEnabledAndIsNonLocked(String email, boolean isEnabled, boolean isNonLocked);

}
