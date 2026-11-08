package bg.fitnessplanner.account.web;

import bg.fitnessplanner.account.application.DuplicateEmailException;
import bg.fitnessplanner.account.application.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AccountController {
    private final RegistrationService registration;

    public AccountController(RegistrationService registration) {
        this.registration = registration;
    }

    @InitBinder("registrationForm")
    void bindRegistration(WebDataBinder binder) {
        binder.setAllowedFields("email", "password", "confirmPassword");
    }

    @GetMapping("/login")
    String login() { return "account/login"; }

    @GetMapping("/register")
    String register(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "account/register";
    }

    @PostMapping("/register")
    String register(@Valid @ModelAttribute RegistrationForm form, BindingResult errors) {
        if (!errors.hasErrors()) {
            try {
                registration.register(form.getEmail(), form.getPassword());
                return "redirect:/login?registered";
            } catch (DuplicateEmailException duplicate) {
                errors.rejectValue("email", "duplicate", duplicate.getMessage());
            }
        }
        form.setPassword(null);
        form.setConfirmPassword(null);
        return "account/register";
    }
}
