package bg.fitnessplanner.planning.application;

public class MissingProfileException extends RuntimeException {
    public MissingProfileException() { super("Save your fitness profile before generating a plan."); }
}
