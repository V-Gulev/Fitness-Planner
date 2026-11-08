package bg.fitnessplanner.catalog.domain;

public enum MuscleRole {
    PRIMARY("Primary"),
    SECONDARY("Secondary");

    private final String label;

    MuscleRole(String label) { this.label = label; }

    public String getLabel() { return label; }
}

