package bg.fitnessplanner.planning.application;

public class PlanConflictException extends RuntimeException {
    public PlanConflictException() { super("Your profile or catalog changed during generation. Review your current profile and try again."); }
}
