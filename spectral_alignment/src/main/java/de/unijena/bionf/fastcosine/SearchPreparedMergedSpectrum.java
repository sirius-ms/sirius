package de.unijena.bionf.fastcosine;

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
public class SearchPreparedMergedSpectrum extends SearchPreparedSpectrum {
    final float[] mergedMaxIntensities;

    private record MergedPeak(double mz, float intensity, float queryIntensity) {
    }

    protected static class Builder {
        private final double parentmass;
        private double parentIntensity;
        private final List<MergedPeak> peaks;

        public Builder(double parentmass, double parentIntensity) {
            this.parentmass = parentmass;
            this.parentIntensity = parentIntensity;
            this.peaks = new ArrayList<>();
        }

        void addPeak(double mz, float avgIntens, float maxIntens) {
            this.peaks.add(new MergedPeak(mz, avgIntens,maxIntens));
        }

        SearchPreparedMergedSpectrum done() {
            // renormalize intensities
            peaks.sort(Comparator.comparingDouble(x->x.mz));

            // ensure parent peak is not in there
            final double precursorMzThreshold = parentmass-0.5d;
            for (int j=peaks.size()-1; j>=0; --j ) {
                if (peaks.get(j).mz() >= precursorMzThreshold) peaks.remove(j);
                else break;
            }

            double norm = Math.sqrt(peaks.stream().mapToDouble(x->x.intensity*x.intensity).sum());
            final double[] mz = new double[peaks.size()];
            final float[] intens = new float[peaks.size()];
            final float[] qintens = new float[peaks.size()];
            for (int k=0; k < peaks.size(); ++k) {
                final MergedPeak p = peaks.get(k);
                mz[k] = p.mz;
                intens[k] = (float)(p.intensity/norm);
                qintens[k] = p.queryIntensity;
            }

            return new SearchPreparedMergedSpectrum(parentmass, (float)(parentIntensity/norm), mz, intens, qintens);
        }
    }

    @JsonCreator
    SearchPreparedMergedSpectrum(@JsonProperty("parentMass") double parentMass,
                                 @JsonProperty("parentIntensity") float parentIntensity,
                                 @JsonProperty("mz") double[] mz,
                                 @JsonProperty("intensities") float[] intensities,
                                 @JsonProperty("mergedMaxIntensities") float[] mergedMaxIntensities
    ) {
        super(parentMass, parentIntensity, mz, intensities);
        this.mergedMaxIntensities = mergedMaxIntensities;
    }

    public SearchPreparedSpectrum asSearchPreparedSpectrum() {
        return new SearchPreparedSpectrum(getParentMass(), getParentIntensity(), mz, intensities);
    }

    public SearchPreparedSpectrum asUpperboundSearchPreparedSpectrum() {
        return new SearchPreparedSpectrum(getParentMass(), getParentIntensity(), mz, mergedMaxIntensities);
    }
}
