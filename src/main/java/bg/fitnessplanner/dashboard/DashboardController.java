package bg.fitnessplanner.dashboard;

import bg.fitnessplanner.account.application.AccountPrincipal;
import bg.fitnessplanner.planning.application.PlanQueryService;
import bg.fitnessplanner.planning.application.PlanViews;
import bg.fitnessplanner.tracking.application.WorkoutService;
import bg.fitnessplanner.tracking.application.WorkoutViews;
import bg.fitnessplanner.tracking.web.WorkoutCalendar;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Controller
public class DashboardController {
    private final PlanQueryService plans;
    private final WorkoutService workouts;

    public DashboardController(PlanQueryService plans, WorkoutService workouts) {
        this.plans = plans;
        this.workouts = workouts;
    }

    @GetMapping("/dashboard")
    String dashboard(@AuthenticationPrincipal AccountPrincipal account, Model model) {
        Long owner = account.getAccountId();
        LocalDate today = workouts.today();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var page = plans.page(owner, null);
        var activity = workouts.activity(owner, weekStart, today);
        var next = nextWorkout(page.plan(), today, activity.finishedToday());
        YearMonth currentMonth = YearMonth.from(today);
        var bounds = WorkoutCalendar.gridBounds(currentMonth);
        var calendar = WorkoutCalendar.of(currentMonth, today, today,
                workouts.calendarSessions(owner, bounds.from(), bounds.through()));
        var recentDetails = workouts.sessionDetailsByIds(owner,
                activity.recent().stream().map(WorkoutViews.Summary::id).toList());

        model.addAttribute("page", page);
        model.addAttribute("activity", activity);
        model.addAttribute("recentDetails", recentDetails);
        model.addAttribute("calendar", calendar);
        model.addAttribute("today", today);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("next", next);
        if (page.plan() != null) {
            model.addAttribute("planWeek", planWeek(page.plan(), next));
        }
        return "dashboard/index";
    }

    private static List<WeekSlot> planWeek(PlanViews.Details plan, ScheduledWorkout next) {
        return Arrays.stream(DayOfWeek.values()).map(weekday -> {
            var day = plan.days().stream().filter(workout -> workout.weekdayNumber() == weekday.getValue())
                    .findFirst().orElse(null);
            return new WeekSlot(weekday.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    weekday.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), day,
                    day != null && next != null && day.id().equals(next.day().id()));
        }).toList();
    }

    private static ScheduledWorkout nextWorkout(PlanViews.Details plan, LocalDate today, Set<Long> finishedToday) {
        if (plan == null || plan.profileChanged()) {
            return null;
        }
        return plan.days().stream().map(day -> {
            int offset = Math.floorMod(day.weekdayNumber() - today.getDayOfWeek().getValue(), 7);
            if (offset == 0 && finishedToday.contains(day.id())) {
                offset = 7;
            }
            return new ScheduledWorkout(day, today.plusDays(offset));
        }).min(Comparator.comparing(ScheduledWorkout::date)).orElse(null);
    }

    public record ScheduledWorkout(PlanViews.Day day, LocalDate date) { }
    public record WeekSlot(String weekday, String shortLabel, PlanViews.Day day, boolean next) { }
}
