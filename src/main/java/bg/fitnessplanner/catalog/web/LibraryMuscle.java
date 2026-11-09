package bg.fitnessplanner.catalog.web;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.MuscleGroup;

import java.util.Set;

/** Library anatomy and exercise examples are independent of the planner's muscle taxonomy. */
public enum LibraryMuscle {
    CHEST, BACK, QUADRICEPS, HAMSTRINGS, GLUTES, SHOULDERS, BICEPS, TRICEPS, CALVES, CORE,

    CHEST_UPPER(MuscleGroup.CHEST, "Upper chest", "Clavicular fibers near the collarbone.",
            "A low incline can emphasize this region. The study used a barbell, and the library shows the comparable dumbbell press.",
            study(33049982), "INCLINE_DUMBBELL_PRESS"),
    CHEST_MIDDLE(MuscleGroup.CHEST, "Middle chest", "Central fibers of the broad sternocostal portion.",
            "Flat presses involve both the middle and lower regions of the pectoralis major.",
            study(33049982), "BARBELL_BENCH_PRESS", "DUMBBELL_BENCH_PRESS"),
    CHEST_LOWER(MuscleGroup.CHEST, "Lower chest", "Inferior fibers along the lower edge of the pectoralis major.",
            "Flat presses involve both the middle and lower regions of the pectoralis major.",
            study(33049982), "BARBELL_BENCH_PRESS", "DUMBBELL_BENCH_PRESS"),

    BACK_UPPER(MuscleGroup.BACK, "Trapezius and rhomboids", "The trapezius covers much of the upper back. The rhomboids lie beneath it between the spine and shoulder blades.",
            "Rows involve these muscles as the shoulder blades draw back. The map shows their shared upper-back area.",
            study(15228624), "CABLE_ROW", "CHEST_SUPPORTED_DUMBBELL_ROW", "BARBELL_ROW"),
    BACK_LATS(MuscleGroup.BACK, "Latissimus dorsi", "The lats cover the sides of the back and attach to the upper arms.",
            "Pulldowns and rows both involve the lats, along with other back and arm muscles.",
            study(15228624), "LAT_PULLDOWN", "NEUTRAL_GRIP_LAT_PULLDOWN", "CABLE_ROW", "PULL_UP"),
    BACK_ERECTORS(MuscleGroup.BACK, "Erector spinae", "Long muscles along the spine help extend and steady the trunk. The map marks their lower-back location.",
            "The spinal erectors help hold the trunk steady during deadlifts and unsupported rows.",
            study(19197209), "BARBELL_DEADLIFT", "BARBELL_ROW", "PENDLAY_ROW"),

    QUADRICEPS_RECTUS(MuscleGroup.QUADRICEPS, "Rectus femoris", "The central front thigh muscle crosses both the hip and knee.",
            "Lunges involve the knee extensors. The rectus femoris works alongside the other quadriceps during these movements.",
            study(41316621), "BODYWEIGHT_FORWARD_LUNGE", "DUMBBELL_REVERSE_LUNGE"),
    QUADRICEPS_LATERALIS(MuscleGroup.QUADRICEPS, "Vastus lateralis", "The outer quadriceps muscle straightens the knee.",
            "Squats and leg presses train the vasti together.",
            study(36498298), "BARBELL_BACK_SQUAT", "DUMBBELL_GOBLET_SQUAT", "LEG_PRESS"),
    QUADRICEPS_MEDIALIS(MuscleGroup.QUADRICEPS, "Vastus medialis", "The inner quadriceps muscle is visible near the inside of the knee.",
            "Squats and leg presses train the vasti together.",
            study(36498298), "BARBELL_BACK_SQUAT", "DUMBBELL_GOBLET_SQUAT", "LEG_PRESS"),
    QUADRICEPS_INTERMEDIUS(MuscleGroup.QUADRICEPS, "Vastus intermedius", "This knee extensor lies beneath the rectus femoris. The map reveals it on one side.",
            "Squats and leg presses train the vasti together.",
            study(36498298), "BARBELL_BACK_SQUAT", "DUMBBELL_GOBLET_SQUAT", "LEG_PRESS"),

    HAMSTRINGS_BICEPS(MuscleGroup.HAMSTRINGS, "Biceps femoris", "The outer hamstring has two heads. Both bend the knee, and the long head also crosses the hip.",
            "Leg curls involve both heads. Hip hinges such as the Romanian deadlift load the hamstrings that cross the hip.",
            study(33009197), "LEG_CURL", "PRONE_MACHINE_LEG_CURL", "DUMBBELL_ROMANIAN_DEADLIFT"),
    HAMSTRINGS_TENDINOSUS(MuscleGroup.HAMSTRINGS, "Semitendinosus", "A medial hamstring with a long tendon that runs to the inner side of the knee.",
            "The semitendinosus and semimembranosus both contribute during leg curls.",
            study(33009197), "LEG_CURL", "PRONE_MACHINE_LEG_CURL", "SINGLE_LEG_SEATED_LEG_CURL"),
    HAMSTRINGS_MEMBRANOSUS(MuscleGroup.HAMSTRINGS, "Semimembranosus", "A medial hamstring that sits deeper than the semitendinosus. The map reveals it on one side.",
            "The semimembranosus and semitendinosus both contribute during leg curls.",
            study(33009197), "LEG_CURL", "PRONE_MACHINE_LEG_CURL", "SINGLE_LEG_SEATED_LEG_CURL"),

    GLUTES_MAXIMUS(MuscleGroup.GLUTES, "Gluteus maximus", "The large superficial buttock muscle helps extend the hip.",
            "Hip thrusts, bridges and single-leg hinges involve hip extension.",
            study(19574661), "BARBELL_HIP_THRUST", "DUMBBELL_HIP_THRUST", "GLUTE_BRIDGE", "DUMBBELL_SINGLE_LEG_RDL"),
    GLUTES_MEDIUS(MuscleGroup.GLUTES, "Gluteus medius", "The upper outer hip muscle helps move the thigh out and steady the pelvis on one leg.",
            "In these single-leg exercises, the medius contributes to hip and pelvis stability.",
            study(19321909), "DUMBBELL_STEP_UP", "DUMBBELL_SINGLE_LEG_RDL", "SINGLE_LEG_GLUTE_BRIDGE"),
    GLUTES_MINIMUS(MuscleGroup.GLUTES, "Gluteus minimus", "A deeper hip abductor beneath the gluteus medius. The map reveals it on one side.",
            "A fine-wire EMG study measured activity in the posterior gluteus minimus during a single-leg bridge.",
            study(29584533), "SINGLE_LEG_GLUTE_BRIDGE"),

    SHOULDERS_FRONT(MuscleGroup.SHOULDERS, "Anterior deltoid", "The front shoulder fibers help raise the arm forward.",
            "Overhead pressing involves the anterior deltoid alongside the other shoulder muscles.",
            study(33312291), "DUMBBELL_SHOULDER_PRESS", "BARBELL_OVERHEAD_PRESS", "SEATED_DUMBBELL_SHOULDER_PRESS"),
    SHOULDERS_SIDE(MuscleGroup.SHOULDERS, "Middle deltoid", "The outer shoulder fibers help raise the arm sideways.",
            "Lateral raises and overhead presses involve the middle deltoid.",
            study(33312291), "DUMBBELL_LATERAL_RAISE", "CABLE_LATERAL_RAISE", "DUMBBELL_SHOULDER_PRESS"),
    SHOULDERS_REAR(MuscleGroup.SHOULDERS, "Posterior deltoid", "The rear shoulder fibers help move the arm backward and outward.",
            "Reverse flyes and face pulls use rear-shoulder movement. Other shoulder and upper-back muscles assist.",
            study(39593452), "DUMBBELL_REVERSE_FLY", "CHEST_SUPPORTED_REVERSE_FLY", "CABLE_FACE_PULL"),

    BICEPS_LONG(MuscleGroup.BICEPS, "Biceps long head", "The outer head starts above the shoulder socket and joins the short head toward the elbow.",
            "Both heads contribute to curling. The library shows the same biceps exercises for both heads.",
            anatomy("NBK519538")),
    BICEPS_SHORT(MuscleGroup.BICEPS, "Biceps short head", "The inner head starts at the coracoid process of the shoulder blade.",
            "Both heads contribute to curling. The library shows the same biceps exercises for both heads.",
            anatomy("NBK519538")),

    TRICEPS_LONG(MuscleGroup.TRICEPS, "Triceps long head", "The long head crosses the shoulder as well as the elbow.",
            "Overhead extensions lengthen the long head at the shoulder and train the triceps together.",
            study(35819335), "CABLE_OVERHEAD_TRICEPS_EXTENSION", "DUMBBELL_OVERHEAD_TRICEPS_EXTENSION"),
    TRICEPS_LATERAL(MuscleGroup.TRICEPS, "Triceps lateral head", "The outer head starts on the humerus and helps straighten the elbow.",
            "All three heads extend the elbow. The library shows shared triceps exercises for the lateral and medial heads.",
            anatomy("NBK536996")),
    TRICEPS_MEDIAL(MuscleGroup.TRICEPS, "Triceps medial head", "This head lies mostly under the other heads. The map reveals it on one side.",
            "All three heads extend the elbow. The library shows shared triceps exercises for the lateral and medial heads.",
            anatomy("NBK536996")),

    CALVES_MEDIAL(MuscleGroup.CALVES, "Medial gastrocnemius", "The inner head of the superficial calf crosses the knee and ankle.",
            "Both gastrocnemius heads responded more to standing than seated calf raises in one training study.",
            study(38156065), "BODYWEIGHT_CALF_RAISE", "DUMBBELL_CALF_RAISE", "SINGLE_LEG_DUMBBELL_CALF_RAISE"),
    CALVES_LATERAL(MuscleGroup.CALVES, "Lateral gastrocnemius", "The outer head of the superficial calf also crosses the knee and ankle.",
            "Both gastrocnemius heads responded more to standing than seated calf raises in one training study.",
            study(38156065), "BODYWEIGHT_CALF_RAISE", "DUMBBELL_CALF_RAISE", "SINGLE_LEG_DUMBBELL_CALF_RAISE"),
    CALVES_SOLEUS(MuscleGroup.CALVES, "Soleus", "The soleus lies beneath the gastrocnemius and crosses the ankle only. The map reveals it on one side.",
            "The soleus works in standing and bent-knee raises. One training study found similar soleus growth with standing and seated raises.",
            study(38156065), "DUMBBELL_CALF_RAISE", "SEATED_DUMBBELL_CALF_RAISE", "BENT_KNEE_BODYWEIGHT_CALF_RAISE"),

    CORE_RECTUS(MuscleGroup.CORE, "Rectus abdominis", "The paired muscle at the front of the abdomen helps curl the trunk.",
            "Crunch variations involve trunk flexion and load the rectus abdominis.",
            study(12937449), "DUMBBELL_WEIGHTED_CRUNCH", "CABLE_CRUNCH", "REVERSE_CRUNCH"),
    CORE_OBLIQUES(MuscleGroup.CORE, "Abdominal obliques", "The internal and external obliques run along the sides of the abdomen in different layers.",
            "Side-bending and rotation involve the obliques alongside other trunk muscles.",
            study(12937449), "SIDE_PLANK_HIP_LIFT", "CABLE_WOOD_CHOP"),
    CORE_TRANSVERSE(MuscleGroup.CORE, "Transversus abdominis", "The deep abdominal layer wraps around the trunk. The map reveals it on one side.",
            "Bird dog and dead bug use several muscles to steady the trunk. Research has measured this layer during both exercises.",
            study(38651428), "BIRD_DOG", "DEAD_BUG");

    private final MuscleGroup group;
    private final String label;
    private final String anatomy;
    private final String evidence;
    private final String sourceUrl;
    private final Set<String> examples;

    LibraryMuscle() {
        group = MuscleGroup.valueOf(name());
        label = group.getLabel();
        anatomy = "";
        evidence = "";
        sourceUrl = "";
        examples = Set.of();
    }

    LibraryMuscle(MuscleGroup group, String label, String anatomy, String evidence, String sourceUrl, String... examples) {
        this.group = group;
        this.label = label;
        this.anatomy = anatomy;
        this.evidence = evidence;
        this.sourceUrl = sourceUrl;
        this.examples = Set.of(examples);
    }

    private static String study(int pmid) { return "https://pubmed.ncbi.nlm.nih.gov/" + pmid + "/"; }
    private static String anatomy(String id) { return "https://www.ncbi.nlm.nih.gov/books/" + id + "/"; }

    public MuscleGroup getGroup() { return group; }
    public String getLabel() { return label; }
    public String getAnatomy() { return anatomy; }
    public String getEvidence() { return evidence; }
    public String getSourceUrl() { return sourceUrl; }
    public boolean isRegion() { return !sourceUrl.isEmpty(); }

    public boolean isPrimary(ExerciseSnapshot exercise) { return exercise.primaryMuscle() == group; }

    public String getRegionTag() {
        if (group == MuscleGroup.CHEST) return "Regional emphasis";
        return examples.isEmpty() ? "Shared group exercise" : "Exercise example";
    }

    public boolean matches(ExerciseSnapshot exercise) {
        if (isRegion()) return examples.isEmpty() ? isPrimary(exercise) : examples.contains(exercise.code());
        return isPrimary(exercise) || exercise.secondaryMuscles().contains(group);
    }
}
