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
package de.unijena.bioinf.FragmentationTreeConstruction.computation.parentpeak;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A parent peak detection strategy:
 * - mass of parent peak is given by user
 * - this may be the focused mass of the instrument, the exact mass of the compound or the mass of the precursor peak
 * - if there is a peak with exact the mass of the given input, than this detector assumes that the user know the
 * correct parent peak and the detector will use this peak
 * - if there is a peak that mass deviates in the maximum of the allowed mass-deviation-window and the precision of
 * the input mass, the detector assumes that the input mass is a focused mass or an exact mass and the peak is
 * marked as candidate for a proper parent peak. From all candidates the detector will take the candidate with
 * highest intensity.
 * Example: For input mass 128.2 the detector search for peaks in massrange [128.15, 128.25]
 * - if there is no such peak, a synthetic parent peak is created
 * <p>
 * TODO: This detector assumes that the ionization of the parent peak has charge (+-)1!
 * For other ionization types the detector have to be rewritten!
 */
public class SearchMatchingInputParentPeak implements ParentPeakDetector {

    private double delta = 1e-5;

    @Override
    public ProcessedPeak detectParentPeak(Ms2Experiment experiment) {

        ProcessedPeak parent = searchForExactHit(experiment);
        if (parent != null) return parent;

        parent = searchInMassWindow(experiment);
        if (parent != null) return parent;

        return createSyntheticParentPeak(experiment);

    }

    protected ProcessedPeak searchForExactHit(Ms2Experiment experiment) {
        final double from = experiment.getIonMass() - delta;
        final double to = experiment.getIonMass() + delta;
        for (Ms2Spectrum<? extends Peak> spec : experiment.getMs2Spectra()) {
            for (Peak p : spec) {
                if (from <= p.getMass() && p.getMass() <= to) {
                    final MS2Peak ms2 = (p instanceof MS2Peak) ? (MS2Peak) p : new MS2Peak(spec, p.getMass(), p.getIntensity());
                    return new ProcessedPeak(ms2);
                }
            }
        }
        return null;
    }

    protected ProcessedPeak searchInMassWindow(Ms2Experiment experiment) {
        final Deviation window = getSearchWindow(experiment);
        final List<MS2Peak> peaks = new ArrayList<MS2Peak>();
        for (Ms2Spectrum<? extends Peak> spec : experiment.getMs2Spectra()) {
            for (Peak p : spec) {
                if (window.inErrorWindow(experiment.getIonMass(), p.getMass())) {
                    final MS2Peak ms2 = (p instanceof MS2Peak) ? (MS2Peak) p : new MS2Peak(spec, p.getMass(), p.getIntensity());
                    peaks.add(ms2);
                }
            }
        }
        if (peaks.isEmpty()) return null;
        final ProcessedPeak parent = new ProcessedPeak(Collections.max(peaks, new MS2Peak.IntensityComparator()));
        return parent;
    }

    protected ProcessedPeak createSyntheticParentPeak(Ms2Experiment experiment) {
        final ProcessedPeak peak = new ProcessedPeak();
        peak.setCollisionEnergy(new CollisionEnergy(0, 0));
        peak.setMz(experiment.getIonMass());
        peak.setIntensity(1);
        peak.setOriginalPeaks(Collections.<MS2Peak>emptyList());
        return peak;
    }

    protected Deviation getSearchWindow(Ms2Experiment experiment) {
        Deviation dev = experiment.getAnnotationOrDefault(MS1MassDeviation.class).allowedMassDeviation;
        final double precision = getPrecision(experiment.getIonMass());
        final double absolute = Math.max(Math.pow(10, -precision - 1) * 5, dev.getAbsolute());
        return new Deviation(dev.getPpm(), absolute);
    }

    /**
     * @param fp floating point number
     * @return exponent k for 10^-k == last digit of fp
     */
    protected static double getPrecision(double fp) {
        final double delta = 1e-9;
        int precision = 0;
        while (Math.abs(fp - (long) fp) > delta) {
            fp *= 10;
            ++precision;
        }
        return precision;
    }


}
