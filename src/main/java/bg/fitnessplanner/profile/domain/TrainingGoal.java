package bg.fitnessplanner.profile.domain;

public enum TrainingGoal {
    HYPERTROPHY("Build muscle"),
    STRENGTH("Build strength"),
    GENERAL_FITNESS("General fitness");

    private final String label;

    TrainingGoal(String label) { this.label = label; }

    public String getLabel() { return label; }
}

