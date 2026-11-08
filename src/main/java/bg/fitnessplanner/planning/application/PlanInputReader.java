package bg.fitnessplanner.planning.application;

import bg.fitnessplanner.catalog.application.CatalogService;
import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.profile.application.ProfileService;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanInputReader {
    private final ProfileService profiles;
    private final CatalogService catalog;
    public PlanInputReader(ProfileService profiles, CatalogService catalog) { this.profiles = profiles; this.catalog = catalog; }

    @Transactional(readOnly = true)
    public Inputs read(Long owner) {
        return new Inputs(profiles.snapshot(owner).orElseThrow(MissingProfileException::new), catalog.snapshot());
    }

    public record Inputs(ProfileSnapshot profile, List<ExerciseSnapshot> catalog) {
        public Inputs { catalog = List.copyOf(catalog); }
    }
}
