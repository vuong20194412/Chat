package vuong20194412.chat.authentication_api_gateway_service;

import org.springframework.data.jpa.repository.JpaRepository;

interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByUserIdOrEmail(Long userId, String email);

    Account findByEmail(String email);

    Account findByEmailAndIsEnabledAndIsNonLocked(String email, boolean isEnabled, boolean isNonLocked);

}
