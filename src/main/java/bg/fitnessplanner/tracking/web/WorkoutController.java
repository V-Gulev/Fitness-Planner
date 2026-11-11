package bg.fitnessplanner.tracking.web;

import bg.fitnessplanner.account.application.AccountPrincipal;
import bg.fitnessplanner.planning.application.PlanQueryService;
import bg.fitnessplanner.tracking.application.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Controller
public class WorkoutController {
    private final WorkoutService workouts;
    private final bg.fitnessplanner.catalog.application.CatalogService catalog;
    private final PlanQueryService plans;

    public WorkoutController(WorkoutService workouts,
                             bg.fitnessplanner.catalog.application.CatalogService catalog,
                             PlanQueryService plans) {
        this.workouts = workouts;
        this.catalog = catalog;
        this.plans = plans;
    }
    @InitBinder("startWorkoutForm") void bindStart(WebDataBinder binder) {
        binder.setAllowedFields("dayId", "date", "submissionToken", "previewHash", "acceptExerciseIds");
    }
    @InitBinder("workoutLogForm") void bindLog(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(12);
        binder.setAllowedFields("version", "performances[*].id", "performances[*].status",
                "performances[*].sets[*].reps", "performances[*].sets[*].weightKg", "performances[*].sets[*].rir");
    }
    @GetMapping("/workouts")
    String history(@AuthenticationPrincipal AccountPrincipal account,
                   @RequestParam(required = false) String month,
                   @RequestParam(required = false) String date,
                   Model model) {
        Long owner = account.getAccountId();
        LocalDate today = workouts.today();
        YearMonth requestedMonth = parseMonth(month);
        LocalDate requestedDate = parseDate(date);
        YearMonth resolvedMonth = WorkoutCalendar.resolveMonth(requestedMonth, requestedDate, today);
        var bounds = WorkoutCalendar.gridBounds(resolvedMonth);
        var calendar = WorkoutCalendar.of(resolvedMonth, requestedDate, today,
                workouts.calendarSessions(owner, bounds.from(), bounds.through()));
        model.addAttribute("history", workouts.history(owner));
        model.addAttribute("calendar", calendar);
        model.addAttribute("today", today);
        model.addAttribute("plan", plans.page(owner, null).plan());
        model.addAttribute("openDayIds", workouts.openDraftDayIds(owner));
        return "tracking/history";
    }
    private static YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) return null;
        try { return YearMonth.parse(value.trim()); }
        catch (DateTimeParseException ignored) { return null; }
    }
    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDate.parse(value.trim()); }
        catch (DateTimeParseException ignored) { return null; }
    }
    @PostMapping("/workouts/start/{dayId}") String quickStart(@AuthenticationPrincipal AccountPrincipal account,
            @PathVariable Long dayId, RedirectAttributes redirect) {
        try {
            return "redirect:/workouts/" + workouts.quickStart(account.getAccountId(), dayId);
        } catch (WorkoutConflictException | WorkoutInputException problem) {
            redirect.addFlashAttribute("notice", problem.getMessage());
            return "redirect:/workouts";
        }
    }
    @GetMapping("/workouts/start/{dayId}") String startRedirect(@PathVariable Long dayId) {
        return "redirect:/workouts/new/" + dayId;
    }
    @GetMapping("/workouts/new/{dayId}") String preview(@AuthenticationPrincipal AccountPrincipal account, @PathVariable Long dayId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model, RedirectAttributes redirect) {
        try {
            var preview = workouts.preview(account.getAccountId(), dayId, date == null ? workouts.today() : date);
            if (preview.openSessionId() != null) return "redirect:/workouts/" + preview.openSessionId();
            var form = new StartWorkoutForm(); form.setDayId(dayId); form.setDate(preview.date());
            form.setSubmissionToken(UUID.randomUUID().toString()); form.setPreviewHash(preview.hash());
            model.addAttribute("preview", preview); model.addAttribute("startWorkoutForm", form); model.addAttribute("today", workouts.today());
            return "tracking/start";
        } catch (WorkoutConflictException | WorkoutInputException problem) {
            redirect.addFlashAttribute("notice", problem.getMessage()); return "redirect:/plans";
        }
    }
    @PostMapping("/workouts") String start(@AuthenticationPrincipal AccountPrincipal account, @Valid @ModelAttribute StartWorkoutForm form,
                                           BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) { redirect.addFlashAttribute("notice", "Reload the workout preview and review your choices."); return "redirect:/plans"; }
        try { return "redirect:/workouts/" + workouts.start(account.getAccountId(), form); }
        catch (WorkoutConflictException | WorkoutInputException problem) {
            redirect.addFlashAttribute("notice", problem.getMessage());
            return "redirect:/workouts/new/" + form.getDayId() + "?date=" + form.getDate();
        }
    }
    @GetMapping("/workouts/{id}") String detail(@AuthenticationPrincipal AccountPrincipal account, @PathVariable Long id, Model model) {
        var detail = workouts.detail(account.getAccountId(), id);
        model.addAttribute("workout", detail); model.addAttribute("workoutLogForm", workouts.form(detail)); return "tracking/session";
    }
    @PostMapping("/workouts/{id}") String save(@AuthenticationPrincipal AccountPrincipal account, @PathVariable Long id,
            @Valid @ModelAttribute WorkoutLogForm form, BindingResult errors, @RequestParam(defaultValue = "SAVE") String action,
            Model model, RedirectAttributes redirect) {
        var detail = workouts.detail(account.getAccountId(), id);
        if (!action.equals("SAVE") && !action.equals("FINISH")) errors.reject("action", "Choose Save draft or Finish workout.");
        boolean structure = form.getPerformances() != null && form.getPerformances().size() == detail.performances().size();
        if (structure) for (int i = 0; i < detail.performances().size(); i++) {
            if (form.getPerformances().get(i) == null || !detail.performances().get(i).id().equals(form.getPerformances().get(i).getId())) structure = false;
        }
        if (!structure) { redirect.addFlashAttribute("notice", "The exercise rows changed. Reloaded your saved workout; no changes were saved."); return "redirect:/workouts/" + id; }
        if (!errors.hasErrors()) try {
            workouts.save(account.getAccountId(), id, form, action.equals("FINISH"));
            redirect.addFlashAttribute("notice", action.equals("FINISH") ? "Workout completed. Your history is now saved." : "Workout draft saved.");
            return "redirect:/workouts/" + id;
        } catch (WorkoutInputException invalid) { errors.reject("invalid", invalid.getMessage()); }
          catch (WorkoutConflictException conflict) { redirect.addFlashAttribute("notice", conflict.getMessage()); return "redirect:/workouts/" + id; }
        for (int i = 0; i < detail.performances().size(); i++) {
            var row = form.getPerformances().get(i);
            if (row.getSets() == null) row.setSets(new java.util.ArrayList<>());
            while (row.getSets().size() < detail.performances().get(i).prescription().sets()) row.getSets().add(new WorkoutLogForm.SetForm());
        }
        model.addAttribute("workout", detail); return "tracking/session";
    }
    @PostMapping("/workouts/{id}/cancel") String cancel(@AuthenticationPrincipal AccountPrincipal account, @PathVariable Long id,
            @RequestParam Long version, RedirectAttributes redirect) {
        try { workouts.cancel(account.getAccountId(), id, version); redirect.addFlashAttribute("notice", "Draft cancelled. It is excluded from progression evidence."); }
        catch (WorkoutConflictException problem) { redirect.addFlashAttribute("notice", problem.getMessage()); }
        return "redirect:/workouts/" + id;
    }
    @GetMapping("/workouts/exercises/{exerciseId}") String exerciseHistory(@AuthenticationPrincipal AccountPrincipal account, @PathVariable Long exerciseId, Model model) {
        var history = workouts.exerciseHistory(account.getAccountId(), exerciseId);
        String name = history.isEmpty() ? catalog.snapshot().stream().filter(e -> e.id().equals(exerciseId)).findFirst()
                .map(bg.fitnessplanner.catalog.application.ExerciseSnapshot::name).orElseThrow(WorkoutNotFoundException::new)
                : history.getFirst().prescription().name();
        model.addAttribute("history", history);
        model.addAttribute("exerciseName", name);
        model.addAttribute("hasRecordedSets", history.stream().anyMatch(item -> !item.actual().isEmpty()));
        return "tracking/exercise-history";
    }
}
