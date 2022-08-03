import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TestMethods {

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
