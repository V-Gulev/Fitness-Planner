package bg.fitnessplanner.tracking.web;

import bg.fitnessplanner.account.application.AccountPrincipal;
import bg.fitnessplanner.catalog.application.CatalogService;
import bg.fitnessplanner.config.SecurityConfig;
import bg.fitnessplanner.planning.application.PlanQueryService;
import bg.fitnessplanner.tracking.application.WorkoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkoutController.class)
@Import(SecurityConfig.class)
class WorkoutControllerBindingTest {
    @Autowired MockMvc mvc;
    @MockitoBean WorkoutService workouts;
    @MockitoBean CatalogService catalog;
    @MockitoBean PlanQueryService plans;

    @Test
    void rejectsSetIndexesBeyondTheFormLimitBeforeCallingTheService() throws Exception {
        var account = new AccountPrincipal(1L, "fixture@example.com", "unused");
        mvc.perform(post("/workouts/2").with(user(account)).with(csrf())
                        .param("version", "0")
                        .param("performances[0].id", "1")
                        .param("performances[0].sets[12].reps", "10"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(workouts);
    }
}
