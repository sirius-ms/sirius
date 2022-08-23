import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;

public class MsTest {
    private static void isEqual(File firstFile, File secondFile) throws IOException {
        if(!FileUtils.contentEquals(firstFile, secondFile)){
            throw new RuntimeException();
        }
    }

    public static void main(String[] args) throws IOException {
        // formula annotation
        File preFormulaFA = new File("sirius_cli/src/test/test_results/ms_candidates/" +
                "formula_annotation/formula_candidates.tsv");
        File tempFormulaFA = new File("sirius_cli/src/test/ms_temp_summary_fa/formula_candidates.tsv");

        isEqual(preFormulaFA, tempFormulaFA);


        // compound class annotation
        File preFormulaCCA = new File("sirius_cli/src/test/test_results/ms_candidates/" +
                "compound_class_annotation/formula_candidates.tsv");
        File tempFormulaCCA = new File("sirius_cli/src/test/ms_temp_summary_cca/formula_candidates.tsv");

        isEqual(preFormulaCCA, tempFormulaCCA);

        File preStructureCCA = new File("sirius_cli/src/test/test_results/mgf_candidates/" +
                "compound_class_annotation/structure_candidates.tsv");
        File tempStructureCCA = new File("sirius_cli/src/test/ms_temp_summary_cca/structure_candidates.tsv");

        isEqual(preStructureCCA, tempStructureCCA);


        // ignore formula
        File preFormulaIF = new File("sirius_cli/src/test/test_results/ms_candidates/" +
                "ignore_formula/formula_candidates.tsv");
        File tempFormulaIF = new File("sirius_cli/src/test/ms_temp_summary_if/formula_candidates.tsv");

        isEqual(preFormulaIF, tempFormulaIF);

    }
}
