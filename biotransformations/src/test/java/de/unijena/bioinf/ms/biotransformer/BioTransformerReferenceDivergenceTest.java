package de.unijena.bioinf.ms.biotransformer;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the places where SIRIUS deliberately does not match the recorded reference, so they cannot
 * drift unnoticed. These are not SIRIUS defects - in both cases SIRIUS is the more correct of the
 * two, and the baseline is kept as recorded so the difference stays visible.
 *
 * <h2>Reactions the reference never evaluates</h2>
 * Upstream BioTransformer computes a reaction score as {@code score >= scoreThreshold} without
 * checking for null, and the score is null whenever a reaction has no entry in the biosystem's
 * occurrence ratios. The resulting NullPointerException aborts the reaction loop, so every reaction
 * after the first null score is silently lost. Running the reference CLI shows it directly:
 * <pre>
 * $ java -jar BioTransformer3.0_20230525.jar -k pred -b hgut -ismi "CC(=O)OC1=CC=CC=C1C(O)=O" -s 1
 * Cannot invoke "java.lang.Double.doubleValue()" because "score" is null
 * Unique Biotransformations: 1
 * </pre>
 * Our patched copy of {@code biotransformer.btransformers.Biotransformer} guards the null and keeps
 * going, so it finds the reactions the reference skipped - for aspirin, the ester hydrolysis that
 * yields salicylic and acetic acid. This accounts for 26 of the 30 cells that still differ.
 *
 * <h2>Stereo duplicates the reference emits</h2>
 * For morphine SIRIUS reports fewer products, and its products are a strict subset of the
 * reference's. The extras are stereo-undefined duplicates of metabolites SIRIUS already reports
 * (same InChIKey skeleton block, {@code UHFFFAOYSA} stereo layer), which CDK 2.7.1 produced because
 * it perceived stereo inconsistently. CDK 2.12 does not, so the duplicates are gone.
 */
class BioTransformerReferenceDivergenceTest {

    @Test
    void findsGutReactionsTheReferenceLosesToItsNullScoreBug() throws Exception {
        assertEquals(Set.of("VHJPVKQXMOFZPM-UHFFFAOYSA-N"),
                GoldenBaseline.expectedKeys("aspirin", "hgut_s1"), "baseline changed unexpectedly");

        Set<String> actual = GoldenBaseline.run("aspirin", "hgut_s1");

        assertEquals(Set.of(
                        "VHJPVKQXMOFZPM-UHFFFAOYSA-N",  // the acyl glucuronide, the reference has this too
                        "YGSDEFSMJLZEOE-UHFFFAOYSA-N",  // salicylic acid, from the hydrolysis it never reaches
                        "QTBSBXVTEAMEQO-UHFFFAOYSA-N"), // acetic acid, the other hydrolysis product
                actual,
                "The extra products from the null-score fix changed. If SIRIUS now matches the "
                        + "reference, upstream fixed the bug and this test should go.");
    }

    @Test
    void producesAGlucuronideTheReferenceNeverReaches() throws Exception {
        assertTrue(GoldenBaseline.expectedKeys("thymol", "hgut_s1").isEmpty(),
                "baseline changed unexpectedly");
        assertEquals(Set.of("ADQJSAVCKZSGMK-UHFFFAOYSA-N"), GoldenBaseline.run("thymol", "hgut_s1"));
    }

    @Test
    void doesNotEmitTheStereoDuplicatesTheReferenceHas() throws Exception {
        Set<String> reference = GoldenBaseline.expectedKeys("morphine", "cyp450_s1_cm3");
        Set<String> actual = GoldenBaseline.run("morphine", "cyp450_s1_cm3");

        assertTrue(reference.containsAll(actual), "SIRIUS should report a subset of the reference here");
        assertEquals(Set.of("AMAPEXTUMXQULJ-UHFFFAOYSA-N"), difference(reference, actual));
        assertTrue(actual.contains("AMAPEXTUMXQULJ-APQDOHRLSA-N"),
                "the same metabolite with its stereochemistry perceived");
    }

    private static Set<String> difference(Set<String> a, Set<String> b) {
        Set<String> d = new java.util.TreeSet<>(a);
        d.removeAll(b);
        return d;
    }
}
