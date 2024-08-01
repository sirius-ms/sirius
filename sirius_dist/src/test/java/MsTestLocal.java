import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class MsTestLocal {

    private static final String absPath = System.getProperty("user.dir").split("sirius_dist")[0];
    private static final String sep = System.getProperty("file.separator");
    private static final String temp_root = absPath + "sirius_cli/build/test/temp_results/";
    private static final String temp_output = temp_root + "ms_temp_output/1_Bicuculline_Bicuculline/".replace("/", sep);
    private static final String temp_summary = temp_root + "ms_temp_summary/1_Bicuculline_Bicuculline/".replace("/", sep);

    @BeforeAll
    public static void createTempDir() throws IOException {
        Files.createDirectories(Path.of(temp_root));
    }

//    @Test
    @DisplayName("Testing if SIRIUS calculates expected formula candidates with ms file.")
    public void testTopCandidates() throws IOException {
        TestMethods.isDirExisting(temp_output + "canopus");
        TestMethods.isDirExisting(temp_output + "canopus_npc");
        TestMethods.isDirExisting(temp_output + "fingerid");
        TestMethods.isDirExisting(temp_output + "fingerprints");

        TestMethods.isDirNotEmpty(Paths.get(temp_output + "canopus"));
        TestMethods.isDirNotEmpty(Paths.get(temp_output + "canopus_npc"));
        TestMethods.isDirNotEmpty(Paths.get(temp_output + "fingerid"));
        TestMethods.isDirNotEmpty(Paths.get(temp_output + "fingerprints"));

        TestMethods.areContentsEqual(temp_output + "canopus", temp_output + "canopus_npc", temp_output + "fingerprints");


        int rank_count = 3;
        int table_feature = 1;
        String[] pre_formula = TestMethods.readCandidates(absPath + "sirius_cli/src/test/test_results/ms_candidates/ignore_formula/formula_candidates.tsv".replace("/", sep), rank_count, table_feature);
        String[] post_formula = TestMethods.readCandidates(temp_summary + "formula_candidates.tsv", rank_count, table_feature);
        System.out.println(String.join(",", pre_formula));
        System.out.println(String.join(",", post_formula));

        rank_count = 3;
        table_feature = 6;
        String[] pre_structure = TestMethods.readCandidates(absPath + "sirius_cli/src/test/test_results/ms_candidates/compound_class_annotation/structure_candidates.tsv".replace("/", sep), rank_count, table_feature);
        String[] post_structure = TestMethods.readCandidates(temp_summary + "structure_candidates.tsv", rank_count, table_feature);
        System.out.println(String.join(",", pre_structure));
        System.out.println(String.join(",", post_structure));

        assertArrayEquals(pre_formula, post_formula);
        assertArrayEquals(pre_structure, post_structure);
        System.out.println("MS passed");
    }
}
