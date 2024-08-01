package de.unijena.bioinf.cmlDesign.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class BuildingBlockWriter {

    private File bbFile;
    private String[][] bbSmiles;

    public BuildingBlockWriter(File bbFile, String[][] bbSmiles){
        this.bbFile = bbFile;
        this.bbSmiles = bbSmiles;
    }

    public void write2File() throws IOException {
        try(BufferedWriter fileWriter = Files.newBufferedWriter(this.bbFile.toPath())){
            fileWriter.write("bb_group\tid\tsmiles");
            fileWriter.newLine();

            for(int bbGroupIdx = 0; bbGroupIdx < this.bbSmiles.length; bbGroupIdx++){
                for(int bbIdx = 0; bbIdx < this.bbSmiles[bbGroupIdx].length; bbIdx++){
                    fileWriter.write(bbGroupIdx+'\t'+(bbIdx+1)+'\t'+this.bbSmiles[bbGroupIdx][bbIdx]);
                    fileWriter.newLine();
                }
            }
        }
    }
}
