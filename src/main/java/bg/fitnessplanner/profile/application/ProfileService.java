package bg.fitnessplanner.profile.application;

import bg.fitnessplanner.catalog.application.CatalogService;
import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.progression.engine.ProgressionConfig;
import bg.fitnessplanner.profile.domain.ExerciseRule;
import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.profile.persistence.FitnessProfile;
import bg.fitnessplanner.profile.persistence.FitnessProfileRepository;
import bg.fitnessplanner.shared.domain.ExperienceLevel;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class ProfileService {
    private final FitnessProfileRepository profiles;
    private final CatalogService catalog;
    private final ProgressionConfig progression;

    public ProfileService(FitnessProfileRepository profiles, CatalogService catalog, ProgressionConfig progression) {
        this.profiles = profiles;
        this.catalog = catalog;
        this.progression = progression;
    }

    @Transactional(readOnly = true)
    public ProfileForm form(Long userId) {
        return profiles.findById(userId).map(this::toForm).orElseGet(() -> {
            ProfileForm form = new ProfileForm();
            form.setExperience(ExperienceLevel.BEGINNER);
            form.setGoal(TrainingGoal.GENERAL_FITNESS);
            form.setSessionMinutes(45);
            form.setWeekdays(Set.of(1, 3, 5));
            return form;
        });
    }

    @Transactional
    public void save(Long userId, @Valid ProfileForm form) {
        validateExerciseRules(form);
        var steps = new java.util.EnumMap<bg.fitnessplanner.catalog.domain.Equipment, java.math.BigDecimal>(bg.fitnessplanner.catalog.domain.Equipment.class);
        form.getLoadSteps().forEach((equipment, value) -> {
            if (!progression.defaultSteps().containsKey(equipment))
                throw new InvalidProfileException("loadSteps", "Choose increments only for supported resistance equipment.");
            if (value != null && form.getEquipment().contains(equipment)) steps.put(equipment, value);
        });
        FitnessProfile profile = profiles.findById(userId).orElseGet(() -> new FitnessProfile(userId));
        if (!Objects.equals(form.getVersion(), profile.getVersion())) {
            throw new InvalidProfileException("version", "Your profile changed elsewhere. Reload it before saving again.");
        }
        Map<Long, ExerciseRule> rules = new HashMap<>();
        form.getPreferredExerciseIds().forEach(id -> rules.put(id, ExerciseRule.PREFERRED));
        form.getExcludedExerciseIds().forEach(id -> rules.put(id, ExerciseRule.EXCLUDED));
        profile.update(form.getExperience(), form.getGoal(), form.getSessionMinutes(), form.getWeekdays(),
                form.getEquipment(), rules, form.getExcludedMovements());
        profile.updateLoadSteps(steps);
        try {
            profiles.saveAndFlush(profile);
        } catch (DataIntegrityViolationException conflict) {
            // Two initially blank forms can race to insert the same owner's primary key.
            if (conflict.getMostSpecificCause() instanceof SQLException sql && sql.getErrorCode() == 1062) {
                throw new InvalidProfileException("version", "Your profile changed elsewhere. Reload it before saving again.");
            }
            throw conflict;
        }
    }

    @Transactional(readOnly = true)
    public Optional<ProfileSnapshot> snapshot(Long userId) {
        return profiles.findById(userId).map(profile -> {
            ProfileForm form = toForm(profile);
            return new ProfileSnapshot(userId, profile.getVersion(), form.getExperience(), form.getGoal(),
                    form.getSessionMinutes(), form.getWeekdays(), form.getEquipment(),
                    form.getPreferredExerciseIds(), form.getExcludedExerciseIds(), form.getExcludedMovements());
        });
    }

    private void validateExerciseRules(ProfileForm form) {
        Map<Long, ExerciseSnapshot> known = catalog.snapshot().stream()
                .collect(Collectors.toMap(ExerciseSnapshot::id, e -> e));
        for (Long id : form.getPreferredExerciseIds()) {
            if (!known.containsKey(id)) {
                throw new InvalidProfileException("preferredExerciseIds", "A preferred exercise is no longer available. Choose from the library.");
            }
            if (form.getExcludedExerciseIds().contains(id)
                    || form.getExcludedMovements().contains(known.get(id).movementPattern())) {
                throw new InvalidProfileException("preferredExerciseIds", "A preferred exercise is also excluded by your exercise or movement choices.");
            }
        }
        if (!known.keySet().containsAll(form.getExcludedExerciseIds())) {
            throw new InvalidProfileException("excludedExerciseIds", "An excluded exercise is not in the current library.");
        }
    }

    private ProfileForm toForm(FitnessProfile p) {
        ProfileForm form = new ProfileForm();
        form.setVersion(p.getVersion());
        form.setLoadSteps(new HashMap<>(p.getLoadSteps()));
        form.setExperience(p.getExperience());
        form.setGoal(p.getGoal());
        form.setSessionMinutes(p.getSessionMinutes());
        form.setWeekdays(p.getWeekdays());
        form.setEquipment(p.getEquipment());
        form.setPreferredExerciseIds(idsWithRule(p, ExerciseRule.PREFERRED));
        form.setExcludedExerciseIds(idsWithRule(p, ExerciseRule.EXCLUDED));
        form.setExcludedMovements(p.getExcludedMovements());
        return form;
    }

    private Set<Long> idsWithRule(FitnessProfile profile, ExerciseRule rule) {
        return profile.getExerciseRules().entrySet().stream().filter(e -> e.getValue() == rule)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }
}
