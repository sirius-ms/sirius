package de.unijena.bioinf.lcms;

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

    public List<ChromatographicPeak> searchChimerics(ProcessedSample sample, Scan ms1Scan, Precursor precursor, IsolationWindow isolationWindow, ChromatographicPeak ms1Feature) {
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
        final ArrayList<ChromatographicPeak> chimerics = new ArrayList<>();
        final double signalLevel = scan.getIntensityAt(mostIntensive)*0.25;
        for (int k=bgindex; k < scan.size() && scan.getMzAt(k) <= to; ++k) {
            if (k!=mostIntensive && scan.getIntensityAt(k)>signalLevel) {
                // build a mass trace
                Optional<ChromatographicPeak> chim = sample.builder.detectExact(ms1Scan, scan.getMzAt(k));
                if (chim.isPresent()) {
                    if (chim.get().samePeak(ms1Feature)) {
                        System.err.println("WTF?");
                        sample.builder.detectExact(ms1Scan, scan.getMzAt(k));
                    }
                    // check if it is an isotope
                    if (CorrelatedPeakDetector.hasMassOfAnIsotope(ms1Mass, scan.getMzAt(k)) && detector.correlate(ms1Feature,segment.get(),chim.get()).filter(f->f.getCorrelation()>=0.9).isPresent()) {
                        // ignore this peak
                        continue;
                    } else {
                        chimerics.add(chim.get());
                    }
                }
            }
        }
        return chimerics;
    }

}
