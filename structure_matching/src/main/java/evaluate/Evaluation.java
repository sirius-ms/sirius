package evaluate;

import matching.algorithm.EDIC;

import java.io.File;

public class Evaluation {

    public static void main(String[] args){
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);

        try {
            FileMoleculesMatcher<EDIC> fileMatcher = new FileMoleculesMatcher<>(inputFile,1,2," - ",false,EDIC::new);
            fileMatcher.processData(outputFile);
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
