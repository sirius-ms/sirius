package de.unijena.bioinf.MassDecomposer;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.MassDecomposer.Chemistry.ChemicalAlphabetWrapper;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import org.junit.Test;

import java.util.*;

import static junit.framework.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: Marcus
 * Date: 04.01.13
 * Time: 16:26
 * To change this template use File | Settings | File Templates.
 */
public class MassDecomposerTest {


    @Test
    public void testFormulaConstraints() {
        double mass = 212.11;
        final MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer();
        final FormulaConstraints constraints = new FormulaConstraints("CHNO[1-3]");
        final List<MolecularFormula> formulas = decomposer.decomposeNeutralMassToFormulas(mass, new Deviation(10), constraints);
        String violation=null;
        for (MolecularFormula formula : formulas) {
            if (formula.numberOfOxygens() < 1 || formula.numberOfOxygens() > 3) {
                violation = formula.toString();
                break;
            }
        }
        assertNull("All formulas satisfy constraint: CHNO[1-3].", violation);
    }

    @Test
    public void testElementBoundaries() {

        double mass = 212.11;
        final MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer();
        final List<MolecularFormula> ref = decomposer.decomposeNeutralMassToFormulas(mass, new Deviation(20, 1e-3));

        final HashMap<Element, Interval> map = new HashMap<Element, Interval>();
        map.put(PeriodicTable.getInstance().getByName("C"), new Interval(0, 10));
        final List<MolecularFormula> result1 = decomposer.decomposeNeutralMassToFormulas(mass, new Deviation(20, 1e-3), map );
        assertTrue("filtered list should be smaller that unfiltered list", ref.size() > result1.size());
        for (MolecularFormula f : result1) {
            assertTrue("formula should have maximal 10 carbon atoms", f.numberOfCarbons() <= 10);
        }
        int expectedSize1 = ref.size();
        for (MolecularFormula f : ref) {
            if (f.numberOfCarbons() > 10) --expectedSize1;
        }
        assertEquals(expectedSize1, result1.size());
        map.put(PeriodicTable.getInstance().getByName("C"), new Interval(7, 10));
        final List<MolecularFormula> result2 = decomposer.decomposeNeutralMassToFormulas(mass, new Deviation(20, 1e-3), map );
        assertTrue("strict filtered list should be smaller that permissive filtered list", result1.size() > result2.size());
        for (MolecularFormula f : result2) {
            assertTrue("formula should have at least 7 carbon atoms", f.numberOfCarbons() >= 7);
            assertTrue("formula should have maximal 10 carbon atoms", f.numberOfCarbons() <= 10);
        }
        int expectedSize2 = ref.size();
        for (MolecularFormula f : ref) {
            if (f.numberOfCarbons() > 10 || f.numberOfCarbons() < 7) {
                --expectedSize2;
            }
        }
        assertEquals(expectedSize2, result2.size());


    }

    @Test
    public void decomposerTest() {
        String[] result1 = new String[]{  //mass: 279.43, default value (ppm 20 relative error, absolute mass error in Dalton 0.001, --nofilter, -e CHNOPS, precision 1e-5
                "H180N7",
                "CH186O5",
                "C13H53N5",
                "C2H66NO7PS",
                "CH60N8O2PS",
                "H66N4O4P3",
                "C7H67O2S3",
                "C9H56N6P",
                "H76OP5S",
                "C6H63O7S",
                "C5H57N7O2S",
                "H61N3O11",
                "C4H63N3O4P2",
                "C3H70NO2PS3",
                "CH68N4OPS3",
                "C5H59N7P2",
                "C6H65O5P2",
                "H74O3P3S2",
                "C7H59N4O3S",
                "C2H190S2",
                "C8H60N2O4P",
                "H67N6S4",
                "C5H65N3OS3",
                "CH62N8P3",
                "C2H68NO5P3",
                "C3H62N5O3PS",
                "C2H71N2P4S",
                "CH76PS5",
                "C12H57NO4",
                "C2H182N4O",
                "C15H55N2O",
                "C6H68NP3S",
                "H59N10OS2",
                "CH65N3O6S2",
                "H64N4O6PS",
                "C7H69P2S2",
                "C11H58N3OP",
                "C10H65P2S",
                "C4H61N3O6S",
                "C3H55N10OS",
                "C3H72NP3S2",
                "H51N14O2",
                "CH57N7O7",
                "C2H63O12",
                "C2H61N6O3P2",
                "H72O5PS3",
                "C7H61N4OP2",
                "C9H61NO4S",
                "CH189NPS",
                "C6H58N5O3P",
                "C2H69N3OS4",
                "C3H63N6S3",
                "C3H64N5OP3",
                "C2H69N2O2P2S2",
                "C5H64N2O4PS",
                "C5H186S",
                "C10H55N4O3",
                "C4H184NO2",
                "C6H66NO2PS2",
                "CH67N3O4P2S",
                "C2H61N7O2S2",
                "C3H67O7S2",
                "C3H72OP5",
                "H187N2O2P",
                "C10H63O2S2",
                "C13H60O2P",
                "C2H59N6O5S",
                "CH53N13S",
                "C2H53N11O3",
                "C3H59N4O8",
                "CH65N2O7P2",
                "H59N9O2P2",
                "C9H63NO2P2",
                "C10H57N5S",
                "C5H62NO7P",
                "C4H56N8O2P",
                "C4H71O2S4",
                "C2H67N2O4S3",
                "C5H66N2O2P3",
                "H67N5OP2S2",
                "C6H60N6PS",
                "C9H59O7",
                "C8H53N7O2",
                "H74NO2PS4",
                "CH69N3O2P4",
                "C4H64N4OPS2",
                "C2H63N7P2S",
                "C3H69O5P2S",
                "C3H70O3P3S",
                "C4H63N4O3S2",
                "CH70N3P5",
                "H75NP2S4",
                "C8H61N3OS2",
                "H66N5O3PS2",
                "C5H67N2P4",
                "C4H72PS4",
                "CH63N2O9S",
                "H57N9O4S",
                "C4H55N8O4",
                "C5H61NO9",
                "C12H59N2OS",
                "C9H64NP3",
                "H58N9O4P",
                "CH64N2O9P",
                "C3H60N4O6P",
                "C2H54N11OP",
                "C4H73P2S3",
                "H65N5O3S3",
                "C8H62N3OPS",
                "C13H61P2",
                "H188N2P2",
                "C7H57N3O6",
                "C6H51N10O",
                "H76NP3S3",
                "C3H71O3P4",
                "C3H68O5PS2",
                "C2H62N7PS2",
                "C4H65N4OP2S",
                "CH68N3O2P3S",
                "C6H65NO4S2",
                "H73NO2S5",
                "C4H185NP"
        };


        String[] result2 = new String[]{  //mass: 479.43, default value (ppm 20 relative error, absolute mass error in Dalton 0.001, -e CHNOPS, precision 1e-5, with filter
                "C20H54N11P",
                "C24H51N10",
                "C26H53N7O",
                "C31H59OS",
                "C28H55N4O2",
                "C29H57N3S",
                "C21H53N9O3",
                "C30H57NO3",
                "C19H51N12O2",
                "C30H58NOP",
                "C34H55O",
                "C17H49N15O",
                "C16H49N17",
                "C28H56N4P",
                "C32H53N3",
                "C33H55N2",
                "C29H58N3P",
                "C15H47N18",
                "C18H51N14O",
                "C31H60OP",
                "C20H53N11O2",
                "C29H55N2O3",
                "C20H53N11S"
        };


        String[] result3 = new String[]{   //mass: 222.22, default value (ppm 20 relative error, absolute mass error in Dalton 0.001, -e CHNOPSFeClBr, precision 1e-5, --nofilter
                "C9H33ClNO2",
                "CH41N3PS3",
                "H47BrO2S2",
                "C4H36N2O3P2",
                "C5H30N6OS",
                "C8H33NO3P",
                "C5H38N2S3",
                "C5H36ClN2O2P",
                "C2H44Cl2FeNO",
                "H39N3O3P3",
                "C6H36Cl2N2O",
                "CH33N7OPS",
                "C2H39O6PS",
                "C12H30O3",
                "H39BrN4O3",
                "H37Cl2N6P",
                "CH44ClFeNO2P",
                "CH39ClN3O2P2",
                "C3H40FeN2O2S",
                "C2H33ClN7S",
                "CH37ClN3O4S",
                "C3H39ClO5S",
                "CH38FeN5OS",
                "H44FeNO3P2",
                "C3H47Fe2P",
                "C2H39Cl2N3OP",
                "H37N3O5PS",
                "C4H34Cl2N5",
                "C5H41ClFeO2",
                "C3H39Cl3N3",
                "C3H34ClN5OP",
                "C4H41FeO3P",
                "CH46FeNS3",
                "H43ClFeN3OS",
                "C4H34N2O5S",
                "H42Cl2NO4S",
                "C3H28N9S",
                "C2H34N5O2P2",
                "C11H36Fe",
                "H34N2O10",
                "H49BrP2S",
                "C7H31ClN4O",
                "C3H43OPS3",
                "C3H42ClNPS2",
                "C7H32N3O2S",
                "C6H31N4O2P",
                "C4H43ClS3",
                "C7H39FeNP",
                "C2H42NOP2S2",
                "H41Cl3N2O3",
                "H42Cl2FeN4",
                "C2H41O4P3",
                "C7H39ClS2",
                "C3H35N4O2PS",
                "C10H28N3O2",
                "H46ClNPS3",
                "C2H41BrNO4",
                "C3H42FeN2P2",
                "C3H41ClO3P2",
                "C6H39OPS2",
                "C4H35ClN4OS",
                "C3H45BrP2",
                "H42FeNO5S",
                "C2H155N3",
                "C4H41Cl2O2P",
                "C15H28N",
                "H32N9S2",
                "C3H38Cl2NO4",
                "CH38N2O5S2",
                "C3H39ClFeN3O",
                "C5H41Cl3O",
                "CH45FeO3PS",
                "H38ClN5OPS",
                "C2H38ClNO5P",
                "CH32ClN8P",
                "C2H39FeN3O2P",
                "H44Cl2NO2P2",
                "C11H31N2P",
                "C2H45ClFeO2S",
                "CH38Cl2N5S",
                "C2H32N5O4S",
                "CH38NO6P2",
                "H32N8OP2",
                "CH44Cl3NOP",
                "H24N13O",
                "CH30N6O6",
                "C6H35ClO5",
                "C5H29ClN7",
                "C6H36FeN2O2",
                "CH46Cl2P2S",
                "C2H44Cl4N",
                "C7H34N3P2",
                "H46BrClNOS",
                "C9H34O3S",
                "C5H35O6P",
                "C4H29N7OP",
                "H46ClOP3S",
                "C2H40NO3S3",
                "H40N4P2S2",
                "C2H42N2S4",
                "C3H37N4P3",
                "C5H37NO3PS",
                "C8H26N6O",
                "C3H37BrN5",
                "C3H43BrO2S",
                "C4H37N3PS2",
                "C6H37ClNO2S",
                "C4H157O",
                "CH40N2O3P2S",
                "C2H34N6OS2",
                "CH36Cl2N4O3",
                "CH37ClFeN6",
                "C2H44Fe2NO2",
                "CH47FeOP3",
                "C8H34N2S2",
                "C2H40ClN2O2PS",
                "CH47BrFeNO",
                "H36ClN4O4P",
                "H37FeN6OP",
                "H160NOP",
                "C2H47ClFeP2",
                "CH36NO8S",
                "C3H40Cl2N2OS",
                "H30N8O3S",
                "CH160ClN",
                "C2H26N10O2",
                "CH44Cl2O2S2",
                "C3H32N3O7",
                "H44FeN2O2S2",
                "C4H33ClN3O4",
                "H43ClO5S2",
                "C4H34FeN5O",
                "C9H36OP2",
                "H51Fe2PS",
                "H44ClO3PS2",
                "H45FeN2PS2",
                "C3H33N3O5P",
                "C2H27N10P",
                "H38N4O2S3",
                "C10H36ClP",
                "H43Cl3N3S"
        };


        final PeriodicTable table = PeriodicTable.getInstance();
        final TableSelection tableSelection = TableSelection.fromString(table, "CHNOPSFeClBr");
        Set<MolecularFormula> searchedFormula1 = new HashSet<MolecularFormula>();
        for (int i = 0; i < result1.length; i++) {
            searchedFormula1.add(tableSelection.parse(result1[i]));
        }
        Set<MolecularFormula> searchedFormula2 = new HashSet<MolecularFormula>();
        for (int i = 0; i < result2.length; i++) {
            searchedFormula2.add(tableSelection.parse(result2[i]));
        }
        Set<MolecularFormula> searchedFormula3 = new HashSet<MolecularFormula>();
        for (int i = 0; i < result3.length; i++) {
            searchedFormula3.add(tableSelection.parse(result3[i]));
        }


        double mass = 279.43;
        final Deviation dev = new Deviation(20, 0.001);
        ChemicalAlphabet alphabet = new ChemicalAlphabet();
        MassDecomposer<Element> decomposer = new MassDecomposer<Element>(new ChemicalAlphabetWrapper(alphabet));


        Map<Element, Interval> boundary = new HashMap<Element, Interval>();
        boundary.put(table.getByName("C"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("H"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("N"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("O"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("P"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("S"), new Interval(0, Integer.MAX_VALUE));

        List<int[]> compomers = decomposer.decompose(mass, dev, boundary);
        List<MolecularFormula> formulas = new ArrayList<MolecularFormula>(compomers.size());
        for (int[] c : compomers) {
            formulas.add(alphabet.decompositionToFormula(c));
        }

        assertTrue("result1[] and formulas-List differ in length", result1.length == formulas.size());
        for (MolecularFormula formula : formulas) {
            assertTrue(formula + " was not found", searchedFormula1.contains(formula));
        }


        //with more Elements
        boundary = new HashMap<Element, Interval>();
        boundary.put(table.getByName("C"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("H"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("N"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("O"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("P"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("S"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("Fe"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("Cl"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("Br"), new Interval(0, Integer.MAX_VALUE));

        alphabet = new ChemicalAlphabet(tableSelection, table.getAllByName("C", "H", "N", "O", "P", "S", "Fe", "Cl", "Br"));
        decomposer = new MassDecomposer<Element>(new ChemicalAlphabetWrapper(alphabet));

        mass = 222.22;
        compomers = decomposer.decompose(mass, dev, boundary);
        formulas = new ArrayList<MolecularFormula>(compomers.size());
        for (int[] c : compomers) {
            formulas.add(alphabet.decompositionToFormula(c));
        }

        for (String x : result3) {
            final MolecularFormula f = MolecularFormula.parseOrThrow(x);
            if (!formulas.contains(f)) {
                System.out.println(f + " with mass " + f.getMass() + " is missing!");
            }
        }

        assertEquals("result3[] and formulas-List differ in length", result3.length, formulas.size());
        for (MolecularFormula formula : formulas) {
            assertTrue(formula + " was not found", searchedFormula3.contains(formula));
        }


        //test MassDecomposerFast
        //getting same results?
        mass = 279.43;
        alphabet = new ChemicalAlphabet();
        RangeMassDecomposer<Element> decomposerFast = new RangeMassDecomposer<Element>(new ChemicalAlphabetWrapper(alphabet));


        boundary = new HashMap<Element, Interval>();
        boundary.put(table.getByName("C"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("H"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("N"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("O"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("P"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("S"), new Interval(0, Integer.MAX_VALUE));

        compomers = decomposerFast.decompose(mass, dev, boundary);
        formulas = new ArrayList<MolecularFormula>(compomers.size());
        for (int[] c : compomers) {
            formulas.add(alphabet.decompositionToFormula(c));
        }


        assertTrue("result1[] and formulas-List differ in length", result1.length == formulas.size());
        for (MolecularFormula formula : formulas) {
            assertTrue(formula + " was not found", searchedFormula1.contains(formula));
        }

        // test iterator
        {
            DecompIterator<Element> elementDecompIterator = decomposerFast.decomposeIterator(mass, dev, boundary);
            final HashSet<MolecularFormula> tosearchin = new HashSet<>(searchedFormula1);
            while (elementDecompIterator.next())
                assertTrue(tosearchin.remove(alphabet.decompositionToFormula(elementDecompIterator.getCurrentCompomere())));
            assertTrue(tosearchin.isEmpty());
        }

        // test mf iterator
        {
            MassToFormulaDecomposer mf = new MassToFormulaDecomposer(alphabet);
            Iterator<MolecularFormula> molecularFormulaIterator = mf.neutralMassFormulaIterator(222.1, dev, new FormulaConstraints("CHNOPS"));
            final HashSet<MolecularFormula> tosearchin = new HashSet<>(mf.decomposeNeutralMassToFormulas(222.1, dev, new FormulaConstraints("CHNOPS")));
            while (molecularFormulaIterator.hasNext())
                assertTrue(tosearchin.remove(molecularFormulaIterator.next()));
            assertTrue(tosearchin.isEmpty());
        }


        //with more Elements
        boundary = new HashMap<Element, Interval>();
        boundary.put(table.getByName("C"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("H"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("N"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("O"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("P"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("S"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("Fe"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("Cl"), new Interval(0, Integer.MAX_VALUE));
        boundary.put(table.getByName("Br"), new Interval(0, Integer.MAX_VALUE));

        alphabet = new ChemicalAlphabet(tableSelection, table.getAllByName("C", "H","N","O","P","S","Fe","Cl", "Br"));
        decomposerFast = new RangeMassDecomposer<Element>(new ChemicalAlphabetWrapper(alphabet));

        mass = 222.22;
        compomers = decomposerFast.decompose(mass, dev, boundary);
        formulas = new ArrayList<MolecularFormula>(compomers.size());
        for (int[] c : compomers) {
            formulas.add(alphabet.decompositionToFormula(c));
        }

        assertTrue("result3[] and formulas-List differ in length", result3.length == formulas.size());
        for (MolecularFormula formula : formulas) {
            assertTrue(formula+" was not found", searchedFormula3.contains(formula));
        }

    }

    @Test
    public void singleElementTest(){
        final PeriodicTable table = PeriodicTable.getInstance();
        final TableSelection tableSelection = TableSelection.fromString(table, "CHNOPS");
        final MolecularFormula mf = MolecularFormula.parseOrThrow("C16");
        final double mass = mf.getMass();
        final Deviation dev = new Deviation(100, 0.001);
        ChemicalAlphabet alphabet = new ChemicalAlphabet(tableSelection, table.getAllByName("C"));
        MassDecomposer<Element> decomposer = new RangeMassDecomposer<>(new ChemicalAlphabetWrapper(alphabet));

        Map<Element, Interval> boundary = new HashMap<Element, Interval>();
        boundary.put(table.getByName("C"), new Interval(0, Integer.MAX_VALUE));

        List<int[]> compomers = decomposer.decompose(mass, dev, boundary);

        List<MolecularFormula> formulas = new ArrayList<MolecularFormula>(compomers.size());
        for (int[] c : compomers) {
            formulas.add(alphabet.decompositionToFormula(c));
        }

        assertEquals(mf, formulas.get(0));
    }
}
