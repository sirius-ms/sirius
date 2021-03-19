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

package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.model.lcms.ChromatographicPeak;
import de.unijena.bioinf.model.lcms.Precursor;
import de.unijena.bioinf.model.lcms.Scan;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ChimericDetector {

    protected IsolationWindow isolationWindow;
    protected NormalDistribution guessedFilter;

    public ChimericDetector(IsolationWindow isolationWindow) {
        this.isolationWindow = isolationWindow;
        final double window = isolationWindow.getWindowWidth();
        final double offset = isolationWindow.getWindowOffset();
        this.guessedFilter = new NormalDistribution(offset,(window*0.5)*(window*0.5));
    }

    public static class Chimeric {
        public final ChromatographicPeak peak;
        public final double estimatedIntensityThatPassesFilter;

        public Chimeric(ChromatographicPeak peak, double estimatedIntensityThatPassesFilter) {
            this.peak = peak;
            this.estimatedIntensityThatPassesFilter = estimatedIntensityThatPassesFilter;
        }
    }

    public List<Chimeric> searchChimerics(ProcessedSample sample, Scan ms1Scan, Precursor precursor, ChromatographicPeak ms1Feature) {
        final Optional<ChromatographicPeak.Segment> segment = ms1Feature.getSegmentForScanId(ms1Scan.getIndex());
        if (segment.isEmpty()) {
            throw new IllegalArgumentException("MS1 feature does not contain MS1 scan");
        }
        SimpleSpectrum scan = sample.storage.getScan(ms1Scan);
        final double window = isolationWindow.getWindowWidth();
        final double offset = isolationWindow.getWindowOffset();
        final double from = precursor.getMass()+offset-window;
        final double to = precursor.getMass()+offset+window;
        int bgindex=Spectrums.indexOfFirstPeakWithin(scan, from,to);
        int mostIntensive=-1;
        for (int m=1; m < 5; ++m) {
            final Deviation dev = sample.builder.getAllowedMassDeviation().multiply(m);
            for (int k=bgindex; k < scan.size() && scan.getMzAt(k) <= to; ++k) {
                if ((mostIntensive<0 || scan.getIntensityAt(k)>scan.getIntensityAt(mostIntensive)) && dev.inErrorWindow(precursor.getMass(),scan.getMzAt(k))) {
                    mostIntensive=k;
                }
            }
            if (mostIntensive>=0)
                break;
        }
        if (mostIntensive<0) {
            LoggerFactory.getLogger(ChimericDetector.class).warn("Do not find precursor ion in MS1 scan.");
            return Collections.emptyList();
        }
        final double ms1Mass = scan.getMzAt(mostIntensive);
        final CorrelatedPeakDetector detector = new CorrelatedPeakDetector(Collections.emptySet());
        // now add all other peaks as chimerics
        final ArrayList<Chimeric> chimerics = new ArrayList<>();
        final double signalLevel = scan.getIntensityAt(mostIntensive)*0.25;
        final double norm = guessedFilter.getDensity(offset);
        for (int k=bgindex; k < scan.size() && scan.getMzAt(k) <= to; ++k) {
            final double mzdiff = scan.getMzAt(k)-ms1Mass;
            final double possInt = scan.getIntensityAt(k)*guessedFilter.getDensity(mzdiff)/norm;
            if (k!=mostIntensive && possInt>signalLevel) {
                // build a mass trace
                Optional<ChromatographicPeak> chim = sample.builder.detectExact(ms1Scan, scan.getMzAt(k));
                if (chim.isPresent()) {
                    if (chim.get().samePeak(ms1Feature)) {
                        LoggerFactory.getLogger(ChimericDetector.class).warn(precursor.toString() + ": there is a chimeric peak that overlaps with the ion. It is unclear, if this chimeric is a real feature or just a badly picked peak. We will ignore it.");
                        continue;
                    }
                    // check if it is an isotope
                    if (CorrelatedPeakDetector.hasMassOfAnIsotope(ms1Mass, scan.getMzAt(k)) && detector.correlate(ms1Feature,segment.get(),chim.get()).filter(f->f.getCosine()>=CorrelatedPeakDetector.COSINE_THRESHOLD).isPresent()) {
                        // ignore this peak
                        continue;
                    } else {
                        chimerics.add(new Chimeric(chim.get(), possInt));
                    }
                }
            }
        }
        return chimerics;
    }

}
