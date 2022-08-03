import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class MgfTestLocal {

    private static final String absPath = System.getProperty("user.dir").split("sirius_dist")[0];
    private static final String sep = System.getProperty("file.separator");

    @Test
    @DisplayName("Testing if SIRIUS calculates expected formula candidates with mgf file.")
    public void testTopCandidates() throws IOException {
        TestMethods.isDirExisting(absPath + "sirius_cli/src/test/temp_results/mgf_temp_output/0_laudanosine_FEATURE_1/canopus");
        TestMethods.isDirExisting(absPath + "sirius_cli/src/test/temp_results/mgf_temp_output/0_laudanosine_FEATURE_1/canopus_npc");
        TestMethods.isDirExisting(absPath + "sirius_cli/src/test/temp_results/mgf_temp_output/0_laudanosine_FEATURE_1/fingerid");
        TestMethods.isDirExisting(absPath + "sirius_cli/src/test/temp_results/mgf_temp_output/0_laudanosine_FEATURE_1/fingerprints");

        TestMethods.isDirNotEmpty(Paths.get(absPath + "sirius_cli/src/test/temp_results/mgf_temp_output/0_laudanosine_FEATURE_1/canopus"));
        TestMethods.isDirNotEmpty(Paths.get(absPath + "sirius_cli/src/test/temp_results/mgf_temp_output/0_laudanosine_FEATURE_1/canopus_npc"));
        TestMethods.isDirNotEmpty(Paths.get(absPath + "sirius_cli/src/test/temp_results/mgf_temp_output/0_laudanosine_FEATURE_1/fingerid"));
        TestMethods.isDirNotEmpty(Paths.get(absPath + "sirius_cli/src/test/temp_results/mgf_temp_output/0_laudanosine_FEATURE_1/fingerprints"));

        int rank_count = 3;
        int table_feature = 1;
        String[] pre_formula = TestMethods.readCandidates(absPath + "sirius_cli/src/test/test_results/mgf_candidates/formula_candidates.tsv".replace("/", sep), rank_count, table_feature);
        String[] post_formula = TestMethods.readCandidates(absPath + "sirius_cli/src/test/temp_results/mgf_temp_summary/0_laudanosine_FEATURE_1/formula_candidates.tsv".replace("/", sep), rank_count, table_feature);

        rank_count = 3;
        table_feature = 6;
        String[] pre_structure = TestMethods.readCandidates(absPath + "sirius_cli/src/test/test_results/mgf_candidates/structure_candidates.tsv".replace("/", sep), rank_count, table_feature);
        String[] post_structure = TestMethods.readCandidates(absPath + "sirius_cli/src/test/temp_results/mgf_temp_summary/0_laudanosine_FEATURE_1/structure_candidates.tsv".replace("/", sep), rank_count, table_feature);

        assertArrayEquals(pre_formula, post_formula);
        assertArrayEquals(pre_structure, post_structure);
        System.out.println("MGF passed");
    }
}


