package de.unijena.bioinf.ms.biotransformer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KNOWN DEFECTS in the human-gut transformer, pinned deliberately. Two separate problems:
 * <p>
 * <b>1. Order dependence.</b> Called as the first transformation in a fresh JVM, HUMAN_GUT fails
 * with {@code AbstractMethodError: TetrahedralChirality.updateCarriers} even for achiral substrates.
 * After any CYP450 transformation has run in the same JVM it works. This is <em>not</em> asserted
 * here: Gradle reuses one JVM across test classes, so whether this class sees a cold JVM depends on
 * execution order. Reproduce it with a single-method run, e.g.
 * {@code ./gradlew :biotransformations:test --tests '*BioTransformerGutDefectTest*' --rerun-tasks}
 * and remove the warm-up below.
 * <p>
 * <b>2. Wrong results once it does run.</b> Even warmed up, the gut transformer disagrees with the
 * BioTransformer 3.0 reference: it reports products the reference does not. The disagreement is
 * stable and reproducible, so it is pinned here exactly.
 * <p>
 * Both are expected to be resolved by replacing the relocated CDK 2.7.1 with the project's CDK 2.12.
 * When that lands, delete this class and move the hgut cells into the normal baseline assertions.
 */
class BioTransformerGutDefectTest {

    /** Reach the deterministic "warm" state; see defect 1 above. */
    @BeforeEach
    void warmUp() throws Exception {
        GoldenBaseline.run("thymol", "cyp450_s1_cm1");
    }

    @Test
    void gutTransformerReportsMoreProductsThanTheReference() throws Exception {
        Set<String> reference = GoldenBaseline.expectedKeys("aspirin", "hgut_s1");
        assertEquals(Set.of("VHJPVKQXMOFZPM-UHFFFAOYSA-N"), reference, "baseline changed unexpectedly");

        Set<String> actual = GoldenBaseline.run("aspirin", "hgut_s1");

        assertEquals(Set.of(
                        "QTBSBXVTEAMEQO-UHFFFAOYSA-N",
                        "VHJPVKQXMOFZPM-UHFFFAOYSA-N",
                        "YGSDEFSMJLZEOE-UHFFFAOYSA-N"),
                actual,
                "The known gut-transformer divergence changed shape. If SIRIUS now matches the "
                        + "reference, delete this test and assert the baseline instead.");
        assertNotEquals(reference, actual, "defect resolved - move this cell into the baseline tests");
    }

    @Test
    void gutTransformerProducesAProductWhereTheReferenceProducesNone() throws Exception {
        assertTrue(GoldenBaseline.expectedKeys("thymol", "hgut_s1").isEmpty(),
                "baseline changed unexpectedly");

        Set<String> actual = GoldenBaseline.run("thymol", "hgut_s1");

        assertEquals(Set.of("ADQJSAVCKZSGMK-UHFFFAOYSA-N"), actual,
                "The known gut-transformer divergence changed shape. If SIRIUS now produces nothing "
                        + "here, delete this test and assert the baseline instead.");
    }
}
