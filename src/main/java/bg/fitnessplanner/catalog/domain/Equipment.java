package bg.fitnessplanner.catalog.domain;

public enum Equipment {
    BARBELL("Barbell"),
    RACK("Squat rack"),
    BENCH("Bench"),
    DUMBBELL("Dumbbells"),
    CABLE("Cable machine"),
    PULLUP_BAR("Pull-up bar"),
    LEG_PRESS("Leg press machine"),
    LEG_CURL("Leg curl machine"),
    CALF_MACHINE("Calf raise machine");

    private final String label;

    Equipment(String label) { this.label = label; }

    public String getLabel() { return label; }
}

