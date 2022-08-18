import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;

public class TxtTest {
    private static void isEqual(File firstFile, File secondFile) throws IOException {
        if(!FileUtils.contentEquals(firstFile, secondFile)){
            throw new RuntimeException();
        }
    }

    public static void main(String[] args) throws IOException {
        File preFormula = new File("sirius_cli/src/test/test_results/txt_candidates/formula_candidates.tsv");
        File tempFormula = new File("sirius_cli/src/test/txt_temp_summary/formula_candidates.tsv");

        isEqual(preFormula, tempFormula);
    }
}
