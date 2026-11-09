package bg.fitnessplanner.catalog.web;

import bg.fitnessplanner.catalog.domain.MovementPattern;
import jakarta.validation.constraints.Size;

public class CatalogFilter {
    @Size(max = 100, message = "Search must be at most 100 characters.")
    private String q = "";
    private LibraryMuscle muscle;
    private MovementPattern movement;

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q == null ? "" : q.strip(); }
    public LibraryMuscle getMuscle() { return muscle; }
    public void setMuscle(LibraryMuscle muscle) { this.muscle = muscle; }
    public MovementPattern getMovement() { return movement; }
    public void setMovement(MovementPattern movement) { this.movement = movement; }
}
