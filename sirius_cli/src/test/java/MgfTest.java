//import org.apache.commons.io.FileUtils;
//import java.io.File;
//import java.io.IOException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class MgfTest {
//    private static void isEqual(File firstFile, File secondFile) throws IOException {
//        if(!FileUtils.contentEquals(firstFile, secondFile)){
//            throw new RuntimeException();
//        }
//    }
//
//    public static void main(String[] args) throws IOException {
//        File preFormula = new File("sirius_cli/src/test/test_results/mgf_candidates/formula_candidates.tsv");
//        File tempFormula = new File("sirius_cli/src/test/mgf_temp_summary/0_laudanosine_FEATURE_1/formula_candidates.tsv");
//
//        File preStructure = new File("sirius_cli/src/test/test_results/mgf_candidates/structure_candidates.tsv");
//        File tempStructure = new File("sirius_cli/src/test/mgf_temp_summary/0_laudanosine_FEATURE_1/structure_candidates.tsv");
//
//        isEqual(preFormula, tempFormula);
//        isEqual(preStructure, tempStructure);
//    }

    private static boolean isEqual(Path File_One, Path File_Two) throws IOException {
        long file_size = Files.size(File_One);
        if (file_size != Files.size(File_Two)) {
            return false;
        }

        if (file_size < 2048) {
            return Arrays.equals(Files.readAllBytes(File_One), Files.readAllBytes(File_Two));
        }

        // Compare byte-by-byte
        BufferedReader content1 = Files.newBufferedReader(File_One);
        BufferedReader content2 = Files.newBufferedReader(File_Two);
        int byt;
        while ((byt = content1.read()) != -1) {
            if (byt != content2.read()) {
                return false;
            }
        }
        content1.close();
        content2.close();
        return true;
    }

    public static void main(String[] args) throws IOException {
        File preFormula = new File("sirius_cli/src/test/test_results/mgf_candidates/formula_candidates.tsv");
        File tempFormula = new File("sirius_cli/src/test/mgf_temp_summary/0_laudanosine_FEATURE_1/formula_candidates.tsv");

        File preStructure = new File("sirius_cli/src/test/test_results/mgf_candidates/structure_candidates.tsv");
        File tempStructure = new File("sirius_cli/src/test/mgf_temp_summary/0_laudanosine_FEATURE_1/structure_candidates.tsv");

        boolean comparisonFormula = isEqual(preFormula.toPath(), tempFormula.toPath());
        if (!comparisonFormula) {
            throw new RuntimeException("Files not equal!");
        }

        boolean comparisonStructure = isEqual(preStructure.toPath(), tempStructure.toPath());
        if (!comparisonStructure) {
            throw new RuntimeException("Files not equal!");
        }
    }
}


