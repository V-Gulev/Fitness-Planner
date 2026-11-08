package bg.fitnessplanner.profile.domain;

public enum ExerciseRule {
    PREFERRED("Preferred"),
    EXCLUDED("Excluded");

    private final String label;

    ExerciseRule(String label) { this.label = label; }

    public String getLabel() { return label; }
}

