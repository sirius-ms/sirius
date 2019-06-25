package de.unijena.bioinf.ChemistryBase.chem;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class IonTest {

    protected PeriodicTable T = PeriodicTable.getInstance();

    @Test
    public void testIonParser() {
        final PrecursorIonType protonation  = T.ionByNameOrThrow("[M + H]+");
        assertEquals("[M+H]+ should be a protonation", MolecularFormula.parseOrThrow("H"), protonation.getIonization().getAtoms());
        assertEquals("whitespaces should be ignored", T.ionByNameOrThrow("[M+H]+"), protonation);
        assertTrue("protonation is positive ion mode", protonation.getIonization().getCharge() > 0);

        final PrecursorIonType deprotonation  = T.ionByNameOrThrow("[M - H]-");
        assertEquals("[M-H]- should be a deprotonation", MolecularFormula.parseOrThrow("H").negate(), deprotonation.getIonization().getAtoms());
        assertEquals("whitespaces should be ignored", T.ionByNameOrThrow("[M-H]-"), deprotonation);
        assertTrue("protonation is negative ion mode", deprotonation.getIonization().getCharge() < 0);
    }

    @Test
    public void testSpecialCasesOfIonParser() {
        {
            final PrecursorIonType methanol  = T.ionByNameOrThrow("[M - MeOH + H]+");
            assertEquals("MeOH is methanol with formula CH4O", methanol.getInSourceFragmentation().toString(), "CH4O");
            assertEquals(methanol.getIonization().getAtoms(), MolecularFormula.parseOrThrow("H"));
        }

        {
            final PrecursorIonType twoWaters = T.ionByNameOrThrow("[M - 2H2O + H]+");
            assertEquals("number before formula is a multiplier", twoWaters.getInSourceFragmentation().toString(), "H4O2");
            assertEquals(twoWaters.getIonization().getAtoms(), MolecularFormula.parseOrThrow("H"));
        }
    }

}
