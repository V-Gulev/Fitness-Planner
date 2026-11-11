package bg.fitnessplanner.catalog.web;

import bg.fitnessplanner.catalog.application.CatalogService;
import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.MovementPattern;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Controller
public class CatalogController {
    private static final int PAGE_SIZE = 6;

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) { this.catalog = catalog; }

    @InitBinder("catalogFilter")
    void bindFilter(WebDataBinder binder) { binder.setAllowedFields("q", "muscle", "movement"); }

    @GetMapping("/exercises")
    String library(@Valid @ModelAttribute CatalogFilter filter, BindingResult errors,
                   @RequestParam(name = "page", defaultValue = "1") int requestedPage,
                   Model model, HttpServletResponse response) {
        var targets = Arrays.stream(LibraryMuscle.values()).toList();
        model.addAttribute("muscles", targets.stream().filter(target -> !target.isRegion()).toList());
        model.addAttribute("regions", targets.stream().filter(LibraryMuscle::isRegion).toList());
        model.addAttribute("movements", MovementPattern.values());
        var entries = catalog.snapshot();
        model.addAttribute("catalogSize", entries.size());
        List<ExerciseSnapshot> matches = List.of();
        if (errors.hasErrors()) {
            response.setStatus(400);
        } else {
            String query = filter.getQ().toLowerCase(Locale.ROOT);
            var muscle = filter.getMuscle();
            model.addAttribute("matchedMuscle", muscle);
            matches = entries.stream()
                    .filter(e -> e.name().toLowerCase(Locale.ROOT).contains(query))
                    .filter(e -> muscle == null || muscle.matches(e))
                    .filter(e -> filter.getMovement() == null || e.movementPattern() == filter.getMovement())
                    .sorted(Comparator.comparing((ExerciseSnapshot e) -> muscle != null && !muscle.isPrimary(e))
                            .thenComparing(ExerciseSnapshot::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        int pageCount = Math.max(1, Math.ceilDiv(matches.size(), PAGE_SIZE));
        int page = Math.clamp(requestedPage, 1, pageCount);
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, matches.size());
        model.addAttribute("exercises", matches.subList(start, end));
        model.addAttribute("matchCount", matches.size());
        model.addAttribute("firstResult", start + 1);
        model.addAttribute("lastResult", end);
        model.addAttribute("page", page);
        model.addAttribute("pageCount", pageCount);
        return "catalog/library";
    }
}
