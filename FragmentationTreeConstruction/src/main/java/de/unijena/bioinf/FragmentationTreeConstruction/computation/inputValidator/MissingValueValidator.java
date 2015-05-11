/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2SpectrumImpl;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 19.04.13
 * Time: 18:11
 * To change this template use File | Settings | File Templates.
 */
public class MissingValueValidator implements InputValidator {


    private boolean validDouble(double val, boolean mayNegative) {
        return !Double.isInfinite(val) && !Double.isNaN(val) && (mayNegative || val > 0d);
    }

    @Override
    public Ms2Experiment validate(Ms2Experiment originalInput, Warning warn, boolean repair) throws InvalidException {
        final Ms2ExperimentImpl input = new Ms2ExperimentImpl(originalInput);
        if (input.getMs2Spectra() == null || input.getMs2Spectra().isEmpty())
            throw new InvalidException("Miss MS2 spectra");
        if (input.getMs1Spectra() == null) input.setMs1Spectra(new ArrayList<Spectrum<Peak>>());
        checkMeasurementProfile(warn, repair, input);
        removeEmptySpectra(warn, input);
        checkIonization(warn, repair, input);
        checkMergedMs1(warn, repair, input);
        checkIonMass(warn, repair, input);
        checkNeutralMass(warn, repair, input);
        return input;
    }

    protected void checkMeasurementProfile(Warning warn, boolean repair, Ms2ExperimentImpl input) {
        if (input.getMeasurementProfile() == null) throw new InvalidException("Measurement profile is missing");
        final MutableMeasurementProfile profile = new MutableMeasurementProfile(input.getMeasurementProfile());
        if (profile.getFormulaConstraints() == null) {
            throwOrWarn(warn, repair, "Measurement profile: Formula constraints are missing");
            profile.setFormulaConstraints(new FormulaConstraints(new ChemicalAlphabet()));
        }
        // get at least one deviation
        Deviation dev = profile.getAllowedMassDeviation();
        if (dev == null) dev = profile.getStandardMs2MassDeviation();
        if (dev == null) dev = profile.getStandardMs1MassDeviation();
        if (profile.getAllowedMassDeviation() == null) {
            throwOrWarn(warn, repair && dev != null, "Measurement profile: Maximal allowed Mass deviation is missing");
            profile.setAllowedMassDeviation(dev);
        }
        if (profile.getStandardMs2MassDeviation() == null) {
            throwOrWarn(warn, repair && dev != null, "Measurement profile: GraphFragment Mass deviation is missing");
            profile.setStandardMs2MassDeviation(dev);
        }
        if (profile.getStandardMs1MassDeviation() == null) {
            throwOrWarn(warn, repair && dev != null, "Measurement profile: Ion Mass deviation is missing");
            profile.setStandardMs1MassDeviation(dev);
        }
        checkAlphabet(warn, repair, input, profile);
        input.setMeasurementProfile(profile);
    }

    protected void checkAlphabet(Warning warn, boolean repair, Ms2ExperimentImpl input, MutableMeasurementProfile prof) {
        if (input.getMolecularFormula() != null) {
            final FormulaConstraints constraints = prof.getFormulaConstraints();
            final ChemicalAlphabet alphabet = constraints.getChemicalAlphabet();
            final Set<Element> elements = new HashSet<Element>(input.getMolecularFormula().elements());
            elements.removeAll(alphabet.getElements());
            if (!elements.isEmpty()) {
                throwOrWarn(warn, repair, "Missing elements " + elements.toString());
                final Set<Element> both = new HashSet<Element>(input.getMolecularFormula().elements());
                both.addAll(alphabet.getElements());
                elements.addAll(alphabet.getElements());
                final ChemicalAlphabet newAlphabet = new ChemicalAlphabet(both.toArray(new Element[elements.size()]));
                final FormulaConstraints newConstraints = new FormulaConstraints(newAlphabet, constraints.getFilters());
                for (Element e : alphabet.getElements()) {
                    newConstraints.setUpperbound(e, Math.max(input.getMolecularFormula().numberOf(e), constraints.getUpperbound(e)));
                }
                prof.setFormulaConstraints(newConstraints);
            }
        }
    }

    protected void checkNeutralMass(Warning warn, boolean repair, Ms2ExperimentImpl input) {
        if (input.getMoleculeNeutralMass() == 0 || !validDouble(input.getMoleculeNeutralMass(), false)) {
            throwOrWarn(warn, repair, "Neutral mass is missing");
            if (input.getMolecularFormula() != null) {
                input.setMoleculeNeutralMass(input.getMolecularFormula().getMass());
            } else if (input.getIonization() != null) {
                input.setMoleculeNeutralMass(input.getIonization().subtractFromMass(input.getIonMass()));
            }
        }
    }

    protected void removeEmptySpectra(Warning warn, Ms2ExperimentImpl input) {
        final Iterator<Ms2Spectrum<? extends Peak>> iter = input.getMs2Spectra().iterator();
        while (iter.hasNext()) {
            final Ms2Spectrum spec = iter.next();
            if (spec.size() == 0) {
                warn.warn("Empty Spectrum at collision energy: " + spec.getCollisionEnergy());
                iter.remove();
            }
        }
    }

    protected void checkIonization(Warning warn, boolean repair, Ms2ExperimentImpl input) {
        if (input.getIonization() == null) {
            throwOrWarn(warn, repair, "No ionization is given");
            if (validDouble(input.getIonMass(), false) && validDouble(input.getMoleculeNeutralMass(), false)) {
                double modificationMass = input.getIonMass() - input.getMoleculeNeutralMass();
                Ionization ion = PeriodicTable.getInstance().ionByMass(modificationMass, 1e-2);
                if (ion == null && input.getMolecularFormula() != null) {
                    modificationMass = input.getIonMass() - input.getMolecularFormula().getMass();
                    ion = PeriodicTable.getInstance().ionByMass(modificationMass, 1e-2);
                }
                if (ion == null) {
                    searchForIon(warn, input);
                } else {
                    warn.warn("set ion to " + ion);
                    input.setIonization(ion);
                }
            } else if (input.getMolecularFormula() != null || validDouble(input.getMoleculeNeutralMass(), false)) {
                searchForIon(warn, input);
            }
        }
        if (repair && input.getIonization() instanceof Charge && (input.getMolecularFormula() != null)) {
            double modificationMass = input.getIonMass() - (input.getMolecularFormula() != null ? input.getMolecularFormula().getMass() : input.getMoleculeNeutralMass());
            Ionization ion = PeriodicTable.getInstance().ionByMass(modificationMass, 1e-2, input.getIonization().getCharge());
            if (ion != null) {
                warn.warn("Set ion to " + ion.toString());
                input.setIonization(ion);
            } else {
                searchForIon(warn, input);
            }
        }
    }

    private void searchForIon(Warning warn, Ms2ExperimentImpl input) {
        final double neutral = (input.getMolecularFormula() != null) ? input.getMolecularFormula().getMass() : input.getMoleculeNeutralMass();
        final ArrayList<Spectrum<? extends Peak>> spectra = new ArrayList<Spectrum<? extends Peak>>(input.getMs1Spectra());
        for (Ms2Spectrum<? extends Peak> ms2 : input.getMs2Spectra()) spectra.add(ms2);
        // TODO: negative ions
        // search for [M+H]+
        final Ionization mhp = PeriodicTable.getInstance().ionByName("[M+H]+");
        final double mz = mhp.addToMass(neutral);
        final Deviation dev = input.getMeasurementProfile().getAllowedMassDeviation();
        for (Spectrum<? extends Peak> spec : spectra) {
            final int peak = Spectrums.search(spec, mz, dev);
            if (peak >= 0) {
                warn.warn("Set ion to " + mhp.toString());
                input.setIonization(mhp);
                input.setIonMass(spec.getMzAt(peak));
                return;
            }
        }
        // search for other ions
        final List<Ionization> ions = PeriodicTable.getInstance().getIons();
        for (Spectrum<? extends Peak> spec : spectra) {
            for (Ionization ion : ions) {
                if (Spectrums.search(spec, ion.addToMass(neutral), dev) >= 0) {
                    warn.warn("Set ion to " + ion.toString());
                    input.setIonization(ion);
                    return;
                }
            }
        }
        throw new InvalidException("Unknown ionization");
    }

    protected void checkMergedMs1(Warning warn, boolean repair, Ms2ExperimentImpl input) {
        if (input.getMergedMs1Spectrum() == null && !input.getMs1Spectra().isEmpty()) {
            warn.warn("No merged spectrum is given");
            if (repair) {
                if (input.getMs1Spectra().size() == 1)
                    input.setMergedMs1Spectrum(input.getMs1Spectra().get(0));
            }
        }
    }

    protected void checkIonMass(Warning warn, boolean repair, Ms2ExperimentImpl input) {
        if (!validDouble(input.getIonMass(), false) || input.getIonMass() == 0) {
            throwOrWarn(warn, repair, "No ion mass is given");
            if (input.getMolecularFormula() == null && !validDouble(input.getMoleculeNeutralMass(), false)) {
                final Spectrum<Peak> ms1 = input.getMergedMs1Spectrum();
                // maybe the ms2 spectra have a common precursor
                boolean found = true;
                double mz = input.getMs2Spectra().get(0).getPrecursorMz();
                for (Ms2Spectrum s : input.getMs2Spectra()) {
                    if (!validDouble(s.getPrecursorMz(), false) || s.getPrecursorMz() == 0) {
                        found = false;
                        break;
                    }
                    final double newMz = s.getPrecursorMz();
                    if (Math.abs(mz - newMz) > 1e-3) {
                        found = false;
                        break;
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
                        for (Peak p : normalized)
                            if (p.getIntensity() > 0.1d) {
                                if (Math.abs(p.getMass() - parent.getMass()) < 1e-2) {
                                    if (p.getIntensity() > parent.getIntensity()) parent = p;
                                } else if (p.getMass() > parent.getMass()) parent = p;
                            }
                        input.setIonMass(parent.getMass());
                    } else {
                        // take peak with highest intensity
                        int index = Spectrums.getIndexOfPeakWithMaximalIntensity(ms1);
                        // move backward, maybe you are in the middle of an isotope pattern
                        while (index > 0) {
                            if (Math.abs(ms1.getMzAt(index) - ms1.getMzAt(index - 1)) > 1.1d) break;
                            --index;
                        }
                        // hopefully, this is the correct isotope peak
                        warn.warn("Predict ion mass from MS1: " + ms1.getMzAt(index));
                        input.setIonMass(ms1.getMzAt(index));
                    }
                }
            } else {
                final double parentMz = input.getIonization().addToMass(input.getMoleculeNeutralMass());
                input.setIonMass(parentMz);
                for (int i = 0; i < input.getMs2Spectra().size(); ++i) {
                    final Ms2Spectrum s = input.getMs2Spectra().get(i);
                    if (Math.abs(s.getPrecursorMz() - parentMz) > 0.1d) {
                        final Ms2Spectrum t = new Ms2SpectrumImpl(s, s.getCollisionEnergy(), parentMz, s.getTotalIonCount());
                        input.getMs2Spectra().set(i, t);
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
