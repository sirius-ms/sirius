import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

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


    /**
     * Test if canopus, canopus_npc, fingerid and fingerprints folders exist and are filled for MGF
     */
    public static void isDirNotEmpty(Path directory) throws IOException {
        DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory);
        if(!dirStream.iterator().hasNext()){
            dirStream.close();
            throw new RuntimeException("The "+ directory +" folder is empty!");
        }
        dirStream.close();
    }

    public static void isDirExisting(String directory){
        File f = new File(directory);
        if(!f.exists()) {
            throw new RuntimeException("The "+directory+" folder does not exist or is not a folder!");
        }
    }

    public static void areContentsEqual(String dir1, String dir2, String dir3){
        File folder1 = new File(dir1);
        File[] listOfFilesIn1 = folder1.listFiles();
        File folder2 = new File(dir2);
        File[] listOfFilesIn2 = folder2.listFiles();
        File folder3 = new File(dir3);
        File[] listOfFilesIn3 = folder3.listFiles();
        if(!(listOfFilesIn1.length == listOfFilesIn2.length) || !(listOfFilesIn2.length == listOfFilesIn3.length)){
            throw new RuntimeException("The canopus and fingerprint folders do not include files for the same compounds!");
        }

        for (int i = 0; i < listOfFilesIn1.length; i++){
            if(!((listOfFilesIn1[i].getName().split("\\.")[0].equals(listOfFilesIn2[i].getName().split("\\.")[0])) &&
                    (listOfFilesIn2[i].getName().split("\\.")[0].equals(listOfFilesIn3[i].getName().split("\\.")[0])))){
                throw new RuntimeException("The canopus and fingerprint folders do not include files for the same compounds!");
            }
        }
    }
}
