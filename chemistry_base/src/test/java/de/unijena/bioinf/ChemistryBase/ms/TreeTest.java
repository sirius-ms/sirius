package de.unijena.bioinf.ChemistryBase.ms;

/**
 * Created by kaidu on 06.10.2015.
 */
public class TreeTest {

    /*private final FTree getExampleTree() {
        final FTree a = new FTree(MolecularFormula.parse("C6H12O6"));
        Fragment u = a.addFragment(a.getRoot(), MolecularFormula.parse("C6H10O5"));
        Fragment v = a.addFragment(a.getRoot(), MolecularFormula.parse("C5H12O4"));
        Fragment w = a.addFragment(u, MolecularFormula.parse("C4H8O5"));
        a.addFragment(w, MolecularFormula.parse("C3H8O3"));
        a.addFragment(v, MolecularFormula.parse("C4H11O2"));
        return a;
    }


    @Test
    public void testRootSwapping() {
        FTree x = getExampleTree();
        Fragment oldRoot = x.getRoot();
        assertEquals(x.getRoot().getFormula(), MolecularFormula.parse("C6H12O6"));
        x.addRoot(MolecularFormula.parse("C6H14O7"));
        assertEquals(x.getRoot().getFormula(), MolecularFormula.parse("C6H14O7"));
        assertEquals(x.getRoot().getChildren(0).getFormula(), MolecularFormula.parse("C6H12O6"));
        assertEquals(x.getRoot().getChildren(0), oldRoot);
        assertEquals(x.getRoot().getOutgoingEdge(0).getFormula(), MolecularFormula.parse("H2O"));
        assertEquals(oldRoot.getIncomingEdge().getFormula(), MolecularFormula.parse("H2O"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRootSwapping2() {
        FTree x = getExampleTree();
        x.addRoot(MolecularFormula.parse("C5H14O7"));
    }*/

}
