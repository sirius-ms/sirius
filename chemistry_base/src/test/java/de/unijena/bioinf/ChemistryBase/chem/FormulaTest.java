
package de.unijena.bioinf.ChemistryBase.chem;

import org.junit.Test;

import static junit.framework.Assert.*;

public class FormulaTest {
	
	@Test
	public void testFormulaCreation() {
		final MolecularFormula formula = MolecularFormula.parseOrThrow("C6H12O6");
		assertEquals("C6H12O6", formula.formatByHill());
		final MolecularFormula formula2 = MolecularFormula.parseOrThrow("H6C8Fe3Cl7N8");
		assertEquals(3, formula2.numberOf(PeriodicTable.getInstance().getByName("Fe")));
	}

	@Test
	public void testHillNotation() {
		final MolecularFormula formula = MolecularFormula.parseOrThrow("MgN4C55OH72");
		assertEquals("Formula is ordered according to Hill notation", "C55H72MgN4O", formula.formatByHill());
		final MolecularFormula formula2 = MolecularFormula.parseOrThrow("H").add(MolecularFormula.parseOrThrow("Cl"));
		assertEquals("Formula is ordered according to Hill notation", "ClH", formula2.formatByHill());
		final MolecularFormula formula3 = MolecularFormula.parseOrThrow("NH3");
		assertEquals("Formula is ordered according to Hill notation", "H3N", formula3.formatByHill());
		assertEquals("Default String representation of formula is Hill", formula.formatByHill(), formula.toString());
		assertEquals("Default String representation of formula is Hill", formula2.formatByHill(), formula2.toString());
		assertEquals("Default String representation of formula is Hill", formula3.formatByHill(), formula3.toString());
	}
	
	@Test
	public void testCaching() {
		PeriodicTable.getInstance().cache.clearCache();
		final MolecularFormula formula = MolecularFormula.parseOrThrow("C6H12O6N3Cl2Fe6S3");
		final MolecularFormula formula2 = MolecularFormula.parseOrThrow("C6H12O6N3Cl2Na");
		assertSame(formula.getTableSelection(), formula2.getTableSelection());
		final MolecularFormula formula3 = MolecularFormula.parseOrThrow("ZnMgMnC12I3");
		assertNotSame("Both formulas should have different table selections", 
				formula.getTableSelection(), formula3.getTableSelection());
		final MolecularFormula formula4 = MolecularFormula.parseOrThrow("C6H12O6Mg2");
		assertSame(formula3.getTableSelection(), formula4.getTableSelection());
		final MolecularFormula formula5 = MolecularFormula.parseOrThrow("H6C8Fe3Cl7N8");
		assertSame(formula.getTableSelection(), formula5.getTableSelection());
	}
	
	@Test
	public void testGrouping() {
		final MolecularFormula formula = MolecularFormula.parseOrThrow("C3H6(CH2)8C3H6");
		assertEquals("C14H28", formula.formatByHill());
		final MolecularFormula formula2 = MolecularFormula.parseOrThrow("C3H6(CH2(COH)2)8C3H6");
		assertEquals("C30H44O16", formula2.formatByHill());
	}

	@Test
	public void testContains() {
		final MolecularFormula formula = MolecularFormula.parseOrThrow("C3H6O2");
		final MolecularFormula formula2 = MolecularFormula.parseOrThrow("C2H4");
		final MolecularFormula formula3 = MolecularFormula.parseOrThrow("C7H4");
		final MolecularFormula formula4 = MolecularFormula.parseOrThrow("C3H6O2");
		final MolecularFormula formula5 = MolecularFormula.emptyFormula();
		assertTrue(formula.contains(formula2));
		assertFalse(formula.contains(formula3));
		assertTrue(formula.contains(formula4));
		assertTrue(formula.contains(formula5));
		assertTrue(formula.contains(null));
	}
	

}
