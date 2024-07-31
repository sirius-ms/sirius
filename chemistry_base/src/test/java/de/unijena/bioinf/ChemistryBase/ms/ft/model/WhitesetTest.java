package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import org.junit.jupiter.api.Test;


import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WhitesetTest {
    Set<MolecularFormula> measuredFormulas = getFormulaSet("C6H12O6", "C9H12NO", "C6H10O5", "C6H14O4", "C6H8N5");
    Set<MolecularFormula> neutralFormulas = getFormulaSet("C6H12O6", "C6H10O5","C6H14O7","C9H14NO2", "C12H34SO4", "C12H34F2O4", "C5H13N2P", "C4H16N4OS", "C8H14N3O");

    Whiteset whiteset = Whiteset.ofMeasuredFormulas(measuredFormulas, WhitesetTest.class).add(Whiteset.ofNeutralizedFormulas(neutralFormulas, WhitesetTest.class));

    @Test
    void filterByFormulaSet() {
        Set<MolecularFormula> filterSet = getFormulaSet("C6H12O6", "C12H32SO3");

        PossibleAdducts adducts;
        Whiteset filteredWhiteset;

        //filterByMeasuredFormulas
        //case 1
        adducts = new PossibleAdducts(ionType("[M+H]+"));
        filteredWhiteset = whiteset.filterByMeasuredFormulas(filterSet, adducts.getAdducts(), WhitesetTest.class);

        assertEquals(getFormulaSet("C6H12O6"), filteredWhiteset.getMeasuredFormulas());
        assertEquals(getFormulaSet("C6H12O6"), filteredWhiteset.getNeutralFormulas());

        //case 2
        adducts = new PossibleAdducts(ionType("[M+H-H2O]+"));
        filteredWhiteset = whiteset.filterByMeasuredFormulas(filterSet, adducts.getAdducts(), WhitesetTest.class);

        assertEquals(getFormulaSet("C6H12O6"), filteredWhiteset.getMeasuredFormulas());
        assertEquals(getFormulaSet("C6H14O7", "C12H34SO4"), filteredWhiteset.getNeutralFormulas());


        //case 3
        adducts = new PossibleAdducts(ionType("[M+H+H2O]+"));
        filteredWhiteset = whiteset.filterByMeasuredFormulas(filterSet, adducts.getAdducts(), WhitesetTest.class);

        assertEquals(getFormulaSet("C6H12O6"), filteredWhiteset.getMeasuredFormulas());
        assertEquals(getFormulaSet("C6H10O5"), filteredWhiteset.getNeutralFormulas());

        //filterByNeutralFormulas
        //case 1
        adducts = new PossibleAdducts(ionType("[M+H]+"));
        filteredWhiteset = whiteset.filterByNeutralFormulas(filterSet, adducts.getAdducts(), WhitesetTest.class);

        assertEquals(getFormulaSet("C6H12O6"), filteredWhiteset.getMeasuredFormulas());
        assertEquals(getFormulaSet("C6H12O6"), filteredWhiteset.getNeutralFormulas());

        //case 2
        adducts = new PossibleAdducts(ionType("[M+H-H2O]+"));
        filteredWhiteset = whiteset.filterByNeutralFormulas(filterSet, adducts.getAdducts(), WhitesetTest.class);

        assertEquals(getFormulaSet("C6H10O5"), filteredWhiteset.getMeasuredFormulas());
        assertEquals(getFormulaSet("C6H12O6"), filteredWhiteset.getNeutralFormulas());


        //case 3
        adducts = new PossibleAdducts(ionType("[M+H+H2O]+"));
        filteredWhiteset = whiteset.filterByNeutralFormulas(filterSet, adducts.getAdducts(), WhitesetTest.class);

        assertEquals(getFormulaSet(), filteredWhiteset.getMeasuredFormulas());
        assertEquals(getFormulaSet("C6H12O6"), filteredWhiteset.getNeutralFormulas());
    }

    @Test
    void filterFormulas() {
        PossibleAdducts adducts;
        Whiteset filteredWhiteset;
        FormulaConstraints formulaConstraints = FormulaConstraints.fromString("CHO[0-5]NFS").withNewFilters(Collections.emptyList());

        //case 1
        adducts = new PossibleAdducts(ionType("[M+H-H2O]+"));
        filteredWhiteset = whiteset.filter(formulaConstraints, adducts.getAdducts(), WhitesetTest.class);

        assertEquals(getFormulaSet("C9H12NO", "C6H14O4", "C6H8N5"), filteredWhiteset.getMeasuredFormulas());
        assertEquals(getFormulaSet("C6H10O5", "C9H14NO2", "C12H34SO4", "C12H34F2O4", "C8H14N3O", "C4H16N4OS"), filteredWhiteset.getNeutralFormulas());

        //case2
        adducts = new PossibleAdducts(ionType("[M+H+H2O]+"));
        filteredWhiteset = whiteset.filter(formulaConstraints, adducts.getAdducts(), WhitesetTest.class);

        assertEquals(getFormulaSet("C6H12O6", "C9H12NO", "C6H10O5", "C6H14O4"), filteredWhiteset.getMeasuredFormulas());
        assertEquals(getFormulaSet("C6H10O5", "C9H14NO2", "C12H34SO4", "C12H34F2O4", "C8H14N3O", "C4H16N4OS"), filteredWhiteset.getNeutralFormulas());
    }

    @Test
    void resolve() {
        //this is an important step in FragmentationPatternAnalysis and IsotopePatternAnalysis

        Deviation deviation = new Deviation(20);

        double parentMassWithError;
        PossibleAdducts adducts;
        Set<MolecularFormula> expectedSet, resolvedSet;

        parentMassWithError = ionType("[M+H]+").addIonAndAdduct(formula("C9H12NO").getMass())-0.001;

        //case 1
        adducts = new PossibleAdducts(ionType("[M+H]+"));
        expectedSet = getFormulaSet("C9H12NO", "C6H14O4");

        resolvedSet = whiteset.resolve(parentMassWithError, deviation, adducts.getAdducts()).stream().map(Decomposition::getCandidate).collect(Collectors.toSet());
        assertEquals(expectedSet, resolvedSet);

        //case 2
        adducts = new PossibleAdducts(ionType("[M+H+H2O]+"));
        expectedSet = getFormulaSet("C9H12NO", "C5H15N2OP", "C6H14O4");

        resolvedSet = whiteset.resolve(parentMassWithError, deviation, adducts.getAdducts()).stream().map(Decomposition::getCandidate).collect(Collectors.toSet());
        assertEquals(expectedSet, resolvedSet);

        //case 3
        adducts = new PossibleAdducts(ionType("[M+H+H2O]+"), ionType("[M+H-H2O]+"));
        expectedSet = getFormulaSet("C9H12NO", "C5H15N2OP", "C6H14O4", "C4H14N4S");

        resolvedSet = whiteset.resolve(parentMassWithError, deviation, adducts.getAdducts()).stream().map(Decomposition::getCandidate).collect(Collectors.toSet());
        assertEquals(expectedSet, resolvedSet);

        //case 4
        adducts = new PossibleAdducts(ionType("[M+H+H2O]+"), ionType("[M+H-H2O]+"), ionType("[M+NH4]+"), ionType("[M+K]+"), ionType("[M+Na]+"), ionType("[M+H+CH3]+"), ionType("[M+H-SO4]+"));
        expectedSet = getFormulaSet("C9H12NO", "C5H15N2OP", "C6H14O4", "C4H14N4S");

        resolvedSet = whiteset.resolve(parentMassWithError, deviation, adducts.getAdducts()).stream().map(Decomposition::getCandidate).collect(Collectors.toSet());
        assertEquals(expectedSet, resolvedSet);


        //negative ion mode
        parentMassWithError = ionType("[M-H]-").addIonAndAdduct(formula("C9H12NO").getMass())-0.001;

        //case 5
        adducts = new PossibleAdducts(ionType("[M-H]-"));
        expectedSet = getFormulaSet("C9H12NO", "C6H14O4");

        resolvedSet = whiteset.resolve(parentMassWithError, deviation, adducts.getAdducts()).stream().map(Decomposition::getCandidate).collect(Collectors.toSet());
        assertEquals(expectedSet, resolvedSet);

        //case 6
        adducts = new PossibleAdducts(ionType("[M-H]-"), ionType("[M-H2O-H]-"));
        expectedSet = getFormulaSet("C9H12NO", "C6H14O4", "C4H14N4S");

        resolvedSet = whiteset.resolve(parentMassWithError, deviation, adducts.getAdducts()).stream().map(Decomposition::getCandidate).collect(Collectors.toSet());
        assertEquals(expectedSet, resolvedSet);


        //"ignore" mass deviation (allow super large mass dev)
        whiteset = whiteset.setIgnoreMassDeviationToResolveIonType(true);

        //case 7
        adducts = new PossibleAdducts(ionType("[M-H]-"), ionType("[M-H2O-H]-"));
        expectedSet = getFormulaSet("C9H12NO", "C6H14O4", "C4H14N4S", "C6H8N5", "C8H12N3");


        resolvedSet = whiteset.resolve(parentMassWithError, deviation, adducts.getAdducts()).stream().map(Decomposition::getCandidate).collect(Collectors.toSet());
        assertEquals(expectedSet, resolvedSet);

    }

    @Test
    void containsMeasuredFormula() {
        PossibleAdducts adducts;

        //in measured
        assertEquals(true, whiteset.containsMeasuredFormula(formula("C9H12NO"), ionType("[M+H]+")));
        assertEquals(true, whiteset.containsMeasuredFormula(formula("C6H8N5"), ionType("[M+H+C34H300]+")));
        assertEquals(true, whiteset.containsMeasuredFormula(formula("C6H14O4"), ionType("[M+H+H2O]+")));
        assertEquals(true, whiteset.containsMeasuredFormula(formula("C6H14O4"), ionType("[M+H-H2O]+")));

        //in neutral
        assertEquals(true, whiteset.containsMeasuredFormula(formula("C12H34F2O4"), ionType("[M+H]+")));
        assertEquals(true, whiteset.containsMeasuredFormula(formula("C4H19N5OS"), ionType("[M+H+NH3]+")));

        //not contained
        assertEquals(false, whiteset.containsMeasuredFormula(formula("C9H12NO5"), ionType("[M+H]+")));
        assertEquals(false, whiteset.containsMeasuredFormula(formula("C4H16N4OS"), ionType("[M+H+H2O]+")));
        assertEquals(false, whiteset.containsMeasuredFormula(formula("C4H16N4OS"), ionType("[M+H-H2O]+")));
    }

    @Test
    void asMeasuredFormulas() {
        Set<MolecularFormula> measuredFormulas = getFormulaSet("C6H12O6", "C6H8N5", "C12H23O3N2S");
        Set<MolecularFormula> neutralFormulas = getFormulaSet("C6H12O6", "C12H34F2O4", "C4H16N4OS", "C8H14N3O", "C12H24");

        Whiteset whiteset = Whiteset.ofMeasuredFormulas(measuredFormulas, WhitesetTest.class).add(Whiteset.ofNeutralizedFormulas(neutralFormulas, WhitesetTest.class));

        PossibleAdducts adducts;
        Set<MolecularFormula> expectedSet;
        Whiteset measuredSet;

        //case 1
        adducts = new PossibleAdducts(ionType("[M-H2O-H]-"));
        expectedSet = getFormulaSet(
                "C6H12O6", "C6H8N5", "C12H23O3N2S",
                "C6H10O5", "C12H32F2O3", "C4H14N4S", "C8H12N3"
                );

        measuredSet = whiteset.asMeasuredFormulas(adducts.getAdducts());
        assertEquals(expectedSet, measuredSet.getMeasuredFormulas());
        assertEquals(Collections.emptySet(), measuredSet.getNeutralFormulas());

        //case 2
        adducts = new PossibleAdducts(ionType("[M-H2O-H]-"), ionType("[M+H+NH3]+"));
        expectedSet = getFormulaSet(
                "C6H12O6", "C6H8N5", "C12H23O3N2S",
                "C6H10O5", "C12H32F2O3", "C4H14N4S", "C8H12N3",
                "C6H15O6N", "C12H37F2O4N", "C4H19N5OS", "C8H17N4O", "C12H27N"
        );

        measuredSet = whiteset.asMeasuredFormulas(adducts.getAdducts());
        assertEquals(expectedSet, measuredSet.getMeasuredFormulas());
        assertEquals(Collections.emptySet(), measuredSet.getNeutralFormulas());

        //case 3
        adducts = new PossibleAdducts(ionType("[M-H2O-H]-"), ionType("[M+H+NH3]+"), ionType("[M+H]+"));
        expectedSet = getFormulaSet(
                "C6H12O6", "C6H8N5", "C12H23O3N2S",
                "C6H10O5", "C12H32F2O3", "C4H14N4S", "C8H12N3",
                "C6H15O6N", "C12H37F2O4N", "C4H19N5OS", "C8H17N4O", "C12H27N",
                "C6H12O6", "C12H34F2O4", "C4H16N4OS", "C8H14N3O", "C12H24"
        );

        measuredSet = whiteset.asMeasuredFormulas(adducts.getAdducts());
        assertEquals(expectedSet, measuredSet.getMeasuredFormulas());
        assertEquals(Collections.emptySet(), measuredSet.getNeutralFormulas());

        //case 4
        adducts = new PossibleAdducts(ionType("[M-H2O-H]-"), ionType("[M+H+NH3]+"), ionType("[M]+"));
        expectedSet = getFormulaSet(
                "C6H12O6", "C6H8N5", "C12H23O3N2S",
                "C6H10O5", "C12H32F2O3", "C4H14N4S", "C8H12N3",
                "C6H15O6N", "C12H37F2O4N", "C4H19N5OS", "C8H17N4O", "C12H27N",
                "C6H12O6", "C12H34F2O4", "C4H16N4OS", "C8H14N3O", "C12H24"
        );

        measuredSet = whiteset.asMeasuredFormulas(adducts.getAdducts());
        assertEquals(expectedSet, measuredSet.getMeasuredFormulas());
        assertEquals(Collections.emptySet(), measuredSet.getNeutralFormulas());

        //case 5
        adducts = new PossibleAdducts(ionType("[M-H2O-H]-"), ionType("[M+H+NH3]+"), ionType("[M]-"));
        expectedSet = getFormulaSet(
                "C6H12O6", "C6H8N5", "C12H23O3N2S",
                "C6H10O5", "C12H32F2O3", "C4H14N4S", "C8H12N3",
                "C6H15O6N", "C12H37F2O4N", "C4H19N5OS", "C8H17N4O", "C12H27N",
                "C6H12O6", "C12H34F2O4", "C4H16N4OS", "C8H14N3O", "C12H24"
        );

        measuredSet = whiteset.asMeasuredFormulas(adducts.getAdducts());
        assertEquals(expectedSet, measuredSet.getMeasuredFormulas());
        assertEquals(Collections.emptySet(), measuredSet.getNeutralFormulas());
    }



    private static Set<MolecularFormula> getFormulaSet(String... formulaStrings) {
        Set<MolecularFormula> set = new HashSet<>();
        Arrays.stream(formulaStrings).forEach(f -> set.add(formula(f)));
        return set;
    }

    private static void addFormulas(Set<MolecularFormula> set, String... formulaStrings) {
        Arrays.stream(formulaStrings).forEach(f -> set.add(formula(f)));
    }

    private static MolecularFormula formula(String formulaString) {
        return MolecularFormula.parseOrThrow(formulaString);
    }

    private static PrecursorIonType ionType(String ionTypeString) {
        return PrecursorIonType.getPrecursorIonType(ionTypeString);
    }
}