package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MsExperiments {

    public static class PrecursorCandidates extends ArrayList<Peak> {
        public PrecursorCandidates(int initialCapacity) {
            super(initialCapacity);
        }

        public PrecursorCandidates(Collection<? extends Peak> c) {
            super(c);
        }

        private Peak defaultPrecursor = null;

        public Peak getDefaultPrecursor() {
            return defaultPrecursor;
        }
    }

    public static PrecursorCandidates findPossiblePrecursorPeaks(final Ms2Experiment ex) {
        ArrayList<SimpleSpectrum> ms1Spectra = new ArrayList<>(ex.getMs1Spectra());
        if (ex.getMergedMs1Spectrum() != null)
            ms1Spectra.add(ex.getMergedMs1Spectrum());
        return findPossiblePrecursorPeaks(ms1Spectra, ex.getMs2Spectra(), ex.getIonMass());
    }

    public static PrecursorCandidates findPossiblePrecursorPeaks(final List<? extends SimpleSpectrum> ms1Spectra, final List<? extends Ms2Spectrum<Peak>> ms2Spectra, final double ionMass) {
        //todo this could be improved
        double maxInt = -1;
        final SimpleMutableSpectrum massBuffer = new SimpleMutableSpectrum();

        // falls MS1 verfügbar biete MS1 Peaks an, ansonsten nehme MS2 und normalisiere global
        Peak bestDataIon = null;
        final Deviation dev = new Deviation(20);

        if (!ms1Spectra.isEmpty()) {
            for (SimpleSpectrum cms1 : ms1Spectra) {
                final double cms1MaxInt = cms1.getMaxIntensity();
                if (ionMass > 0) {
                    final int i = Spectrums.mostIntensivePeakWithin(cms1, ionMass, dev);
                    if (i >= 0) {
                        massBuffer.addPeak(cms1.getMzAt(i), cms1.getIntensityAt(i) / cms1MaxInt);
                        bestDataIon = cms1.getPeakAt(i);
                    }
                }
                // for each isotope pattern add starting peak with at least 2% intensity
                for (int i = 0; i < cms1.size(); ++i) {
                    int j = Spectrums.mostIntensivePeakWithin(cms1, cms1.getMzAt(i) - 1.0033, dev);
                    if (j < 0 || cms1.getIntensityAt(j) < 0.02) {
                        massBuffer.addPeak(cms1.getMzAt(i), cms1.getIntensityAt(i) / cms1MaxInt);
                    }
                }
            }

        } else {
            // take the highest peak with at least 5% intensity that is not preceeded by
            // possible isotope peaks

            final SimpleMutableSpectrum mergedSpec = new SimpleMutableSpectrum(Spectrums.mergeSpectra(new Deviation(20), true, true, ms2Spectra));
            Spectrums.normalize(mergedSpec, Normalization.Max(1d));
            Spectrums.applyBaseline(mergedSpec, 0.05);
            final SimpleSpectrum spec = new SimpleSpectrum(mergedSpec);
            // search parent peak
            int largestPeak = spec.size() - 1;
            for (; largestPeak > 0; --largestPeak) {
                final int isotopePeak = Spectrums.mostIntensivePeakWithin(spec, spec.getMzAt(largestPeak) - 1d, new Deviation(10, 0.2));
                if (isotopePeak < 0 || spec.getIntensityAt(isotopePeak) < spec.getIntensityAt(largestPeak)) break;
            }
            double expectedParentMass = 0d;
            if (largestPeak > 0) {
                expectedParentMass = spec.getMzAt(largestPeak);
            }


            for (Ms2Spectrum<? extends Peak> sp : ms2Spectra) {
                final double spMaxInt = sp.getMaxIntensity();
                for (int i = 0; i < sp.size(); i++) {
                    if (sp.getPeakAt(i).getIntensity() > maxInt) {
                        maxInt = sp.getPeakAt(i).getIntensity();
                    }
                    massBuffer.addPeak(sp.getMzAt(i), sp.getIntensityAt(i) / spMaxInt);
                    if ((ionMass > 0 && dev.inErrorWindow(sp.getPeakAt(i).getMass(), ionMass)) || (expectedParentMass > 0 && dev.inErrorWindow(sp.getPeakAt(i).getMass(), expectedParentMass))) {
                        if (bestDataIon == null || sp.getPeakAt(i).getIntensity() > bestDataIon.getIntensity())
                            bestDataIon = sp.getPeakAt(i);
                    }
                }
            }
        }

        Spectrums.mergePeaksWithinSpectrum(massBuffer, dev, false, true);

        final PrecursorCandidates possiblePrecursors = new PrecursorCandidates(massBuffer.size());

        Peak defaultIon = null;
        for (Peak p : massBuffer) {
            possiblePrecursors.add(p);
            if (bestDataIon != null && dev.inErrorWindow(p.getMass(), bestDataIon.getMass())) {
                defaultIon = p;
            }
            if (bestDataIon == null && (defaultIon == null || p.getMass() > defaultIon.getMass())) {
                defaultIon = p;
            }
        }

        if (defaultIon == null)
            defaultIon = bestDataIon;

        possiblePrecursors.defaultPrecursor = defaultIon;

        return possiblePrecursors;
    }
}
