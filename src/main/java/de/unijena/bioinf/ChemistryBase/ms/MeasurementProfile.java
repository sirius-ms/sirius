package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;

/**
 * The information in this profile should represent properties of the instrument, the measurement and the quality
 * of the data. It should not contains details or parameters of the scorings.
 * For example: meanNoiseIntensity may be an attribute in this interface. The derived parameter lambda for
 * noise probability is NO attribute of this interface!
 * In practice, instances of this class could be provided by a factory. e.g. ProfileFactory.create(Instrument.TOF, Quality.HIGH);
 * or modified by the user (e.g. myProfile.setExpectedFragmentMassDeviation(20) to consider a unexpected low quality of MS2 spectra).
 */
public interface MeasurementProfile {

    public Deviation getExpectedIonMassDeviation();

    public Deviation getExpectedMassDifferenceDeviation();

    public Deviation getExpectedFragmentMassDeviation();

    public FormulaConstraints getFormulaConstraints();

}
