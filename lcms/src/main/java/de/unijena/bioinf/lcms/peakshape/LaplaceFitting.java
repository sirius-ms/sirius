package de.unijena.bioinf.lcms.peakshape;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.ChromatographicPeak;
import org.apache.commons.math3.distribution.LaplaceDistribution;

public class LaplaceFitting implements PeakShapeFitting<LaplaceShape> {
    @Override
    public LaplaceShape fit(ProcessedSample sample, ChromatographicPeak peak, ChromatographicPeak.Segment segment) {
        long median = peak.getRetentionTimeAt(segment.getApexIndex()); // just use apex instead
        double std = 0d;
        final int apex = segment.getApexIndex();
        double totalInt = 0d, maxIntensity = 0d;
        final Range<Integer> integerRange = segment.calculateFWHM(0.25);
        long rtA = peak.getRetentionTimeAt(integerRange.lowerEndpoint()), rtB = peak.getRetentionTimeAt(integerRange.upperEndpoint());
        for (int k=integerRange.lowerEndpoint(); k <= integerRange.upperEndpoint(); ++k) {
            final double intensity = peak.getIntensityAt(k);
            double diffFromMean = peak.getRetentionTimeAt(k) - median;
            std += Math.abs(diffFromMean*intensity);
            totalInt += intensity;
            maxIntensity = Math.max(intensity, maxIntensity);
        }
        std /= totalInt;
        if (std <= 0) return new LaplaceShape(Double.NEGATIVE_INFINITY, median, 1e-8, maxIntensity);
        // now calculate score as absolute difference between integrals of the function
        final LaplaceDistribution distribution = new LaplaceDistribution(median, std);
        double error = 0d;
        double deltaRt = peak.getRetentionTimeAt(integerRange.upperEndpoint())-peak.getRetentionTimeAt(integerRange.lowerEndpoint());
        double nonMonotonicIntensity = 0d;
        for (int k=integerRange.lowerEndpoint(); k < integerRange.upperEndpoint(); ++k) {
            final double r = peak.getRetentionTimeAt(k);
            final double expectedIntensity = distribution.density(r)/distribution.density(median);
            final double measuredIntensity = peak.getIntensityAt(k)/maxIntensity;
            final double r2 = peak.getRetentionTimeAt(k+1);
            final double expectedIntensity2 = distribution.density(r2)/distribution.density(median);
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
        // add the deviation of the apex from the mean
        error += Math.pow(Math.abs(median-peak.getRetentionTimeAt(segment.getApexIndex()))/(rtB-rtA) , 2 ) ;

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

        return new LaplaceShape(-error, median, std, maxIntensity);
    }

}
