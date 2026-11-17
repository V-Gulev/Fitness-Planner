package bg.fitnessplanner.account.application;
import java.util.Locale;
import java.util.Objects;

public final class EmailPolicy {
    private EmailPolicy() { }


    public static String normalize(String email) {
        return Objects.requireNonNull(email).strip().toLowerCase(Locale.ROOT);
    }
}
