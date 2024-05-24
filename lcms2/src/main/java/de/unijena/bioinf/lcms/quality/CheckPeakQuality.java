package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.lcms.adducts.TraceProvider;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;

import java.io.IOException;
import java.util.Locale;

public class CheckPeakQuality implements FeatureQualityChecker{
    @Override
    public void addToReport(QualityReport report, MergedLCMSRun run, AlignedFeatures feature, TraceProvider traceProvider) throws IOException {
        double[] retentionTimes = run.getRetentionTimeAxis().get().getRetentionTimes();
        double noise = run.getRetentionTimeAxis().get().getNoiseLevelPerScan()[feature.getTraceRef().absoluteApexId()];
        // majors
        QualityReport.Category peakQuality = new QualityReport.Category(QualityReport.PEAK_QUALITY);
        // 1. Peak should be clearly above noise level
        final double ratio = feature.getSnr();
        if (ratio >= 20) {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "peak intensity is %.1f-fold above noise intensity", ratio), DataQuality.GOOD, QualityReport.Weight.MAJOR
            ));
        } else if (ratio >= 5) {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "peak intensity is %.1f-fold above noise intensity", ratio), DataQuality.DECENT, QualityReport.Weight.MAJOR
            ));
        } else if (ratio > 1) {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "peak intensity is %.1f-fold above noise intensity", ratio), DataQuality.BAD, QualityReport.Weight.MAJOR
            ));
        } else {
            peakQuality.getItems().add(new QualityReport.Item(
                    "peak intensity is below noise level", DataQuality.LOWEST, QualityReport.Weight.MAJOR
            ));
        }

        // 2. Peak should be not too wide
        final double medianPeakWidthOfIntensivePeaks = run.getSampleStats().getMedianPeakWidthInSeconds();
        final double minimumPeakWidth = (retentionTimes[1]-retentionTimes[0])*6;

        if (feature.getFwhm() < minimumPeakWidth) {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "peak has too few data points (fwhm is %.1f seconds)", feature.getFwhm()), DataQuality.BAD, QualityReport.Weight.MAJOR
            ));
        } else {
            if (feature.getFwhm() <= 3*medianPeakWidthOfIntensivePeaks) {
                peakQuality.getItems().add(new QualityReport.Item(
                        String.format(Locale.US, "proper peak width of %.1f seconds", feature.getFwhm()), DataQuality.GOOD, QualityReport.Weight.MAJOR
                ));
            } else {
                peakQuality.getItems().add(new QualityReport.Item(
                        String.format(Locale.US, "peak is too wide with fwhm is %.1f seconds", feature.getFwhm()), DataQuality.BAD, QualityReport.Weight.MAJOR
                ));
            }
        }
        // peak should have at least one isotope peak
        if (feature.getIsotopicFeatures().isPresent() && feature.getIsotopicFeatures().get().size()>=1) {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "at least one isotope peak was detected in the LC/MS run", feature.getFwhm()), DataQuality.GOOD, QualityReport.Weight.MAJOR
            ));
        } else {
            peakQuality.getItems().add(new QualityReport.Item(
                    String.format(Locale.US, "no isotope peak was detected in the LC/MS run", feature.getFwhm()), DataQuality.BAD, QualityReport.Weight.MAJOR
            ));
        }

        // minors


        // 1. peak should have clearly defined start and end points
        final MergedTrace trace = traceProvider.getMergeTrace(feature).orElse(null);
        if (trace!=null) {
            float start = trace.getIntensities().getFloat(feature.getTraceRef().getStart());
            float end = trace.getIntensities().getFloat(feature.getTraceRef().getStart());
            double ratioStart = feature.getApexIntensity()/ start;
            double ratioEnd = feature.getApexIntensity() / end;
            boolean goodStart = (start <= 2*noise && ratioStart>3) || ratioStart>=10;
            boolean goodEnd = (end <= 2*noise && ratioEnd>3) || ratioEnd>=10;
            if (goodStart && goodEnd) {
                peakQuality.getItems().add(new QualityReport.Item(
                        "peak has clearly defined start and end points.", DataQuality.GOOD, QualityReport.Weight.MINOR
                ));
            } else if (goodStart) {
                peakQuality.getItems().add(new QualityReport.Item(
                        "the right edge of the peak is not clearly defined", DataQuality.BAD, QualityReport.Weight.MINOR
                ));
            } else if (goodEnd) {
                peakQuality.getItems().add(new QualityReport.Item(
                        "the left edge of the peak is not clearly defined", DataQuality.BAD, QualityReport.Weight.MINOR
                ));
            }
        }
        report.addCategory(peakQuality);
    }
}
