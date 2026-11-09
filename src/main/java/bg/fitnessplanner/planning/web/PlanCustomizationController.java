package bg.fitnessplanner.planning.web;

import bg.fitnessplanner.account.application.AccountPrincipal;
import bg.fitnessplanner.planning.application.*;
import bg.fitnessplanner.planning.engine.PlanningModel.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/plans")
public class PlanCustomizationController {
    private final PlanGenerationService generation;
    private final PlanEditingService editing;
    private final PlanPublisher publisher;

    public PlanCustomizationController(PlanGenerationService generation, PlanEditingService editing, PlanPublisher publisher) {
        this.generation = generation;
        this.editing = editing;
        this.publisher = publisher;
    }

    @PostMapping("/options")
    String options(@AuthenticationPrincipal AccountPrincipal account, @RequestParam Long profileVersion, Model model) {
        var choices = generation.choices(account.getAccountId(), profileVersion);
        model.addAttribute("choices", choices);
        model.addAttribute("names", choices.input().catalog().stream()
                .collect(Collectors.toMap(e -> e.id(), e -> e.name())));
        return "planning/options";
    }

    @PostMapping("/select")
    String select(@AuthenticationPrincipal AccountPrincipal account, @RequestParam Long profileVersion,
                  @RequestParam String token, @RequestParam String candidate, RedirectAttributes redirect) {
        Long id = generation.select(account.getAccountId(), profileVersion, token, candidate);
        redirect.addFlashAttribute("notice", "Your selected plan is now active.");
        return "redirect:/plans/" + id;
    }

    @GetMapping("/{id}/edit")
    String edit(@AuthenticationPrincipal AccountPrincipal account, @PathVariable Long id, Model model) {
        var editor = editing.open(account.getAccountId(), id);
        return editorPage(id, editor, editor.base(), model);
    }

    @PostMapping("/{id}/edit")
    String review(@AuthenticationPrincipal AccountPrincipal account, @PathVariable Long id,
                  @RequestParam Long profileVersion, @RequestParam(defaultValue = "") String previewToken,
                  @RequestParam(defaultValue = "preview") String action, @RequestParam MultiValueMap<String, String> fields,
                  Model model, RedirectAttributes redirect) {
        Long owner = account.getAccountId();
        var editor = editing.open(owner, id);
        if (!editor.input().profile().version().equals(profileVersion)) throw new PlanConflictException();

        Plan draft;
        try {
            draft = readDraft(editor.base(), fields);
        } catch (IllegalArgumentException invalid) {
            model.addAttribute("formProblem", "The editor could not read those values. Review the exercise rows and try again.");
            return editorPage(id, editor, editor.base(), model);
        }
        var review = editing.review(editor, draft);
        if ("save".equals(action) && previewToken.equals(review.token()) && review.problems().isEmpty()
                && review.impact().changed()) {
            Long saved = publisher.publishEdited(owner, editor.input(), review.result(), id);
            redirect.addFlashAttribute("notice", "Your edited plan is now active.");
            redirect.addFlashAttribute("editImpact", review.impact());
            return "redirect:/plans/" + saved;
        }
        model.addAttribute("review", review);
        if ("save".equals(action) && !previewToken.equals(review.token())) {
            model.addAttribute("formProblem", "Review this updated comparison before saving.");
        }
        return editorPage(id, editor, draft, model);
    }

    private static Plan readDraft(Plan base, MultiValueMap<String, String> fields) {
        var days = new ArrayList<Day>();
        for (Day original : base.days()) {
            int weekday = original.weekday();
            String type = fields.getFirst("type-" + weekday);
            if (type == null) throw new IllegalArgumentException("Missing day type.");
            var ids = values(fields, "exercise", weekday);
            var sets = values(fields, "sets", weekday);
            var min = values(fields, "min", weekday);
            var max = values(fields, "max", weekday);
            var rest = values(fields, "rest", weekday);
            if (ids.size() > 8 || sets.size() != ids.size() || min.size() != ids.size()
                    || max.size() != ids.size() || rest.size() != ids.size()) {
                throw new IllegalArgumentException("Invalid exercise rows.");
            }
            var exercises = new ArrayList<Exercise>();
            for (int i = 0; i < ids.size(); i++) {
                long exerciseId = Long.parseLong(ids.get(i));
                exercises.add(new Exercise(exerciseId, Integer.parseInt(sets.get(i)), Integer.parseInt(min.get(i)),
                        Integer.parseInt(max.get(i)), Integer.parseInt(rest.get(i)), "Selected in your plan editor.", null));
            }
            days.add(new Day(weekday, DayType.valueOf(type), exercises));
        }
        return new Plan(days.stream().map(day -> day.type().name()).collect(Collectors.joining("/")), days);
    }

    private static List<String> values(MultiValueMap<String, String> fields, String name, int weekday) {
        return fields.getOrDefault(name + "-" + weekday, List.of());
    }

    private static String editorPage(Long id, PlanEditingService.Editor editor, Plan draft, Model model) {
        model.addAttribute("planId", id);
        model.addAttribute("editor", editor);
        model.addAttribute("draft", draft);
        model.addAttribute("dayTypes", DayType.values());
        model.addAttribute("eligibleIds", editor.eligible().stream().map(e -> e.id()).collect(Collectors.toSet()));
        return "planning/edit";
    }

    @ExceptionHandler(PlanConflictException.class)
    String conflict(RedirectAttributes redirect) {
        redirect.addFlashAttribute("notice", "Your plan, profile, or exercise library changed. Open the current plan and try again.");
        return "redirect:/plans";
    }

    @ExceptionHandler(MissingProfileException.class)
    String missingProfile() {
        return "redirect:/profile";
    }
}
