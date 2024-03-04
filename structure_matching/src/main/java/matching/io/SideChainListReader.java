package matching.io;

import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import lombok.Getter;
import matching.datastructures.AtomContainerE;
import matching.datastructures.AtomE;
import matching.datastructures.SideChain;
import matching.datastructures.SideChainList;
import org.openscience.cdk.AtomRef;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.*;
import java.nio.file.Files;

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
     * -- GETTER --
     *  Returns the
     *  object which contains all side chains contained in
     * .<br>
     *
     *  has to be called before.
     *
     * @return the list of sideChains which are contained in {@link #file}

     */
    @Getter
    private SideChainList sideChainList;

    /**
     * The {@link File} that contains the side chains that are to be read.
     */
    private BufferedReader reader;

    /**
     * Constructs a new SideChainListReader object with a specified file.
     *
     * @param in the {@link Reader} that is used to read the side chains from the stream
     * @throws IllegalArgumentException if the given file does not exist, is a directory or cannot be read
     */
    public SideChainListReader(Reader in){
        if(in instanceof BufferedReader){
            this.reader = (BufferedReader) in;
        }else{
            this.reader = new BufferedReader(in);
        }
    }

    public SideChainListReader(File file) throws IOException {
        this(Files.newBufferedReader(file.toPath()));
    }


    /**
     * Reads the given side chains from the reader/input stream and adds all side chains to {@link #sideChainList}.
     *
     * @throws IOException if an I/O error occurs
     */
    public void readFile() throws IOException, CDKException {
        if (this.sideChainList == null) {
            this.sideChainList = new SideChainList();
            final SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            String next = this.reader.readLine();

            while(next != null) {
                next = next.trim();
                if (!next.isEmpty()) {
                    if (next.startsWith(">")) { // side chain discovered
                        final StringBuilder strBuilder = new StringBuilder();
                        next = this.reader.readLine();
                        while (next != null) {
                            next = next.trim();
                            if (!next.isEmpty()) {
                                if (next.charAt(0) != '>') {
                                    strBuilder.append(next);
                                } else {
                                    break;
                                }
                            }
                            next = this.reader.readLine();
                        }
                        AtomContainerE sc = this.getAtomContainerE(smiParser, strBuilder.toString());
                        this.sideChainList.add(new SideChain(sc, (AtomE) sc.getAtom(0)));
                    }else{
                        next = this.reader.readLine();
                    }
                }else{
                    next = this.reader.readLine();
                }
            }
            this.reader.close();
        }
    }

    private AtomContainerE getAtomContainerE(SmilesParser smiParser, String smiles) throws CDKException {
        IAtomContainer molecule = smiParser.parseSmiles(smiles);
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
        Aromaticity.cdkLegacy().apply(molecule);
        return new AtomContainerE(molecule);
    }
}
