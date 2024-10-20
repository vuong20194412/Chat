package vuong20194412.chat.authentication_api_gateway_service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vuong20194412.chat.authentication_api_gateway_service.model.Account;
import vuong20194412.chat.authentication_api_gateway_service.repository.AccountRepository;

import java.time.Instant;
import java.util.List;

@Service
class AccountDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Autowired
    AccountDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String emailAndToken) throws UsernameNotFoundException {
        Account account = getAccount(emailAndToken);
        if (account == null)
            return null;

        return new User(account.getEmail(), // email instead of username
                account.getPassword(),
                account.isEnabled(), true, true,
                account.isNonLocked(), List.of());
    }

    private Account getAccount(@NonNull String emailAndToken) {
        String[] parts = emailAndToken.split("\n");
        if (parts.length == 1)
            return accountRepository.findByEmail(parts[0]);

        if (parts.length == 2)
            return accountRepository.findByEmailAndValidToken(parts[0], parts[1], Instant.now().getEpochSecond());

        return null;
    }
}
