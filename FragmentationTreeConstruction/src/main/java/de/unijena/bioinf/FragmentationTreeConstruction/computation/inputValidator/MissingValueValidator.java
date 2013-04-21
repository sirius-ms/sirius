package de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProfileImpl;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 19.04.13
 * Time: 18:11
 * To change this template use File | Settings | File Templates.
 */
public class MissingValueValidator implements InputValidator {


    private boolean validDouble(double val, boolean mayNegative) {
        return !Double.isInfinite(val) && !Double.isNaN(val) && (mayNegative || val >= 0d);
    }

    @Override
    public Ms2Experiment validate(Ms2Experiment originalInput,Warning warn, boolean repair) throws InvalidException {
        final Ms2ExperimentImpl input = new Ms2ExperimentImpl(originalInput);
        if (input.getMs2Spectra() == null || input.getMs2Spectra().isEmpty()) throw new InvalidException("Miss MS2 spectra");
        if (input.getMs1Spectra() == null) input.setMs1Spectra(new ArrayList<Spectrum<Peak>>());
        removeEmptySpectra(warn, input);
        checkIonization(warn, repair, input);
        checkMergedMs1(warn, repair, input);
        checkIonMass(warn, repair, input);
        checkNeutralMass(warn, repair, input);
        checkMeasurementProfile(warn, repair, input);
        return input;
    }

    protected void checkMeasurementProfile(Warning warn, boolean repair, Ms2ExperimentImpl input) {
        if (input.getMeasurementProfile() == null) throw new InvalidException("Measurement profile is missing");
        final ProfileImpl profile = new ProfileImpl(input.getMeasurementProfile());
        if (profile.getChemicalAlphabet() == null) {
            throwOrWarn(warn, repair, "Measurement profile: Chemical alphabet is missing");
            profile.setChemicalAlphabet(new ChemicalAlphabet());
        }
        // get at least one deviation
        Deviation dev = profile.getExpectedIonMassDeviation();
        if (dev == null) dev = profile.getExpectedFragmentMassDeviation();
        if (profile.getExpectedFragmentMassDeviation() == null) {
            throwOrWarn(warn, repair && dev!=null, "Measurement profile: Fragment Mass deviation is missing");
            profile.setExpectedFragmentMassDeviation(dev);
        }
        if (profile.getExpectedIonMassDeviation() == null) {
            throwOrWarn(warn, repair && dev!=null, "Measurement profile: Ion Mass deviation is missing");
            profile.setExpectedIonMassDeviation(dev);
        }
        input.setMeasurementProfile(profile);
    }

    protected void checkNeutralMass(Warning warn, boolean repair, Ms2ExperimentImpl input) {
        if (input.getMoleculeNeutralMass() == 0 || !validDouble(input.getMoleculeNeutralMass(), false)) {
            throwOrWarn(warn, repair, "Neutral mass is missing");
            if (input.getMolecularFormula() != null) {
                input.setMoleculeNeutralMass(input.getMolecularFormula().getMass());
            } else {
                input.setMoleculeNeutralMass(input.getIonization().subtractFromMass(input.getIonMass()));
            }
        }
    }

    protected void removeEmptySpectra(Warning warn, Ms2ExperimentImpl input) {
        final Iterator<Ms2Spectrum> iter = input.getMs2Spectra().iterator();
        while (iter.hasNext()) {
            final Ms2Spectrum spec = iter.next();
            if (spec.size()==0) {
                warn.warn("Empty Spectrum at collision energy: " + spec.getCollisionEnergy());
                iter.remove();
            }
        }
    }

    protected void checkIonization(Warning warn, boolean repair, Ms2ExperimentImpl input) {
        if (input.getIonization() == null) {
            throwOrWarn(warn, repair, "No ionization is given");
            if (validDouble(input.getIonMass(), false) && validDouble(input.getMoleculeNeutralMass(), false) ) {
                final double modificationMass = input.getIonMass() - input.getMoleculeNeutralMass();
                final Ionization ion = PeriodicTable.getInstance().ionByMass(modificationMass, 2e-3);
                if (ion == null)  throw new InvalidException("Unknown adduct with mass " + modificationMass);
                input.setIonization(ion);
            } else throw new InvalidException("Unknown ionization");
        }
    }

    protected void checkMergedMs1(Warning warn, boolean repair, Ms2ExperimentImpl input) {
        if (input.getMergedMs1Spectrum() == null && !input.getMs1Spectra().isEmpty()) {
            warn.warn("No merged spectrum is given");
            if (repair) {
                if (input.getMs1Spectra().size()==1)
                    input.setMergedMs1Spectrum(input.getMs1Spectra().get(0));
            }
        }
    }

    protected void checkIonMass(Warning warn, boolean repair, Ms2ExperimentImpl input) {
        if (!validDouble(input.getIonMass(), false)) {
            throwOrWarn(warn, repair, "No ion mass is given");
            if (input.getMolecularFormula() != null) {
                final Spectrum<Peak> ms1 = input.getMergedMs1Spectrum();
                // maybe the ms2 spectra have a common precursor
                boolean found = true;
                double mz = input.getMs2Spectra().get(0).getPrecursorMz();
                for (Ms2Spectrum s : input.getMs2Spectra()) {
                    final double newMz = s.getPrecursorMz();
                    if (Math.abs(mz-newMz) > 1e-3) {
                        found = false; break;
                    }
                }
                if (found) {
                    input.setIonMass(mz);
                } else {
                    if (ms1 == null) {
                        // use the highest mass you find in the MS2 spectra with lowest collision energy as parent mass.
                        // (only if its intensity is higher than 10% and higher than peaks in its neighbourhood)
                        Ms2Spectrum spec = input.getMs2Spectra().get(0);
                        for (Ms2Spectrum spec2 : input.getMs2Spectra()) {
                            if (spec2.getCollisionEnergy().lowerThan(spec.getCollisionEnergy())) spec = spec2;
                        }
                        final Spectrum<Peak> normalized = Spectrums.getNormalizedSpectrum(spec, Normalization.Max(1d));
                        Peak parent = normalized.getPeakAt(Spectrums.getIndexOfPeakWithMaximalIntensity(normalized));
                        for (Peak p : normalized) if (p.getIntensity() > 0.1d) {
                            if (Math.abs(p.getMass() - parent.getMass()) < 1e-2) {
                                if (p.getIntensity() > parent.getIntensity()) parent=p;
                            } else if (p.getMass() > parent.getMass()) parent = p;
                        }
                        input.setIonMass(parent.getMass());
                    } else {
                        // take peak with highest intensity
                        int index = Spectrums.getIndexOfPeakWithMaximalIntensity(ms1);
                        // move backward, maybe you are in the middle of an isotope pattern
                        while (index > 0) {
                            if (Math.abs(ms1.getMzAt(index) - ms1.getMzAt(index-1)) > 1.1d) break;
                            --index;
                        }
                        // hopefully, this is the correct isotope peak
                        warn.warn("Predict ion mass from MS1: " + ms1.getMzAt(index));
                        input.setIonMass(ms1.getMzAt(index));
                    }
                }
            }
        }
    }

    protected void throwOrWarn(Warning warn, boolean repair, String message) {
        if (repair) warn.warn(message);
        else throw new InvalidException(message);
    }
}
