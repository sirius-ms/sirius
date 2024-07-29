import matching.datastructures.AtomContainerE;
import matching.datastructures.AtomE;
import matching.datastructures.SideChainList;
import matching.io.SideChainListReader;
import org.junit.Test;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomRef;
import org.openscience.cdk.Bond;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.isomorphism.VentoFoggia;
import org.openscience.cdk.silent.Atom;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

public class SideChainListReaderTest {

    @Test
    public void testNumberOfReadSideChains(){
        try{
            InputStream is = getClass().getClassLoader().getResourceAsStream("sideChainsTest.txt");
            SideChainListReader scReader = new SideChainListReader(new InputStreamReader(is));
            scReader.readFile();

            int actualNumberOfReadSideChains = scReader.getSideChainList().size();
            assertEquals(4, actualNumberOfReadSideChains);
        }catch (IOException e){
            e.printStackTrace();
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCorrectlyReadSideChainsStructure(){
        try{
            InputStream is = getClass().getClassLoader().getResourceAsStream("sideChainsTest.txt");
            SideChainListReader scReader = new SideChainListReader(new InputStreamReader(is));
            scReader.readFile();
            SideChainList scList = scReader.getSideChainList();

            String[] smilesStrings = {"[c]1[c]([H])[c]([H])[c]([C](=[O])[O][H])[c]([H])[c]1([H])",
                    "[C](=[O])[O][H]", "[O][H]", "[H]"};

            SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            VentoFoggia vf;

            for(int i = 0; i < 4; i++){
                AtomContainerE sc = new AtomContainerE(smiParser.parseSmiles(smilesStrings[i]));
                AtomContainerE readSC = scList.get(i).getSideChain();

                vf = (VentoFoggia) VentoFoggia.findIdentical(sc);
                Mappings ism = vf.matchAll(readSC);

                assertEquals(true, ism.atLeast(1));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
