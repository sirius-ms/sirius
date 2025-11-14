package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.lcms.adducts.TraceProvider;
import de.unijena.bioinf.ms.persistence.model.core.DefaultQualityCategory;
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
        QualityReport.Category peakQuality = new QualityReport.Category(DefaultQualityCategory.ALIGNMENT_QUALITY);

        if (run.getRuns().isPresent()) {
            int maximalNumberOfSamples = run.getRuns().get().size();
            if (maximalNumberOfSamples <= 1) {
                peakQuality.getItems().add(new QualityReport.Item("There is nothing to align.", DataQuality.NOT_APPLICABLE, QualityReport.Weight.MAJOR));
                return;
            } else if (maximalNumberOfSamples <= 2) {
                peakQuality.getItems().add(new QualityReport.Item("There are only two samples to align.", DataQuality.NOT_APPLICABLE, QualityReport.Weight.MAJOR));
                return;
            } else if (maximalNumberOfSamples <= 3) {
                peakQuality.getItems().add(new QualityReport.Item("There are only three samples to align - not enough to assess alignment quality.", DataQuality.NOT_APPLICABLE, QualityReport.Weight.MAJOR));
                return;
            }
        }

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
                DoubleArrayList correctedCorrelations = new DoubleArrayList();
                MergedTrace mergedTrace = maybeMergedTrace.get();
                final float[] mergedIntensities = mergedTrace.getIntensities().subList(feature.getTraceRef().getStart(),
                        feature.getTraceRef().getEnd()+1).toFloatArray();

                {
                    float averageIntensity=0f;
                    for (float value : mergedIntensities) averageIntensity+=value;
                    averageIntensity /= mergedIntensities.length;
                    for (int k=0; k < mergedIntensities.length; ++k) {
                        mergedIntensities[k] -= averageIntensity;
                    }
                }

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
                        final double maximumInt = fls.doubleStream().max().orElse(1d);
                        TraceRef r = sourceTrace.get().left();
                        double avg = 0f;
                        for (int k=r.getStart(); k < r.getEnd(); ++k) avg+=fls.getFloat(k)/maximumInt;
                        avg /= (r.getEnd()-r.getStart()+1);

                        double correlation = 0d;
                        double yy = 0d;
                        for (int i=r.getStart(); i <= r.getEnd(); ++i) {
                            final double lv = fls.getFloat(i)/maximumInt - avg;
                            final int shiftedIndex = (i+r.getScanIndexOffsetOfTrace()) - offset;
                            final double rv = (shiftedIndex >= 0 && shiftedIndex < mergedIntensities.length) ? mergedIntensities[shiftedIndex] : 0f;

                            correlation += lv*rv;
                            yy += lv*lv;
                        }
                        correlation = correlation/Math.sqrt(xx*yy);

                        correlations.add(correlation);
                        {
                            // move the apex of the trace ontop the apex of the merged trace and repeat analysis
                            // if correlation gets much better, this is a sign that the recalibration did a bad job
                            double correlation2 = 0d;
                            double yy2 = 0d;
                            int sourceOffset = r.getApex() - (feature.getTraceRef().getApex() - feature.getTraceRef().getStart());
                            double xx2 = 0f;
                            for (int indexMerged=0; indexMerged < mergedIntensities.length; ++indexMerged) {
                                final int indexSource = sourceOffset+indexMerged;
                                final double lv = ((indexSource>=0&&indexSource<fls.size()) ? fls.getFloat(indexSource) : 0f)/maximumInt - avg;
                                final double rv = mergedIntensities[indexMerged];
                                correlation2 += lv*rv;
                                yy2 += lv*lv;
                                xx2 += rv*rv;
                            }
                            for (int indexSource = mergedIntensities.length+sourceOffset; indexSource <= r.getEnd(); ++indexSource) {
                                final double lv = fls.getFloat(indexSource);
                                yy2 += lv*lv;
                            }
                            correlation2 = correlation2/Math.sqrt(xx2*yy2);
                            correctedCorrelations.add(correlation2);
                        }
                    }
                }
                correlations.sort(null);
                correctedCorrelations.sort(null);
                final double medianCorrelation = correlations.getDouble(correlations.size()/2);
                final double percentil25 = correlations.getDouble((int)Math.floor(correlations.size()*0.25));
                final double medianCorrected = correctedCorrelations.getDouble(correctedCorrelations.size()/2);
                final double medianCorrected25 = correctedCorrelations.getDouble((int)Math.floor(correlations.size()*0.25));

                if (medianCorrelation >= 0.8 && percentil25 >= 0.75) {
                    peakQuality.getItems().add(new QualityReport.Item(
                            String.format("High correlation between traces and consensus trace (median = %.2f, 25%% quantile is %.2f)",
                                    medianCorrelation, percentil25),
                            DataQuality.GOOD, QualityReport.Weight.MAJOR
                    ));
                } else if (medianCorrelation >= 0.75 && percentil25 >= 0.6) {
                    peakQuality.getItems().add(new QualityReport.Item(
                            String.format("Decent correlation between traces and consensus trace (median = %.2f, 25%% quantile is %.2f)",
                                    medianCorrelation, percentil25),
                            DataQuality.DECENT, QualityReport.Weight.MAJOR
                    ));
                } else if (medianCorrected >= 0.8 && medianCorrected25>=0.75) {
                    peakQuality.getItems().add(new QualityReport.Item(
                            String.format("Strong calibration error for some of the samples. After shifting, correlation between traces and consensus trace is quite good (median = %.2f, 25%% quantile is %.2f).",
                                    medianCorrected,medianCorrected25),
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
