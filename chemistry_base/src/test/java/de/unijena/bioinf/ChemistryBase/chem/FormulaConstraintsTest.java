package de.unijena.bioinf.ChemistryBase.chem;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FormulaConstraintsTest {

    @Test
    public void testStringConstructor() {
        final PeriodicTable T = PeriodicTable.getInstance();
        final Element S = T.getByName("S");
        final Element P = T.getByName("P");
        final Element Br = T.getByName("Br");
        FormulaConstraints constraints = new FormulaConstraints("CHNOP[5]S[2]");
        assertEquals("alphabe has 6 elements",6, constraints.getChemicalAlphabet().getElements().size());
        assertEquals("sulphur occurs maximal 2 times",2, constraints.getUpperbound(S));
        assertEquals("phosphor occurs maximal 5 times",5, constraints.getUpperbound(P));
        assertEquals("sulphur occurs at least 0 times",0, constraints.getLowerbound(S));
        assertEquals("phosphor occurs at least 0 times",0, constraints.getLowerbound(P));
        assertTrue("oxygen occurs maximal infinite times", constraints.getUpperbound(T.getByName("O"))>=Short.MAX_VALUE);

        constraints = new FormulaConstraints("CHNBr[1-3]");
        assertEquals("alphabe has 4 elements",4, constraints.getChemicalAlphabet().getElements().size());
        assertEquals("bromine occurs at least 1 times",1, constraints.getLowerbound(Br));
        assertEquals("bromine occurs maximal 3 times",3, constraints.getUpperbound(Br));

        constraints = new FormulaConstraints("CHNBr[-3]");
        assertEquals("bromine occurs at least 0 times",0, constraints.getLowerbound(Br));
        assertEquals("bromine occurs maximal 3 times",3, constraints.getUpperbound(Br));

        constraints = new FormulaConstraints("CHNBr[1-]");
        assertEquals("bromine occurs at least 1 times",1, constraints.getLowerbound(Br));
        assertTrue("bromine occurs maximal infinite times", constraints.getUpperbound(Br)>=Short.MAX_VALUE);

    }

    @Test
    public void testVarargConstructor() {
        final PeriodicTable T = PeriodicTable.getInstance();
        final Element S = T.getByName("S");
        final Element P = T.getByName("P");
        final Element Br = T.getByName("Br");
        FormulaConstraints constraints = FormulaConstraints.create("C", "H", "N", "O", "P", 5, "S", 2);
        assertEquals("alphabe has 6 elements",6, constraints.getChemicalAlphabet().getElements().size());
        assertEquals("sulphur occurs maximal 2 times",2, constraints.getUpperbound(S));
        assertEquals("phosphor occurs maximal 5 times",5, constraints.getUpperbound(P));
        assertEquals("sulphur occurs at least 0 times",0, constraints.getLowerbound(S));
        assertEquals("phosphor occurs at least 0 times",0, constraints.getLowerbound(P));
        assertTrue("oxygen occurs maximal infinite times", constraints.getUpperbound(T.getByName("O"))>=Short.MAX_VALUE);

        constraints = FormulaConstraints.create("C", "H", "N", "Br", 1, 3);
        assertEquals("alphabe has 4 elements",4, constraints.getChemicalAlphabet().getElements().size());
        assertEquals("bromine occurs at least 1 times",1, constraints.getLowerbound(Br));
        assertEquals("bromine occurs maximal 3 times",3, constraints.getUpperbound(Br));

        constraints = FormulaConstraints.create("CHNBr[-3]");
        assertEquals("bromine occurs at least 0 times",0, constraints.getLowerbound(Br));
        assertEquals("bromine occurs maximal 3 times",3, constraints.getUpperbound(Br));
    }


}
