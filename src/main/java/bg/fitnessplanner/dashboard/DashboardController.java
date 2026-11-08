package bg.fitnessplanner.dashboard;

import bg.fitnessplanner.account.application.AccountPrincipal;
import bg.fitnessplanner.planning.application.PlanQueryService;
import bg.fitnessplanner.planning.application.PlanViews;
import bg.fitnessplanner.tracking.application.WorkoutService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
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

        model.addAttribute("page", page);
        model.addAttribute("activity", activity);
        model.addAttribute("today", today);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("next", nextWorkout(page.plan(), today, activity.finishedToday()));
        return "dashboard/index";
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
}
