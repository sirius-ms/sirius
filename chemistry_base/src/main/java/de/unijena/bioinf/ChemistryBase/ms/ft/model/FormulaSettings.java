package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * This configurations hold the information how to autodetect elements based on the given formula constraints.
 * Note: If the compound is already assigned to a specific molecular formula, this annotation is ignored.
 */
public class FormulaSettings implements Ms2ExperimentAnnotation {
    @NotNull protected final FormulaConstraints enforced;
    @NotNull protected final FormulaConstraints fallback;
    @NotNull protected final ChemicalAlphabet detectable;

    /**
     * @param enforced   Enforced elements are always considered
     * @param detectable Detectable elements are added to the chemical alphabet, if there are indications for them (e.g. in isotope pattern)
     * @param fallback   Fallback elements are used, if the autodetection fails (e.g. no isotope pattern available)
     */
    @DefaultInstanceProvider
    public static FormulaSettings newInstance(@DefaultProperty FormulaConstraints enforced, @DefaultProperty ChemicalAlphabet detectable, @DefaultProperty FormulaConstraints fallback) {
        return new FormulaSettings(enforced, detectable, fallback);
    }

    public FormulaSettings(@NotNull FormulaConstraints enforced, @NotNull ChemicalAlphabet detectable, @NotNull FormulaConstraints fallback) {
        this.enforced = enforced;
        this.fallback = fallback;
        this.detectable = detectable;
    }

    public FormulaConstraints getEnforcedAlphabet() {
        return enforced;
    }

    public FormulaConstraints getFallbackAlphabet() {
        return fallback;
    }

    @NotNull public Set<Element> getAutoDetectionElements() {
        return detectable.toSet();
    }


    public boolean isElementDetectionEnabled() {
        return detectable.size() > 0;
    }

    @NotNull public FormulaSettings autoDetect(Element... elems) {
        return new FormulaSettings(enforced,detectable.extend(elems),fallback);
    }

    @NotNull public FormulaSettings withoutAutoDetect() {
        return new FormulaSettings(enforced, new ChemicalAlphabet(new Element[0]), fallback);
    }

    public ChemicalAlphabet getAutoDetectionAlphabet() {
        return detectable;
    }

    public FormulaSettings enforce(FormulaConstraints constraints) {
        return new FormulaSettings(constraints, detectable, fallback);
    }

    public FormulaSettings withFallback(FormulaConstraints constraints) {
        return new FormulaSettings(enforced, detectable, constraints);
    }
}
