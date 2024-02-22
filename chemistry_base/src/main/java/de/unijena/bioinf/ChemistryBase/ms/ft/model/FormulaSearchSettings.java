package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * These settings define the behaviour of de novo and bottom-up molecular formula generation.
 * Candidate formulas from database or user input are handled independently via {@link CandidateFormulas}.
 * Candidate formulas from input files are always prioritized. An (internal) parameter exist to override this.
 */
public class FormulaSearchSettings implements Ms2ExperimentAnnotation {

    /**
     * min mass from which to perform bottom-up search (Infinity to disable)
     */
    public final double enableBottomUpFromMass;


    /**
     * max mass until which to use de novo decompotion (0 to disable)
     */
    public final double disableDeNovoAboveMass;

    /**
     * if true, formula candidates from inputfiles are always prioritized and any other candidates from database or de novo generation ignored.
     * For now, this is an internal paramter and always true.
     */
    public final boolean prioritizeAndForceCandidatesFromInputFiles;

    /**
     * force use of all neutral formulas that are provided by user, database or input file through {@link CandidateFormulas}
     * independent of the mass deviation to the measured mass
     */
    public final boolean ignoreMassDeviationForCandidateList; //this is currently not set. I prepared this so it is easy to add/force user-specified formula lists for bad-quality features -> e.g. for checking want went wrong

    /**
     * apply {@link de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints} (chemical alphabet / element filter, and filters such as {@link de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter}) to formulas from bottum-up search
     */
    public final boolean applyFormulaContraintsToBottomUp; //todo ElementFilter: should we name it formulaConstraints (the class we apply internally) or elementFilter (the way we communicate it to the outside)

    /**
     * apply {@link de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints} (chemical alphabet / element filter, and filters such as {@link de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter}) to candiate lists provided by the user or database search through {@link CandidateFormulas}
     */
    public final boolean applyFormulaContraintsToCandidateLists; //todo ElementFilter: see above

    protected final static FormulaSearchSettings
            BOTTOM_UP_ONLY = new FormulaSearchSettings(0,0, false, false, true, false),
            DENOVO_FOR_LOW_MASSES = new FormulaSearchSettings(0,400d, false, false,true, false),
            DENOVO_ONLY = new FormulaSearchSettings(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, false, false, true, false);

    public static FormulaSearchSettings deNovoOnly() {
        return DENOVO_ONLY;
    }

    public static FormulaSearchSettings bottomUpOnly() {
        return BOTTOM_UP_ONLY;
    }

    @DefaultInstanceProvider
    public static FormulaSearchSettings newInstance(@DefaultProperty(propertyKey = "enableBottomUpFromMass") double enableBottomUpFromMass, @DefaultProperty(propertyKey = "disableDeNovoAboveMass") double disableDeNovoAboveMass, @DefaultProperty(propertyKey = "applyFormulaContraintsToBottomUp") boolean applyFormulaContraintsToBottomUp, @DefaultProperty(propertyKey = "applyFormulaContraintsToCandidateLists") boolean applyFormulaContraintsToCandidateLists) {
        return new FormulaSearchSettings(enableBottomUpFromMass, disableDeNovoAboveMass, applyFormulaContraintsToBottomUp, applyFormulaContraintsToCandidateLists);
    }

    public FormulaSearchSettings(double enableBottomUpFromMass, double disableDeNovoAboveMass, boolean  applyFormulaContraintsToBottomUp, boolean applyFormulaContraintsToCandidateLists) {
        this(enableBottomUpFromMass, disableDeNovoAboveMass, applyFormulaContraintsToBottomUp, applyFormulaContraintsToCandidateLists, true, false);
    }

    private FormulaSearchSettings(double enableBottomUpFromMass, double disableDeNovoAboveMass, boolean applyFormulaContraintsToBottomUp, boolean applyFormulaContraintsToCandidateLists, boolean prioritizeAndForceCandidatesFromInputFiles, boolean ignoreMassDeviationForCandidateList) {
        this.enableBottomUpFromMass = enableBottomUpFromMass;
        this.disableDeNovoAboveMass = disableDeNovoAboveMass;
        this.prioritizeAndForceCandidatesFromInputFiles = prioritizeAndForceCandidatesFromInputFiles;
        this.applyFormulaContraintsToBottomUp = applyFormulaContraintsToBottomUp;
        this.applyFormulaContraintsToCandidateLists = applyFormulaContraintsToCandidateLists;
        this.ignoreMassDeviationForCandidateList = ignoreMassDeviationForCandidateList;
    }

    public boolean useBottomUpFor(double ionMass) {
        return ionMass > enableBottomUpFromMass;
    }

    public boolean useDeNovoFor(double mass) {
        return mass <= disableDeNovoAboveMass;
    }

    public boolean useDeNovoOrBottomUpFor(double mass) {
        return useDeNovoFor(mass) || useBottomUpFor(mass);
    }

    public boolean isPrioritizeAndForceCandidatesFromInputFiles() {
        return prioritizeAndForceCandidatesFromInputFiles;
    }
}
