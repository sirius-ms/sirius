package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

import java.util.Arrays;
import java.util.HashSet;
/**
 * This class holds the formula constraints and detected elements
 * Note: during Validation this is compared to the molecular formular an may be changed
 *
 *
 * */
public class FormulaSettings implements Ms2ExperimentAnnotation {

    private final FormulaConstraints constraints;
    private final HashSet<Element> automaticDetectionEnabled;
    private final boolean allowIsotopeElementFiltering;


    public static FormulaSettings defaultWithMs1() {
        return create(new FormulaConstraints("CHNOP[5]SFI[5]BrCl"), true, "S", "Br", "Cl", "B", "Se");
    }
    public static FormulaSettings defaultWithMs2Only() {
        return create(new FormulaConstraints("CHNOP[5]S"), true, "S", "Br", "Cl", "B", "Se");
    }

    public FormulaConstraints getConstraints() {
        return constraints;
    }

    public HashSet<Element> getAutomaticDetectionEnabled() {
        return automaticDetectionEnabled;
    }

    public boolean isAllowIsotopeElementFiltering() {
        return allowIsotopeElementFiltering;
    }

    public boolean isElementDetectionEnabled() {
        return automaticDetectionEnabled.size()>0;
    }

    public static FormulaSettings create(FormulaConstraints constraints, boolean allowIsotopeElementFiltering, String... autoDetect) {
        final HashSet<Element> set = new HashSet<>();
        final PeriodicTable T = PeriodicTable.getInstance();
        for (String e : autoDetect) set.add(T.getByName(e));
        return new FormulaSettings(constraints, set, allowIsotopeElementFiltering);
    }

    public static FormulaSettings create(FormulaConstraints constraints, boolean allowIsotopeElementFiltering, Element... autoDetect) {
        final HashSet<Element> set = new HashSet<>();
        for (Element e : autoDetect) set.add(e);
        return new FormulaSettings(constraints, set, allowIsotopeElementFiltering);
    }

    protected FormulaSettings(FormulaConstraints constraints, HashSet<Element> automaticDetectionEnabled, boolean allowIsotopeElementFiltering) {
        this.constraints = constraints;
        this.automaticDetectionEnabled = automaticDetectionEnabled;
        this.allowIsotopeElementFiltering = allowIsotopeElementFiltering;
    }

    public FormulaSettings withAdditionalConstraints(FormulaConstraints constraints) {
        return new FormulaSettings(this.constraints.getExtendedConstraints(constraints), automaticDetectionEnabled, allowIsotopeElementFiltering);
    }

    public FormulaSettings withConstraints(FormulaConstraints constraints) {
        return new FormulaSettings(constraints, automaticDetectionEnabled, allowIsotopeElementFiltering);
    }

    public FormulaSettings autoDetect(Element... elems) {
        final HashSet<Element> set = (HashSet<Element>) automaticDetectionEnabled.clone();
        set.addAll(Arrays.asList(elems));
        return new FormulaSettings(constraints, set, allowIsotopeElementFiltering);
    }

    public FormulaSettings withoutAutoDetect() {
        return new FormulaSettings(constraints, new HashSet<Element>(), allowIsotopeElementFiltering);
    }

    public FormulaSettings withIsotopeFormulaFiltering() {
        return new FormulaSettings(constraints, automaticDetectionEnabled, true);
    }

    public FormulaSettings withoutIsotopeFormulaFiltering() {
        return new FormulaSettings(constraints, automaticDetectionEnabled, false);
    }
}
