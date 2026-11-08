package bg.fitnessplanner.progression.engine;

import bg.fitnessplanner.catalog.domain.*;
import bg.fitnessplanner.progression.engine.ProgressionModel.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ProgressionEngineTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 6);
    ProgressionConfig config;
    ProgressionEngine engine;
    Target target = new Target(1L, 3, 8, 12, 120, LoadType.TOTAL_EXTERNAL, new BigDecimal("2.5"));
    @BeforeEach void setup() throws Exception {
        try (var in = getClass().getResourceAsStream("/progression-v1.json")) {
            config = new ObjectMapper().readValue(in, ProgressionConfig.class);
        }
        engine = new ProgressionEngine(config);
    }
    @Test void twoQualifyingExposuresSuggestExactDecimalIncrementWithEvidence() {
        var result = engine.recommend(target, List.of(exposure(1, 2, "60", 2, 12,12,12), exposure(2, 0,"60", 2,12,12,12)), TODAY);
        assertThat(result.action()).isEqualTo(Action.INCREASE);
        assertThat(result.suggestedLoad()).isEqualByComparingTo("62.5");
        assertThat(result.evidenceIds()).containsExactly(2L, 1L);
    }
    @Test void partialRepAccumulationMaintainsLoad() {
        assertThat(decide(exposure(1,0,"60",2,12,10,9)).suggestedLoad()).isEqualByComparingTo("60");
        assertThat(decide(exposure(1,0,"60",2,12,10,9)).action()).isEqualTo(Action.MAINTAIN);
    }
    @Test void missingRirMaintainsAndExplainsUnknownEffort() {
        var result = engine.recommend(target, List.of(exposure(2,0,"60",null,12,12,12), exposure(1,2,"60",2,12,12,12)),TODAY);
        assertThat(result.reason()).isEqualTo("EFFORT_UNKNOWN");
        assertThat(result.action()).isEqualTo(Action.MAINTAIN);
    }
    @Test void highEffortPreventsIncrease() { assertThat(decide(exposure(1,0,"60",1,12,12,12)).reason()).isEqualTo("EFFORT_HIGH"); }
    @Test void oneQualifyingExposureRequiresConfirmation() { assertThat(decide(exposure(1,0,"60",2,12,12,12)).action()).isEqualTo(Action.MAINTAIN); }
    @Test void threeMeaningfulDeclinesRequireReview() {
        var result=engine.recommend(target,List.of(exposure(1,4,"60",2,12,12,12),exposure(2,2,"60",2,11,11,10),exposure(3,0,"60",2,10,9,9)),TODAY);
        assertThat(result.reason()).isEqualTo("DECLINING_REPS"); assertThat(result.suggestedLoad()).isNull();
    }
    @Test void smallOrNonMonotonicDeclinesDoNotTriggerReview() {
        assertThat(engine.recommend(target,List.of(exposure(1,4,"60",2,12,12,12),exposure(2,2,"60",2,12,12,11),exposure(3,0,"60",2,11,11,10)),TODAY).action()).isEqualTo(Action.MAINTAIN);
    }
    @ParameterizedTest @ValueSource(ints={0,2,4}) void incompleteAndExtraSetsAreInsufficient(int sets) {
        assertThat(decide(exposure(1,0,"60",2,Collections.nCopies(sets,12).stream().mapToInt(Integer::intValue).toArray())).action()).isEqualTo(Action.INSUFFICIENT_DATA);
    }
    @Test void skippedLatestInterruptsEarlierSuccess() {
        var skipped=new Exposure(3L,1L,TODAY,Instant.EPOCH,3,8,12,120,LoadType.TOTAL_EXTERNAL,true,List.of());
        assertThat(engine.recommend(target,List.of(skipped,exposure(2,2,"60",2,12,12,12),exposure(1,4,"60",2,12,12,12)),TODAY).reason()).isEqualTo("INCOMPARABLE_LATEST");
    }
    @Test void earlierSkipCannotBeSkippedOverForIncrease() {
        var skipped=new Exposure(2L,1L,TODAY.minusDays(2),Instant.EPOCH,3,8,12,120,LoadType.TOTAL_EXTERNAL,true,List.of());
        assertThat(engine.recommend(target,List.of(exposure(3,0,"60",2,12,12,12),skipped,exposure(1,4,"60",2,12,12,12)),TODAY).action()).isEqualTo(Action.MAINTAIN);
    }
    @Test void differentLoadResetsChain() { assertThat(engine.recommend(target,List.of(exposure(2,0,"60",2,12,12,12),exposure(1,2,"57.5",2,12,12,12)),TODAY).action()).isEqualTo(Action.MAINTAIN); }
    @Test void mixedWeightsAreInsufficient() {
        var e=exposure(1,0,"60",2,12,12,12);
        var sets=new ArrayList<>(e.sets()); sets.set(1,new WorkSet(2,12,new BigDecimal("57.5"),2));
        assertThat(decide(copy(e,sets,3,120)).action()).isEqualTo(Action.INSUFFICIENT_DATA);
    }
    @Test void duplicateSetNumbersAreInsufficient() {
        var e=exposure(1,0,"60",2,12,12,12);
        assertThat(decide(copy(e,List.of(e.sets().getFirst(),e.sets().getFirst(),e.sets().getLast()),3,120)).action()).isEqualTo(Action.INSUFFICIENT_DATA);
    }
    @Test void changedPrescriptionOrRestRequiresFreshEvidence() {
        var e=exposure(1,0,"60",2,12,12,12);
        assertThat(decide(copy(e,e.sets(),2,120)).action()).isEqualTo(Action.INSUFFICIENT_DATA);
        assertThat(decide(copy(e,e.sets(),3,90)).action()).isEqualTo(Action.INSUFFICIENT_DATA);
    }
    @Test void windowIncludesDay21ButNotDay22OrFuture() {
        assertThat(decide(exposure(1,21,"60",2,12,12,12)).action()).isEqualTo(Action.MAINTAIN);
        assertThat(decide(exposure(1,22,"60",2,12,12,12)).reason()).isEqualTo("OUTSIDE_WINDOW");
        assertThat(decide(exposure(1,-1,"60",2,12,12,12)).reason()).isEqualTo("OUTSIDE_WINDOW");
    }
    @Test void exactTenPercentAllowedButLargerStepRequiresReview() {
        var history=List.of(exposure(2,0,"10",2,12,12,12),exposure(1,2,"10",2,12,12,12));
        assertThat(engine.recommend(new Target(1L,3,8,12,120,LoadType.TOTAL_EXTERNAL,BigDecimal.ONE),history,TODAY).action()).isEqualTo(Action.INCREASE);
        assertThat(engine.recommend(new Target(1L,3,8,12,120,LoadType.TOTAL_EXTERNAL,new BigDecimal("2")),history,TODAY).reason()).isEqualTo("INCREMENT_TOO_LARGE");
    }
    @Test void unsupportedConventionsAndInvalidStepsAreInsufficient() {
        for (LoadType type:List.of(LoadType.BODYWEIGHT,LoadType.ASSISTED)) assertThat(engine.recommend(new Target(1L,3,8,12,120,type,BigDecimal.ONE),List.of(),TODAY).reason()).isEqualTo("UNSUPPORTED_LOAD");
        for (BigDecimal step:Arrays.asList(null,BigDecimal.ZERO,new BigDecimal("-1"),new BigDecimal("0.001"))) assertThat(engine.recommend(new Target(1L,3,8,12,120,LoadType.TOTAL_EXTERNAL,step),List.of(),TODAY).reason()).isEqualTo("MISSING_INCREMENT");
    }
    @Test void zeroLoadAndNoHistoryAreInsufficient() {
        assertThat(decide(exposure(1,0,"0",2,12,12,12)).action()).isEqualTo(Action.INSUFFICIENT_DATA);
        assertThat(engine.recommend(target,List.of(),TODAY).reason()).isEqualTo("NO_HISTORY");
    }
    private Decision decide(Exposure e) { return engine.recommend(target,List.of(e),TODAY); }
    private Exposure exposure(long id,int age,String weight,Integer rir,int... reps) {
        List<WorkSet> sets=new ArrayList<>(); for(int i=0;i<reps.length;i++) sets.add(new WorkSet(i+1,reps[i],new BigDecimal(weight),rir));
        return new Exposure(id,1L,TODAY.minusDays(age),Instant.EPOCH.plusSeconds(id),3,8,12,120,LoadType.TOTAL_EXTERNAL,false,sets);
    }
    private Exposure copy(Exposure e,List<WorkSet> sets,int count,int rest) { return new Exposure(e.id(),e.exerciseId(),e.date(),e.recordedAt(),count,e.repMin(),e.repMax(),rest,e.loadType(),e.skipped(),sets); }
}
