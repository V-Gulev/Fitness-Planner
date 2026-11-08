package bg.fitnessplanner.planning.persistence;

import bg.fitnessplanner.planning.engine.PlanningModel.Status;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "workout_plan")
public class WorkoutPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false, updatable = false) private Long userId;
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime(6)") private Instant createdAt;
    @Column(name = "algorithm_version", nullable = false, length = 40) private String algorithmVersion;
    @Column(name = "config_version", nullable = false, length = 64) private String configVersion;
    @Column(name = "catalog_hash", nullable = false, length = 64, columnDefinition = "char(64)") private String catalogHash;
    @Column(name = "config_hash", nullable = false, length = 64, columnDefinition = "char(64)") private String configHash;
    @Column(name = "profile_hash", nullable = false, length = 64, columnDefinition = "char(64)") private String profileHash;
    @Column(name = "profile_version", nullable = false) private Long profileVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Status status;
    @Column(name = "candidate_key", nullable = false, length = 120) private String candidateKey;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "input_snapshot", nullable = false, columnDefinition = "json") private String inputSnapshot;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "config_snapshot", nullable = false, columnDefinition = "json") private String configSnapshot;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "catalog_snapshot", nullable = false, columnDefinition = "json") private String catalogSnapshot;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "evaluation_json", nullable = false, columnDefinition = "json") private String evaluationJson;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "diagnostics_json", nullable = false, columnDefinition = "json") private String diagnosticsJson;
    @Column(name = "candidates_constructed", nullable = false) private int candidatesConstructed;
    @Column(name = "additions_examined", nullable = false) private long additionsExamined;
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL)
    @OrderBy("weekday ASC")
    private Set<WorkoutDay> days = new LinkedHashSet<>();

    protected WorkoutPlan() { }

    public WorkoutPlan(Long owner, Instant createdAt, String algorithmVersion, String configVersion,
                       String catalogHash, String configHash, String profileHash, Long profileVersion,
                       Status status, String candidateKey, String inputSnapshot, String configSnapshot,
                       String catalogSnapshot, String evaluationJson, String diagnosticsJson,
                       int candidatesConstructed, long additionsExamined) {
        this.userId = owner; this.createdAt = createdAt; this.algorithmVersion = algorithmVersion;
        this.configVersion = configVersion; this.catalogHash = catalogHash; this.configHash = configHash;
        this.profileHash = profileHash; this.profileVersion = profileVersion; this.status = status;
        this.candidateKey = candidateKey; this.inputSnapshot = inputSnapshot; this.configSnapshot = configSnapshot;
        this.catalogSnapshot = catalogSnapshot; this.evaluationJson = evaluationJson; this.diagnosticsJson = diagnosticsJson;
        this.candidatesConstructed = candidatesConstructed; this.additionsExamined = additionsExamined;
    }
    public void addDay(WorkoutDay day) { days.add(day); }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getAlgorithmVersion() { return algorithmVersion; }
    public String getConfigVersion() { return configVersion; }
    public String getCatalogHash() { return catalogHash; }
    public String getConfigHash() { return configHash; }
    public String getProfileHash() { return profileHash; }
    public Long getProfileVersion() { return profileVersion; }
    public Status getStatus() { return status; }
    public String getCandidateKey() { return candidateKey; }
    public String getInputSnapshot() { return inputSnapshot; }
    public String getConfigSnapshot() { return configSnapshot; }
    public String getCatalogSnapshot() { return catalogSnapshot; }
    public String getEvaluationJson() { return evaluationJson; }
    public String getDiagnosticsJson() { return diagnosticsJson; }
    public int getCandidatesConstructed() { return candidatesConstructed; }
    public long getAdditionsExamined() { return additionsExamined; }
    public Set<WorkoutDay> getDays() { return Set.copyOf(days); }
}
