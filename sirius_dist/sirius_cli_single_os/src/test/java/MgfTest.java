//import org.apache.commons.io.FileUtils;
//import java.io.File;
//import java.io.IOException;

//import java.io.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.DisplayName;

public class MgfTest {

    private static String[] pre_candidates = null;
    private static String[] post_candidates = null;

    @BeforeClass
    public static void getCandidates(){
        pre_candidates  = readCandidates("/builds/bioinf-mit/ms/sirius_frontend/sirius_cli/src/test/test_results/mgf_candidates/formula_candidates.tsv");
        post_candidates = readCandidates("/builds/bioinf-mit/ms/sirius_frontend/sirius_cli/src/test/temp_results/mgf_temp_summary/0_laudanosine_FEATURE_1/formula_candidates.tsv");
    }

    @Test
    @DisplayName("Testing if SIRIUS calculates expected formula candidates with mgf file.")
    public void testTopCandidates(){

        System.out.println(Arrays.toString(pre_candidates));
        System.out.println(Arrays.toString(post_candidates));

        assertArrayEquals(pre_candidates, post_candidates);
    }

    public static String[] readCandidates(String filePath){

        BufferedReader reader;

        try{
            reader = new BufferedReader(new FileReader(filePath));
        }catch(IOException e){
            throw new RuntimeException("The file is not in the specified directory!");
        }
        int candidates_num = 3;
        String[] top_results = new String[candidates_num];

        try{
            for(int i = 0; i< candidates_num -1; i++){
                String line = reader.readLine();

                top_results[i] = line.split("\t")[1];
            }
        }
        catch(IOException e){
            throw new RuntimeException("There are more required top candidates than candidates in the file.");
        }

        try{
            reader.close();
        }catch(IOException e){
            System.err.println("The file was not closed properly.");
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


