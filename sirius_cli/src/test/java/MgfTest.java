import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;

public class MgfTest {
    private static void isEqual(File firstFile, File secondFile) throws IOException {
        if(!FileUtils.contentEquals(firstFile, secondFile)){
            throw new RuntimeException();
        }
    }

    public static void main(String[] args) throws IOException {
        File preFormula = new File("sirius_cli/src/test/test_results/mgf_candidates/formula_candidates.tsv");
        File tempFormula = new File("sirius_cli/src/test/mgf_temp_summary/formula_candidates.tsv");

        File preStructure = new File("sirius_cli/src/test/test_results/mgf_candidates/structure_candidates.tsv");
        File tempStructure = new File("sirius_cli/src/test/mgf_temp_summary/structure_candidates.tsv");

        isEqual(preFormula, tempFormula);
        isEqual(preStructure, tempStructure);
    }
}
