package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.lcms.adducts.TraceProvider;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AbstractFeature;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CheckAlignmentQuality implements FeatureQualityChecker{
    @Override
    public void addToReport(QualityReport report, MergedLCMSRun run, AlignedFeatures feature, TraceProvider provider) throws IOException {
        // majors
        QualityReport.Category peakQuality = new QualityReport.Category(QualityReport.ALIGNMENT_QUALITY);

        if (feature.getFeatures().isEmpty()) {
            peakQuality.getItems().add(new QualityReport.Item("There are no aligned features.", DataQuality.LOWEST, QualityReport.Weight.CRITICAL));
            return;
        }

        // 1. number of alignments
        final int medianAl = Math.max((int)(run.getRuns().map(List::size).orElse(0) * 0.15), run.getSampleStats().getMedianNumberOfAlignments());
        int minimumNumber = (int)Math.max(3, medianAl * 0.1);

        int actualNumber = feature.getFeatures().map(List::size).orElse(0); //robust against empty features but they should not occur?

        if (actualNumber == 0 || actualNumber < minimumNumber) {
            peakQuality.getItems().add(new QualityReport.Item(
                    "feature alignment consists of only " + actualNumber + " features.", DataQuality.LOWEST, QualityReport.Weight.MAJOR
            ));
        } else if (actualNumber < medianAl) {
            peakQuality.getItems().add(new QualityReport.Item(
                    "feature alignment consists of " + actualNumber + " features, less than the median which is " + medianAl, DataQuality.DECENT, QualityReport.Weight.MAJOR
            ));
        } else {
            peakQuality.getItems().add(new QualityReport.Item(
                    "feature alignment consists of " + actualNumber + " features.", DataQuality.GOOD, QualityReport.Weight.MAJOR
            ));
        }

        if (actualNumber > 2) {

            // 2. retention time deviations
            double retentionTimeDeviationsInSeconds = run.getSampleStats().getRetentionTimeDeviationsInSeconds();
            final double w = feature.getRetentionTime().getRetentionTimeInSeconds();
            final double std = feature.getFeatures().get().stream().mapToDouble(x -> Math.abs(x.getRetentionTime().getRetentionTimeInSeconds() - w)).average().orElse(0d);
            final double w2 = feature.getRetentionTime().getStartTime()+(feature.getRetentionTime().getEndTime()-feature.getRetentionTime().getStartTime())/2d;
            final double std2 = feature.getFeatures().get().stream().mapToDouble(x ->
                    Math.abs((x.getRetentionTime().getStartTime()+(x.getRetentionTime().getEndTime()-x.getRetentionTime().getStartTime())/2d) - w2)).average().orElse(0d);
            final double deviation = Math.min(std,std2);
            double relativeDeviationToStd = Math.min(std,std2) / retentionTimeDeviationsInSeconds;

            if (relativeDeviationToStd < 1) {
                peakQuality.getItems().add(new QualityReport.Item(
                        String.format(Locale.US, "low average retention time error of %.1f s", deviation), DataQuality.GOOD, QualityReport.Weight.MAJOR
                ));
            } else if (relativeDeviationToStd < 2) {
                peakQuality.getItems().add(new QualityReport.Item(
                        String.format(Locale.US, "medium average retention time error of %.1f s", deviation), DataQuality.DECENT, QualityReport.Weight.MAJOR
                ));
            } else if (relativeDeviationToStd < 3) {
                peakQuality.getItems().add(new QualityReport.Item(
                        String.format(Locale.US, "high average retention time error of %.1f s", deviation), DataQuality.BAD, QualityReport.Weight.MAJOR
                ));
            } else {
                peakQuality.getItems().add(new QualityReport.Item(
                        String.format(Locale.US, "very high average retention time error of %.1f s", deviation), DataQuality.LOWEST, QualityReport.Weight.MAJOR
                ));
            }

            /*
            // check if alignment is uniformly distributed
            if (feature.getFeatures().isPresent()) {

                double[] apexTimes = feature.getFeatures().get().stream().mapToDouble(x->x.getRetentionTime().getRetentionTimeInSeconds()).toArray();
                double[] middleTimes = feature.getFeatures().get().stream().mapToDouble(x->x.getRetentionTime().getStartTime() + (x.getRetentionTime().getEndTime()-x.getRetentionTime().getStartTime())/2d).toArray();
                final double[] rets;
                {
                    int c=0; double a=0, b=0;
                    for (int i=0; i < apexTimes.length; ++i) {
                        for (int j=0; j < i; ++j) {
                            ++c;
                            a += Math.pow(apexTimes[i]-apexTimes[j], 2);
                            b += Math.pow(middleTimes[i]-middleTimes[j], 2);
                        }
                    }
                    a/=c;
                    b/=c;
                    if (a < b) {
                        rets = apexTimes;
                    } else {
                        rets = middleTimes;
                    }
                }


                Arrays.sort(rets);
                double largestGap = 0;
                int gapPos=0;
                for (int k=1; k < rets.length; ++k) {
                    final double gap = (rets[k]-rets[k-1]);
                    if (gap>largestGap) {
                        largestGap = gap;
                        gapPos = k;
                    }
                }
                // divide into two clusters and compute inner-cluster variances
                double innerClusterStd;
                if (gapPos >= rets.length/2) {
                    double dev = 0d; int count=0;
                    for (int i=0; i < gapPos; ++i) {
                        for (int j=0; j < i; ++j) {
                            dev += Math.pow(rets[i]-rets[j],2);
                            ++count;
                        }
                    }
                    innerClusterStd = Math.sqrt(dev);
                } else {
                    double dev = 0d; int count=0;
                    for (int i=gapPos; i < rets.length; ++i) {
                        for (int j=gapPos; j < i; ++j) {
                            dev += Math.pow(rets[i]-rets[j],2);
                            ++count;
                        }
                    }
                    dev /= count;
                    innerClusterStd = Math.sqrt(dev);
                }
                innerClusterStd = Math.max(innerClusterStd, 2*run.getSampleStats().getRetentionTimeDeviationsInSeconds());
                if (largestGap > 2*innerClusterStd) {
                    final int clusterSizeLeft = gapPos;
                    final int clusterSizeRight = rets.length-gapPos;
                    final int smallerCluster = Math.min(clusterSizeRight, clusterSizeLeft);
                    final double smallerClusterRatio = ((double)smallerCluster)/rets.length;
                    if (smallerCluster <= 1) {
                        peakQuality.getItems().add(new QualityReport.Item(
                                "feature alignment with a single outlier.", DataQuality.DECENT, QualityReport.Weight.MAJOR
                        ));
                    } else if (smallerClusterRatio <= 0.1) {
                        peakQuality.getItems().add(new QualityReport.Item(
                                "feature alignment with some outliers.", DataQuality.DECENT, QualityReport.Weight.MAJOR
                        ));
                    } else if (largestGap < 3*innerClusterStd){
                        peakQuality.getItems().add(new QualityReport.Item(
                                "no unambigous feature alignment. Could be different features instead.", DataQuality.BAD, QualityReport.Weight.MAJOR
                        ));
                    } else {
                        peakQuality.getItems().add(new QualityReport.Item(
                                "no unambigous feature alignment. Could be different features instead.", DataQuality.LOWEST, QualityReport.Weight.MAJOR
                        ));
                    }



                }
            }
             */

            // cosine similarity to consensus trace
            final Optional<MergedTrace> maybeMergedTrace = provider.getMergeTrace(feature);
            final double intThreshold;
            Long2DoubleMap intensityMap = provider.getIntensities(feature);
            {
                double[] intensities = intensityMap.values().toDoubleArray();
                Arrays.sort(intensities);
                double minThreshold = intensities[Math.max(0, intensities.length-3)];
                double maxThreshold = intensities[intensities.length-1]*0.33;
                intThreshold = Math.min(minThreshold, maxThreshold);
            }
            // take at least
            if (maybeMergedTrace.isPresent() && feature.getFeatures().isPresent()) {
                DoubleArrayList correlations = new DoubleArrayList();
                MergedTrace mergedTrace = maybeMergedTrace.get();
                final float[] mergedIntensities = mergedTrace.getIntensities().subList(feature.getTraceRef().getStart(),
                        feature.getTraceRef().getEnd()+1).toFloatArray();
                /*
                {
                    float averageIntensity=0f;
                    for (float value : mergedIntensities) averageIntensity+=value;
                    averageIntensity /= mergedIntensities.length;
                    for (int k=0; k < mergedIntensities.length; ++k) {
                        mergedIntensities[k] -= averageIntensity;
                    }
                }
                 */
                double xx=0d;
                for (int k=0; k < mergedIntensities.length; ++k) {
                    xx += mergedIntensities[k]*mergedIntensities[k];
                }
                final int offset = feature.getTraceRef().getScanIndexOffsetOfTrace() + feature.getTraceRef().getStart();;
                for (Feature f : feature.getFeatures().get()) {
                    if (intensityMap.get(f.getRunId()) < intThreshold) continue;
                    Optional<Pair<TraceRef, SourceTrace>> sourceTrace = provider.getSourceTrace(feature, f.getRunId());
                    if (sourceTrace.isPresent()) {
                        FloatList fls = sourceTrace.get().right().getIntensities();
                        TraceRef r = sourceTrace.get().left();
                        double correlation = 0d;
                        double yy = 0d;
                        for (int i=r.getStart(); i <= r.getEnd(); ++i) {
                            final float lv = fls.getFloat(i); //- avg;
                            final int shiftedIndex = (i+r.getScanIndexOffsetOfTrace()) - offset;
                            final float rv = (shiftedIndex >= 0 && shiftedIndex < mergedIntensities.length) ? mergedIntensities[shiftedIndex] : 0f;

                            correlation += lv*rv;
                            yy += lv*lv;
                        }
                        correlation = correlation/Math.sqrt(xx*yy);

                        correlations.add(correlation);
                    }
                }
                correlations.sort(null);
                final double medianCorrelation = correlations.getDouble(correlations.size()/2);
                final double percentil25 = correlations.getDouble((int)Math.floor(correlations.size()*0.25));

                if (medianCorrelation >= 0.8 && percentil25 >= 0.75) {
                    peakQuality.getItems().add(new QualityReport.Item(
                            String.format("High correlation between traces and consensus trace (median = %.2f, 25%% quantile is %.2f)",
                                    medianCorrelation, percentil25),
                            DataQuality.GOOD, QualityReport.Weight.MAJOR
                    ));
                } else if (medianCorrelation >= 0.75 && percentil25 >= 0.6)  {
                    peakQuality.getItems().add(new QualityReport.Item(
                            String.format("Decent correlation between traces and consensus trace (median = %.2f, 25%% quantile is %.2f)",
                                    medianCorrelation, percentil25),
                            DataQuality.DECENT, QualityReport.Weight.MAJOR
                    ));
                } else if (medianCorrelation >= 0.5 && percentil25 >= 0.5) {
                    peakQuality.getItems().add(new QualityReport.Item(
                            String.format("Bad correlation between traces and consensus trace (median = %.2f, 25%% quantile is %.2f)",
                                    medianCorrelation, percentil25),
                            DataQuality.BAD, QualityReport.Weight.MAJOR
                    ));
                } else {
                    peakQuality.getItems().add(new QualityReport.Item(
                            String.format("Very bad correlation between traces and consensus trace (median = %.2f, 25%% quantile is %.2f)",
                                    medianCorrelation, percentil25),
                            DataQuality.LOWEST, QualityReport.Weight.MAJOR
                    ));
                }

            }



            // minors
            // check if there is a minimum number of intensive features
            double[] ints = feature.getFeatures().stream().flatMap(List::stream).mapToDouble(AbstractFeature::getApexIntensity).toArray();
            double max = Arrays.stream(ints).max().orElse(1d);
            int intensiveFeatures = (int) Arrays.stream(ints).filter(x -> x > max * 0.33d).count();
            if (intensiveFeatures < minimumNumber) {
                peakQuality.getItems().add(new QualityReport.Item(
                        "feature alignment is very imbalanced with only " + intensiveFeatures + " have a high apex intensity.", DataQuality.BAD, QualityReport.Weight.MINOR
                ));
            } else {
                peakQuality.getItems().add(new QualityReport.Item(
                        "feature alignment is decently balanced with" + intensiveFeatures + " have a high apex intensity.", DataQuality.GOOD, QualityReport.Weight.MINOR
                ));
            }

        }



        report.addCategory(peakQuality);
    }
}
