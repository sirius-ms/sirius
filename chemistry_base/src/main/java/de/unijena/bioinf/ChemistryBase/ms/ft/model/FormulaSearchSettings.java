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
    public final double performBottomUpAboveMz;


    /**
     * max mass until which to use de novo decompotion (0 to disable)
     */
    public final double performDeNovoBelowMz;

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
     * apply chemical alphabet / element filter specified via {@link de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints} to formulas from bottum-up search.
     * Filters such as {@link de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter}) are always applied
     */
    public final boolean applyFormulaConstraintsToBottomUp;

    /**
     * apply {@link de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints} (chemical alphabet / element filter, and filters such as {@link de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter}) to candiate lists provided by the user or database search through {@link CandidateFormulas}
     */
    public final boolean applyFormulaConstraintsToDatabaseCandidates;

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
    public static FormulaSearchSettings newInstance(@DefaultProperty(propertyKey = "performBottomUpAboveMz") double performBottomUpAboveMz, @DefaultProperty(propertyKey = "performDeNovoBelowMz") double performDeNovoBelowMz, @DefaultProperty(propertyKey = "applyFormulaConstraintsToBottomUp") boolean applyFormulaConstraintsToBottomUp, @DefaultProperty(propertyKey = "applyFormulaConstraintsToDatabaseCandidates") boolean applyFormulaConstraintsToDatabaseCandidates) {
        return new FormulaSearchSettings(performBottomUpAboveMz, performDeNovoBelowMz, applyFormulaConstraintsToBottomUp, applyFormulaConstraintsToDatabaseCandidates);
    }

    public FormulaSearchSettings(double performBottomUpAboveMz, double performDeNovoBelowMz, boolean  applyFormulaConstraintsToBottomUp, boolean applyFormulaConstraintsToDatabaseCandidates) {
        this(performBottomUpAboveMz, performDeNovoBelowMz, applyFormulaConstraintsToBottomUp, applyFormulaConstraintsToDatabaseCandidates, true, false);
    }

    private FormulaSearchSettings(double performBottomUpAboveMz, double performDeNovoBelowMz, boolean applyFormulaConstraintsToBottomUp, boolean applyFormulaConstraintsToDatabaseCandidates, boolean prioritizeAndForceCandidatesFromInputFiles, boolean ignoreMassDeviationForCandidateList) {
        this.performBottomUpAboveMz = performBottomUpAboveMz;
        this.performDeNovoBelowMz = performDeNovoBelowMz;
        this.prioritizeAndForceCandidatesFromInputFiles = prioritizeAndForceCandidatesFromInputFiles;
        this.applyFormulaConstraintsToBottomUp = applyFormulaConstraintsToBottomUp;
        this.applyFormulaConstraintsToDatabaseCandidates = applyFormulaConstraintsToDatabaseCandidates;
        this.ignoreMassDeviationForCandidateList = ignoreMassDeviationForCandidateList;
    }

    public boolean useBottomUpFor(double ionMass) {
        return ionMass > performBottomUpAboveMz;
    }

    public boolean useDeNovoFor(double mass) {
        return mass <= performDeNovoBelowMz;
    }

    public boolean useDeNovoOrBottomUpFor(double mass) {
        return useDeNovoFor(mass) || useBottomUpFor(mass);
    }

    public boolean isPrioritizeAndForceCandidatesFromInputFiles() {
        return prioritizeAndForceCandidatesFromInputFiles;
    }
}
