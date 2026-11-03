package de.unijena.bioinf.ms.biotransformer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fast per-commit gate: SIRIUS must reproduce the upstream BioTransformer reference on a small,
 * representative subset. Runs in a few seconds.
 * <p>
 * The cells avoid the human-gut transformer, which still disagrees with the reference - see
 * {@link BioTransformerReferenceDivergenceTest}. The full matrix lives in
 * {@code BioTransformerGoldenAcceptanceTest} and is run manually.
 */
class BioTransformerGoldenSmokeTest {

    @ParameterizedTest(name = "{0} / {1}")
    @CsvSource({
            "thymol, cyp450_s1_cm1",
            "thymol, ecbased_s1",
            "thymol, phaseII_s1_pm1",
            "acetaminophen, cyp450_s1_cm1",
            "acetaminophen, ecbased_s1",
            "acetaminophen, phaseII_s1_pm1",
            "choline_cation, cyp450_s1_cm1",
            "choline_cation, ecbased_s1",
            "choline_cation, phaseII_s1_pm1",
            "aspirin, cyp450_s1_cm1",
            "aspirin, ecbased_s1",
            "aspirin, phaseII_s1_pm1",
            "benzoic_acid, cyp450_s1_cm1",
            "benzoic_acid, ecbased_s1",
            "benzoic_acid, phaseII_s1_pm1",
            "caffeine, cyp450_s1_cm1",
            // Stereochemistry: these failed outright while BioTransformer carried its own CDK, so
            // they are the regression guard for the jar sharing ours.
            "ibuprofen_S, cyp450_s1_cm1",
            "naproxen_S, cyp450_s1_cm1",
            "nicotine_S, cyp450_s1_cm1",
            "epicatechin, ecbased_s1",
            "tryptophan_L, ecbased_s1",
            "phenyl_glucuronide, ecbased_s1",
            // substrates the reference leaves untransformed: these pin down "correctly produces nothing"
            "caffeine, ecbased_s1",
            "warfarin, ecbased_s1",
    })
    void matchesReferenceImplementation(String substrate, String config) throws Exception {
        Set<String> expected = GoldenBaseline.expectedKeys(substrate, config);
        Set<String> actual = GoldenBaseline.run(substrate, config);
        assertEquals(expected, actual,
                () -> "Transformation products differ from the BioTransformer 3.0 reference for "
                        + substrate + " / " + config);
    }
}
