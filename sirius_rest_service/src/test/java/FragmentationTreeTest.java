import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.ms.middleware.model.annotations.FragmentationTree;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

public class FragmentationTreeTest {

    @Test
    public void testFragmentationTreeFromFTree() throws IOException {     InputStream inputStream;
        //test for 3 different adducts

        //[M+H]+
        inputStream = getClass().getClassLoader().getResourceAsStream("data/C14H12N2O4/C14H12N2O4_M+H+.json");
        FTJsonReader treeReader = new FTJsonReader();
        FTree tree = treeReader.parse(inputStream, null);
        FragmentationTree fragmentationTree = FragmentationTree.fromFtree(tree);

        Assertions.assertEquals(MolecularFormula.parseOrNull("C14H12N2O4"), MolecularFormula.parseOrNull(fragmentationTree.getMolecularFormula()));
        Assertions.assertEquals(PrecursorIonType.fromString("[M+H]+"), PrecursorIonType.fromString(fragmentationTree.getAdduct()));
        Assertions.assertEquals(40, IonTreeUtils.getNumberOfTrueFragments(tree)); //test may be moved to a better suited location. But here we have some data.


        //[M+H+NH3]+
        inputStream = getClass().getClassLoader().getResourceAsStream("data/C14H12N2O4/C14H12N2O4_M+NH4+.json");
        treeReader = new FTJsonReader();
        tree = treeReader.parse(inputStream, null);
        fragmentationTree = FragmentationTree.fromFtree(tree);

        Assertions.assertEquals(MolecularFormula.parseOrNull("C14H9NO4"), MolecularFormula.parseOrNull(fragmentationTree.getMolecularFormula()));
        Assertions.assertEquals(PrecursorIonType.fromString("[M+NH4]+"), PrecursorIonType.fromString(fragmentationTree.getAdduct()));
        Assertions.assertEquals(36, IonTreeUtils.getNumberOfTrueFragments(tree)); //test may be moved to a better suited location. But here we have some data.

        //[M-H2O+H]+
        inputStream = getClass().getClassLoader().getResourceAsStream("data/C14H12N2O4/C14H12N2O4_M-H2O+H+.json");
        treeReader = new FTJsonReader();
        tree = treeReader.parse(inputStream, null);

        fragmentationTree = FragmentationTree.fromFtree(tree);

        Assertions.assertEquals(MolecularFormula.parseOrNull("C14H14N2O5"), MolecularFormula.parseOrNull(fragmentationTree.getMolecularFormula()));
        Assertions.assertEquals(PrecursorIonType.fromString("[M-H2O+H]+"), PrecursorIonType.fromString(fragmentationTree.getAdduct()));
        Assertions.assertEquals(40, IonTreeUtils.getNumberOfTrueFragments(tree)); //test may be moved to a better suited location. But here we have some data.
    }
}
