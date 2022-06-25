import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class MgfTestLocal {

    private static String[] pre_candidates;
    private static String[] post_candidates;
    private static int rank_count = 3;
    private static int table_feature = 1;


    @BeforeAll
    public static void getCandidates(){

        // generate absolute path to summary files
        String absPath = System.getProperty("user.dir");
        absPath = absPath.split("sirius_dist\\\\sirius_cli_single_os")[0];

        pre_candidates  = readCandidates(absPath + "sirius_cli\\src\\test\\test_results\\mgf_candidates\\formula_candidates.tsv", rank_count, table_feature);
        post_candidates = readCandidates(absPath + "sirius_cli\\src\\test\\temp_results\\mgf_temp_summary\\0_laudanosine_FEATURE_1\\formula_candidates.tsv", rank_count, table_feature);
    }

    @Test
    @DisplayName("Testing if SIRIUS calculates expected formula candidates with mgf file.")
    public void testTopCandidates(){

        System.out.println(Arrays.toString(pre_candidates));
        System.out.println(Arrays.toString(post_candidates));

        assertArrayEquals(pre_candidates, post_candidates);
    }

    /**
     * A method returning a String[] containing a number of features of the same type from the specified file.
     * @param   filePath        the file to read from
     * @param   candidates_num  the number of rows to read from the file
     * @param   feature         the column of the split String to read
     * @return                  the String[] containing the specified information
     */
    public static String @NotNull [] readCandidates(String filePath, int candidates_num, int feature){

        BufferedReader reader;
        String[] top_results = new String[candidates_num];

        try{
            reader = new BufferedReader(new FileReader(filePath));
        }catch(IOException e) {
            throw new RuntimeException("The file is not in the specified directory!");
        }

        try{
            reader.readLine();
            for(int i = 0; i < candidates_num; i++){
                String line = reader.readLine();

                top_results[i] = line.split("\t")[feature];
            }
        }
        catch(IOException e){
            throw new RuntimeException("There are more required top candidates than candidates in the file.");
        }

        try{
            reader.close();
        }catch(IOException e){
            throw new RuntimeException("The file was not closed properly.");
        }

        return top_results;
    }












    /*
    public static void main(String[] args) throws IOException {

        //System.out.println("Working Directory = " + System.getProperty("user.dir"));

        File preFormula = new File("/builds/bioinf-mit/ms/sirius_frontend/sirius_cli/src/test/test_results/mgf_candidates/formula_candidates.tsv");
        File tempFormula = new File("/builds/bioinf-mit/ms/sirius_frontend/sirius_cli/src/test/temp_results/mgf_temp_summary/0_laudanosine_FEATURE_1/formula_candidates.tsv");

        // the Linux distro (in contrast to Windows) does NOT produce a structure_candidates.tsv file !!!
//        File preStructure = new File("/builds/bioinf-mit/ms/sirius_frontend/sirius_cli/src/test/test_results/mgf_candidates/structure_candidates.tsv");
//        File tempStructure = new File("/builds/bioinf-mit/ms/sirius_frontend/sirius_cli/src/test/temp_results/mgf_temp_summary/0_laudanosine_FEATURE_1/structure_candidates.tsv");

        boolean comparisonFormula = isEqual(preFormula.toPath(), tempFormula.toPath());
        if (!comparisonFormula) {
            throw new RuntimeException("Files not equal!");
        }

        //best practice für die Tests
        // Context, wie die Tests abgelaufen werden
        // Prüfen nur auf Kandidaten richtig, nicht Byteweise




//        boolean comparisonStructure = isEqual(preStructure.toPath(), tempStructure.toPath());
//        if (!comparisonStructure) {
//            throw new RuntimeException("Files not equal!");
//        }
    }

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
    */

}


