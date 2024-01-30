import matching.datastructures.AtomContainerE;
import matching.datastructures.SideChainList;
import matching.io.SideChainListReader;
import org.junit.Test;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.isomorphism.VentoFoggia;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SideChainListReaderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testPathnameOfNoExistingFile(){
        File file = new File("./res/Testdateien_SCLReader/notExistingFile.txt");
        SideChainListReader scReader = new SideChainListReader(file);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPathnameOfADirectory(){
        File file = new File("./res/Testdateien_SCLReader");
        SideChainListReader scReader = new SideChainListReader(file);
    }


    @Test
    public void testNumberOfReadSideChains(){
        try{
            File file = new File("./res/Testdateien_SCLReader/sideChainsTest.txt");
            SideChainListReader scReader = new SideChainListReader(file);
            scReader.readFile();

            int actualNumberOfReadSideChains = scReader.getSideChainList().size();
            assertEquals(4, actualNumberOfReadSideChains);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testCorrectlyReadSideChainsStructure(){
        try{
            File file = new File("./res/Testdateien_SCLReader/sideChainsTest.txt");
            SideChainListReader scReader = new SideChainListReader(file);
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

    @Test
    public void testSetNewFile(){
        try{
            File file = new File("./res/Testdateien_SCLReader/sideChainsTest.txt");
            SideChainListReader scReader = new SideChainListReader(file);
            scReader.readFile();

            scReader.setFile(new File("./res/Testdateien_SCLReader/sideChains.txt"));
            assertEquals(0, scReader.getSideChainList().size());

            scReader.readFile();
            assertEquals(7, scReader.getSideChainList().size());

        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
