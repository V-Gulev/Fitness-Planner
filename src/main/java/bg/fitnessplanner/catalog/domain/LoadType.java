package bg.fitnessplanner.catalog.domain;

public enum LoadType {
    TOTAL_EXTERNAL("Total external load"),
    PER_HAND_EXTERNAL("Load per hand"),
    BODYWEIGHT("Bodyweight"),
    ASSISTED("Assisted");

    private final String label;

    LoadType(String label) { this.label = label; }

    public String getLabel() { return label; }
}

