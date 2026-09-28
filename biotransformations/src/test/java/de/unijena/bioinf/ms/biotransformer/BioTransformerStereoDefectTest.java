package de.unijena.bioinf.ms.biotransformer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KNOWN DEFECT, pinned deliberately: any substrate carrying tetrahedral stereochemistry fails.
 * <p>
 * The BioTransformer jar ships a relocated CDK 2.7.1 ({@code io.siriusms.shadow.bt.cdk}) whose
 * implementation classes are bound to the project's CDK 2.12 interfaces. {@code IStereoElement}
 * gained {@code updateCarriers} after 2.7.1, so the relocated {@code TetrahedralChirality} does not
 * implement it and the JVM raises an {@link AbstractMethodError} as soon as a stereo element is
 * copied. The reference implementation transforms all of these substrates without trouble.
 * <p>
 * In production this is a silent, partial failure: {@code BioTransformerJJob} catches it per
 * substrate, logs "Skipping the result. Some transformation products might be missing." and drops
 * the result, so a custom database import yields no transformation products for chiral structures.
 * <p>
 * This test exists so the defect cannot change shape unnoticed. When the CDK relocation is fixed,
 * delete this class and move these cells into {@link BioTransformerGoldenSmokeTest} /
 * the acceptance matrix — the baseline already records the products that should appear.
 */
class BioTransformerStereoDefectTest {

    @ParameterizedTest(name = "{0} / {1}")
    @CsvSource({
            "ibuprofen_S, cyp450_s1_cm1",
            "ibuprofen_S, ecbased_s1",
            "naproxen_S, cyp450_s1_cm1",
            "epicatechin, ecbased_s1",
            "epicatechin, phaseII_s1_pm1",
            "nicotine_S, cyp450_s1_cm1",
            "nicotine_S, ecbased_s1",
            "tryptophan_L, cyp450_s1_cm1",
            "tryptophan_L, ecbased_s1",
            "testosterone, cyp450_s1_cm1",
            "estradiol, cyp450_s1_cm1",
            "morphine, cyp450_s1_cm1",
            "phenyl_glucuronide, ecbased_s1",
    })
    void stereoSubstratesStillFail(String substrate, String config) {
        assertFalse(GoldenBaseline.expectedKeys(substrate, config).isEmpty(),
                "precondition: the reference produced products for " + substrate + " / " + config);

        AbstractMethodError error = assertThrows(AbstractMethodError.class,
                () -> GoldenBaseline.run(substrate, config),
                "Stereo handling appears to be fixed for " + substrate + " / " + config
                        + " - if so, remove this test and assert the baseline instead.");

        assertTrue(error.getMessage().contains("updateCarriers"),
                () -> "Expected the known IStereoElement.updateCarriers gap, got: " + error.getMessage());
    }
}
