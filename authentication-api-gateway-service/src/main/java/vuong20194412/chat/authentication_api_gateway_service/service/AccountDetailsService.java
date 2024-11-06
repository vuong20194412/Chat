package vuong20194412.chat.authentication_api_gateway_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vuong20194412.chat.authentication_api_gateway_service.entity.Account;
import vuong20194412.chat.authentication_api_gateway_service.repository.AccountRepository;

import java.util.List;

@Service
class AccountDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Autowired
    AccountDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(email);
        if (account == null)
            throw new UsernameNotFoundException("Not found user for email " + email);

        return new User(account.getEmail(), // email instead of username
                account.getPassword(),
                account.isEnabled(), true, true,
                account.isNonLocked(), List.of());
    }

}
