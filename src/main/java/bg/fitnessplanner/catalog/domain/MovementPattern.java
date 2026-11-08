package bg.fitnessplanner.catalog.domain;

public enum MovementPattern {
    SQUAT("Squat"),
    HINGE("Hip hinge"),
    HORIZONTAL_PUSH("Horizontal push"),
    VERTICAL_PUSH("Vertical push"),
    HORIZONTAL_PULL("Horizontal pull"),
    VERTICAL_PULL("Vertical pull"),
    KNEE_FLEXION("Knee flexion"),
    HIP_EXTENSION("Hip extension"),
    ELBOW_FLEXION("Elbow flexion"),
    ELBOW_EXTENSION("Elbow extension"),
    CALF_RAISE("Calf raise"),
    SHOULDER_ABDUCTION("Shoulder abduction"),
    CORE("Core");

    private final String label;

    MovementPattern(String label) { this.label = label; }

    public String getLabel() { return label; }
}

