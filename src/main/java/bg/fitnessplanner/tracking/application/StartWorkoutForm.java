package bg.fitnessplanner.tracking.application;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.*;
public class StartWorkoutForm {
    @NotNull @Positive private Long dayId;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate date;
    @NotNull @Pattern(regexp = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") private String submissionToken;
    @NotNull @Pattern(regexp = "[0-9a-f]{64}") private String previewHash;
    @NotNull @Size(max = 8) private Set<@NotNull @Positive Long> acceptExerciseIds = new HashSet<>();
    public Long getDayId() { return dayId; }
    public void setDayId(Long dayId) { this.dayId = dayId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getSubmissionToken() { return submissionToken; }
    public void setSubmissionToken(String token) { submissionToken = token; }
    public String getPreviewHash() { return previewHash; }
    public void setPreviewHash(String hash) { previewHash = hash; }
    public Set<Long> getAcceptExerciseIds() { return acceptExerciseIds; }
    public void setAcceptExerciseIds(Set<Long> ids) { acceptExerciseIds = ids; }
}
