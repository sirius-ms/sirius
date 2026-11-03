package de.unijena.bioinf.ms.biotransformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;

/**
 * Access to the golden baseline recorded from the upstream BioTransformer 3.0 CLI.
 * See {@code src/test/resources/biotransformer/README.md} for provenance and how to re-record it.
 */
final class GoldenBaseline {
    static final String RESOURCE = "/biotransformer/golden-baseline.json";

    private static JsonNode root;

    private GoldenBaseline() {
    }

    static synchronized JsonNode root() {
        if (root == null) {
            try (InputStream in = GoldenBaseline.class.getResourceAsStream(RESOURCE)) {
                if (in == null)
                    throw new IllegalStateException("Golden baseline resource missing: " + RESOURCE);
                root = new ObjectMapper().readTree(in);
            } catch (Exception e) {
                throw new IllegalStateException("Could not read golden baseline " + RESOURCE, e);
            }
        }
        return root;
    }

    static String smiles(String substrate) {
        JsonNode n = root().path("substrates").path(substrate).path("smiles");
        if (n.isMissingNode())
            throw new IllegalArgumentException("Unknown substrate in baseline: " + substrate);
        return n.asText();
    }

    /** Product InChIKeys the reference implementation produced. Empty means "ran, produced nothing". */
    static Set<String> expectedKeys(String substrate, String config) {
        JsonNode cell = root().path("results").path(substrate).path(config);
        if (cell.isMissingNode())
            throw new IllegalArgumentException("No baseline cell for " + substrate + "/" + config);
        if (cell.has("error") || cell.path("exitCode").asInt() != 0)
            throw new IllegalStateException("Baseline cell was not recorded successfully: "
                    + substrate + "/" + config);
        Set<String> keys = new TreeSet<>();
        cell.path("products").forEach(p -> keys.add(p.path("inchikey").asText()));
        return keys;
    }

    /**
     * The SIRIUS settings matching a baseline config id. The reference CLI's {@code -cm}/{@code -pm}
     * values are 1-based and line up with {@link Cyp450Mode#getCyp450ModeOrdinal()} / {@link P2Mode#P2ModeOrdinal()}.
     */
    static BioTransformerSettings settings(String config) {
        BioTransformerSettings s = new BioTransformerSettings()
                .setUseDB(false)      // the baseline was recorded with -useDB false; keep it offline
                .setUseSub(false)
                .setCyp450Mode(Cyp450Mode.RULE_BASED)
                .setP2Mode(P2Mode.BT_RULE_BASED)
                .setIterations(1);
        return switch (config) {
            case "cyp450_s1_cm1" -> s.setMetabolicTransformation(MetabolicTransformation.PHASE_1_CYP450);
            case "cyp450_s1_cm3" -> s.setCyp450Mode(Cyp450Mode.COMBINED)
                    .setMetabolicTransformation(MetabolicTransformation.PHASE_1_CYP450);
            case "cyp450_s2_cm3" -> s.setIterations(2).setCyp450Mode(Cyp450Mode.COMBINED)
                    .setMetabolicTransformation(MetabolicTransformation.PHASE_1_CYP450);
            case "ecbased_s1" -> s.setMetabolicTransformation(MetabolicTransformation.EC_BASED);
            case "ecbased_s2" -> s.setIterations(2).setMetabolicTransformation(MetabolicTransformation.EC_BASED);
            case "phaseII_s1_pm1" -> s.setMetabolicTransformation(MetabolicTransformation.PHASE_2);
            case "phaseII_s1_pm3" -> s.setP2Mode(P2Mode.COMBINED_RULES)
                    .setMetabolicTransformation(MetabolicTransformation.PHASE_2);
            case "hgut_s1" -> s.setMetabolicTransformation(MetabolicTransformation.HUMAN_GUT);
            case "hgut_s2" -> s.setIterations(2).setMetabolicTransformation(MetabolicTransformation.HUMAN_GUT);
            case "allHuman_s2_cm3" -> s.setIterations(2).setCyp450Mode(Cyp450Mode.COMBINED)
                    .setMetabolicTransformation(MetabolicTransformation.ALL_HUMAN);
            case "seq_cyp450_2_phaseII_1" -> {
                s.setCyp450Mode(Cyp450Mode.COMBINED)
                        .setMetabolicTransformation(MetabolicTransformation.HUMAN_CUSTOM_MULTI);
                s.addSequenceStep(MetabolicTransformation.PHASE_1_CYP450, 2);
                s.addSequenceStep(MetabolicTransformation.PHASE_2, 1);
                yield s;
            }
            default -> throw new IllegalArgumentException("Unmapped baseline config: " + config);
        };
    }

    /** Runs SIRIUS' BioTransformer integration and returns the product InChIKeys. */
    static Set<String> run(String substrate, String config) throws Exception {
        IAtomContainer mol = InChISMILESUtils.getAtomContainerFromSmiles(smiles(substrate));
        BioTransformerResult result = BioTransformerWrapper.transform(mol, settings(config));
        Set<String> keys = new TreeSet<>();
        if (result != null)
            for (BioTransformation bt : result.biotranformations())
                for (IAtomContainer product : bt.getProducts())
                    keys.add(inchiKey(product));
        return keys;
    }

    /** Mirrors BioTransformerWrapper's own key lookup: reuse the property, fall back to computing it. */
    private static String inchiKey(IAtomContainer container) throws Exception {
        String key = container.getProperty("InChIKey", String.class);
        if (key == null || key.isBlank())
            key = container.getProperty("INCHIKEY", String.class);
        if (key == null || key.isBlank()) {
            InChI inchi = InChISMILESUtils.getInchi(container, true);
            if (inchi != null)
                key = inchi.key;
        }
        return key;
    }
}
