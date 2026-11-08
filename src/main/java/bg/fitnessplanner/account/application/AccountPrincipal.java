package bg.fitnessplanner.account.application;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class AccountPrincipal extends User {
    private final Long accountId;

    public AccountPrincipal(Long accountId, String email, String passwordHash) {
        super(email, passwordHash, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.accountId = accountId;
    }

    public Long getAccountId() {
        return accountId;
    }
}
