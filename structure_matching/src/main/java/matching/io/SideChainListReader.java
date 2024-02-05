package matching.io;

import matching.datastructures.AtomContainerE;
import matching.datastructures.AtomE;
import matching.datastructures.SideChain;
import matching.datastructures.SideChainList;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * <p>
 * An object of this class can read a file that contains several side chains.
 * </p>
 * <p>
 * These files which can be read by a SideChainListReader have to contain side chains which are specified as
 * SMILES strings with explicit hydrogen atoms. Every side chain in this file consists of a name starting with '>' and
 * a following SMILES string that has to occur in the next lines.
 * The SMILES string has to start with the bridge node.
 * </p>
 */
public class SideChainListReader {

    /**
     * The {@link SideChainList} object that contains all side chains which are specified in the given {@link #file}.<br>
     * This object will be constructed after calling {@link #readFile()}.
     */
    private SideChainList sideChainList;

    /**
     * The {@link File} that contains the side chains that are to be read.
     */
    private File file;

    /**
     * Constructs a new SideChainListReader object with a specified file.
     *
     * @param file the {@link File} that contains the side chains that are to be read
     * @throws IllegalArgumentException if the given file does not exist, is a directory or cannot be read
     */
    public SideChainListReader(File file) throws IllegalArgumentException{
        if(file.canRead() && file.isFile()) {
            this.sideChainList = new SideChainList();
            this.file = file;
        }else{
            throw new IllegalArgumentException("The given file cannot be read, is a directory or does not exist."+
            "The path of this file is: "+file.getAbsolutePath());
        }
    }

    /**
     * Sets a new file which will be read.<br>
     * {@link #sideChainList} will be cleared.
     *
     * @param file the {@link File} that contains the side chains that are to be read
     */
    public void setFile(File file){
        this.file = file;
        this.sideChainList.clear();
    }

    /**
     * Returns the {@link SideChainList} object which contains all side chains contained in {@link #file}.<br>
     * {@link #readFile()} has to be called before.
     *
     * @return the list of sideChains which are contained in {@link #file}
     */
    public SideChainList getSideChainList(){
        return this.sideChainList;
    }

    private int getNumberOfSideChains(File file) throws IOException{
        int numOfSideChains = 0;

        BufferedReader fileReader = new BufferedReader(new FileReader(file));
        String next = fileReader.readLine();

        while(next != null){
            next = next.trim();

            if(next.length() > 0){
                if(next.charAt(0) == '>'){
                    numOfSideChains++;
                }
            }

            next = fileReader.readLine();
        }

        fileReader.close();

        return numOfSideChains;
    }

    /**
     * Reads the given {@link #file} and adds all side chains to {@link #sideChainList}
     * which are contained in this file.
     *
     * @throws IOException if an I/O error occurs
     */
    public void readFile() throws IOException{
        int numOfSideChains = this.getNumberOfSideChains(this.file);

        if(numOfSideChains > 0){
            BufferedReader fileReader = new BufferedReader(new FileReader(this.file));
            String next = fileReader.readLine();

            //finde erste Seitenkette:
            while(next != null){
                next = next.trim();

                if(next.length() > 0){
                    if(next.charAt(0) == '>'){
                        break;
                    }
                }
                next = fileReader.readLine();
            }

            StringBuilder smilesSideChain = new StringBuilder("");
            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());

            //next zeigt nun auf den Header der ersten Seitenkette:
            for(int i = 0; i < numOfSideChains; i++){
                next = fileReader.readLine();

                while(next != null){
                    next = next.trim();

                    if(next.length() > 0){
                        if(next.charAt(0) != '>'){
                            if(next.charAt(0) != '#'){
                                smilesSideChain.append(next);
                            }
                        }else{
                            //neue Seitenkette erreicht:
                            break;
                        }
                    }
                    next = fileReader.readLine();
                }

                try {
                    AtomContainerE sc = this.getAtomContainerE(smiParser, smilesSideChain.toString());
                    this.sideChainList.add(new SideChain(sc, (AtomE) sc.getAtom(0)));
                } catch (CDKException e) {
                    System.out.println("The given file contains an invalid smiles string at side chain "+(i+1)+".");
                    e.printStackTrace();
                }
                smilesSideChain.setLength(0);
            }

            fileReader.close();
        }
    }

    private AtomContainerE getAtomContainerE(SmilesParser smiParser, String smiles) throws CDKException {
        AtomContainerE molecule = new AtomContainerE(smiParser.parseSmiles(smiles));
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
        Aromaticity.cdkLegacy().apply(molecule);
        return molecule;
    }
}
