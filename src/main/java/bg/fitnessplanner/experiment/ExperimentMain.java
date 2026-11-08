package bg.fitnessplanner.experiment;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.MuscleGroup;
import bg.fitnessplanner.planning.application.SnapshotCodec;
import bg.fitnessplanner.planning.engine.BaselinePlanner;
import bg.fitnessplanner.planning.engine.PlanEvaluator;
import bg.fitnessplanner.planning.engine.Planner;
import bg.fitnessplanner.planning.engine.PlannerConfig;
import bg.fitnessplanner.planning.engine.PlanningModel.Result;
import bg.fitnessplanner.planning.engine.ScoredPlanner;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/** Offline entry point. Uses the shipped engines without starting the web application or database. */
public final class ExperimentMain {
    private static final List<String> VIOLATIONS = List.of("equipment", "exclusion", "catalog_skill", "prescription",
            "same_day_duplicate", "schedule", "empty_day", "duration", "recovery", "bounds", "day_type");
    private static final String QUALITY_HEADER = "suite,case_id,variant,algorithm,profile_sha256,config_sha256,"
            + "output_sha256,status,publishable,candidate,constructed_candidates,additions_examined,hard_violations,"
            + String.join(",", VIOLATIONS) + ",coverage,primary_coverage,volume_deviation,volume_shortfall,"
            + "volume_excess,time_utilization,repeated_occurrences,duplicate_ratio,total_seconds";
    private static final JsonMapper JSON = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
    private static final SnapshotCodec CODEC = new SnapshotCodec(JSON);
    private static volatile Result consumed;

    private ExperimentMain() { }

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3 || (args.length == 3 && !args[2].equals("--quality-only"))) {
            throw new IllegalArgumentException("Usage: ExperimentMain INPUT_DIR NEW_OUTPUT_DIR [--quality-only]");
        }
        run(Path.of(args[0]), Path.of(args[1]), args.length == 3);
    }

    private static void run(Path input, Path output, boolean qualityOnly) throws IOException {
        Instant started = Instant.now();
        byte[] manifestBytes = Files.readAllBytes(input.resolve("manifest.json"));
        var manifest = JSON.readValue(manifestBytes, Manifest.class);
        if (!manifest.files().keySet().equals(Set.of("catalog.json", "cases.json", "planner-config.json"))
                || manifest.warmupInvocations() < 1 || manifest.timedBatches() < 1 || manifest.invocationsPerBatch() < 1) {
            throw new IllegalArgumentException("Invalid frozen input manifest.");
        }
        Map<String, byte[]> snapshots = new HashMap<>();
        for (var entry : manifest.files().entrySet()) {
            byte[] bytes = Files.readAllBytes(input.resolve(entry.getKey()));
            if (!hash(bytes).equals(entry.getValue())) throw new IllegalArgumentException("Input digest mismatch: " + entry.getKey());
            snapshots.put(entry.getKey(), bytes);
        }
        List<ExerciseSnapshot> catalog = JSON.readValue(snapshots.get("catalog.json"), new TypeReference<>() { });
        List<Fixture> fixtures = JSON.readValue(snapshots.get("cases.json"), new TypeReference<>() { });
        var base = JSON.readValue(snapshots.get("planner-config.json"), PlannerConfig.class);
        var variants = variants(base, manifest);

        Files.createDirectory(output);
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("configurations.json").toFile(), variants);
        try (var mainQuality = Files.newBufferedWriter(output.resolve("quality.csv"));
             var mainMuscles = Files.newBufferedWriter(output.resolve("muscle-volumes.csv"));
             var sensitivity = Files.newBufferedWriter(output.resolve("sensitivity-quality.csv"));
             var sensitivityMuscles = Files.newBufferedWriter(output.resolve("sensitivity-muscle-volumes.csv"));
             var failures = Files.newBufferedWriter(output.resolve("failures.csv"));
             var evidence = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(
                     Files.newOutputStream(output.resolve("outputs.jsonl.gz"))), StandardCharsets.UTF_8))) {
            mainQuality.write(QUALITY_HEADER + "\n");
            sensitivity.write(QUALITY_HEADER + "\n");
            String muscleHeader = "suite,case_id,variant,algorithm,muscle,target_half_sets,actual_half_sets,primary_sets\n";
            mainMuscles.write(muscleHeader);
            sensitivityMuscles.write(muscleHeader);
            failures.write("suite,case_id,variant,algorithm,status,hard_violations,diagnostics\n");

            for (Variant variant : variants) {
                boolean primary = variant.id().equals("default");
                for (Fixture fixture : fixtures) {
                    export(fixture, variant, catalog, primary ? mainQuality : sensitivity,
                            primary ? mainMuscles : sensitivityMuscles, failures, evidence);
                }
            }
        }
        if (!qualityOnly) time(output, manifest, base, fixtures, catalog);
        writeMetadata(output, manifest, manifestBytes, started, fixtures.size(), catalog.size(), variants.size(), qualityOnly);
    }

    private static List<Variant> variants(PlannerConfig base, Manifest manifest) throws IOException {
        var variants = new ArrayList<Variant>();
        variants.add(new Variant("default", base));
        ObjectNode weights = JSON.valueToTree(base.weights());
        var fields = weights.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            for (BigDecimal multiplier : manifest.weightMultipliers()) {
                ObjectNode changed = JSON.valueToTree(base);
                ((ObjectNode) changed.get("weights")).put(field, weights.get(field).decimalValue().multiply(multiplier));
                variants.add(new Variant("weight-" + field + "-" + multiplier.toPlainString(),
                        JSON.treeToValue(changed, PlannerConfig.class)));
            }
        }
        for (int credit : manifest.secondaryHalfCredits()) {
            if (credit == base.secondaryHalfCredits()) continue;
            ObjectNode changed = JSON.valueToTree(base);
            changed.put("secondaryHalfCredits", credit);
            variants.add(new Variant("secondary-half-credits-" + credit, JSON.treeToValue(changed, PlannerConfig.class)));
        }
        return variants;
    }

    private static void export(Fixture fixture, Variant variant, List<ExerciseSnapshot> catalog,
                               BufferedWriter quality, BufferedWriter muscles, BufferedWriter failures,
                               BufferedWriter evidence) throws IOException {
        var config = variant.config();
        var evaluator = new PlanEvaluator(config);
        var known = new HashMap<Long, ExerciseSnapshot>();
        catalog.forEach(exercise -> known.put(exercise.id(), exercise));
        String profileHash = CODEC.profileHash(fixture.profile());
        String configHash = CODEC.hash(config);
        for (Planner planner : planners(config)) {
            Result result = planner.generate(fixture.profile(), catalog);
            if (!result.equals(planner.generate(fixture.profile(), catalog.reversed()))) {
                throw new IllegalStateException("Catalog ordering changed " + planner.version() + " output for " + fixture.id());
            }
            var checked = result.plan() == null ? null : evaluator.evaluate(fixture.profile(), catalog, result.plan());
            boolean publishable = result.publishable() && checked != null && checked.hardViolations() == 0
                    && checked.status() == result.status();
            String outputHash = hash(JSON.writeValueAsBytes(result));
            Object[] key = {fixture.suite(), fixture.id(), variant.id(), planner.version()};
            var row = new ArrayList<>(Arrays.asList(key));
            row.addAll(Arrays.asList(profileHash, configHash, outputHash, result.status(), publishable,
                    result.plan() == null ? null : result.plan().candidateKey(), result.candidatesConstructed(),
                    result.additionsExamined(), checked == null ? null : checked.hardViolations()));
            for (String violation : VIOLATIONS) {
                row.add(checked == null ? null : checked.violations().getOrDefault(violation, 0));
            }
            if (publishable) {
                var primary = new HashMap<MuscleGroup, Integer>();
                int occurrences = 0;
                for (var day : result.plan().days()) {
                    occurrences += day.exercises().size();
                    for (var item : day.exercises()) {
                        primary.merge(known.get(item.exerciseId()).primaryMuscle(), item.sets(), Integer::sum);
                    }
                }
                int targetTotal = 0, shortfall = 0, excess = 0;
                for (MuscleGroup muscle : MuscleGroup.values()) {
                    int target = checked.targetHalfSets().get(muscle);
                    int actual = checked.volumeHalfSets().get(muscle);
                    targetTotal += target;
                    shortfall += Math.max(0, target - actual);
                    excess += Math.max(0, actual - target);
                    writeRow(muscles, key, muscle, target, actual, primary.getOrDefault(muscle, 0));
                }
                int count = MuscleGroup.values().length;
                row.addAll(List.of(ratio(count - checked.missingMuscles(), count), ratio(primary.size(), count),
                        ratio(shortfall + excess, targetTotal), ratio(shortfall, targetTotal), ratio(excess, targetTotal),
                        ratio(checked.totalSeconds(), fixture.profile().trainingDays() * fixture.profile().sessionMinutes() * 60),
                        checked.repeatedOccurrences(), ratio(checked.repeatedOccurrences(), Math.max(1, occurrences)),
                        checked.totalSeconds()));
            } else {
                row.addAll(java.util.Collections.nCopies(9, null));
                var diagnostics = new ArrayList<>(result.diagnostics());
                if (result.publishable()) diagnostics.add("Rejected by independent output validation.");
                writeRow(failures, key, result.status(), checked == null ? null : checked.hardViolations(),
                        String.join(" | ", diagnostics));
            }
            writeRow(quality, row.toArray());
            evidence.write(JSON.writeValueAsString(new Evidence(fixture.suite(), fixture.id(), variant.id(),
                    planner.version(), profileHash, configHash, outputHash, result)));
            evidence.newLine();
        }
    }

    private static void time(Path output, Manifest manifest, PlannerConfig config, List<Fixture> fixtures,
                             List<ExerciseSnapshot> catalog) throws IOException {
        try (var writer = Files.newBufferedWriter(output.resolve("timing-batches.csv"))) {
            writer.write("suite,case_id,algorithm,batch,order,invocations,elapsed_ns,checksum\n");
            var planners = planners(config);
            for (Fixture fixture : fixtures) {
                for (int i = 0; i < manifest.warmupInvocations(); i++) {
                    consumed = planners.get(i % 2).generate(fixture.profile(), catalog);
                    consumed = planners.get(1 - i % 2).generate(fixture.profile(), catalog);
                }
                for (int batch = 0; batch < manifest.timedBatches(); batch++) {
                    for (int position = 0; position < planners.size(); position++) {
                        Planner planner = planners.get((batch + position) % planners.size());
                        long checksum = 0;
                        long start = System.nanoTime();
                        for (int i = 0; i < manifest.invocationsPerBatch(); i++) {
                            Result result = planner.generate(fixture.profile(), catalog);
                            consumed = result;
                            checksum += result.additionsExamined() + result.candidatesConstructed() + result.status().ordinal();
                        }
                        long elapsed = System.nanoTime() - start;
                        writeRow(writer, new Object[]{fixture.suite(), fixture.id(), planner.version()},
                                batch + 1, position + 1, manifest.invocationsPerBatch(), elapsed, checksum);
                    }
                }
                writer.flush();
                System.out.println("Timed " + fixture.id());
            }
        }
    }

    private static List<Planner> planners(PlannerConfig config) {
        return List.of(new BaselinePlanner(config), new ScoredPlanner(config));
    }

    private static void writeMetadata(Path output, Manifest manifest, byte[] manifestBytes, Instant started,
                                      int cases, int exercises, int variants, boolean qualityOnly) throws IOException {
        var metadata = new java.util.TreeMap<String, Object>();
        metadata.put("protocol", manifest.protocol());
        metadata.put("manifestSha256", hash(manifestBytes));
        metadata.put("inputs", manifest);
        metadata.put("startedUtc", started.toString());
        metadata.put("finishedUtc", Instant.now().toString());
        metadata.put("cases", cases);
        metadata.put("catalogExercises", exercises);
        metadata.put("configurations", variants);
        metadata.put("qualityOnly", qualityOnly);
        metadata.put("java", System.getProperty("java.runtime.version"));
        metadata.put("jvm", System.getProperty("java.vm.name"));
        metadata.put("cpu", System.getProperty("experiment.cpu", "unknown"));
        metadata.put("sourceRevision", System.getProperty("experiment.revision", "unknown"));
        metadata.put("artifactSha256", System.getProperty("experiment.artifactSha256", "unknown"));
        metadata.put("jvmFlags", ManagementFactory.getRuntimeMXBean().getInputArguments());
        metadata.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        metadata.put("architecture", System.getProperty("os.arch"));
        metadata.put("processors", Runtime.getRuntime().availableProcessors());
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("metadata.json").toFile(), metadata);
    }

    private static BigDecimal ratio(int numerator, int denominator) {
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 8, RoundingMode.HALF_UP);
    }

    private static void writeRow(BufferedWriter writer, Object[] prefix, Object... values) throws IOException {
        var columns = new ArrayList<>(Arrays.asList(prefix));
        columns.addAll(Arrays.asList(values));
        var cells = new ArrayList<String>();
        for (Object value : columns) {
            String cell = value == null ? "" : value.toString();
            if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
                cell = "\"" + cell.replace("\"", "\"\"") + "\"";
            }
            cells.add(cell);
        }
        writer.write(String.join(",", cells));
        writer.newLine();
    }

    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    record Manifest(String protocol, String seedSha256, Map<String, String> files, int warmupInvocations,
                    int timedBatches, int invocationsPerBatch, List<BigDecimal> weightMultipliers,
                    List<Integer> secondaryHalfCredits, Map<String, BigDecimal> progressionDefaultSteps) { }
    record Fixture(String id, String suite, String label, ProfileSnapshot profile) { }
    record Variant(String id, PlannerConfig config) { }
    record Evidence(String suite, String caseId, String variant, String algorithm, String profileSha256,
                    String configSha256, String outputSha256, Result result) { }
}
