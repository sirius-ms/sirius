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
package de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.sirius.MS2Peak;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalizes such that the base peak gets intensity 1.0 and all other peaks have intensities relative to the base peak.
 * If the parent peak is the base peak, then use the second most intensive peak to performNormalization.
 * This fix problems with spectra where the parent peak has 99% of sumed intensities.
 */
public class LeaveOutParentPeakMaxNormalizer implements Normalizer {
    @Override
    public List<List<ProcessedPeak>> normalize(Ms2Experiment experiment, NormalizationType type) {
        final double parentMass  = experiment.getIonMass();
        final ArrayList<List<ProcessedPeak>> peaklist = new ArrayList<List<ProcessedPeak>>(100);
        double globalMaxIntensity = 0d;
        for (Ms2Spectrum<? extends Peak> s : experiment.getMs2Spectra()) {
            final ArrayList<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(s.size());
            for (Peak p : s) peaks.add(new ProcessedPeak((MS2Peak)p));
            // now performNormalization spectrum. Ignore peaks near to the parent peak
            final double lowerbound = parentMass - 0.75d;
            double scale = 0d;
            for (int i=0; i < peaklist.size() && peaks.get(i).getMz() < lowerbound; ++i) {
                scale = Math.max(scale, peaks.get(i).getIntensity());
            }

            // this happens if the spectrum contains only one peak...
            if (scale == 0) {
                scale = peaks.get(0).getIntensity();
            }

            // now set local relative intensities
            for (int i=0; i < peaklist.size(); ++i) {
                final ProcessedPeak peak = peaks.get(i);
                peak.setLocalRelativeIntensity(peak.getIntensity()/scale);
            }
            // and adjust global relative intensity
            globalMaxIntensity = Math.max(globalMaxIntensity, scale);
            peaklist.add(peaks);
        }
        // now calculate global normalized intensities
        for (List<ProcessedPeak> peaks : peaklist) {
            for (ProcessedPeak peak : peaks) {
                peak.setGlobalRelativeIntensity(peak.getIntensity()/globalMaxIntensity);
                peak.setRelativeIntensity(type == NormalizationType.GLOBAL ? peak.getGlobalRelativeIntensity() : peak.getLocalRelativeIntensity());
            }
        }
        // finished!
        return peaklist;
    }
}
