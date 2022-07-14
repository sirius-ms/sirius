import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TxtTestLocal {

    private static String[] pre_candidates;
    private static String[] post_candidates;
    private static int rank_count = 3;
    private static int table_feature = 1;

    @BeforeAll
    public static void getCandidates(){

        char sep = SystemUtils.IS_OS_MAC ? System.getProperty("file.separator").charAt(0) : '/';

        pre_candidates  = readCandidates("/builds/bioinf-mit/ms/sirius_frontend/sirius_cli/src/test/test_results/txt_candidates/formula_candidates.tsv".replace('/', sep), rank_count, table_feature);
        post_candidates = readCandidates("/builds/bioinf-mit/ms/sirius_frontend/sirius_cli/src/test/temp_results/txt_temp_summary/0_laudanosine_FEATURE_1/formula_candidates.tsv".replace('/', sep), rank_count, table_feature);

    }
    @Test
    @DisplayName("Testing if SIRIUS calculates expected formula candidates with mgf file.")
    public void testTopCandidates(){
        assertArrayEquals(pre_candidates, post_candidates);
    }

    /**
         * A method returning a String[] containing a number of features of the same type from the specified file.
         * @param   filePath        the file to read from
         * @param   candidates_num  the number of rows to read from the file
         * @param   feature         the column of the split String to read
         * @return                  the String[] containing the specified information
         */
    public static String[] readCandidates(String filePath, int candidates_num, int feature){

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
}