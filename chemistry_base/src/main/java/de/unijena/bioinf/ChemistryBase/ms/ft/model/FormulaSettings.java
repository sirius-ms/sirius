package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bioinf.ms.properties.PropertyManager;

import java.util.Arrays;
import java.util.HashSet;
/**
 * This class holds the information how to autodetect elements based on the given FormulaConstraints
 * Note: during Validation this is compared to the molecular formula an may be changed
 *
 *
 * */
@DefaultProperty
public class FormulaSettings implements Ms2ExperimentAnnotation {
    public static FormulaSettings DEFAULT = PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSettings.class);

    private final HashSet<Element> autoDetectionElements;
    private final boolean allowIsotopeElementFiltering;

    public HashSet<Element> getAutoDetectionElements() {
        return autoDetectionElements;
    }

    public boolean isAllowIsotopeElementFiltering() {
        return allowIsotopeElementFiltering;
    }

    public boolean isElementDetectionEnabled() {
        return autoDetectionElements.size() > 0;
    }

    public static FormulaSettings create(boolean allowIsotopeElementFiltering, String... autoDetect) {
        final HashSet<Element> set = new HashSet<>();
        final PeriodicTable T = PeriodicTable.getInstance();
        for (String e : autoDetect) set.add(T.getByName(e));
        return new FormulaSettings(set, allowIsotopeElementFiltering);
    }

    public static FormulaSettings create(boolean allowIsotopeElementFiltering, Element... autoDetect) {
        final HashSet<Element> set = new HashSet<>();
        for (Element e : autoDetect) set.add(e);
        return new FormulaSettings(set, allowIsotopeElementFiltering);
    }

    protected FormulaSettings(HashSet<Element> automaticDetectionEnabled, boolean allowIsotopeElementFiltering) {
        this.autoDetectionElements = automaticDetectionEnabled;
        this.allowIsotopeElementFiltering = allowIsotopeElementFiltering;
    }

    private FormulaSettings() {
        this(null, true);
    }

    public FormulaSettings autoDetect(Element... elems) {
        final HashSet<Element> set = (HashSet<Element>) autoDetectionElements.clone();
        set.addAll(Arrays.asList(elems));
        return new FormulaSettings(set, allowIsotopeElementFiltering);
    }

    public FormulaSettings withoutAutoDetect() {
        return new FormulaSettings(new HashSet<Element>(), allowIsotopeElementFiltering);
    }

    public FormulaSettings withIsotopeFormulaFiltering() {
        return new FormulaSettings(autoDetectionElements, true);
    }

    public FormulaSettings withoutIsotopeFormulaFiltering() {
        return new FormulaSettings(autoDetectionElements, false);
    }
}
