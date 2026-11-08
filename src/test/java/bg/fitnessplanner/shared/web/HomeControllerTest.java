package bg.fitnessplanner.shared.web;

import bg.fitnessplanner.config.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {

    @MockitoBean
    private UserDetailsService users;

    @Autowired
    private MockMvc mvc;

    @Test
    void rendersThymeleafHomeWithSharedNavigationAndHonestAvailability() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("A stronger week")))
                .andExpect(content().string(containsString("Skip to content")))
                .andExpect(content().string(containsString("Log actual sets and review explained load suggestions")))
                .andExpect(content().string(not(containsString("th:replace"))));
    }

    @Test
    void servesTheSharedStylesheet() throws Exception {
        mvc.perform(get("/css/app.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));
    }

    @Test
    void servesTheLocalDisplayFont() throws Exception {
        mvc.perform(get("/fonts/BarlowCondensed-SemiBold.ttf"))
                .andExpect(status().isOk());
    }

    @Test
    void unlistedRoutesRequireAuthenticationByDefault() throws Exception {
        mvc.perform(get("/workout-plans").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
