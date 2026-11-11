package bg.fitnessplanner.tracking.web;

import bg.fitnessplanner.tracking.application.WorkoutViews.CalendarSession;
import bg.fitnessplanner.tracking.persistence.SessionStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static bg.fitnessplanner.tracking.application.WorkoutService.EARLIEST_WORKOUT_DATE;

public record WorkoutCalendar(
        YearMonth month,
        YearMonth previousMonth,
        YearMonth nextMonth,
        YearMonth currentMonth,
        LocalDate selectedDate,
        long trainingDays,
        long completedSessions,
        long loggedSets,
        List<Day> days
) {
    private static final YearMonth FIRST_MONTH = YearMonth.from(EARLIEST_WORKOUT_DATE);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);

    public record DateRange(LocalDate from, LocalDate through) { }

    public static DateRange gridBounds(YearMonth month) {
        var first = month.atDay(1);
        var last = month.atEndOfMonth();
        var gridStart = first.minusDays(first.getDayOfWeek().getValue() - 1L);
        var gridEndExclusive = last.plusDays(8L - last.getDayOfWeek().getValue());
        return new DateRange(gridStart, gridEndExclusive.minusDays(1));
    }

    public static YearMonth resolveMonth(YearMonth month, LocalDate date, LocalDate today) {
        var current = YearMonth.from(today);
        var requested = month != null ? month : date != null ? YearMonth.from(date) : current;
        if (requested.isBefore(FIRST_MONTH)) return FIRST_MONTH;
        return requested.isAfter(current) ? current : requested;
    }

    public static WorkoutCalendar of(YearMonth month, LocalDate date, LocalDate today, List<CalendarSession> sessions) {
        var current = YearMonth.from(today);
        var byDate = sessions.stream().collect(Collectors.groupingBy(CalendarSession::date));
        var bounds = gridBounds(month);
        var days = bounds.from().datesUntil(bounds.through().plusDays(1)).map(day -> {
            var logs = byDate.getOrDefault(day, List.of());
            long completed = logs.stream().filter(log -> log.status() == SessionStatus.COMPLETED).count();
            long drafts = logs.stream().filter(log -> log.status() == SessionStatus.IN_PROGRESS).count();
            long cancelled = logs.stream().filter(log -> log.status() == SessionStatus.CANCELLED).count();
            return new Day(day, month.equals(YearMonth.from(day)), day.equals(today), day.isAfter(today),
                    completed, drafts, cancelled, logs);
        }).toList();

        var selected = date != null && month.equals(YearMonth.from(date)) && !date.isAfter(today)
                ? date
                : days.stream().filter(day -> day.inMonth() && !day.sessions().isEmpty())
                        .max(Comparator.comparing(Day::trained).thenComparing(Day::date))
                        .map(Day::date).orElse(month.equals(current) ? today : month.atDay(1));

        var monthDays = days.stream().filter(Day::inMonth).toList();
        long trainingDays = monthDays.stream().filter(Day::trained).count();
        long completedSessions = monthDays.stream().mapToLong(Day::completed).sum();
        long loggedSets = monthDays.stream()
                .flatMap(d -> d.sessions().stream())
                .filter(s -> s.status() == SessionStatus.COMPLETED)
                .mapToLong(CalendarSession::totalSets)
                .sum();

        return new WorkoutCalendar(month, month.equals(FIRST_MONTH) ? null : month.minusMonths(1),
                month.equals(current) ? null : month.plusMonths(1), current, selected,
                trainingDays, completedSessions, loggedSets, days);
    }

    public String label() { return month.format(MONTH_LABEL); }

    public boolean currentMonthActive() { return month.equals(currentMonth); }

    public Day selectedDay() {
        return days.stream().filter(d -> d.date().equals(selectedDate)).findFirst()
                .orElseGet(() -> days.stream().filter(Day::inMonth).findFirst().orElse(days.getFirst()));
    }

    public List<List<Day>> weeks() {
        return IntStream.range(0, days.size() / 7).mapToObj(week -> days.subList(week * 7, (week + 1) * 7)).toList();
    }

    public record Day(
            LocalDate date,
            boolean inMonth,
            boolean today,
            boolean future,
            long completed,
            long drafts,
            long cancelled,
            List<CalendarSession> sessions
    ) {
        public boolean trained() { return completed > 0; }
        public boolean hasDraft() { return drafts > 0; }
        public boolean hasSessions() { return !sessions.isEmpty(); }
        public boolean selectable() { return inMonth && !future; }
        public String label() { return date.format(DAY_LABEL); }
        public String primaryTitle() {
            return sessions.stream()
                    .filter(s -> s.status() == SessionStatus.COMPLETED)
                    .findFirst()
                    .or(() -> sessions.stream().filter(s -> s.status() == SessionStatus.IN_PROGRESS).findFirst())
                    .or(() -> sessions.stream().findFirst())
                    .map(CalendarSession::shortLabel)
                    .orElse(null);
        }
    }
}
