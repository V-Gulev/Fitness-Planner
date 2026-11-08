package bg.fitnessplanner.profile.application;

import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.shared.domain.ExperienceLevel;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileFormTest {
    private static final ValidatorFactory VALIDATION = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidation() { VALIDATION.close(); }

    @Test
    void acceptsAValidBodyweightOnlyProfile() {
        assertThat(VALIDATION.getValidator().validate(validForm())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {19, 121})
    void rejectsSessionLengthsOutsideBounds(int minutes) {
        var form = validForm();
        form.setSessionMinutes(minutes);
        assertThat(VALIDATION.getValidator().validate(form)).isNotEmpty();
    }

    @Test
    void rejectsFewerThanTwoDays() {
        var form = validForm();
        form.setWeekdays(Set.of(1));
        assertThat(VALIDATION.getValidator().validate(form)).isNotEmpty();
    }

    @Test
    void rejectsMoreThanFiveDays() {
        var form = validForm();
        form.setWeekdays(Set.of(1, 2, 3, 4, 5, 6));
        assertThat(VALIDATION.getValidator().validate(form)).isNotEmpty();
    }

    @Test
    void rejectsOutOfRangeWeekdayAndExerciseIds() {
        var form = validForm();
        form.setWeekdays(Set.of(1, 8));
        form.setPreferredExerciseIds(Set.of(-1L));
        assertThat(VALIDATION.getValidator().validate(form)).hasSize(2);
    }

    static ProfileForm validForm() {
        var form = new ProfileForm();
        form.setExperience(ExperienceLevel.BEGINNER);
        form.setGoal(TrainingGoal.GENERAL_FITNESS);
        form.setSessionMinutes(45);
        form.setWeekdays(Set.of(1, 3, 5));
        return form;
    }
}
