package de.unijena.bioinf.ms.biotransformer;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The full golden matrix: every substrate against every recorded configuration.
 * <p>
 * Excluded from the default {@code test} task by the {@code *AcceptanceTest} suffix; run it
 * manually as the final gate, in particular before and after the CDK relocation work:
 * <pre>./gradlew :biotransformations:acceptanceTest -q</pre>
 * Expect tens of minutes.
 * <p>
 * It is expected to FAIL today. The stereo and human-gut defects pinned by
 * {@link BioTransformerStereoDefectTest} and {@link BioTransformerGutDefectTest} affect a large part
 * of the matrix. The report printed below is the point of this test: it lists exactly which cells
 * disagree and how, so progress can be measured cell by cell.
 */
class BioTransformerGoldenAcceptanceTest {

    private record Diff(String substrate, String config, int expected, int actual, String note) {
    }

    @Test
    void wholeMatrixMatchesReferenceImplementation() {
        JsonNode root = GoldenBaseline.root();
        List<String> substrates = new ArrayList<>();
        root.path("substrates").fieldNames().forEachRemaining(substrates::add);
        List<String> configs = new ArrayList<>();
        root.path("configs").fieldNames().forEachRemaining(configs::add);

        List<Diff> diffs = new ArrayList<>();
        Map<String, int[]> perConfig = new LinkedHashMap<>();   // [agree, differ]
        int agree = 0, total = 0;
        long t0 = System.currentTimeMillis();

        for (String substrate : substrates) {
            for (String config : configs) {
                total++;
                int[] tally = perConfig.computeIfAbsent(config, k -> new int[2]);
                Set<String> expected = GoldenBaseline.expectedKeys(substrate, config);
                Set<String> actual;
                String note = "";
                try {
                    actual = GoldenBaseline.run(substrate, config);
                } catch (Throwable t) {
                    actual = Set.of();
                    note = t.getClass().getSimpleName() + ": " + t.getMessage();
                }
                if (note.isEmpty() && expected.equals(actual)) {
                    agree++;
                    tally[0]++;
                } else {
                    tally[1]++;
                    diffs.add(new Diff(substrate, config, expected.size(), actual.size(), note));
                }
            }
        }

        final int cells = total;
        System.out.printf("%nGolden matrix: %d/%d cells agree with BioTransformer 3.0 (%.0fs)%n",
                agree, cells, (System.currentTimeMillis() - t0) / 1000.0);
        perConfig.forEach((config, t) ->
                System.out.printf("  %-24s agree=%-4d differ=%d%n", config, t[0], t[1]));
        if (!diffs.isEmpty()) {
            System.out.println("  differing cells:");
            diffs.forEach(d -> System.out.printf("    %-22s %-24s reference=%-4d sirius=%-4d %s%n",
                    d.substrate(), d.config(), d.expected(), d.actual(), d.note()));
        }

        assertTrue(diffs.isEmpty(),
                () -> diffs.size() + " of " + cells + " cells differ from the BioTransformer 3.0 "
                        + "reference; see the report above.");
    }
}
