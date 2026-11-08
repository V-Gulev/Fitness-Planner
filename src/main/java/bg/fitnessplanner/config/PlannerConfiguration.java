package bg.fitnessplanner.config;

import bg.fitnessplanner.planning.engine.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration(proxyBeanMethods = false)
public class PlannerConfiguration {
    @Bean
    PlannerConfig plannerConfig(ObjectMapper mapper) throws IOException {
        try (var input = new ClassPathResource("planner-v1.json").getInputStream()) {
            return mapper.readValue(input, PlannerConfig.class);
        }
    }
    @Bean ScoredPlanner scoredPlanner(PlannerConfig config) { return new ScoredPlanner(config); }
    @Bean BaselinePlanner baselinePlanner(PlannerConfig config) { return new BaselinePlanner(config); }
    @Bean PlanEvaluator planEvaluator(PlannerConfig config) { return new PlanEvaluator(config); }
}
