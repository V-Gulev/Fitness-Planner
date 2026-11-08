package bg.fitnessplanner.account.application;

import java.util.Locale;
import java.util.Objects;

public final class EmailPolicy {
    private EmailPolicy() { }

    /** Accounts deliberately treat the entire email address as case-insensitive. */
    public static String normalize(String email) {
        return Objects.requireNonNull(email).strip().toLowerCase(Locale.ROOT);
    }
}
