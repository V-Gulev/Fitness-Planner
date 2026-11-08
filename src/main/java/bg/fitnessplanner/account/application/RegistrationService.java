package bg.fitnessplanner.account.application;

import bg.fitnessplanner.account.persistence.UserAccount;
import bg.fitnessplanner.account.persistence.UserAccountRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Clock;

@Service
@Validated
public class RegistrationService {
    private final UserAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public RegistrationService(UserAccountRepository accounts, PasswordEncoder encoder, Clock clock) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Transactional
    public Long register(@NotBlank @Email @Size(max = 254) String email,
                         @NotBlank @Size(min = 10, max = 64) String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must fit within 72 UTF-8 bytes.");
        }
        String normalized = EmailPolicy.normalize(email);
        if (accounts.existsByEmailNormalized(normalized)) {
            throw new DuplicateEmailException();
        }
        try {
            return accounts.saveAndFlush(new UserAccount(normalized, encoder.encode(password), clock.instant())).getId();
        } catch (DataIntegrityViolationException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && sql.getErrorCode() == 1062) {
                    throw new DuplicateEmailException();
                }
            }
            throw failure;
        }
    }
}
