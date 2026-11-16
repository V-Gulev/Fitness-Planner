package bg.fitnessplanner.planning.web;

import bg.fitnessplanner.account.application.AccountPrincipal;
import bg.fitnessplanner.config.SecurityConfig;
import bg.fitnessplanner.planning.application.PlanEditingService;
import bg.fitnessplanner.planning.application.PlanGenerationService;
import bg.fitnessplanner.planning.application.PlanInputReader;
import bg.fitnessplanner.planning.application.PlanPublisher;
import bg.fitnessplanner.planning.engine.PlanningModel.Day;
import bg.fitnessplanner.planning.engine.PlanningModel.DayType;
import bg.fitnessplanner.planning.engine.PlanningModel.Exercise;
import bg.fitnessplanner.planning.engine.PlanningModel.Plan;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.shared.domain.ExperienceLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanCustomizationController.class)
@Import(SecurityConfig.class)
class PlanEditorRenderingTest {
    @Autowired MockMvc mvc;
    @MockitoBean PlanGenerationService generation;
    @MockitoBean PlanEditingService editing;
    @MockitoBean PlanPublisher publisher;

    @Test
    void rendersDistinctLabelsAndAcceptsWholeSecondRestTimes() throws Exception {
        var account = new AccountPrincipal(1L, "fixture@example.com", "unused");
        var profile = new ProfileSnapshot(1L, 0L, ExperienceLevel.BEGINNER, TrainingGoal.GENERAL_FITNESS,
                60, Set.of(1, 3), Set.of(), Set.of(), Set.of(), Set.of());
        var exercises = List.of(new Exercise(1L, 3, 8, 12, 83, "", null),
                new Exercise(2L, 3, 8, 12, 90, "", null));
        var plan = new Plan("FULL/FULL", List.of(new Day(1, DayType.FULL, exercises),
                new Day(3, DayType.FULL, exercises)));
        when(editing.open(1L, 2L)).thenReturn(new PlanEditingService.Editor(
                new PlanInputReader.Inputs(profile, List.of()), plan, null, List.of()));

        String html = mvc.perform(get("/plans/2/edit").with(user(account)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        var restLabels = Pattern.compile("<label for=\"(edit-rest-[^\"]+)\">Rest").matcher(html)
                .results().map(match -> match.group(1)).toList();
        assertThat(restLabels).hasSize(6).doesNotHaveDuplicates();
        for (String id : restLabels) {
            assertThat(html).containsPattern("<input[^>]*step=\"1\"[^>]*id=\"" + id + "\"");
        }
        assertThat(html).contains("value=\"83\"");
    }
}
