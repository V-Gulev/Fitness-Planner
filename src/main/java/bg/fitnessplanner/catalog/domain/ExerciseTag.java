package bg.fitnessplanner.catalog.domain;

public enum ExerciseTag {
    UNILATERAL("One side at a time"),
    SUPPORTED("Supported"),
    OVERHEAD("Overhead");

    private final String label;

    ExerciseTag(String label) { this.label = label; }

    public String getLabel() { return label; }
}

