package bg.fitnessplanner.catalog.domain;

public enum MuscleGroup {
    CHEST("Chest"),
    BACK("Back"),
    QUADRICEPS("Quadriceps"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    CALVES("Calves"),
    CORE("Core");

    private final String label;

    MuscleGroup(String label) { this.label = label; }

    public String getLabel() { return label; }
}

