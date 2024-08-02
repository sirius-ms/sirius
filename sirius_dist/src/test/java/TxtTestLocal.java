import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TxtTestLocal {

    private static final String absPath = System.getProperty("user.dir").split("sirius_dist")[0];
    private static final String sep = System.getProperty("file.separator");

    private static final String temp_root = absPath + "sirius_cli/build/test/temp_results/";

    @BeforeAll
    public static void createTempDir() throws IOException {
        Files.createDirectories(Path.of(temp_root));
    }

//    @Test
    @DisplayName("Testing if SIRIUS calculates expected formula candidates with txt file.")
    public void testTopCandidates(){
        int rank_count = 3;
        int table_feature = 1;
        String[] pre_formula = TestMethods.readCandidates(absPath + "sirius_cli/src/test/test_results/txt_candidates/formula_candidates.tsv".replace("/", sep), rank_count, table_feature);
        String[] post_formula = TestMethods.readCandidates(temp_root + "txt_temp_summary/1_unknown_/formula_candidates.tsv".replace("/", sep), rank_count, table_feature);
        System.out.println(String.join(",", pre_formula));
        System.out.println(String.join(",", post_formula));
        assertArrayEquals(pre_formula, post_formula);
        System.out.println("TXT passed");
    }
}