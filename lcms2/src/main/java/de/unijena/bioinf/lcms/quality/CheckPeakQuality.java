package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.lcms.adducts.TraceProvider;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CheckPeakQuality implements FeatureQualityChecker{
    @Override
    public void addToReport(QualityReport report, MergedLCMSRun run, AlignedFeatures feature, TraceProvider traceProvider) throws IOException {
        double[] retentionTimes = run.getRetentionTimeAxis().get().getRetentionTimes();
        double noise = run.getRetentionTimeAxis().get().getNoiseLevelPerScan()[feature.getTraceRef().absoluteApexId()];
        // majors
        QualityReport.Category peakQuality = new QualityReport.Category(QualityReport.PEAK_QUALITY);
        // 1. Peak should be clearly above noise level
        final double ratio = feature.getSnr();

        final MergedTrace trace = traceProvider.getMergeTrace(feature).orElse(null);
        double ratio2 = ratio;
        final FloatList intensities = trace!=null ? trace.getIntensities() : null;
        Long2ObjectMap<SourceTrace> sourceTraces = traceProvider.getSourceTraces(feature);
        Long2DoubleMap featureIntensities = traceProvider.getIntensities(feature);
        Long2DoubleMap apexIntensities = featureIntensities;
        if (sourceTraces != null) {
            for (Long2ObjectMap.Entry<SourceTrace> pair : sourceTraces.long2ObjectEntrySet()) {
                FloatList intensitiesS = pair.getValue().getIntensities();
                float smallestSignal = Float.POSITIVE_INFINITY;
                for (int k = 0; k < intensitiesS.size(); ++k) {
                    if (intensitiesS.getFloat(k) > 0) {
                        smallestSignal = Math.min(smallestSignal, intensitiesS.getFloat(k));
                    }
                }
                ratio2 = Math.max(ratio2, apexIntensities.get(pair.getLongKey()) / smallestSignal);
            }
        }


        if (ratio2 >= 50 && ratio >= 3) {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "peak intensity is %.1f-fold above noise intensity", ratio), DataQuality.GOOD, QualityReport.Weight.MAJOR
            ));
        } else if (ratio2 >= 10 && ratio >= 1) {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "peak intensity is %.1f-fold above noise intensity", ratio), DataQuality.DECENT, QualityReport.Weight.MAJOR
            ));
        } else if (ratio2 > 10 || ratio > 2) {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "peak intensity is %.1f-fold above noise intensity", ratio), DataQuality.BAD, QualityReport.Weight.MAJOR
            ));
        } else {
            peakQuality.getItems().add(new QualityReport.Item(
                    "peak intensity is below noise level", DataQuality.LOWEST, QualityReport.Weight.MAJOR
            ));
        }

        // 2. Peak should be not too short
        int traceStart = feature.getTraceRef().getStart();
        int traceEnd = feature.getTraceRef().getEnd();
        final int dp = feature.getTraceRef().getEnd()- traceStart +1;
        if (dp <= 10) {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "peak has too few data points (%d datapoints in merged trace)", dp), DataQuality.BAD, QualityReport.Weight.MINOR
            ));
        } else if (dp <= 20) {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "peak has few data points (%d datapoints in merged trace)", dp), DataQuality.DECENT, QualityReport.Weight.MINOR
            ));
        } else {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "peak has many data points (%d datapoints in merged trace)", dp), DataQuality.GOOD, QualityReport.Weight.MINOR
            ));
        }
        // 2. Peak should be not too wide
        final double medianSquareness = run.getSampleStats().getMedianHeightDividedByPeakWidth();
        final double medianPeakWidth = run.getSampleStats().getMedianPeakWidthInSeconds();
        final double squareness = feature.getApexIntensity()/feature.getFwhm();
        {
            if (squareness >= medianSquareness) {
                peakQuality.getItems().add(new QualityReport.Item(
                        String.format(Locale.US, "peak has proper shape.", feature.getFwhm()), DataQuality.GOOD, QualityReport.Weight.MAJOR
                ));
            } else if (feature.getFwhm() >= medianPeakWidth && squareness >= medianSquareness*0.5) {
                peakQuality.getItems().add(new QualityReport.Item(
                        String.format(Locale.US, "peak is too wide with fwhm is %.1f seconds", feature.getFwhm()), DataQuality.DECENT, QualityReport.Weight.MAJOR
                ));
            } else if (feature.getFwhm() >= medianPeakWidth) {
                peakQuality.getItems().add(new QualityReport.Item(
                        String.format(Locale.US, "peak is too wide with fwhm is %.1f seconds", feature.getFwhm()), DataQuality.BAD, QualityReport.Weight.MAJOR
                ));
            }
        }
        // peak should have at least one isotope peak
        if (feature.getIsotopicFeatures().isPresent() && feature.getIsotopicFeatures().get().size()>=1) {
            /*
            // only punish, but don't give score as we do this already in isotope section
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "at least one isotope peak was detected in the LC/MS run", feature.getFwhm()), DataQuality.GOOD, QualityReport.Weight.MAJOR
            ));
             */
        } else {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "no isotope peak was detected in the LC/MS run", feature.getFwhm()), DataQuality.BAD, QualityReport.Weight.MAJOR
            ));
        }

        // peak should have a single apex
        QualityReport.Item multiApexItem = null;
        if (feature.getFeatures().isPresent()) {
            List<Feature> features = feature.getFeatures().get();
            double maximumIntensity = featureIntensities.values().doubleStream().max().orElse(0d);
            LongOpenHashSet runids = new LongOpenHashSet(featureIntensities.keySet().longStream().filter(x -> featureIntensities.get(x) >= maximumIntensity * 0.75).toArray());
            List<Feature> representativeFeatures = features.stream().filter(x->runids.contains(x.getRunId().longValue())).toList();

            float apexes = 0f;
            float noisyness = 0f;
            float mgn = 0f;
            for (Feature representativeFeature : representativeFeatures) {
                // check if there are multiple apexes
                Optional<Pair<TraceRef, SourceTrace>> sourceTrace = traceProvider.getSourceTrace(feature, representativeFeature.getRunId());
                if (sourceTrace.isPresent()) {
                    TraceRef ref = sourceTrace.get().key();
                    SourceTrace src = sourceTrace.get().value();

                    int srcFrom = ref.getStart()+ref.getScanIndexOffsetOfTrace();
                    int srcTo = ref.getEnd()+ref.getScanIndexOffsetOfTrace();
                    int mrgFrom = feature.getTraceRef().getStart()+feature.getTraceRef().getScanIndexOffsetOfTrace();
                    int mrgTo = feature.getTraceRef().getEnd()+feature.getTraceRef().getScanIndexOffsetOfTrace();
                    ///////////////////////////////////////////
                    int from = Math.max(0, Math.min(srcFrom, mrgFrom) - ref.getScanIndexOffsetOfTrace());
                    int to = Math.min(Math.max(srcTo, mrgTo) - ref.getScanIndexOffsetOfTrace(), src.getIntensities().size()-1);
                    ///////////////////////////////////////////
                    final float apxInt = src.getIntensities().getFloat(ref.getApex());
                    TraceStats stats = fastApexDet(src.getIntensities(), from, to, ref.getApex());
                    apexes += (stats.clearApexes.length-0.5f);
                    apexes += (stats.smallApexes.length/5f - 0.1f);
                    noisyness += (stats.noisyness-0.05f);
                    mgn += stats.apexMagnitude;
                }
                mgn /= representativeFeatures.size();
            }
            if (apexes > 0.75) {
                peakQuality.getItems().add(new QualityReport.Item(
                        ("There are many apexes within the trace segment."), DataQuality.LOWEST, QualityReport.Weight.MAJOR
                ));
            } else if (apexes > 0.5f || mgn < 2) {
                peakQuality.getItems().add(new QualityReport.Item(
                        ("The apex of the peak is not clearly defined."), DataQuality.BAD, QualityReport.Weight.MAJOR
                ));
            } else if (apexes <= -0.5*representativeFeatures.size() && mgn >= 10) {
                peakQuality.getItems().add(new QualityReport.Item(
                        ("The apex of the peak is precisely defined."), DataQuality.GOOD, QualityReport.Weight.MAJOR
                ));
            } else {
                peakQuality.getItems().add(new QualityReport.Item(
                        ("The apex of the peak is not clearly defined."), DataQuality.DECENT, QualityReport.Weight.MAJOR
                ));
            }

            if (noisyness > 0.25) {
                peakQuality.getItems().add(new QualityReport.Item(
                        ("Very noisy trace."), DataQuality.LOWEST, QualityReport.Weight.MAJOR
                ));
            } else if (noisyness > 0.1) {
                peakQuality.getItems().add(new QualityReport.Item(
                        ("Noisy trace."), DataQuality.BAD, QualityReport.Weight.MAJOR
                ));
            } else if (noisyness >= 0f) {
                peakQuality.getItems().add(new QualityReport.Item(
                        ("Slightly Noisy trace."), DataQuality.DECENT, QualityReport.Weight.MAJOR
                ));
            } else {
                peakQuality.getItems().add(new QualityReport.Item(
                        "Smoothly increasing and decreasing trace.", DataQuality.GOOD, QualityReport.Weight.MINOR
                ));
            }
        }


        // minors


        // 1. peak should have clearly defined start and end points

        if (trace!=null) {

            float start = intensities.getFloat(traceStart);
            float end = intensities.getFloat(feature.getTraceRef().getEnd());
            double ratioStart = feature.getApexIntensity()/ start;
            double ratioEnd = feature.getApexIntensity() / end;
            boolean goodStart = (start <= 2*noise && ratioStart>3) || ratioStart>=10;
            boolean goodEnd = (end <= 2*noise && ratioEnd>3) || ratioEnd>=10;
            if (goodStart && goodEnd) {
                peakQuality.getItems().add(new QualityReport.Item(
                        "peak has clearly defined start and end points.", DataQuality.GOOD, QualityReport.Weight.MAJOR
                ));
            } else if (goodStart) {
                if (ratioEnd >= 4) {
                    peakQuality.getItems().add(new QualityReport.Item(
                            "the right edge of the peak is not perfectly defined", DataQuality.DECENT, QualityReport.Weight.MAJOR
                    ));
                } else {
                    peakQuality.getItems().add(new QualityReport.Item(
                            "the right edge of the peak is not clearly defined", DataQuality.BAD, QualityReport.Weight.MAJOR
                    ));
                }
            } else if (goodEnd) {
                if (ratioStart >= 4) {
                    peakQuality.getItems().add(new QualityReport.Item(
                            "the left edge of the peak is not perfectly defined", DataQuality.DECENT, QualityReport.Weight.MAJOR
                    ));
                } else {
                    peakQuality.getItems().add(new QualityReport.Item(
                            "the left edge of the peak is not clearly defined", DataQuality.BAD, QualityReport.Weight.MAJOR
                    ));
                }
            } else {
                if (ratioStart>=4 && ratioEnd>=4) {
                    peakQuality.getItems().add(new QualityReport.Item(
                            "the peak has no clearly defined edges.", DataQuality.BAD, QualityReport.Weight.MAJOR
                    ));
                } else {
                    peakQuality.getItems().add(new QualityReport.Item(
                            "the peak has no clearly defined edges.", DataQuality.LOWEST, QualityReport.Weight.MAJOR
                    ));
                }
            }
        }
        report.addCategory(peakQuality);
    }

    record TraceStats (int[] clearApexes, int[] smallApexes, double noisyness, double apexMagnitude) {}

    private TraceStats fastApexDet(FloatList intensities, int from, int to, int apex) {
        final float apexIntensity = intensities.getFloat(apex);
        final float threshold1 = apexIntensity*0.66f, threshold2 = apexIntensity*0.33f;
        final float valleyThreshold1 = apexIntensity*0.15f, valleyThreshold2 = apexIntensity*0.08f;
        final FloatArrayList noisyness = new FloatArrayList();
        IntArrayList apexes = new IntArrayList(), smallApexes = new IntArrayList();
        float totalMinIntensity = Float.POSITIVE_INFINITY;
        // search left
        {
            float lastApex = apexIntensity;
            float minIntensity = Float.POSITIVE_INFINITY;
            for (int k=apex-1; k >= from; --k) {
                float f = intensities.getFloat(k);
                final boolean decreasing = f <intensities.getFloat(k+1);
                if (decreasing) {
                    if (f>0) {
                        minIntensity = Math.min(minIntensity, f);
                        totalMinIntensity = Math.min(totalMinIntensity, f);
                    }
                } else if (k>0 && intensities.getFloat(k-1)<= f) {
                    // check if apex is large enough
                    final float valley = Float.isFinite(minIntensity) ? Math.min(lastApex-minIntensity, f - minIntensity) : 0f;
                    if (valley>0) {
                        noisyness.add(valley/apexIntensity);
                    }
                    if (valley >= valleyThreshold1 && f>=threshold1) {
                        apexes.add(k);
                        lastApex = f;
                        minIntensity = Float.POSITIVE_INFINITY;
                    } else if (valley >= valleyThreshold2 && f >= threshold2 ) {
                        smallApexes.add(k);
                    }
                }
            }
        }
        // search right
        {
            float lastApex = apexIntensity;
            float minIntensity = Float.POSITIVE_INFINITY;
            for (int k=apex+1; k <= to; ++k) {
                float f = intensities.getFloat(k);
                final boolean decreasing = f < intensities.getFloat(k-1);
                if (decreasing) {
                    if (f>0) {
                        minIntensity = Math.min(minIntensity, f);
                        totalMinIntensity = Math.min(totalMinIntensity, f);
                    }
                } else if (k+1<intensities.size() && intensities.getFloat(k+1)<= f) {
                    // check if apex is large enough
                    final float valley = Float.isFinite(minIntensity) ? Math.min(lastApex-minIntensity, f - minIntensity) : 0f;
                    if (valley>0) {
                        noisyness.add(valley/apexIntensity);
                    }
                    if (valley >= valleyThreshold1 && f>=threshold1) {
                        apexes.add(k);
                        lastApex = f;
                        minIntensity = Float.POSITIVE_INFINITY;
                    } else if (valley >= valleyThreshold2 && f >= threshold2 ) {
                        smallApexes.add(k);
                    }
                }
            }
        }
        double nois;
        if (noisyness.size()>=5) {
            noisyness.sort(null);
            nois=noisyness.subList(noisyness.size()-5,noisyness.size()).doubleStream().average().orElse(0d);
        } else nois=0d;

        return new TraceStats(apexes.toIntArray(), smallApexes.toIntArray(),nois, apexIntensity/totalMinIntensity);
    }
}
