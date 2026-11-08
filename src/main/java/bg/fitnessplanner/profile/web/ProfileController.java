package bg.fitnessplanner.profile.web;

import bg.fitnessplanner.account.application.AccountPrincipal;
import bg.fitnessplanner.catalog.application.CatalogService;
import bg.fitnessplanner.catalog.domain.Equipment;
import bg.fitnessplanner.catalog.domain.MovementPattern;
import bg.fitnessplanner.profile.application.InvalidProfileException;
import bg.fitnessplanner.profile.application.ProfileForm;
import bg.fitnessplanner.profile.application.ProfileService;
import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.shared.domain.ExperienceLevel;
import jakarta.validation.Valid;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;

@Controller
public class ProfileController {
    private final ProfileService profiles;
    private final CatalogService catalog;
    private final bg.fitnessplanner.progression.engine.ProgressionConfig progression;

    public ProfileController(ProfileService profiles, CatalogService catalog, bg.fitnessplanner.progression.engine.ProgressionConfig progression) {
        this.profiles = profiles;
        this.catalog = catalog;
        this.progression = progression;
    }

    @InitBinder("profileForm")
    void bindProfile(WebDataBinder binder) {
        binder.setAllowedFields("experience", "goal", "sessionMinutes", "weekdays", "equipment",
                "preferredExerciseIds", "excludedExerciseIds", "excludedMovements", "version", "loadSteps[*]");
    }

    @GetMapping("/profile")
    String edit(@AuthenticationPrincipal AccountPrincipal account, Model model) {
        model.addAttribute("profileForm", profiles.form(account.getAccountId()));
        choices(model);
        return "profile/edit";
    }

    @PostMapping("/profile")
    String save(@AuthenticationPrincipal AccountPrincipal account, @Valid @ModelAttribute ProfileForm form,
                BindingResult errors, Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                profiles.save(account.getAccountId(), form);
                redirect.addFlashAttribute("notice", "Your fitness profile has been saved.");
                return "redirect:/profile";
            } catch (InvalidProfileException invalid) {
                errors.rejectValue(invalid.getField(), "invalid", invalid.getMessage());
            } catch (OptimisticLockingFailureException conflict) {
                errors.rejectValue("version", "conflict", "Your profile changed elsewhere. Reload it before saving again.");
            }
        }
        choices(model);
        return "profile/edit";
    }

    private void choices(Model model) {
        model.addAttribute("loadStepChoices", progression.defaultSteps().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).toList());
        model.addAttribute("experiences", ExperienceLevel.values());
        model.addAttribute("goals", TrainingGoal.values());
        model.addAttribute("equipmentChoices", Equipment.values());
        model.addAttribute("movements", MovementPattern.values());
        model.addAttribute("exerciseChoices", catalog.snapshot());
        model.addAttribute("days", Arrays.stream(DayOfWeek.values())
                .map(day -> new WeekdayChoice(day.getValue(), day.getDisplayName(TextStyle.FULL, Locale.ENGLISH))).toList());
    }

    public record WeekdayChoice(int value, String label) { }
}
