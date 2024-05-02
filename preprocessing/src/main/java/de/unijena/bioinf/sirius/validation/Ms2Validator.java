/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.sirius.validation;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.InvalidException;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Ms2Validator extends Ms1Validator {

    @Override
    public boolean validate(MutableMs2Experiment input, Warning warn, boolean repair) throws InvalidException {
        checkInchi(warn, repair, input);
        if (input.getMs2Spectra() == null)
            throw new InvalidException("Missing MS2 spectra");
        removeEmptySpectra(warn, input);
        if (input.getMs2Spectra().isEmpty() && input.getMs1Spectra().isEmpty() && input.getMergedMs1Spectrum()==null)
            throw new InvalidException("Missing MS2 and MS1 spectra");
        if (input.getMs1Spectra() == null) input.setMs1Spectra(new ArrayList<SimpleSpectrum>());
        checkScanNumbers(warn, repair,input);
        checkIntrinsicalCharged(warn,repair,input);
        checkIonization(warn, repair, input);
        checkMergedMs1(warn, repair, input);
        checkIonMass(warn, repair, input);
        checkMolecularFormula(warn, repair, input);
        correctCollisionEnergy(warn, repair, input);
        return true;
    }

    @Override
    protected void checkIonization(Warning warn, boolean repair, MutableMs2Experiment input) {
        Deviation devMs1 = input.getAnnotationOrDefault(MS1MassDeviation.class).allowedMassDeviation;
        Deviation devMs2 = input.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation;
        checkIonization(warn, repair, input, new Deviation(Math.max(devMs1.getPpm(), devMs2.getPpm()), Math.max(devMs1.getAbsolute(), devMs2.getAbsolute())));
    }

    private void correctCollisionEnergy(Warning warn,boolean repair, MutableMs2Experiment input) {
        HashMap<String, Pair<Double,Double>> instrumentCorrection = new HashMap<>(); //Map saves correction values for min and max energy separatly
        instrumentCorrection.put("Bruker Q-ToF (LCMS)", new Pair<>(5d,5d));
        instrumentCorrection.put("Q-ToF (LCMS)", new Pair<>(5d,5d));
        instrumentCorrection.put("Tripple-Quadrupole", new Pair<>(5d,5d));

        String instrumentType = input.getAnnotation(MsInstrumentation.class).map(x->x.description()).orElse(null);

        for (MutableMs2Spectrum spectrum : input.getMs2Spectra()) {
            if (spectrum.getCollisionEnergy()==null) {
                spectrum.setCollisionEnergy(CollisionEnergy.none());
            }
            if (!spectrum.getCollisionEnergy().isCorrected()) {
                if (instrumentCorrection.containsKey(instrumentType) && !spectrum.getCollisionEnergy().equals(CollisionEnergy.none())) {
                    spectrum.getCollisionEnergy().setMinEnergy(spectrum.getCollisionEnergy().minEnergySource() + instrumentCorrection.get(instrumentType).getFirst());
                    spectrum.getCollisionEnergy().setMaxEnergy(spectrum.getCollisionEnergy().maxEnergySource() + instrumentCorrection.get(instrumentType).getSecond());
                }else{
                    spectrum.getCollisionEnergy().setMinEnergy(spectrum.getCollisionEnergy().minEnergySource());
                    spectrum.getCollisionEnergy().setMaxEnergy(spectrum.getCollisionEnergy().maxEnergySource());
                }
            }

        }
    }

    private void checkScanNumbers(Warning warn, boolean repair, MutableMs2Experiment input) {
        for (int k=0; k < input.getMs2Spectra().size(); ++k) {
            input.getMs2Spectra().get(k).setScanNumber(k);
        }
    }


    protected void removeEmptySpectra(Warning warn, MutableMs2Experiment input) {
        final Iterator<MutableMs2Spectrum> iter = input.getMs2Spectra().iterator();
        while (iter.hasNext()) {
            final Ms2Spectrum spec = iter.next();
            if (spec.size() == 0) {
                warn.warn(nameOf(input) + ": Empty Spectrum at collision energy: " + spec.getCollisionEnergy());
                iter.remove();
            }
        }
    }

    protected void checkIonMass(Warning warn, boolean repair, MutableMs2Experiment input) {
        if (!validDouble(input.getIonMass(), false) || input.getIonMass() == 0) {
            if (input.getMolecularFormula() == null && !validDouble(input.getMoleculeNeutralMass(), false)) {
                final Spectrum<Peak> ms1 = input.getMergedMs1Spectrum();
                // maybe the ms2 spectra have a common precursor
                boolean found = true;
                if (!input.getMs2Spectra().isEmpty()) {
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
                    }
                } else {
                    if (ms1 == null || ms1.isEmpty()) {
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
                        warn.warn(nameOf(input) + ": No ion mass is given. Choose m/z = " + parent.getMass() + " as parent peak.");
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
                        warn.warn(nameOf(input) + ": Predict ion mass from MS1: " + ms1.getMzAt(index));
                        input.setIonMass(ms1.getMzAt(index));
                    }
                }
            } else {
                final double neutralMass = (input.getMolecularFormula()!=null ? input.getMolecularFormula().getMass() : input.getMoleculeNeutralMass());
                if (neutralMass <= 0) {
                    throwOrWarn(warn, false, nameOf(input) + ": Neither ionmass nor neutral mass nor molecular formula are given. Cannot determine parent peak!");
                }
                final double parentMz = input.getPrecursorIonType().neutralMassToPrecursorMass(neutralMass);
                input.setIonMass(parentMz);
                for (int i = 0; i < input.getMs2Spectra().size(); ++i) {
                    final Ms2Spectrum s = input.getMs2Spectra().get(i);
                    if (Math.abs(s.getPrecursorMz() - parentMz) > 0.1d) {
                        final MutableMs2Spectrum t = new MutableMs2Spectrum(s, parentMz, s.getCollisionEnergy(), 2);
                        input.getMs2Spectra().set(i, t);
                    }
                }
            }
        }
    }

    private boolean validDouble(double val, boolean mayNegative) {
        return !Double.isInfinite(val) && !Double.isNaN(val) && (mayNegative || val > 0d);
    }
}
