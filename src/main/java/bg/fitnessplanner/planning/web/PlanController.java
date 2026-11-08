package bg.fitnessplanner.planning.web;

import bg.fitnessplanner.account.application.AccountPrincipal;
import bg.fitnessplanner.planning.application.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class PlanController {
    private final PlanGenerationService generation;
    private final PlanQueryService queries;
    public PlanController(PlanGenerationService generation, PlanQueryService queries) { this.generation = generation; this.queries = queries; }

    @InitBinder("generationForm")
    void bind(WebDataBinder binder) { binder.setAllowedFields("profileVersion"); }

    @GetMapping({"/plans", "/plans/{id}"})
    String view(@AuthenticationPrincipal AccountPrincipal account, @PathVariable(required = false) Long id, Model model) {
        var page = queries.page(account.getAccountId(), id);
        GenerationForm form = new GenerationForm();
        if (page.profile() != null) form.setProfileVersion(page.profile().version());
        model.addAttribute("page", page);
        model.addAttribute("generationForm", form);
        return "planning/plan";
    }

    @PostMapping("/plans")
    String generate(@AuthenticationPrincipal AccountPrincipal account, @Valid @ModelAttribute GenerationForm form,
                    BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                var outcome = generation.generate(account.getAccountId(), form.getProfileVersion());
                if (outcome.planId() != null) {
                    redirect.addFlashAttribute("notice", "Your new plan has been saved. Previous versions remain available below.");
                    return "redirect:/plans/" + outcome.planId();
                }
                model.addAttribute("generationProblems", outcome.result().diagnostics());
            } catch (MissingProfileException missing) {
                redirect.addFlashAttribute("notice", missing.getMessage());
                return "redirect:/profile";
            } catch (PlanConflictException conflict) {
                model.addAttribute("generationProblems", List.of(conflict.getMessage()));
            }
        }
        model.addAttribute("page", queries.page(account.getAccountId(), null));
        return "planning/plan";
    }
}
