package bg.fitnessplanner.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavigationAdvice {
    @ModelAttribute("navigationPath")
    String navigationPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    @ModelAttribute("signedIn")
    boolean signedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
