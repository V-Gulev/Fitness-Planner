package bg.fitnessplanner.config;
import bg.fitnessplanner.progression.engine.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
@Configuration(proxyBeanMethods = false)
public class ProgressionConfiguration {
    @Bean ProgressionConfig progressionConfig(ObjectMapper mapper) throws IOException {
        try (var input = new ClassPathResource("progression-v1.json").getInputStream()) {
            return mapper.readValue(input, ProgressionConfig.class);
        }
    }
    @Bean ProgressionEngine progressionEngine(ProgressionConfig config) { return new ProgressionEngine(config); }
}
