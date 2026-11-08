package bg.fitnessplanner.account.application;

import bg.fitnessplanner.account.persistence.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDetailsService implements UserDetailsService {
    private final UserAccountRepository accounts;

    public AccountDetailsService(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var account = accounts.findByEmailNormalized(EmailPolicy.normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
        return new AccountPrincipal(account.getId(), account.getEmailNormalized(), account.getPasswordHash());
    }
}
