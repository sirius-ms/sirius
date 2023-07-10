package de.unijena.bioinf.lcms;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.unijena.bioinf.lcms.quality.LCMSQualityCheck;
import gnu.trove.list.array.TFloatArrayList;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class InternalStatistics {

    // peak statistics
    public int numberOfFeatures;
    public final FloatStatistics scanPointsPerFeaturesAt25;
    public final FloatStatistics featureWidths;
    public final FloatStatistics featureFWHM;
    public final FloatStatistics featureHeights;
    public final FloatStatistics segmentsPerPeak;
    public final FloatStatistics msmsPerPeak;

    // msms statistics
    public final FloatStatistics precursorMasses;
    public int numberOfPeaksWithMassAbove1000;
    public final FloatStatistics msmsMergedCosines;
    public final FloatStatistics numberOfPeaksPerMSMs;
    public final FloatStatistics msmsEntropy;
    public final FloatStatistics retentionTimeWindow;
    public final FloatStatistics chimericPollution;

    // ms1 statistics
    public final FloatStatistics numberOfCorrelatedPeaksPerFeature;
    public final FloatStatistics numberOfIsotopePeaksPerFeature;
    public final FloatStatistics isotopicCorrelation;

    // alignment statistics
    public final FloatStatistics retentionTimeShift;
    public final FloatStatistics maximumRetentionTimeShiftPerFeature;
    public final FloatStatistics ratioOfSamplesPerFeature;
    public final FloatStatistics numberOfSamplesPerFeature;

    // adduct statistics
    public final FloatStatistics interFeatureCorrelation;
    public final FloatStatistics intraFeatureCorrelation;
    public float hplus, sodium, potassium, waterLoss, ammonium, strangeAdducts;
    public float deletedFeatures;
    public float adductGood=0, adductMedium=0, peakGood=0, peakMedium=0, isotopeGood=0, isotopeMedium=0, ms2Good=0, ms2Medium=0;

    public InternalStatistics() {
        scanPointsPerFeaturesAt25 = new FloatStatistics();
        featureWidths =  new FloatStatistics();
        featureHeights = new FloatStatistics();
        segmentsPerPeak = new FloatStatistics();
        msmsPerPeak = new FloatStatistics();
        msmsMergedCosines = new FloatStatistics();
        numberOfPeaksPerMSMs = new FloatStatistics();
        msmsEntropy = new FloatStatistics();
        chimericPollution = new FloatStatistics();
        numberOfCorrelatedPeaksPerFeature = new FloatStatistics();
        numberOfIsotopePeaksPerFeature = new FloatStatistics();
        isotopicCorrelation = new FloatStatistics();
        retentionTimeShift = new FloatStatistics();
        ratioOfSamplesPerFeature = new FloatStatistics();
        numberOfSamplesPerFeature = new FloatStatistics();
        interFeatureCorrelation = new FloatStatistics();
        intraFeatureCorrelation = new FloatStatistics();
        retentionTimeWindow  =new FloatStatistics();
        maximumRetentionTimeShiftPerFeature = new FloatStatistics();
        featureFWHM = new FloatStatistics();
        precursorMasses = new FloatStatistics();
    }

    /**
     * @return a vector with all statistics that can be used for k-means or t-sne plotting
     */
    @JsonProperty(value = "vector",access = JsonProperty.Access.READ_ONLY)
    public float[] vector() {
        final TFloatArrayList vec = new TFloatArrayList();
        for (FloatStatistics stats : new FloatStatistics[]{
                scanPointsPerFeaturesAt25,
                featureWidths,
                featureHeights,
                segmentsPerPeak,
                msmsPerPeak,
                msmsMergedCosines,
                numberOfPeaksPerMSMs,
                msmsEntropy,
                chimericPollution,
                numberOfCorrelatedPeaksPerFeature,
                numberOfIsotopePeaksPerFeature,
                isotopicCorrelation,
                retentionTimeShift,
                ratioOfSamplesPerFeature,
                numberOfSamplesPerFeature,
                interFeatureCorrelation,
                intraFeatureCorrelation,
                retentionTimeWindow,
                maximumRetentionTimeShiftPerFeature,
                featureFWHM,
                precursorMasses
        }) {
            vec.add(stats.lowQuantile());
            vec.add(stats.median());
            vec.add(stats.highQuantile());
            vec.add(stats.robustAverage());
            vec.add(stats.robustVariance());
        }
        for (float v : new float[]{
                adductGood, adductMedium, peakGood, peakMedium, isotopeGood, isotopeMedium, ms2Good, ms2Medium,
                hplus, sodium, potassium, waterLoss, ammonium, strangeAdducts, deletedFeatures, numberOfPeaksWithMassAbove1000
        }) {
            vec.add(v);
        }
        return vec.toArray();
    }



    public void collectFromSummary(Iterable<LCMSCompoundSummary> summaries) {
        int adductGood=0, adductMedium=0, peakGood=0, peakMedium=0, isotopeGood=0, isotopeMedium=0, ms2Good=0, ms2Medium=0;
        float n = 0f;
        for (LCMSCompoundSummary sum : summaries) {
            n += 1f;
            if (sum.getAdductQuality().compareTo(LCMSQualityCheck.Quality.GOOD) >= 0) {
                ++adductGood;
            }
            if (sum.getAdductQuality().compareTo(LCMSQualityCheck.Quality.MEDIUM) >= 0) {
                ++adductMedium;
            }
            if (sum.getPeakQuality().compareTo(LCMSQualityCheck.Quality.GOOD) >= 0) {
                ++peakGood;
            }
            if (sum.getPeakQuality().compareTo(LCMSQualityCheck.Quality.MEDIUM) >= 0) {
                ++peakMedium;
            }
            if (sum.getIsotopeQuality().compareTo(LCMSQualityCheck.Quality.GOOD) >= 0) {
                ++isotopeGood;
            }
            if (sum.getIsotopeQuality().compareTo(LCMSQualityCheck.Quality.MEDIUM) >= 0) {
                ++isotopeMedium;
            }
            if (sum.getMs2Quality().compareTo(LCMSQualityCheck.Quality.GOOD) >= 0) {
                ++ms2Good;
            }
            if (sum.getMs2Quality().compareTo(LCMSQualityCheck.Quality.MEDIUM) >= 0) {
                ++ms2Medium;
            }
        }
        this.adductGood = adductGood/n;
        this.adductMedium = adductMedium/n;
        this.peakGood = peakGood/n;
        this.peakMedium = peakMedium /n;
        this.isotopeGood = isotopeGood/n;
        this.isotopeMedium = isotopeMedium/n;
        this.ms2Good = ms2Good/n;
        this.ms2Medium = ms2Medium/n;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
    public static class FloatStatistics {
        private final TFloatArrayList values;
        private double sum;
        private boolean dirty=false;

        public FloatStatistics() {
            this.values = new TFloatArrayList();
        }

        public void add(float[] values) {
            this.values.addAll(values);
            dirty=true;
        }

        public void add(float value) {
            values.add(value);
            dirty=true;
        }
        public void add(double[] values) {
            for (double v : values) this.values.add((float)v);
            dirty=true;
        }

        public void add(double value) {
            values.add((float)value);
            dirty=true;
        }

        private void done() {
            if (dirty) {
                values.sort();
                sum = values.sum();
                dirty=false;
            }
        }
        @JsonProperty(value = "numberOfValues",access = JsonProperty.Access.READ_ONLY)
        public int numberOfValues() {
            return values.size();
        }

        @JsonProperty(value = "robustStandardDeviation",access = JsonProperty.Access.READ_ONLY)
        public float robustVariance() {
            done();
            final int n = values.size();
            if (n <=10) return average();
            final int a = (n/10);
            final int b = n - a;
            double sum = 0d;
            for (int i=a; i < b; ++i) {
                sum += values.getQuick(i);
            }
            final float robustAverage = (float)(sum/(b-a));
            sum = 0d;
            for (int i=a; i < b; ++i) {
                double v = values.getQuick(i)-robustAverage;
                sum += v*v;
            }
            return (float)Math.sqrt((sum/(b-a)));
        }

        @JsonProperty(value = "standardDeviation",access = JsonProperty.Access.READ_ONLY)
        public float variance() {
            double avg = average();
            double sum  = 0d;
            for (int i=0; i < values.size(); ++i) {
                double delta = (values.getQuick(i)-avg);
                sum += delta*delta;
            }
            return (float)Math.sqrt(sum / values.size());
        }

        @JsonProperty(value = "lowQuantile",access = JsonProperty.Access.READ_ONLY)
        public float lowQuantile() {
            done();
            return values.getQuick(values.size()/4);
        }
        @JsonProperty(value = "highQuantile",access = JsonProperty.Access.READ_ONLY)
        public float highQuantile() {
            done();
            return values.getQuick((int)(values.size()*0.75));
        }
        @JsonProperty(value = "average",access = JsonProperty.Access.READ_ONLY)
        public float average() {
            done();
            return (float)(sum/values.size());
        }
        @JsonProperty(value = "robustAverage",access = JsonProperty.Access.READ_ONLY)
        public float robustAverage() {
            done();
            final int n = values.size();
            if (n <=10) return average();
            final int a = (n/10);
            final int b = n - a;
            double sum = 0d;
            for (int i=a; i < b; ++i) {
                sum += values.getQuick(i);
            }
            return (float)(sum/(b-a));
        }
        @JsonProperty(value = "median",access = JsonProperty.Access.READ_ONLY)
        public float median() {
            done();
            final int n = values.size();
            if (n %2==0) {
                double a = values.get(n/2 - 1), b = values.get(n/2);
                return (float)((a+b)/2d);
            } else {
                return values.get(n/2);
            }
        }
    }

}
