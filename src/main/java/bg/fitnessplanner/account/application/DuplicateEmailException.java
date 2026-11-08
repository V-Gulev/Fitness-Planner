package bg.fitnessplanner.account.application;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("An account already uses this email. Try signing in instead.");
    }
}
