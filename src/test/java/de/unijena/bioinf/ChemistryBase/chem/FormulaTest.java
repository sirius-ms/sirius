package de.unijena.bioinf.ChemistryBase.chem;

import org.junit.Test;

import static junit.framework.Assert.*;

public class FormulaTest {
	
	@Test
	public void testFormulaCreation() {
		final MolecularFormula formula = MolecularFormula.parse("C6H12O6");
		assertEquals("C6H12O6", formula.formatByHill());
		final MolecularFormula formula2 = MolecularFormula.parse("H6C8Fe3Cl7N8");
		assertEquals(3, formula2.numberOf(PeriodicTable.getInstance().getByName("Fe")));
	}
	
	@Test
	public void testCaching() {
		PeriodicTable.getInstance().cache.clearCache();
		final MolecularFormula formula = MolecularFormula.parse("C6H12O6N3Cl2Fe6S3");
		final MolecularFormula formula2 = MolecularFormula.parse("C6H12O6N3Cl2Na");
		assertSame(formula.getTableSelection(), formula2.getTableSelection());
		final MolecularFormula formula3 = MolecularFormula.parse("ZnMgMnC12I3");
		assertNotSame("Both formulas should have different table selections", 
				formula.getTableSelection(), formula3.getTableSelection());
		final MolecularFormula formula4 = MolecularFormula.parse("C6H12O6Mg2");
		assertSame(formula3.getTableSelection(), formula4.getTableSelection());
		final MolecularFormula formula5 = MolecularFormula.parse("H6C8Fe3Cl7N8");
		assertSame(formula.getTableSelection(), formula5.getTableSelection());
	}
	
	@Test
	public void testGrouping() {
		final MolecularFormula formula = MolecularFormula.parse("C3H6(CH2)8C3H6");
		assertEquals("C14H28", formula.formatByHill());
		final MolecularFormula formula2 = MolecularFormula.parse("C3H6(CH2(COH)2)8C3H6");
		assertEquals("C30H44O16", formula2.formatByHill());
	}
	

}
