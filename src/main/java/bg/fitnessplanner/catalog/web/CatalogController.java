package bg.fitnessplanner.catalog.web;

import bg.fitnessplanner.catalog.application.CatalogService;
import bg.fitnessplanner.catalog.domain.MovementPattern;
import bg.fitnessplanner.catalog.domain.MuscleGroup;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Locale;

@Controller
public class CatalogController {
    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) { this.catalog = catalog; }

    @InitBinder("catalogFilter")
    void bindFilter(WebDataBinder binder) { binder.setAllowedFields("q", "muscle", "movement"); }

    @GetMapping("/exercises")
    String library(@Valid @ModelAttribute CatalogFilter filter, BindingResult errors,
                   Model model, HttpServletResponse response) {
        model.addAttribute("muscles", MuscleGroup.values());
        model.addAttribute("movements", MovementPattern.values());
        if (errors.hasErrors()) {
            response.setStatus(400);
            model.addAttribute("exercises", List.of());
        } else {
            String query = filter.getQ().toLowerCase(Locale.ROOT);
            model.addAttribute("exercises", catalog.snapshot().stream()
                    .filter(e -> e.name().toLowerCase(Locale.ROOT).contains(query))
                    .filter(e -> filter.getMuscle() == null || e.primaryMuscle() == filter.getMuscle())
                    .filter(e -> filter.getMovement() == null || e.movementPattern() == filter.getMovement()).toList());
        }
        return "catalog/library";
    }
}
