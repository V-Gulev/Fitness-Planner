package bg.fitnessplanner;

import bg.fitnessplanner.account.application.AccountDetailsService;
import bg.fitnessplanner.account.application.DuplicateEmailException;
import bg.fitnessplanner.account.application.RegistrationService;
import bg.fitnessplanner.account.persistence.UserAccountRepository;
import bg.fitnessplanner.catalog.application.CatalogService;
import bg.fitnessplanner.catalog.domain.*;
import bg.fitnessplanner.profile.application.*;
import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.shared.domain.ExperienceLevel;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql-test")
@Transactional
class AccountsProfilesMySqlIT {
    @Autowired MockMvc mvc;
    @Autowired RegistrationService registration;
    @Autowired UserAccountRepository accounts;
    @Autowired AccountDetailsService details;
    @Autowired PasswordEncoder passwords;
    @Autowired ProfileService profiles;
    @Autowired CatalogService catalog;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired Flyway flyway;

    private static final String PASSWORD = "test-only-password-123";

    @Test
    void registrationStoresOnlyAnEncodedPasswordAndCaseNormalizedEmail() {
        String email = UUID.randomUUID() + "@Example.TEST";
        Long id = registration.register(email, PASSWORD);
        entityManager.clear();
        var stored = accounts.findById(id).orElseThrow();
        assertThat(stored.getEmailNormalized()).isEqualTo(email.toLowerCase(Locale.ROOT));
        assertThat(stored.getPasswordHash()).startsWith("{bcrypt}").isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, stored.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsCaseInsensitiveDuplicateRegistration() {
        String email = email();
        registration.register(email, PASSWORD);
        assertThatThrownBy(() -> registration.register(email.toUpperCase(Locale.ROOT), PASSWORD))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void registrationUsesCsrfAndRedirectsToLogin() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("email", email())
                        .param("password", PASSWORD).param("confirmPassword", PASSWORD))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    void validationDoesNotEchoPasswordAndDoesNotCreateAnAccount() throws Exception {
        String email = email();
        mvc.perform(post("/register").with(csrf()).param("email", email)
                        .param("password", PASSWORD).param("confirmPassword", "different-password"))
                .andExpect(status().isOk()).andExpect(model().hasErrors())
                .andExpect(content().string(not(containsString(PASSWORD))));
        assertThat(accounts.existsByEmailNormalized(email)).isFalse();
    }

    @Test
    void registrationDuplicateIsAFormErrorWithoutPasswordEcho() throws Exception {
        String email = email();
        registration.register(email, PASSWORD);
        mvc.perform(post("/register").with(csrf()).param("email", email)
                        .param("password", PASSWORD).param("confirmPassword", PASSWORD))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("registrationForm", "email"))
                .andExpect(content().string(not(containsString(PASSWORD))));
    }

    @Test
    void validLoginNormalizesEmailAndAuthenticates() throws Exception {
        String email = email();
        registration.register(email, PASSWORD);
        mvc.perform(formLogin().user(email.toUpperCase(Locale.ROOT)).password(PASSWORD))
                .andExpect(authenticated().withUsername(email))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void invalidLoginDoesNotAuthenticate() throws Exception {
        String email = email();
        registration.register(email, PASSWORD);
        mvc.perform(formLogin().user(email).password("wrong-password"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void profileRequiresLogin() throws Exception {
        mvc.perform(get("/profile")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void mutationsWithoutCsrfAreRejected() throws Exception {
        UserDetails account = account();
        mvc.perform(post("/register").param("email", email()).param("password", PASSWORD))
                .andExpect(status().isForbidden());
        mvc.perform(post("/profile").with(user(account))).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(user(account))).andExpect(status().isForbidden());
        mvc.perform(post("/login").param("username", account.getUsername()).param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutClearsAuthentication() throws Exception {
        mvc.perform(post("/logout").with(user(account())).with(csrf()))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void profilesRoundTripCollectionsAndExposeImmutableSnapshots() {
        Long owner = registration.register(email(), PASSWORD);
        ProfileForm form = validProfile();
        var exercise = catalog.snapshot().stream().filter(e -> e.code().equals("DUMBBELL_GOBLET_SQUAT")).findFirst().orElseThrow();
        form.setEquipment(Set.of(Equipment.DUMBBELL));
        form.setPreferredExerciseIds(Set.of(exercise.id()));
        form.setExcludedMovements(Set.of(MovementPattern.VERTICAL_PUSH));
        profiles.save(owner, form);
        entityManager.clear();
        var snapshot = profiles.snapshot(owner).orElseThrow();
        assertThat(snapshot.weekdays()).containsExactlyInAnyOrder(1, 3, 5);
        assertThat(snapshot.equipment()).containsExactly(Equipment.DUMBBELL);
        assertThat(snapshot.preferredExerciseIds()).containsExactly(exercise.id());
        assertThat(snapshot.excludedMovements()).containsExactly(MovementPattern.VERTICAL_PUSH);
        assertThat(snapshot.trainingDays()).isEqualTo(3);
        assertThatThrownBy(() -> snapshot.weekdays().add(7)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> exercise.requiredEquipment().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsUnknownAndConflictingExerciseRules() {
        Long owner = registration.register(email(), PASSWORD);
        ProfileForm form = validProfile();
        form.setPreferredExerciseIds(Set.of(Long.MAX_VALUE));
        assertThatThrownBy(() -> profiles.save(owner, form)).isInstanceOf(InvalidProfileException.class);
        Long exercise = catalog.snapshot().getFirst().id();
        form.setPreferredExerciseIds(Set.of(exercise));
        form.setExcludedExerciseIds(Set.of(exercise));
        assertThatThrownBy(() -> profiles.save(owner, form)).isInstanceOf(InvalidProfileException.class);
    }

    @Test
    void rejectsAPreferredExerciseFromAnExcludedMovement() {
        Long owner = registration.register(email(), PASSWORD);
        var exercise = catalog.snapshot().getFirst();
        ProfileForm form = validProfile();
        form.setPreferredExerciseIds(Set.of(exercise.id()));
        form.setExcludedMovements(Set.of(exercise.movementPattern()));
        assertThatThrownBy(() -> profiles.save(owner, form)).isInstanceOf(InvalidProfileException.class);
    }

    @Test
    void serviceValidationRejectsOutOfRangeProfile() {
        Long owner = registration.register(email(), PASSWORD);
        ProfileForm form = validProfile();
        form.setSessionMinutes(5);
        assertThatThrownBy(() -> profiles.save(owner, form)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void staleEditsCannotOverwriteANewerProfile() {
        Long owner = registration.register(email(), PASSWORD);
        profiles.save(owner, validProfile());
        entityManager.clear();
        ProfileForm older = profiles.form(owner);
        ProfileForm newer = profiles.form(owner);
        newer.setSessionMinutes(60);
        profiles.save(owner, newer);
        entityManager.clear();
        assertThatThrownBy(() -> profiles.save(owner, older)).isInstanceOf(InvalidProfileException.class);
    }

    @Test
    void anOlderBlankFormCannotReplaceANewlyCreatedProfile() {
        Long owner = registration.register(email(), PASSWORD);
        ProfileForm olderBlankForm = profiles.form(owner);
        profiles.save(owner, validProfile());
        entityManager.clear();
        assertThatThrownBy(() -> profiles.save(owner, olderBlankForm)).isInstanceOf(InvalidProfileException.class);
    }

    @Test
    void forgedOwnerFieldsCannotReadOrModifyAnotherUsersProfile() throws Exception {
        String emailA = email();
        Long a = registration.register(emailA, PASSWORD);
        Long b = registration.register(email(), PASSWORD);
        profiles.save(a, validProfile());
        ProfileForm second = validProfile();
        second.setSessionMinutes(90);
        profiles.save(b, second);
        entityManager.clear();
        mvc.perform(get("/profile").with(user(details.loadUserByUsername(emailA))).param("userId", b.toString()))
                .andExpect(status().isOk()).andExpect(model().attributeExists("profileForm"))
                .andExpect(result -> assertThat(((ProfileForm) result.getModelAndView().getModel().get("profileForm"))
                        .getSessionMinutes()).isEqualTo(45));
        mvc.perform(post("/profile").with(user(details.loadUserByUsername(emailA))).with(csrf())
                        .param("userId", b.toString()).param("id", b.toString())
                        .param("version", profiles.form(a).getVersion().toString())
                        .param("experience", "BEGINNER").param("goal", "STRENGTH")
                        .param("sessionMinutes", "30").param("weekdays", "2", "5"))
                .andExpect(redirectedUrl("/profile"));
        entityManager.clear();
        assertThat(profiles.snapshot(a).orElseThrow().sessionMinutes()).isEqualTo(30);
        assertThat(profiles.snapshot(b).orElseThrow().sessionMinutes()).isEqualTo(90);
    }

    @Test
    void invalidEnumsAndWeekdaysReturnFormErrors() throws Exception {
        mvc.perform(post("/profile").with(user(account())).with(csrf())
                        .param("experience", "MADE_UP").param("goal", "STRENGTH")
                        .param("sessionMinutes", "30").param("weekdays", "1", "8").param("equipment", "MAGIC"))
                .andExpect(status().isOk()).andExpect(model().hasErrors());
    }

    @Test
    void seededCatalogHasCompleteMetadataAndStableScenarioCodes() {
        var entries = catalog.snapshot();
        assertThat(entries).hasSize(50);
        assertThat(entries).extracting(e -> e.code()).isSorted().doesNotHaveDuplicates()
                .contains("BARBELL_BACK_SQUAT", "BARBELL_DEADLIFT", "DUMBBELL_GOBLET_SQUAT");
        assertThat(entries.stream().map(e -> e.primaryMuscle()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(MuscleGroup.values());
        assertThat(jdbc.queryForList("SELECT exercise_id FROM exercise_muscle GROUP BY exercise_id HAVING SUM(role = 'PRIMARY') <> 1"))
                .isEmpty();
        assertThat(jdbc.queryForList("SELECT code FROM equipment", String.class))
                .containsExactlyInAnyOrder(Arrays.stream(Equipment.values()).map(Enum::name).toArray(String[]::new));
        assertThat(entries).allSatisfy(e -> {
            assertThat(e.instructions()).isNotBlank();
            assertThat(e.secondsPerRep()).isPositive();
            assertThat(e.fatigueCost()).isBetween(java.math.BigDecimal.ZERO, java.math.BigDecimal.ONE);
            if (e.loadEquipment() != null) assertThat(e.requiredEquipment()).contains(e.loadEquipment());
        });
        assertThat(entries.stream().filter(e -> e.loadType() == LoadType.BODYWEIGHT)).isNotEmpty();
    }

    @Test
    void libraryFiltersPersistedCatalogAndRejectsInvalidFilters() throws Exception {
        mvc.perform(get("/exercises").param("q", "goblet").param("muscle", "QUADRICEPS"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Dumbbell goblet squat")))
                .andExpect(content().string(not(containsString("Barbell bench press"))));
        mvc.perform(get("/exercises").param("muscle", "MADE_UP"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/exercises").with(user(account())).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void migrationsAreUpToDateAndSeedsAreNotDuplicated() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM exercise", Integer.class)).isEqualTo(50);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class)).isEqualTo(5);
    }

    private UserDetails account() {
        String email = email();
        registration.register(email, PASSWORD);
        return details.loadUserByUsername(email);
    }

    private String email() { return UUID.randomUUID() + "@example.test"; }

    private ProfileForm validProfile() {
        ProfileForm form = new ProfileForm();
        form.setExperience(ExperienceLevel.BEGINNER);
        form.setGoal(TrainingGoal.GENERAL_FITNESS);
        form.setSessionMinutes(45);
        form.setWeekdays(Set.of(1, 3, 5));
        return form;
    }
}
