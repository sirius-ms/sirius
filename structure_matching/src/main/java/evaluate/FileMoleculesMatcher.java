package evaluate;

import matching.algorithm.Matcher;
import matching.algorithm.MatcherFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.*;

public class FileMoleculesMatcher<M extends Matcher> {

    private File inputFile;
    private int posMolecule1;
    private int posMolecule2;
    private boolean hasHeader;
    private String separator;
    private MatcherFactory<M> factory;

    public FileMoleculesMatcher(File inputFile, int posMolecule1, int posMolecule2, String separator, boolean hasHeader, MatcherFactory<M> factory) throws IOException {
        if(inputFile.isFile()){
            this.inputFile = inputFile;
            this.posMolecule1 = posMolecule1;
            this.posMolecule2 = posMolecule2;
            this.separator = separator;
            this.hasHeader = hasHeader;
            this.factory = factory;
        }else{
            throw new IOException("The given File object 'inputFile' does not exist or is a directory.");
        }
    }

    //FileMoleculesMatcher matcher = new FileMoleculesMatcher(null,0,0,"",true, EDIC::new);
    public void processData(File outputFile) throws Exception {
        if(outputFile.isFile()){
            BufferedReader fileReader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(outputFile));
            SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());

            String currentLine = fileReader.readLine();
            if(this.hasHeader){
                fileWriter.write(currentLine+this.separator+"matching_score");
                fileWriter.newLine();
                currentLine = fileReader.readLine();
            }

            while(currentLine != null){
                String[] data = currentLine.split(this.separator);

                IAtomContainer molecule1 = smilesParser.parseSmiles(data[this.posMolecule1]);
                IAtomContainer molecule2 = smilesParser.parseSmiles(data[this.posMolecule2]);

                M matcher = this.factory.newInstance(molecule1,molecule2);
                double score = matcher.getScore();

                String printLine = currentLine+this.separator+score;
                fileWriter.write(printLine);
                fileWriter.newLine();

                currentLine = fileReader.readLine();
            }

            fileReader.close();
            fileWriter.close();
        }else{
            throw new IOException("The specified File object 'outputFile' does not exist or is a directory.");
        }
    }
}
