package bg.fitnessplanner.tracking.application;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.*;
import bg.fitnessplanner.planning.application.SnapshotCodec;
import bg.fitnessplanner.profile.application.ProfileService;
import bg.fitnessplanner.profile.persistence.*;
import bg.fitnessplanner.progression.engine.*;
import bg.fitnessplanner.progression.engine.ProgressionModel.*;
import bg.fitnessplanner.tracking.persistence.*;
import bg.fitnessplanner.tracking.application.WorkoutViews.*;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service @Validated
public class WorkoutService {
    private static final ZoneId ZONE = ZoneId.of("Europe/Sofia");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH).withZone(ZONE);
    private final WorkoutSessionRepository sessions;
    private final ExercisePerformanceRepository performances;
    private final WorkoutDayRepository days;
    private final FitnessProfileRepository profiles;
    private final ProfileService profileService;
    private final SnapshotCodec codec;
    private final ProgressionConfig config;
    private final ProgressionEngine engine;
    private final Clock clock;
    public WorkoutService(WorkoutSessionRepository sessions, ExercisePerformanceRepository performances, WorkoutDayRepository days,
                          FitnessProfileRepository profiles, ProfileService profileService, SnapshotCodec codec,
                          ProgressionConfig config, ProgressionEngine engine, Clock clock) {
        this.sessions = sessions; this.performances = performances; this.days = days; this.profiles = profiles;
        this.profileService = profileService; this.codec = codec; this.config = config; this.engine = engine; this.clock = clock;
    }
    public LocalDate today() { return LocalDate.now(clock.withZone(ZONE)); }

    @Transactional(readOnly = true)
    public Preview preview(Long owner, Long dayId, LocalDate date) {
        validDate(date);
        var profile = profiles.findById(owner).orElseThrow(() -> new WorkoutConflictException("Save your profile and generate a plan first."));
        return preview(owner, dayId, date, profile);
    }
    private Preview preview(Long owner, Long dayId, LocalDate date, FitnessProfile profile) {
        var day = days.owned(dayId, owner).orElseThrow(WorkoutNotFoundException::new);
        var plan = day.getPlan();
        if (!Objects.equals(profile.getActivePlanId(), plan.getId()))
            throw new WorkoutConflictException("Start new workouts from your current plan. Existing drafts remain in Workouts.");
        if (!codec.profileHash(profileService.snapshot(owner).orElseThrow()).equals(plan.getProfileHash()))
            throw new WorkoutConflictException("Your training preferences changed. Generate a new plan before starting a workout.");
        Map<Long, ExerciseSnapshot> catalog = codec.read(plan.getCatalogSnapshot(), new TypeReference<List<ExerciseSnapshot>>() { })
                .stream().collect(Collectors.toMap(ExerciseSnapshot::id, e -> e));
        List<Choice> choices = day.getExercises().stream().map(e -> {
            Equipment equipment = catalog.get(e.getExerciseId()).loadEquipment();
            BigDecimal step = equipment != null && profile.getEquipment().contains(equipment)
                    ? profile.getLoadSteps().getOrDefault(equipment, config.defaultSteps().get(equipment)) : null;
            Prescription p = new Prescription(e.getExerciseId(), e.getNameSnapshot(), e.getSets(), e.getRepMin(), e.getRepMax(),
                    e.getRestSeconds(), LoadType.valueOf(e.getLoadTypeSnapshot()), equipment, step);
            return new Choice(e.getId(), e.getPosition(), p, engine.recommend(p.target(), exposures(owner, e.getExerciseId(), date, 3), date));
        }).toList();
        var hash = codec.hash(List.of(dayId, plan.getId(), date, profile.getVersion(), choices, config));
        Long open = sessions.findByUserIdAndWorkoutDayIdAndStatus(owner, dayId, SessionStatus.IN_PROGRESS).map(WorkoutSession::getId).orElse(null);
        return new Preview(dayId, plan.getId(), date, profile.getVersion(), choices, hash, open);
    }
    @Transactional
    public Long start(Long owner, @Valid StartWorkoutForm form) {
        var profile = profiles.lockOwner(owner).orElseThrow(() -> new WorkoutConflictException("Save a profile first."));
        var replay = sessions.findByUserIdAndSubmissionToken(owner, form.getSubmissionToken());
        if (replay.isPresent()) return replay.get().getId();
        validDate(form.getDate());
        Preview preview = preview(owner, form.getDayId(), form.getDate(), profile);
        if (preview.openSessionId() != null) return preview.openSessionId();
        if (!preview.hash().equals(form.getPreviewHash()))
            throw new WorkoutConflictException("Your history or profile changed. Review the workout and load choices again.");
        Set<Long> allowed = preview.choices().stream().filter(c -> c.decision().acceptable()).map(Choice::plannedExerciseId).collect(Collectors.toSet());
        if (!allowed.containsAll(form.getAcceptExerciseIds())) throw new WorkoutInputException("Choose only available load suggestions from this workout.");
        var session = new WorkoutSession(owner, preview.planId(), form.getDayId(), form.getDate(), clock.instant(), form.getSubmissionToken(), codec.json(config));
        for (Choice c : preview.choices()) session.add(new ExercisePerformance(session, form.getDayId(), c.plannedExerciseId(),
                c.prescription().exerciseId(), c.position(), codec.json(c.prescription()), codec.json(c.decision()),
                form.getAcceptExerciseIds().contains(c.plannedExerciseId()) ? c.decision().suggestedLoad() : null));
        return sessions.saveAndFlush(session).getId();
    }
    @Transactional
    public Long quickStart(Long owner, Long dayId) {
        var profile = profiles.lockOwner(owner).orElseThrow(() -> new WorkoutConflictException("Save a profile first."));
        LocalDate date = today();
        validDate(date);
        Preview preview = preview(owner, dayId, date, profile);
        if (preview.openSessionId() != null) return preview.openSessionId();
        String token = UUID.randomUUID().toString();
        var session = new WorkoutSession(owner, preview.planId(), dayId, date, clock.instant(), token, codec.json(config));
        for (Choice c : preview.choices()) session.add(new ExercisePerformance(session, dayId, c.plannedExerciseId(),
                c.prescription().exerciseId(), c.position(), codec.json(c.prescription()), codec.json(c.decision()), null));
        return sessions.saveAndFlush(session).getId();
    }
    @Transactional(readOnly = true)
    public Set<Long> openDraftDayIds(Long owner) {
        return sessions.findDayIdsByStatus(owner, SessionStatus.IN_PROGRESS);
    }
    @Transactional(readOnly = true)
    public Detail detail(Long owner, Long id) { return detail(owned(owner, id)); }
    private Detail detail(WorkoutSession s) {
        return new Detail(s.getId(), s.getPlanId(), s.getSessionDate(), s.getStatus(), s.getVersion(), TIME.format(s.getStartedAt()),
                s.getCompletedAt() == null ? null : TIME.format(s.getCompletedAt()),
                codec.read(s.getProgressionConfigSnapshot(), ProgressionConfig.class).version(),
                s.getPerformances().stream().map(p -> new Performance(p.getId(), prescription(p), codec.read(p.getSuggestionSnapshot(), Decision.class),
                        p.getTargetWeightKg(), p.getStatus(), p.actualSets())).toList());
    }
    public WorkoutLogForm form(Detail detail) {
        var form = new WorkoutLogForm(); form.setVersion(detail.version());
        for (Performance p : detail.performances()) {
            var row = new WorkoutLogForm.PerformanceForm(); row.setId(p.id()); row.setStatus(p.status());
            for (WorkSet s : p.actual()) {
                var set = new WorkoutLogForm.SetForm(); set.setReps(s.reps()); set.setWeightKg(s.weightKg()); set.setRir(s.rir()); row.getSets().add(set);
            }
            while (row.getSets().size() < p.prescription().sets()) row.getSets().add(new WorkoutLogForm.SetForm());
            form.getPerformances().add(row);
        }
        return form;
    }
    @Transactional
    public void save(Long owner, Long id, @Valid WorkoutLogForm form, boolean finish) {
        // Serialize completed history with session starts, so accepted evidence cannot change mid-start.
        profiles.lockOwner(owner).orElseThrow(WorkoutNotFoundException::new);
        var session = sessions.lockOwned(id, owner).orElseThrow(WorkoutNotFoundException::new);
        if (finish && session.getStatus() == SessionStatus.COMPLETED) return;
        if (session.getStatus() != SessionStatus.IN_PROGRESS) throw new WorkoutConflictException("This workout is closed and cannot be edited.");
        checkVersion(session, form.getVersion());
        var stored = session.getPerformances();
        if (form.getPerformances().size() != stored.size()) throw new WorkoutConflictException("The exercise rows changed. Reload the workout before saving.");
        List<Actual> normalized = new ArrayList<>();
        for (int i = 0; i < stored.size(); i++) {
            var row = form.getPerformances().get(i);
            if (!stored.get(i).getId().equals(row.getId())) throw new WorkoutConflictException("The exercise rows changed. Reload the workout before saving.");
            List<WorkSet> actual = new ArrayList<>();
            boolean gap = false;
            for (var set : row.getSets()) {
                if (set.blank()) { gap = true; continue; }
                if (set.getReps() == null || set.getWeightKg() == null)
                    throw new WorkoutInputException("Enter both reps and weight for each logged set, or clear the whole row. RIR is optional.");
                if (gap) throw new WorkoutInputException("Keep work sets consecutive. Fill or clear the blank row before a later set.");
                actual.add(new WorkSet(actual.size() + 1, set.getReps(), set.getWeightKg().setScale(2), set.getRir()));
            }
            if (row.getStatus() == PerformanceStatus.SKIPPED && !actual.isEmpty())
                throw new WorkoutInputException("A skipped exercise cannot also have logged sets. Clear its sets or change its status.");
            if (row.getStatus() == PerformanceStatus.COMPLETED && actual.isEmpty())
                throw new WorkoutInputException("Log at least one set for a completed exercise, or mark it skipped.");
            if (finish && row.getStatus() == PerformanceStatus.PENDING)
                throw new WorkoutInputException("Mark every exercise Completed or Skipped before finishing the workout.");
            normalized.add(new Actual(row.getId(), row.getStatus(), actual));
        }
        for (int i = 0; i < stored.size(); i++) stored.get(i).record(normalized.get(i).status(), normalized.get(i).sets());
        session.record(codec.hash(normalized), clock.instant(), finish);
        sessions.flush();
    }
    @Transactional
    public void cancel(Long owner, Long id, Long version) {
        var s = sessions.lockOwned(id, owner).orElseThrow(WorkoutNotFoundException::new);
        if (s.getStatus() == SessionStatus.CANCELLED) return;
        checkVersion(s, version);
        if (s.getStatus() != SessionStatus.IN_PROGRESS) throw new WorkoutConflictException("Completed workouts cannot be cancelled.");
        s.cancel(clock.instant()); sessions.flush();
    }
    @Transactional(readOnly = true)
    public Activity activity(Long owner, LocalDate weekStart, LocalDate today) {
        var draft = sessions.findFirstByUserIdAndStatusOrderByUpdatedAtDescIdDesc(owner, SessionStatus.IN_PROGRESS)
                .map(this::summary).orElse(null);
        var exercises = sessions.findFirstByUserIdAndStatusOrderBySessionDateDescIdDesc(owner, SessionStatus.COMPLETED)
                .map(session -> session.getPerformances().stream()
                        .filter(performance -> performance.getStatus() == PerformanceStatus.COMPLETED)
                        .map(this::prescription).map(p -> new ExerciseLink(p.exerciseId(), p.name())).toList())
                .orElse(List.of());
        return new Activity(draft, history(owner).stream().limit(5).toList(), exercises,
                sessions.countByUserIdAndStatusAndSessionDateBetween(owner, SessionStatus.COMPLETED, weekStart, today),
                performances.countCompletedSets(owner, weekStart, today),
                sessions.findDayIds(owner, SessionStatus.COMPLETED, today));
    }

    private Summary summary(WorkoutSession session) {
        return new Summary(session.getId(), session.getPlanId(), session.getSessionDate(), session.getStatus());
    }

    @Transactional(readOnly = true)
    public List<Summary> history(Long owner) {
        return sessions.findTop20ByUserIdOrderBySessionDateDescIdDesc(owner).stream()
                .map(this::summary).toList();
    }
    @Transactional(readOnly = true)
    public List<History> exerciseHistory(Long owner, Long exercise) {
        return recent(owner, exercise, today(), 10).stream().map(p -> new History(p.getSession().getId(), p.getSession().getSessionDate(),
                prescription(p), p.getStatus(), p.actualSets())).toList();
    }
    private List<Exposure> exposures(Long owner, Long exercise, LocalDate date, int count) {
        return recent(owner, exercise, date, count).stream().map(p -> {
            var t = prescription(p); var s = p.getSession();
            return new Exposure(p.getId(), p.getExerciseId(), s.getSessionDate(), s.getCompletedAt(), t.sets(), t.repMin(), t.repMax(),
                    t.restSeconds(), t.loadType(), p.getStatus() == PerformanceStatus.SKIPPED, p.actualSets());
        }).toList();
    }
    private List<ExercisePerformance> recent(Long owner, Long exercise, LocalDate date, int count) {
        var ids = performances.recentIds(owner, exercise, date, PageRequest.of(0, count));
        if (ids.isEmpty()) return List.of();
        var found = performances.findByIdIn(ids).stream().collect(Collectors.toMap(ExercisePerformance::getId, p -> p));
        return ids.stream().map(found::get).toList();
    }
    private Prescription prescription(ExercisePerformance p) { return codec.read(p.getPrescriptionSnapshot(), Prescription.class); }
    private WorkoutSession owned(Long owner, Long id) { return sessions.findByIdAndUserId(id, owner).orElseThrow(WorkoutNotFoundException::new); }
    private void validDate(LocalDate date) {
        if (date == null || date.isBefore(LocalDate.of(2000, 1, 1)) || date.isAfter(today()))
            throw new WorkoutInputException("Choose a workout date from 1 January 2000 through today.");
    }
    private void checkVersion(WorkoutSession s, Long version) {
        if (!Objects.equals(s.getVersion(), version)) throw new WorkoutConflictException("This workout changed in another tab. Reload it before saving again.");
    }
    private record Actual(Long id, PerformanceStatus status, List<WorkSet> sets) { }
}
