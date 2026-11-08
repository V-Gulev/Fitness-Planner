package bg.fitnessplanner.planning.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class GenerationForm {
    @NotNull(message = "Reload this page before generating a plan.")
    @PositiveOrZero(message = "Reload this page before generating a plan.")
    private Long profileVersion;
    public Long getProfileVersion() { return profileVersion; }
    public void setProfileVersion(Long profileVersion) { this.profileVersion = profileVersion; }
}
