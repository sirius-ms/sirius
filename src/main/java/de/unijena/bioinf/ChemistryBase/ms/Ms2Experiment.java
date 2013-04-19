package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.List;

/**
 * A Ms2Experiment is a MS/MS measurement of a *single* compound. If there are multiple compounds measured in your
 * spectrum, clean up and separate them into multiple Ms2Experiment instances, too!
 */
public interface Ms2Experiment {

    /**
     * @return a list of MS2 spectra belonging to this compound
     */
    public List<Ms2Spectrum> getMs2Spectra();

    /**
     * Notes:
     * - If the data is preprocessed, then there should be a *clean* and single isotope pattern of the
     * ion in each ms1 spectrum. Further peaks are not allowed!
     * @return a list of MS1 spectra with the isotope pattern of this compound
     */
    public List<Spectrum<Peak>> getMs1Spectra();

    /**
     * In practice it seems more accurate to merge all MS1 spectra into a single one and only use this for further
     * analysis. Some tools provide a very accurate peak picking and merging, such that this is something which
     * should not be done in SIRIUS itself.
     * @return merge all ms1 spectra to single one
     */
    public Spectrum<Peak> getMergedMs1Spectrum();

    /**
     * @return the mass-to-charge ratio of the ion to analyze
     */
    public double getIonMass();

    /**
     * Preprocessing means:
     * - pick all isotope patterns from MS1 and delete peaks which do not belong to the isotope pattern of the ion
     * - if there are multiple-charge ions, the peaks have to be neutralized. Thats easier than considering this
     *   special and very unusual case in further steps of the analysis.
     * @return
     */
    public boolean isPreprocessed();

    /**
     * @return the measurement profile which contains information about the expected measurement quality
     */
    public MeasurementProfile getMeasurementProfile();


    /***
     * The further methods provide information which is OPTIONAL. The algorithm should be able to handle cases in
     * which this methods return NULL.
     */

    /**
     * The neutral mass is the mass of the molecule. In contrast the ion mass is the mass of the molecule + ion adduct.
     * Notice that the molecule may be also charged. That doesn't matter. Neutral says nothing about the charge, but
     * about the absence of an ionization.
     * @return the *exact* (idealized) mass of the molecule or 0 if the mass is unknown
     */
    public double getMoleculeNeutralMass();

    /**
     * @return molecular formula of the neutral molecule
     */
    public MolecularFormula getMolecularFormula();

    /**
     * @return the ionization type of the ion or null if this type is unknown
     */
    public Ionization getIonization();

}
