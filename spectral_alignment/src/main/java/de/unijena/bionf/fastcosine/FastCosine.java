package de.unijena.bionf.fastcosine;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.sirius.Ms2Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.NoiseThresholdSettings;
import de.unijena.bioinf.sirius.merging.HighIntensityMsMsMerger;
import de.unijena.bioinf.sirius.peakprocessor.NoiseIntensityThresholdFilter;
import de.unijena.bionf.spectral_alignment.ModifiedCosine;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;

import java.util.*;

public class FastCosine {

    private static boolean mergeMasses = false;

    private final Deviation maxDeviation;
    private final boolean useSquareRootTransform;
    private final NoiseThresholdSettings noiseThresholdSettings;

    public FastCosine(Deviation maxDeviation, boolean useSquareRootTransform, NoiseThresholdSettings noiseThresholdSettings) {
        this.maxDeviation = maxDeviation;
        this.useSquareRootTransform = useSquareRootTransform;
        this.noiseThresholdSettings = noiseThresholdSettings;
    }

    public FastCosine() {
        this(new Deviation(15), true, new NoiseThresholdSettings(0.001, 60, NoiseThresholdSettings.BASE_PEAK.NOT_PRECURSOR, 0));

    }

    public ReferenceLibrarySpectrum prepareQuery(Ms2Experiment ms2data) {
        Optional<MS2MassDeviation> annotation = ms2data.getAnnotation(MS2MassDeviation.class);
        Optional<NoiseThresholdSettings> annotation2 = ms2data.getAnnotation(NoiseThresholdSettings.class);
        ms2data.setAnnotation(MS2MassDeviation.class, new MS2MassDeviation(maxDeviation, maxDeviation, maxDeviation));
        ms2data.setAnnotation(NoiseThresholdSettings.class, noiseThresholdSettings);
        ProcessedInput pinput  = new Ms2Preprocessor().preprocess(ms2data);
        if (annotation.isPresent()) ms2data.setAnnotation(MS2MassDeviation.class, annotation.get());
        else ms2data.removeAnnotation(MS2MassDeviation.class);
        if (annotation2.isPresent()) ms2data.setAnnotation(NoiseThresholdSettings.class, annotation2.get());
        else ms2data.removeAnnotation(NoiseThresholdSettings.class);
        return prepareQuery(pinput);
    }

    public ReferenceLibrarySpectrum prepareQuery(double parentmass, Spectrum<? extends Peak> spectrum)  {
        MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setIonMass(parentmass);
        exp.setAnnotation(MS2MassDeviation.class, new MS2MassDeviation(maxDeviation, maxDeviation, maxDeviation));
        exp.setAnnotation(NoiseThresholdSettings.class, noiseThresholdSettings);
        exp.setMs2Spectra(Arrays.asList(new MutableMs2Spectrum(spectrum, parentmass, CollisionEnergy.none(), 2)));
        ProcessedInput processedInput = new ProcessedInput(exp, exp);
        new HighIntensityMsMsMerger().merge(processedInput);
        new NoiseIntensityThresholdFilter().process(processedInput);
        return prepareQuery(processedInput);
    }

    private ReferenceLibrarySpectrum prepareQuery(ProcessedInput input) {
        // 1. sort peaks by mass
        List<ProcessedPeak> mergedPeaks = input.getMergedPeaks();
        mergedPeaks.sort(Comparator.comparingDouble(ProcessedPeak::getMass));
        // 2. extract mz and intensity
        double[] mz = mergedPeaks.stream().mapToDouble(ProcessedPeak::getMass).toArray();
        float[] intensity = new float[mz.length];
        double norm = 0d;
        for (int k=0; k < mergedPeaks.size(); ++k) {
            intensity[k] = (float) (useSquareRootTransform ? Math.sqrt(mergedPeaks.get(k).getRelativeIntensity()) : mergedPeaks.get(k).getRelativeIntensity());
            // do not normalize the parent peak!
            if (k<mergedPeaks.size()-1) {
                norm += intensity[k] * intensity[k];
            }
        }
        norm = Math.sqrt(norm);
        for (int k=0; k < mergedPeaks.size(); ++k) {
            intensity[k] /= norm;
        }
        return new ReferenceLibrarySpectrum(input.getParentPeak().getMass(), mz, intensity);
    }

    public ReferenceLibraryMergedSpectrum prepareMergedQuery(List<ReferenceLibrarySpectrum> spectra) {
        return performPeakMerging(spectra);
    }

    /**
     * Fast computation of cosine. In contrast to the CosineQueryUtils class we do not have to do spectral alignments
     * as it is guaranteed that there are never two peaks so close to each other such that the assignment is unambiguous.
     * Runs in linear time.
     */
    public SpectralSimilarity fastCosine(ReferenceLibrarySpectrum left, ReferenceLibrarySpectrum right) {
        int i = 0, j = 0;
        double similarity = 0d;
        int matchedPeaks = 0;
        final double parentMassLeft = left.getParentMass();
        final double parentMassRight = right.getParentMass();;
        while (i < left.size() && j < right.size()) {
            double l = left.getMzAt(i);
            double r = right.getMzAt(j);
            if (l >= parentMassLeft || r >= parentMassRight) break; // do not count the parent peak
            double delta = l - r;
            // use minimum to ensure unique assignment. This is not really a constraint as l and r are always extremely close,
            // so it doesn't matter if you use minimum or maximum. The difference should be neglectable.
            double allowedMassDeviation = maxDeviation.absoluteFor(Math.min(l,r));
            if (Math.abs(delta) < allowedMassDeviation) {
                // match
                similarity += left.getIntensityAt(i)*right.getIntensityAt(j);
                ++matchedPeaks;
                ++i;
                ++j;
            } else if (delta < 0) {
                ++i;
            } else if (delta > 0) {
                ++j;
            }
        }
        return new SpectralSimilarity(similarity, matchedPeaks);
    }

    public SpectralSimilarity fastReverseCosine(ReferenceLibrarySpectrum left, ReferenceLibrarySpectrum right) {
        int i = 0, j = 0;
        double similarity = 0d;
        int matchedPeaks = 0;
        while (i < left.size() && j < right.size()) {
            double l = left.getParentMass() - left.getMzAt(i);
            double r = right.getParentMass() - right.getMzAt(j);
            if (l==0 || r==0) break; // hit parent peak
            double delta = l - r;
            // use minimum to ensure unique assignment. This is not really a constraint as l and r are always extremely close,
            // so it doesn't matter if you use minimum or maximum. The difference should be neglectable.
            double allowedMassDeviation = maxDeviation.absoluteFor(Math.min(l,r));
            if (Math.abs(delta) < allowedMassDeviation) {
                // match
                similarity += left.getIntensityAt(i)*right.getIntensityAt(j);
                ++matchedPeaks;
                ++i;
                ++j;
            } else if (delta < 0) {
                ++j;
            } else if (delta > 0) {
                ++i;
            }
        }
        return new SpectralSimilarity(similarity, matchedPeaks);
    }

    public SpectralSimilarity fastModifiedCosine(ReferenceLibrarySpectrum left, ReferenceLibrarySpectrum right) {
        return new ModifiedCosine(maxDeviation).score(left, right, left.getParentMass(), right.getParentMass(), 1d);
    }

    private ReferenceLibraryMergedSpectrum performPeakMerging(List<ReferenceLibrarySpectrum> spectra) {
        final double parentMass = spectra.stream().mapToDouble(ReferenceLibrarySpectrum::getParentMass).average().orElse(0d);
        final SimpleSpectrum merged = Spectrums.mergeSpectra(spectra);
        final Spectrum<Peak> intensityOrdered = Spectrums.getIntensityOrderedSpectrum(merged);
        final BitSet alreadyMerged = new BitSet(merged.size());
        final ReferenceLibraryMergedSpectrum.Builder mergedSpectrum = new ReferenceLibraryMergedSpectrum.Builder(parentMass);
        for (int k=0; k < intensityOrdered.size(); ++k) {
            final double mz = intensityOrdered.getMzAt(k);
            final int index = Spectrums.binarySearch(merged, mz);

            if (alreadyMerged.get(index)) continue;
            // merge all surrounding peaks
            final double dev = maxDeviation.absoluteFor(mz);
            final double min=mz-dev, max=mz+dev;
            int a=index,b=index+1;
            while (a >= 0 && merged.getMzAt(a) >= min) --a;
            ++a;
            while (b < merged.size() && merged.getMzAt(b) <= max) ++b;

            double mzSum=0d,intensitySum=0d;
            for (int j=a; j < b; ++j) {
                if (!alreadyMerged.get(j)) {
                    alreadyMerged.set(j);
                    mzSum += merged.getMzAt(j)*merged.getIntensityAt(j);
                    intensitySum += merged.getIntensityAt(j);
                }
            }
            final double mergedIntensity, mergedMz;
            mergedIntensity = intensitySum;
            if (mergeMasses) {
                mergedMz = mzSum / intensitySum;
            } else mergedMz = mz;

            mergedSpectrum.addPeak(mergedMz, (float)mergedIntensity, (float)intensityOrdered.getIntensityAt(k));
        }
        return mergedSpectrum.done();
    }

    public Deviation getMaxDeviation() {
        return maxDeviation;
    }
}
