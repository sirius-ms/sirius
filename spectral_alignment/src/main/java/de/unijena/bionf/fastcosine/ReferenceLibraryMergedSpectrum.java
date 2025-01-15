package de.unijena.bionf.fastcosine;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A spectrum that is a merge of multiple spectra, such that the cosine distance to this spectrum is
 * a lowerbound to the maximum cosine distance of the merged spectra.
 *
 */
public class ReferenceLibraryMergedSpectrum extends ReferenceLibrarySpectrum {
    final float[] mergedMaxIntensities;

    private record MergedPeak(double mz, float intensity, float queryIntensity) {
    }

    protected static class Builder {
        private final double parentmass;
        private final List<MergedPeak> peaks;

        public Builder(double parentmass) {
            this.parentmass = parentmass;
            this.peaks = new ArrayList<>();
        }

        void addPeak(double mz, float avgIntens, float maxIntens) {
            this.peaks.add(new MergedPeak(mz, avgIntens,maxIntens));
        }

        ReferenceLibraryMergedSpectrum done() {
            // renormalize intensities
            double norm = Math.sqrt(peaks.stream().limit(peaks.size()-1).mapToDouble(x->x.intensity*x.intensity).sum());
            peaks.sort(Comparator.comparingDouble(x->x.mz));
            final double[] mz = new double[peaks.size()];
            final float[] intens = new float[peaks.size()];
            final float[] qintens = new float[peaks.size()];
            for (int k=0; k < peaks.size(); ++k) {
                final MergedPeak p = peaks.get(k);
                mz[k] = p.mz;
                intens[k] = (float)(p.intensity/norm);
                qintens[k] = p.queryIntensity;
            }

            return new ReferenceLibraryMergedSpectrum(parentmass, mz, intens, qintens);
        }
    }

    @JsonCreator ReferenceLibraryMergedSpectrum(@JsonProperty("parentMass") double parentMass, @JsonProperty("mz") double[] mz, @JsonProperty("intensities") float[] intensities,
                                                @JsonProperty("mergedMaxIntensities") float[] mergedMaxIntensities) {
        super(parentMass, mz, intensities);
        this.mergedMaxIntensities = mergedMaxIntensities;
    }

    public ReferenceLibrarySpectrum asReferenceLibrarySpectrum() {
        return new ReferenceLibrarySpectrum(getParentMass(), mz, intensities);
    }

    public ReferenceLibrarySpectrum getUpperboundQuery() {
        return new ReferenceLibrarySpectrum(getParentMass(), mz, mergedMaxIntensities);
    }
}
