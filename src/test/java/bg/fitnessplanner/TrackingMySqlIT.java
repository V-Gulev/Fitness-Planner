package bg.fitnessplanner;

import bg.fitnessplanner.account.application.*;
import bg.fitnessplanner.account.persistence.UserAccountRepository;
import bg.fitnessplanner.catalog.domain.*;
import bg.fitnessplanner.dashboard.DashboardController.ScheduledWorkout;
import bg.fitnessplanner.planning.application.*;
import bg.fitnessplanner.planning.persistence.WorkoutPlanRepository;
import bg.fitnessplanner.profile.application.*;
import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.profile.persistence.FitnessProfileRepository;
import bg.fitnessplanner.shared.domain.ExperienceLevel;
import bg.fitnessplanner.progression.engine.ProgressionModel.Action;
import bg.fitnessplanner.tracking.application.*;
import bg.fitnessplanner.tracking.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("mysql-test")
class TrackingMySqlIT {
    @Autowired RegistrationService registration;
    @Autowired AccountDetailsService accountDetails;
    @Autowired UserAccountRepository accounts;
    @Autowired ProfileService profiles;
    @Autowired FitnessProfileRepository profileRows;
    @Autowired PlanGenerationService generation;
    @Autowired PlanQueryService planQueries;
    @Autowired WorkoutPlanRepository plans;
    @Autowired WorkoutService workouts;
    @Autowired WorkoutSessionRepository sessions;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactions;
    private final List<Long> owners = new ArrayList<>();
    @AfterEach void cleanup() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            for (Long owner : owners) {
                jdbc.update("DELETE ps FROM performed_set ps JOIN exercise_performance p ON p.id=ps.performance_id JOIN workout_session s ON s.id=p.session_id WHERE s.user_id=?", owner);
                jdbc.update("DELETE p FROM exercise_performance p JOIN workout_session s ON s.id=p.session_id WHERE s.user_id=?", owner);
                jdbc.update("DELETE FROM workout_session WHERE user_id=?",owner);
                profileRows.findById(owner).ifPresent(p -> { p.activatePlan(null); profileRows.saveAndFlush(p); });
                plans.findTop10ByUserIdOrderByCreatedAtDescIdDesc(owner).forEach(plans::delete); plans.flush();
                profileRows.findById(owner).ifPresent(profileRows::delete); profileRows.flush(); accounts.deleteById(owner);
            }
        });
    }
    @Test void startSnapshotsPrescriptionButDoesNotInventActualSets() {
        Long owner=owner(); var before=preview(owner); Long id=workouts.start(owner,startForm(before)); var detail=workouts.detail(owner,id);
        assertThat(detail.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(detail.performances()).allSatisfy(p -> { assertThat(p.actual()).isEmpty(); assertThat(p.acceptedTarget()).isNull(); });
        assertThat(detail.progressionVersion()).isEqualTo("double-progression-v1");
        assertThat(detail.performances().getFirst().prescription()).isEqualTo(before.choices().getFirst().prescription());
    }
    @Test void draftActualChangesIncrementAggregateVersionAndRejectStaleTabs() {
        Long owner=owner(),id=start(owner); var original=workouts.detail(owner,id); var form=logged(original);
        workouts.save(owner,id,form,false); var changed=workouts.detail(owner,id);
        assertThat(changed.version()).isGreaterThan(original.version());
        assertThat(changed.performances().getFirst().actual()).hasSize(original.performances().getFirst().prescription().sets());
        assertThatThrownBy(() -> workouts.save(owner,id,form,false)).isInstanceOf(WorkoutConflictException.class);
        var next=workouts.form(changed); next.getPerformances().getFirst().getSets().getFirst().setReps(7);
        workouts.save(owner,id,next,false);
        assertThat(workouts.detail(owner,id).performances().getFirst().actual().getFirst().reps()).isEqualTo(7);
    }
    @Test void finishIsImmutableAndIdempotentAndRegenerationKeepsHistory() {
        Long owner=owner(),id=start(owner); var original=workouts.detail(owner,id); var form=logged(original);
        generate(owner); // A draft remains finishable after its source plan is archived.
        workouts.save(owner,id,form,true); var finished=workouts.detail(owner,id);
        workouts.save(owner,id,form,true); assertThat(workouts.detail(owner,id)).isEqualTo(finished);
        assertThatThrownBy(() -> workouts.save(owner,id,workouts.form(finished),false)).isInstanceOf(WorkoutConflictException.class);
        assertThatThrownBy(() -> workouts.cancel(owner,id,finished.version())).isInstanceOf(WorkoutConflictException.class);
        assertThat(finished.planId()).isEqualTo(original.planId());
        assertThat(finished.status()).isEqualTo(SessionStatus.COMPLETED);
    }
    @Test void incompleteAndSkippedExercisesAreHonestLogsButNotIncreaseEvidence() {
        Long owner=owner(),id=start(owner); var detail=workouts.detail(owner,id); var form=logged(detail);
        form.getPerformances().getFirst().getSets().removeLast();
        workouts.save(owner,id,form,true);
        var next=preview(owner); var first=next.choices().getFirst();
        assertThat(first.decision().action()).isEqualTo(Action.INSUFFICIENT_DATA);
        Long second=workouts.start(owner,startForm(next)); var secondForm=logged(workouts.detail(owner,second));
        secondForm.getPerformances().forEach(p -> { p.setStatus(PerformanceStatus.SKIPPED); p.getSets().clear(); });
        workouts.save(owner,second,secondForm,true);
        assertThat(preview(owner).choices()).allSatisfy(c -> assertThat(c.decision().action()).isEqualTo(Action.INSUFFICIENT_DATA));
    }
    @Test void twoSuccessfulLogsOfferOptInNextTargetWithoutChangingOldLogs() {
        Long owner=owner(); var initial=preview(owner);
        int externalIndex=java.util.stream.IntStream.range(0,initial.choices().size()).filter(i -> initial.choices().get(i).prescription().loadEquipment()!=null
                && initial.choices().get(i).prescription().loadType()!=LoadType.ASSISTED).findFirst().orElseThrow();
        Long first=start(owner); workouts.save(owner,first,logged(workouts.detail(owner,first)),true);
        Long second=start(owner); workouts.save(owner,second,logged(workouts.detail(owner,second)),true);
        var previous=workouts.detail(owner,second);
        var preview=preview(owner); var choice=preview.choices().get(externalIndex);
        assertThat(choice.decision().action()).isEqualTo(Action.INCREASE);
        assertThat(choice.decision().evidenceIds()).hasSize(2);
        var command=startForm(preview); command.setAcceptExerciseIds(Set.of(choice.plannedExerciseId()));
        Long third=workouts.start(owner,command); var next=workouts.detail(owner,third);
        assertThat(next.performances().get(externalIndex).acceptedTarget()).isEqualByComparingTo(choice.decision().suggestedLoad());
        assertThat(next.performances().get(externalIndex).actual()).isEmpty();
        assertThat(workouts.detail(owner,second)).isEqualTo(previous);
    }
    @Test void stalePreviewCannotAcceptChangedHistoryOrLoadSteps() {
        Long owner=owner(); var preview=preview(owner); Long id=start(owner);
        workouts.save(owner,id,logged(workouts.detail(owner,id)),true);
        assertThatThrownBy(() -> workouts.start(owner,startForm(preview))).isInstanceOf(WorkoutConflictException.class);
        var fresh=preview(owner); var profile=profiles.form(owner); profile.getLoadSteps().put(Equipment.BARBELL,new BigDecimal("1.25")); profiles.save(owner,profile);
        assertThatThrownBy(() -> workouts.start(owner,startForm(fresh))).isInstanceOf(WorkoutConflictException.class);
        assertThat(planQueries.page(owner,null).plan().profileChanged()).isFalse();
        assertThat(preview(owner).choices().stream().filter(c -> c.prescription().loadEquipment()==Equipment.BARBELL))
                .allSatisfy(c -> assertThat(c.prescription().loadStep()).isEqualByComparingTo("1.25"));
    }
    @Test void startRetriesAndConcurrentSubmissionsReturnOneOwnedDraft() throws Exception {
        Long owner=owner(); var command=startForm(preview(owner));
        CountDownLatch go=new CountDownLatch(1);
        Callable<Long> attempt=() -> {go.await();return workouts.start(owner,command);};
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(attempt);var b=pool.submit(attempt);go.countDown();assertThat(a.get(10,TimeUnit.SECONDS)).isEqualTo(b.get(10,TimeUnit.SECONDS));
        }
        Long id=workouts.start(owner,command); generate(owner);
        assertThat(workouts.start(owner,command)).isEqualTo(id);
        assertThat(workouts.history(owner)).hasSize(1);
    }
    @Test void finishedRetryFromTwoTabsCompletesOnlyOnce() throws Exception {
        Long owner=owner(),id=start(owner); var form=logged(workouts.detail(owner,id));
        CountDownLatch go=new CountDownLatch(1);
        Callable<Void> attempt=() -> {go.await();workouts.save(owner,id,form,true);return null;};
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(attempt);var b=pool.submit(attempt);go.countDown();a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);
        }
        assertThat(workouts.history(owner)).hasSize(1);
        assertThat(workouts.detail(owner,id).status()).isEqualTo(SessionStatus.COMPLETED);
    }
    @Test void archivedOrStalePlanCannotStartNewSessions() {
        Long owner=owner(); var old=preview(owner); generate(owner);
        assertThatThrownBy(() -> workouts.start(owner,startForm(old))).isInstanceOf(WorkoutConflictException.class);
        var fresh=preview(owner); var form=profiles.form(owner);form.setSessionMinutes(45);profiles.save(owner,form);
        assertThatThrownBy(() -> workouts.preview(owner,fresh.dayId(),workouts.today())).isInstanceOf(WorkoutConflictException.class);
    }
    @Test void cancellationPreservesDraftButExcludesEvidenceAndAllowsReplacement() {
        Long owner=owner(),id=start(owner); var form=logged(workouts.detail(owner,id)); workouts.save(owner,id,form,false);
        var saved=workouts.detail(owner,id);workouts.cancel(owner,id,saved.version());
        assertThat(workouts.detail(owner,id).performances().getFirst().actual()).isNotEmpty();
        assertThat(workouts.exerciseHistory(owner,saved.performances().getFirst().prescription().exerciseId())).isEmpty();
        assertThat(start(owner)).isNotEqualTo(id);
    }
    @Test void validationRejectsPartialSkippedPendingForeignAndExcessiveDataWithoutWrites() {
        Long owner=owner(),id=start(owner);var original=workouts.detail(owner,id);
        var form=logged(original);form.getPerformances().getFirst().getSets().getFirst().setWeightKg(null);
        assertThatThrownBy(() -> workouts.save(owner,id,form,false)).isInstanceOf(WorkoutInputException.class);
        var skipped=logged(original);skipped.getPerformances().getFirst().setStatus(PerformanceStatus.SKIPPED);
        assertThatThrownBy(() -> workouts.save(owner,id,skipped,true)).isInstanceOf(WorkoutInputException.class);
        assertThatThrownBy(() -> workouts.save(owner,id,workouts.form(original),true)).isInstanceOf(WorkoutInputException.class);
        var foreign=logged(original);foreign.getPerformances().getFirst().setId(Long.MAX_VALUE);
        assertThatThrownBy(() -> workouts.save(owner,id,foreign,false)).isInstanceOf(WorkoutConflictException.class);
        var excessive=logged(original);excessive.getPerformances().getFirst().getSets().getFirst().setReps(101);
        assertThatThrownBy(() -> workouts.save(owner,id,excessive,false)).isInstanceOf(ConstraintViolationException.class);
        assertThat(workouts.detail(owner,id)).isEqualTo(original);
    }
    @Test void ownerScopeAndDatabaseConstraintsRejectCrossAccountAccess() throws Exception {
        Long a=owner(),b=owner(),id=start(a);var detail=workouts.detail(a,id);
        assertThatThrownBy(() -> workouts.detail(b,id)).isInstanceOf(WorkoutNotFoundException.class);
        assertThatThrownBy(() -> workouts.save(b,id,logged(detail),true)).isInstanceOf(WorkoutNotFoundException.class);
        assertThat(workouts.history(b)).isEmpty();
        assertThat(workouts.exerciseHistory(b,detail.performances().getFirst().prescription().exerciseId())).isEmpty();
        assertThatThrownBy(() -> jdbc.update("UPDATE workout_session SET user_id=? WHERE id=?",b,id)).isInstanceOf(DataAccessException.class);
        mvc.perform(get("/workouts/"+id).with(user(principal(b)))).andExpect(status().isNotFound());
        mvc.perform(get("/workouts/new/"+preview(a).dayId()).with(user(principal(b)))).andExpect(status().isNotFound());
    }
    @Test void httpRendersAllStatesRequiresCsrfAndRetainsValidationValues() throws Exception {
        Long owner=owner();var principal=principal(owner);var preview=preview(owner);
        mvc.perform(get("/workouts")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/workouts").with(user(principal))).andExpect(status().isOk()).andExpect(content().string(containsString("No workouts recorded yet")));
        mvc.perform(get("/workouts/new/"+preview.dayId()).with(user(principal))).andExpect(status().isOk()).andExpect(content().string(containsString("Actual sets stay blank")));
        mvc.perform(post("/workouts").with(user(principal))).andExpect(status().isForbidden());
        Long id=workouts.start(owner,startForm(preview));var detail=workouts.detail(owner,id);
        mvc.perform(get("/workouts/"+id).with(user(principal))).andExpect(status().isOk()).andExpect(content().string(containsString("Actual work sets")));
        var request=post("/workouts/"+id).with(user(principal)).with(csrf()).param("version",detail.version().toString()).param("action","FINISH");
        for(int i=0;i<detail.performances().size();i++) request.param("performances["+i+"].id",detail.performances().get(i).id().toString()).param("performances["+i+"].status","PENDING")
            .param("performances["+i+"].sets[0].reps","9").param("performances["+i+"].sets[0].weightKg","60");
        mvc.perform(request).andExpect(status().isOk()).andExpect(content().string(containsString("Mark every exercise"))).andExpect(content().string(containsString("value=\"9\"")));
        assertThat(workouts.detail(owner,id).performances().getFirst().actual()).isEmpty();
        workouts.save(owner,id,logged(detail),true);
        mvc.perform(get("/workouts/"+id).with(user(principal))).andExpect(status().isOk()).andExpect(content().string(containsString("Your session, preserved")));
        mvc.perform(get("/workouts/exercises/"+detail.performances().getFirst().prescription().exerciseId()).with(user(principal))).andExpect(status().isOk()).andExpect(content().string(containsString("60.00 kg")));
    }
    @Test void historicalNamesIgnoreCurrentCatalogAndDateCannotBeFuture() {
        Long owner=owner(),id=start(owner);var before=workouts.detail(owner,id);var p=before.performances().getFirst().prescription();
        String name=jdbc.queryForObject("SELECT name FROM exercise WHERE id=?",String.class,p.exerciseId());
        try {jdbc.update("UPDATE exercise SET name='Renamed fixture' WHERE id=?",p.exerciseId());assertThat(workouts.detail(owner,id)).isEqualTo(before);}
        finally {jdbc.update("UPDATE exercise SET name=? WHERE id=?",name,p.exerciseId());}
        assertThatThrownBy(() -> workouts.preview(owner,preview(owner).dayId(),workouts.today().plusDays(1))).isInstanceOf(WorkoutInputException.class);
    }
    @Test void configuredStepsRoundTripThroughHttpAndValidatePrecision() throws Exception {
        Long owner=owner();var principal=principal(owner);var version=profiles.form(owner).getVersion();
        mvc.perform(post("/profile").with(user(principal)).with(csrf()).param("version",version.toString()).param("experience","BEGINNER").param("goal","GENERAL_FITNESS")
                .param("sessionMinutes","60").param("weekdays","1","3","5").param("equipment","BARBELL","RACK","BENCH").param("loadSteps[BARBELL]","1.25"))
                .andExpect(redirectedUrl("/profile"));
        assertThat(profiles.form(owner).getLoadSteps().get(Equipment.BARBELL)).isEqualByComparingTo("1.25");
        mvc.perform(get("/profile").with(user(principal))).andExpect(status().isOk()).andExpect(content().string(containsString("1.25")));
        var invalid=profiles.form(owner);invalid.getLoadSteps().put(Equipment.BARBELL,new BigDecimal("0.001"));
        assertThatThrownBy(() -> profiles.save(owner,invalid)).isInstanceOf(ConstraintViolationException.class);
    }
    @Test void httpPostsBindOnlyOwnedActualValuesAndProtectAllWritesWithCsrf() throws Exception {
        Long owner=owner(),other=owner();var preview=preview(owner);var principal=principal(owner);
        var response=mvc.perform(post("/workouts").with(user(principal)).with(csrf()).param("dayId",preview.dayId().toString())
                .param("date",preview.date().toString()).param("submissionToken",UUID.randomUUID().toString()).param("previewHash",preview.hash())
                .param("userId",other.toString()).param("targetWeightKg","999"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse();
        Long id=Long.valueOf(response.getRedirectedUrl().substring("/workouts/".length()));var detail=workouts.detail(owner,id);
        assertThat(workouts.history(other)).isEmpty();
        assertThat(detail.performances()).allSatisfy(p -> assertThat(p.acceptedTarget()).isNull());
        mvc.perform(post("/workouts/"+id).with(user(principal))).andExpect(status().isForbidden());
        mvc.perform(post("/workouts/"+id+"/cancel").with(user(principal))).andExpect(status().isForbidden());
        var request=post("/workouts/"+id).with(user(principal)).with(csrf()).param("version",detail.version().toString())
                .param("action","FINISH").param("status","CANCELLED").param("userId",other.toString());
        for(int i=0;i<detail.performances().size();i++) {
            request.param("performances["+i+"].id",detail.performances().get(i).id().toString()).param("performances["+i+"].status","COMPLETED");
            for(int n=0;n<detail.performances().get(i).prescription().sets();n++) request.param("performances["+i+"].sets["+n+"].reps","10")
                .param("performances["+i+"].sets["+n+"].weightKg","52.50").param("performances["+i+"].sets["+n+"].rir","2");
        }
        mvc.perform(request).andExpect(redirectedUrl("/workouts/"+id));
        var completed=workouts.detail(owner,id);assertThat(completed.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completed.performances().getFirst().actual().getFirst().weightKg()).isEqualByComparingTo("52.50");
        assertThatThrownBy(() -> jdbc.update("UPDATE performed_set SET weight_kg=-1 WHERE performance_id=?",completed.performances().getFirst().id()))
                .isInstanceOf(DataAccessException.class);
    }
    @Test
    void dashboardUsesOwnedCompletedWorkForWeeklyTotalsAndMovesPastFinishedToday() throws Exception {
        Long owner = owner();
        Long other = owner();
        var today = workouts.today();
        var monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var profile = profiles.form(owner);
        profile.setWeekdays(Set.of(today.getDayOfWeek().getValue(), today.plusDays(2).getDayOfWeek().getValue()));
        profiles.save(owner, profile);
        generate(owner);

        var before = mvc.perform(get("/dashboard").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Start workout")))
                .andReturn().getModelAndView();
        var next = (ScheduledWorkout) before.getModel().get("next");
        assertThat(next.date()).isEqualTo(today);
        Long day = next.day().id();

        Long previous = workouts.start(owner, startForm(workouts.preview(owner, day, monday.minusDays(1))));
        workouts.save(owner, previous, logged(workouts.detail(owner, previous)), true);
        Long completed = workouts.start(owner, startForm(workouts.preview(owner, day, today)));
        var completedDetail = workouts.detail(owner, completed);
        var actual = logged(completedDetail);
        for (var row : actual.getPerformances().subList(1, actual.getPerformances().size())) {
            row.setStatus(PerformanceStatus.SKIPPED);
            row.getSets().clear();
        }
        int expectedSets = actual.getPerformances().getFirst().getSets().size();
        workouts.save(owner, completed, actual, true);

        Long cancelled = workouts.start(owner, startForm(workouts.preview(owner, day, today)));
        workouts.save(owner, cancelled, logged(workouts.detail(owner, cancelled)), false);
        workouts.cancel(owner, cancelled, workouts.detail(owner, cancelled).version());
        Long draft = workouts.start(owner, startForm(workouts.preview(owner, day, today)));
        workouts.save(owner, draft, logged(workouts.detail(owner, draft)), false);
        Long unrelated = start(other);
        workouts.save(other, unrelated, logged(workouts.detail(other, unrelated)), true);

        var response = mvc.perform(get("/dashboard").with(user(principal(owner))).param("userId", other.toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Resume workout")))
                .andExpect(content().string(containsString("View prescribed weekly volume")))
                .andReturn().getModelAndView();
        var activity = (WorkoutViews.Activity) response.getModel().get("activity");
        assertThat(activity.completedWorkouts()).isEqualTo(1);
        assertThat(activity.loggedSets()).isEqualTo(expectedSets);
        assertThat(activity.draft().id()).isEqualTo(draft);
        assertThat(activity.exercises()).extracting(WorkoutViews.ExerciseLink::id)
                .containsExactly(completedDetail.performances().getFirst().prescription().exerciseId());
        assertThat(activity.recent()).extracting(WorkoutViews.Summary::id).doesNotContain(unrelated);
        assertThat(((ScheduledWorkout) response.getModel().get("next")).date()).isEqualTo(today.plusDays(2));

        var first = completedDetail.performances().getFirst().prescription();
        mvc.perform(get("/workouts/exercises/" + first.exerciseId()).with(user(principal(owner))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Recorded performance by workout")))
                .andExpect(content().string(containsString("data-reps=\"" + expectedSets * first.repMax() + "\"")));
    }

    @Test
    void dashboardLinksNewAccountsToProfileAndUnavailablePlansToGeneration() throws Exception {
        Long owner = registration.register(UUID.randomUUID() + "@example.test", "tracking-test-password-123");
        owners.add(owner);
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/dashboard").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Set up your profile")));
        var profile = profiles.form(owner);
        profiles.save(owner, profile);
        mvc.perform(get("/dashboard").with(user(principal(owner))))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Generate a plan")));
    }

    private Long owner() {
        Long id=registration.register(UUID.randomUUID()+"@example.test","tracking-test-password-123");owners.add(id);
        var f=profiles.form(id);f.setExperience(ExperienceLevel.BEGINNER);f.setGoal(TrainingGoal.GENERAL_FITNESS);f.setSessionMinutes(60);f.setWeekdays(Set.of(1,3,5));f.setEquipment(EnumSet.allOf(Equipment.class));profiles.save(id,f);generate(id);return id;
    }
    private void generate(Long owner) { generation.generate(owner,profiles.snapshot(owner).orElseThrow().version()); }
    private WorkoutViews.Preview preview(Long owner) { return workouts.preview(owner,planQueries.page(owner,null).plan().days().getFirst().id(),workouts.today()); }
    private Long start(Long owner) { return workouts.start(owner,startForm(preview(owner))); }
    private StartWorkoutForm startForm(WorkoutViews.Preview p) {
        var f=new StartWorkoutForm();f.setDayId(p.dayId());f.setDate(p.date());f.setPreviewHash(p.hash());f.setSubmissionToken(UUID.randomUUID().toString());return f;
    }
    private WorkoutLogForm logged(WorkoutViews.Detail d) {
        var f=workouts.form(d);
        for(int i=0;i<f.getPerformances().size();i++) {var row=f.getPerformances().get(i);row.setStatus(PerformanceStatus.COMPLETED);
            for(var s:row.getSets()) {s.setReps(d.performances().get(i).prescription().repMax());s.setWeightKg(new BigDecimal("60"));s.setRir(2);}}
        return f;
    }
    private AccountPrincipal principal(Long owner) { return (AccountPrincipal) accountDetails.loadUserByUsername(accounts.findById(owner).orElseThrow().getEmailNormalized()); }
}
