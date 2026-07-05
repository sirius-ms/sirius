package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ChemicalAlphabetTest {

    @Test
    public void testFromString() throws UnknownElementException {
        final PeriodicTable pt = PeriodicTable.getInstance();
        final Element C = pt.getByName("C");
        final Element H = pt.getByName("H");
        final Element N = pt.getByName("N");
        final Element O = pt.getByName("O");

        // Test parsing of "CHNO"
        ChemicalAlphabet alphabet1 = ChemicalAlphabet.fromString("CHNO");
        Set<Element> expected1 = new HashSet<>(Arrays.asList(C, H, N, O));
        assertEquals(expected1, alphabet1.toSet());

        // Test parsing of "C,H,N,O"
        ChemicalAlphabet alphabet2 = ChemicalAlphabet.fromString("C,H,N,O");
        Set<Element> expected2 = new HashSet<>(Arrays.asList(C, H, N, O));
        assertEquals(expected2, alphabet2.toSet());

        // Test parsing of "C,J" fails
        try {
            ChemicalAlphabet.fromString("C,J");
            fail("Expected UnknownElementException");
        } catch (UnknownElementException e) {
            // expected
        }


        // Test parsing of "CJ" results in carbon only alphabet
        // Note: our MolecularFormula parser is currently super error resistant. Thus parsing "CJ" does not fail but parses to C-only.
        //consider changing this in the future.
        ChemicalAlphabet alphabet3 = ChemicalAlphabet.fromString("CJ");
        Set<Element> expected3 = new HashSet<>(Arrays.asList(C));
        assertEquals(expected3, alphabet3.toSet());

    }
}
