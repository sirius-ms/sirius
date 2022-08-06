import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class MsTestLocal {

    private static final String absPath = System.getProperty("user.dir").split("sirius_dist")[0];
    private static final String sep = System.getProperty("file.separator");

    @Test
    @DisplayName("Testing if SIRIUS calculates expected formula candidates with ms file.")
    public void testTopCandidates() throws IOException {
        TestMethods.isDirExisting(absPath + "sirius_cli/src/test/temp_results/ms_temp_output/0_Bicuculline_Bicuculline/canopus".replace("/", sep));
        TestMethods.isDirExisting(absPath + "sirius_cli/src/test/temp_results/ms_temp_output/0_Bicuculline_Bicuculline/canopus_npc".replace("/", sep));
        TestMethods.isDirExisting(absPath + "sirius_cli/src/test/temp_results/ms_temp_output/0_Bicuculline_Bicuculline/fingerid".replace("/", sep));
        TestMethods.isDirExisting(absPath + "sirius_cli/src/test/temp_results/ms_temp_output/0_Bicuculline_Bicuculline/fingerprints".replace("/", sep));

        TestMethods.isDirNotEmpty(Paths.get(absPath + "sirius_cli/src/test/temp_results/ms_temp_output/0_Bicuculline_Bicuculline/canopus".replace("/", sep)));
        TestMethods.isDirNotEmpty(Paths.get(absPath + "sirius_cli/src/test/temp_results/ms_temp_output/0_Bicuculline_Bicuculline/canopus_npc".replace("/", sep)));
        TestMethods.isDirNotEmpty(Paths.get(absPath + "sirius_cli/src/test/temp_results/ms_temp_output/0_Bicuculline_Bicuculline/fingerid".replace("/", sep)));
        TestMethods.isDirNotEmpty(Paths.get(absPath + "sirius_cli/src/test/temp_results/ms_temp_output/0_Bicuculline_Bicuculline/fingerprints".replace("/", sep)));

        TestMethods.areContentsEqual(absPath + "sirius_cli/src/test/temp_results/ms_temp_output/0_Bicuculline_Bicuculline/canopus".replace("/", sep),
                absPath + "sirius_cli/src/test/temp_results/ms_temp_output/0_Bicuculline_Bicuculline/canopus_npc".replace("/", sep),
                absPath + "sirius_cli/src/test/temp_results/ms_temp_output/0_Bicuculline_Bicuculline/fingerprints".replace("/", sep));


        int rank_count = 3;
        int table_feature = 2;
        String[] pre_formula = TestMethods.readCandidates(absPath + "sirius_cli/src/test/test_results/ms_candidates/ignore_formula/formula_candidates.tsv".replace("/", sep), rank_count, table_feature);
        String[] post_formula = TestMethods.readCandidates(absPath + "sirius_cli/src/test/temp_results/ms_temp_summary/0_Bicuculline_Bicuculline/formula_candidates.tsv".replace("/", sep), rank_count, table_feature);

        rank_count = 3;
        table_feature = 6;
        String[] pre_structure = TestMethods.readCandidates(absPath + "sirius_cli/src/test/test_results/ms_candidates/compound_class_annotation/structure_candidates.tsv".replace("/", sep), rank_count, table_feature);
        String[] post_structure = TestMethods.readCandidates(absPath + "sirius_cli/src/test/temp_results/ms_temp_summary/0_Bicuculline_Bicuculline/structure_candidates.tsv".replace("/", sep), rank_count, table_feature);

        assertArrayEquals(pre_formula, post_formula);
        assertArrayEquals(pre_structure, post_structure);
        System.out.println("MS passed");
    }
}
