package de.unijena.bioinf.lcms.peakshape;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.ChromatographicPeak;

public class GaussianFitting implements PeakShapeFitting<GaussianShape> {
    @Override
    public GaussianShape fit(ProcessedSample sample, ChromatographicPeak peak, ChromatographicPeak.Segment segment) {
        // simple maximum likelihood estimator
        double mean = 0d; // use apex as mean
        double std = 0d;
        final int apex = segment.getApexIndex();
        double totalInt = 0d, maxIntensity = 0d;
        final Range<Integer> integerRange = segment.calculateFWHM(0.25);
        long rtA = peak.getRetentionTimeAt(integerRange.lowerEndpoint()), rtB = peak.getRetentionTimeAt(integerRange.upperEndpoint());

        for (int k=integerRange.lowerEndpoint(); k <= integerRange.upperEndpoint(); ++k) {
            final double intensity = peak.getIntensityAt(k);
            mean += peak.getRetentionTimeAt(k) * intensity;
            totalInt += intensity;
            maxIntensity = Math.max(intensity, maxIntensity);
        }
        mean /= totalInt;
        for (int k=integerRange.lowerEndpoint(); k <= integerRange.upperEndpoint(); ++k) {
            final double intensity = peak.getIntensityAt(k);
            double diffFromMean = peak.getRetentionTimeAt(k) - mean;
            std += (diffFromMean*diffFromMean)*intensity;
        }
        std = Math.sqrt(std / totalInt);

        // now calculate score as absolute difference between integrals of the function
        final NormalDistribution distribution = new NormalDistribution(mean, std*std);
        double error = 0d;
        double deltaRt = peak.getRetentionTimeAt(integerRange.upperEndpoint())-peak.getRetentionTimeAt(integerRange.lowerEndpoint());
        double nonMonotonicIntensity = 0d;
        for (int k=integerRange.lowerEndpoint(); k < integerRange.upperEndpoint(); ++k) {
            final double r = peak.getRetentionTimeAt(k);
            final double expectedIntensity = distribution.getDensity(r)/distribution.getDensity(mean);
            final double measuredIntensity = peak.getIntensityAt(k)/maxIntensity;
            final double r2 = peak.getRetentionTimeAt(k+1);
            final double expectedIntensity2 = distribution.getDensity(r2)/distribution.getDensity(mean);
            final double measuredIntensity2 = peak.getIntensityAt(k+1)/maxIntensity;
            double width = ((r2-r)/2)/deltaRt;
            error += Math.abs(expectedIntensity*width - measuredIntensity*width);
            error += Math.abs(expectedIntensity2*width - measuredIntensity2*width);

            if (expectedIntensity < expectedIntensity2) {
                nonMonotonicIntensity += Math.pow(Math.max(0, measuredIntensity-measuredIntensity2),2);
            } else if (expectedIntensity > expectedIntensity2) {
                nonMonotonicIntensity += Math.pow(Math.max(0, measuredIntensity2-measuredIntensity),2);
            }

        }
        error += Math.pow(Math.abs(mean-peak.getRetentionTimeAt(segment.getApexIndex()))/(rtB-rtA), 2  ) ;

        // penalize if edges of the peak are too large
        {
            error += Math.abs(peak.getIntensityAt(segment.getStartIndex())/maxIntensity)/10d;
            error += Math.abs(peak.getIntensityAt(segment.getEndIndex())/maxIntensity)/10d;
        }

        // penalize if peak is non-monotonic
        error += nonMonotonicIntensity;

        // penalize too large peak width
        final double meanPeakWidth = sample.getMeanPeakWidth()*2;
        error += 1d - new NormalDistribution(0d, meanPeakWidth * meanPeakWidth).getErrorProbability(std);

        // penalize not enough peaks on both sides above noise level
        double penalty = 0.3;
        for (int k=segment.getApexIndex(); k <= segment.getEndIndex(); ++k) {
            if (peak.getIntensityAt(k) >= sample.ms1NoiseModel.getSignalLevel(peak.getScanNumberAt(k), peak.getMzAt(k))) {
                penalty -= 0.1;
                if (penalty <= 0) break;
            }
        }
        error += penalty;
        penalty = 0d;
        for (int k=segment.getApexIndex(); k >= segment.getStartIndex(); --k) {
            if (peak.getIntensityAt(k) >= sample.ms1NoiseModel.getSignalLevel(peak.getScanNumberAt(k), peak.getMzAt(k))) {
                penalty -= 0.1;
                if (penalty <= 0) break;
            }
        }
        error += penalty;


        return new GaussianShape(-error, mean, std, maxIntensity);
    }


}
