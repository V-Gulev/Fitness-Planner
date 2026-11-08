package bg.fitnessplanner;

import bg.fitnessplanner.account.application.*;
import bg.fitnessplanner.account.persistence.UserAccountRepository;
import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.*;
import bg.fitnessplanner.planning.application.*;
import bg.fitnessplanner.planning.engine.*;
import bg.fitnessplanner.planning.engine.PlanningModel.*;
import bg.fitnessplanner.planning.persistence.WorkoutPlanRepository;
import bg.fitnessplanner.profile.application.*;
import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.profile.persistence.FitnessProfileRepository;
import bg.fitnessplanner.shared.domain.ExperienceLevel;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql-test")
class PlanningMySqlIT {
    @Autowired RegistrationService registration;
    @Autowired AccountDetailsService accountDetails;
    @Autowired UserAccountRepository accounts;
    @Autowired ProfileService profiles;
    @Autowired FitnessProfileRepository profileRows;
    @Autowired PlanInputReader reader;
    @Autowired PlanGenerationService generation;
    @Autowired PlanPublisher publisher;
    @Autowired PlanQueryService queries;
    @Autowired WorkoutPlanRepository plans;
    @Autowired ScoredPlanner planner;
    @Autowired BaselinePlanner baseline;
    @Autowired PlanEvaluator evaluator;
    @Autowired SnapshotCodec codec;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactions;
    private final List<Long> owners = new ArrayList<>();

    @AfterEach
    void removeOnlyCommittedFixtures() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            for (Long owner : owners) {
                profileRows.findById(owner).ifPresent(profile -> { profile.activatePlan(null); profileRows.saveAndFlush(profile); });
                plans.findTop10ByUserIdOrderByCreatedAtDescIdDesc(owner).forEach(plans::delete);
                plans.flush();
                profileRows.findById(owner).ifPresent(profileRows::delete);
                profileRows.flush();
                accounts.deleteById(owner);
            }
        });
    }

    @Test
    void persistsOwnedPlansWithReplayableSnapshotsAndCorrectActiveStatus() {
        Long owner = owner(true);
        var outcome = generate(owner);
        assertThat(outcome.planId()).isNotNull();
        var stored = plans.findById(outcome.planId()).orElseThrow();
        var input = codec.read(stored.getInputSnapshot(), ProfileSnapshot.class);
        var catalog = codec.read(stored.getCatalogSnapshot(), new TypeReference<List<ExerciseSnapshot>>() { });
        var config = codec.read(stored.getConfigSnapshot(), PlannerConfig.class);
        assertThat(codec.hash(catalog)).isEqualTo(stored.getCatalogHash());
        assertThat(codec.hash(config)).isEqualTo(stored.getConfigHash());
        assertThat(new ScoredPlanner(config).generate(input, catalog).plan()).isEqualTo(outcome.result().plan());
        var page = queries.page(owner, null);
        assertThat(page.plan().active()).isTrue();
        assertThat(page.plan().profileChanged()).isFalse();
        assertThat(page.plan().days()).hasSize(3);
        assertThat(page.plan().days()).allSatisfy(day -> assertThat(day.exercises()).isNotEmpty());
    }

    @Test
    void regenerationPreservesEarlierPrescriptionsAndShowsProfileChanges() {
        Long owner = owner(true);
        Long oldId = generate(owner).planId();
        var before = queries.page(owner, oldId).plan();
        var form = profiles.form(owner);
        form.setSessionMinutes(30);
        profiles.save(owner, form);
        assertThat(queries.page(owner, oldId).plan().profileChanged()).isTrue();
        Long newId = generate(owner).planId();
        assertThat(newId).isNotEqualTo(oldId);
        assertThat(plans.countByUserId(owner)).isEqualTo(2);
        assertThat(queries.page(owner, oldId).plan().days()).isEqualTo(before.days());
        assertThat(queries.page(owner, oldId).plan().active()).isFalse();
        assertThat(queries.page(owner, null).plan().id()).isEqualTo(newId);
        assertThat(queries.page(owner, null).plan().profileChanged()).isFalse();
    }

    @Test
    void infeasibilityKeepsTheOldActivePlanAndWritesNoReplacement() {
        Long owner = owner(true);
        Long previous = generate(owner).planId();
        var form = profiles.form(owner);
        form.setExcludedMovements(EnumSet.allOf(MovementPattern.class));
        profiles.save(owner, form);
        var outcome = generate(owner);
        assertThat(outcome.result().status()).isEqualTo(Status.INFEASIBLE);
        assertThat(outcome.planId()).isNull();
        assertThat(plans.countByUserId(owner)).isEqualTo(1);
        assertThat(profileRows.findById(owner).orElseThrow().getActivePlanId()).isEqualTo(previous);
    }

    @Test
    void publicationRejectsStaleProfileAndCatalogInputs() {
        Long owner = owner(true);
        var old = reader.read(owner);
        var result = planner.generate(old.profile(), old.catalog());
        var form = profiles.form(owner);
        form.setSessionMinutes(50);
        profiles.save(owner, form);
        assertThatThrownBy(() -> publisher.publish(owner, old, result, planner.version())).isInstanceOf(PlanConflictException.class);
        var fresh = reader.read(owner);
        var freshResult = planner.generate(fresh.profile(), fresh.catalog());
        var exercise = fresh.catalog().getFirst();
        try {
            jdbc.update("UPDATE exercise SET setup_seconds = setup_seconds + 1 WHERE id = ?", exercise.id());
            assertThatThrownBy(() -> publisher.publish(owner, fresh, freshResult, planner.version())).isInstanceOf(PlanConflictException.class);
        } finally { jdbc.update("UPDATE exercise SET setup_seconds = ? WHERE id = ?", exercise.setupSeconds(), exercise.id()); }
        assertThat(plans.countByUserId(owner)).isZero();
    }

    @Test
    void invalidEngineOutputCannotReplaceAValidActivePlan() {
        Long owner = owner(true);
        Long previous = generate(owner).planId();
        var input = reader.read(owner);
        var good = planner.generate(input.profile(), input.catalog());
        var bad = new Plan("tampered", List.of(new Day(1, DayType.FULL,
                List.of(new Exercise(Long.MAX_VALUE, 3, 6, 10, 120, "claimed valid", null)))));
        var result = new Result(Status.VALID, bad, good.evaluation(), List.of(), 1, 1);
        assertThatThrownBy(() -> publisher.publish(owner, input, result, planner.version())).isInstanceOf(IllegalArgumentException.class);
        assertThat(plans.countByUserId(owner)).isEqualTo(1);
        assertThat(queries.page(owner, null).plan().id()).isEqualTo(previous);
    }

    @Test
    void concurrentPublicationAcceptsOnlyOneResultForTheSameProfileVersion() throws Exception {
        Long owner = owner(true);
        var input = reader.read(owner);
        var result = planner.generate(input.profile(), input.catalog());
        CountDownLatch start = new CountDownLatch(1);
        Callable<Object> attempt = () -> {
            start.await();
            try { return publisher.publish(owner, input, result, planner.version()); }
            catch (PlanConflictException expected) { return "conflict"; }
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(attempt);
            var b = pool.submit(attempt);
            start.countDown();
            var outcomes = List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            assertThat(outcomes.stream().filter(Long.class::isInstance).count()).isEqualTo(1);
            assertThat(outcomes).contains("conflict");
        }
        assertThat(plans.countByUserId(owner)).isEqualTo(1);
    }

    @Test
    void historicalViewsDoNotReadMutableExerciseNames() {
        Long owner = owner(true);
        var outcome = generate(owner);
        var before = queries.page(owner, outcome.planId()).plan().days();
        Long exerciseId = outcome.result().plan().days().getFirst().exercises().getFirst().exerciseId();
        String original = jdbc.queryForObject("SELECT name FROM exercise WHERE id = ?", String.class, exerciseId);
        try {
            jdbc.update("UPDATE exercise SET name = 'Temporary renamed fixture' WHERE id = ?", exerciseId);
            assertThat(queries.page(owner, outcome.planId()).plan().days()).isEqualTo(before);
        } finally { jdbc.update("UPDATE exercise SET name = ? WHERE id = ?", original, exerciseId); }
    }

    @Test
    void ownershipIsEnforcedByHttpQueriesAndTheActivePlanForeignKey() throws Exception {
        Long a = owner(true), b = owner(true);
        Long planId = generate(a).planId();
        mvc.perform(get("/plans/" + planId).with(user(principal(b)))).andExpect(status().isNotFound());
        assertThatThrownBy(() -> queries.page(b, planId)).isInstanceOf(PlanNotFoundException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE fitness_profile SET active_plan_id = ? WHERE user_id = ?", planId, b))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void generationRequiresAuthenticationCsrfAndASavedProfile() throws Exception {
        mvc.perform(get("/plans")).andExpect(status().is3xxRedirection());
        Long owner = owner(false);
        mvc.perform(get("/plans").with(user(principal(owner)))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Save your profile first.")));
        mvc.perform(post("/plans").with(user(principal(owner))).param("profileVersion", "0"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/plans").with(user(principal(owner))).with(csrf()).param("profileVersion", "0"))
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    void httpGenerationIgnoresForgedOwnerAndAlgorithmFieldsAndRendersDetails() throws Exception {
        Long a = owner(true), b = owner(true);
        var response = mvc.perform(post("/plans").with(user(principal(a))).with(csrf())
                        .param("profileVersion", profiles.snapshot(a).orElseThrow().version().toString())
                        .param("userId", b.toString()).param("algorithmVersion", "baseline-v1"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse();
        Long id = Long.valueOf(response.getRedirectedUrl().substring("/plans/".length()));
        assertThat(plans.findById(id).orElseThrow().getUserId()).isEqualTo(a);
        assertThat(plans.findById(id).orElseThrow().getAlgorithmVersion()).isEqualTo("scored-v1");
        assertThat(plans.countByUserId(b)).isZero();
        mvc.perform(get(response.getRedirectedUrl()).with(user(principal(a))))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Prescribed weekly volume")));
    }

    @Test
    void realCatalogWorksForBothEnginesWithShortSessionsAndWeekBoundary() {
        Long owner = owner(true);
        var form = profiles.form(owner);
        form.setWeekdays(Set.of(1, 7));
        form.setSessionMinutes(20);
        form.setGoal(TrainingGoal.HYPERTROPHY);
        profiles.save(owner, form);
        var input = reader.read(owner);
        for (Planner algorithm : List.of(planner, baseline)) {
            var result = algorithm.generate(input.profile(), input.catalog());
            assertThat(result.publishable()).isTrue();
            assertThat(evaluator.evaluate(input.profile(), input.catalog(), result.plan()).hardViolations()).isZero();
            assertThat(result.status()).isEqualTo(Status.DEGRADED);
            assertThat(result.evaluation().daySeconds().values()).allMatch(seconds -> seconds <= 1200);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    void realCatalogSupportsHigherFrequencyGoalAndExperienceCases(int frequency) {
        Long owner = owner(true);
        var form = profiles.form(owner);
        form.setExperience(frequency == 4 ? ExperienceLevel.INTERMEDIATE : ExperienceLevel.ADVANCED);
        form.setGoal(frequency == 4 ? TrainingGoal.HYPERTROPHY : TrainingGoal.STRENGTH);
        form.setWeekdays(frequency == 4 ? Set.of(1, 2, 4, 5) : Set.of(1, 2, 3, 5, 6));
        form.setSessionMinutes(frequency == 4 ? 60 : 75);
        profiles.save(owner, form);
        var input = reader.read(owner);
        for (Planner algorithm : List.of(planner, baseline)) {
            var result = algorithm.generate(input.profile(), input.catalog());
            assertThat(result.publishable()).isTrue();
            assertThat(result.plan().days()).hasSize(frequency);
            assertThat(evaluator.evaluate(input.profile(), input.catalog(), result.plan()).hardViolations()).isZero();
        }
    }

    @Test
    void realCatalogEnforcesLimitedEquipmentExclusionsAndPreferences() {
        Long owner = owner(true);
        var initial = reader.read(owner);
        Long preferred = initial.catalog().stream().filter(e -> e.code().equals("DUMBBELL_GOBLET_SQUAT")).findFirst().orElseThrow().id();
        Long excluded = initial.catalog().stream().filter(e -> e.code().equals("DUMBBELL_BENCH_PRESS")).findFirst().orElseThrow().id();
        var form = profiles.form(owner);
        form.setEquipment(Set.of(Equipment.DUMBBELL, Equipment.BENCH));
        form.setPreferredExerciseIds(Set.of(preferred));
        form.setExcludedExerciseIds(Set.of(excluded));
        form.setSessionMinutes(45);
        profiles.save(owner, form);
        var input = reader.read(owner);
        var result = planner.generate(input.profile(), input.catalog());
        assertThat(result.publishable()).isTrue();
        assertThat(result.plan().days().stream().flatMap(day -> day.exercises().stream()).map(Exercise::exerciseId).toList())
                .doesNotContain(excluded).contains(preferred);
        assertThat(evaluator.evaluate(input.profile(), input.catalog(), result.plan()).hardViolations()).isZero();
    }

    private Long owner(boolean savedProfile) {
        Long id = registration.register(UUID.randomUUID() + "@example.test", "planner-test-password-123");
        owners.add(id);
        if (savedProfile) {
            ProfileForm form = new ProfileForm();
            form.setExperience(ExperienceLevel.BEGINNER);
            form.setGoal(TrainingGoal.GENERAL_FITNESS);
            form.setSessionMinutes(60);
            form.setWeekdays(Set.of(1, 3, 5));
            form.setEquipment(EnumSet.allOf(Equipment.class));
            profiles.save(id, form);
        }
        return id;
    }
    private AccountPrincipal principal(Long id) {
        return (AccountPrincipal) accountDetails.loadUserByUsername(accounts.findById(id).orElseThrow().getEmailNormalized());
    }
    private PlanGenerationService.Outcome generate(Long owner) {
        return generation.generate(owner, profiles.snapshot(owner).orElseThrow().version());
    }
}
