package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.lcms.adducts.Scorer;
import de.unijena.bioinf.lcms.adducts.TraceProvider;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AbstractFeature;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedIsotopicFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class CheckIsotopeQuality implements FeatureQualityChecker{
    @Override
    public void addToReport(QualityReport report, MergedLCMSRun run, AlignedFeatures feature, TraceProvider traceProvider) throws IOException {
        // majors
        QualityReport.Category peakQuality = new QualityReport.Category(QualityReport.ISOTOPE_QUALITY);
        // 1. if LC/MS exists, check correlation
        if (feature.getIsotopicFeatures().isPresent()) {
            checkLC(feature, traceProvider, peakQuality);
        }
        // 2. check number of isotope peaks
        if (feature.getMSData().isEmpty()) return;
        IsotopePattern isotopePattern = feature.getMSData().get().getIsotopePattern();
        if (isotopePattern !=null) {
            if (isotopePattern.size()>=3) {
                peakQuality.getItems().add(new QualityReport.Item(
                        "Isotope pattern consists of " + isotopePattern.size() + " peaks.",
                        DataQuality.GOOD, QualityReport.Weight.MAJOR

                ));
            } else if (isotopePattern.size()==2) {
                peakQuality.getItems().add(new QualityReport.Item(
                        "Isotope pattern consists of " + isotopePattern.size() + " peaks.",
                        DataQuality.DECENT, QualityReport.Weight.MAJOR

                ));
            } else if (isotopePattern.size()<=1) {
                peakQuality.getItems().add(new QualityReport.Item(
                        "There is no isotope pattern for this feature.",
                        DataQuality.BAD, QualityReport.Weight.MAJOR

                ));
            }
        }
        report.addCategory(peakQuality);
    }


    private void checkLC(AlignedFeatures feature, TraceProvider traceProvider, QualityReport.Category report) throws IOException {
        MergedTrace left = traceProvider.getMergeTrace(feature).orElse(null);
        if (left==null) return;
        Long2DoubleMap intensitiesLeft = traceProvider.getIntensities(feature);
        List<AlignedIsotopicFeatures> isotopicFeatures = feature.getIsotopicFeatures().get();

        double mostIntensiveOne = isotopicFeatures.stream().mapToDouble(AbstractFeature::getApexIntensity).max().orElse(0d);

        for (int k=0; k < isotopicFeatures.size(); ++k) {
            AlignedIsotopicFeatures iso = isotopicFeatures.get(k);
            Long2DoubleMap intensitiesRight = traceProvider.getIntensities(iso);
            MergedTrace right = traceProvider.getMergeTrace(iso).orElse(null);
            if (right==null) continue;
            double correlation = Scorer.correlateTraces(feature.getTraceRef(), left, iso.getTraceRef(), right);
            double reprCorrelation = Scorer.correlateRepresentatives(
                    traceProvider,feature, iso, intensitiesLeft, intensitiesRight
            );
            double stability = Scorer.correlateAcrossSamples(intensitiesLeft, intensitiesRight);
            final boolean mostIntensive = (iso.getApexIntensity()>=mostIntensiveOne);
            final QualityReport.Weight weight = mostIntensive ? QualityReport.Weight.MAJOR : QualityReport.Weight.MINOR;
            final double betterCorrelation = Math.max(correlation, reprCorrelation);
            String betterCorrelationSource = (correlation>reprCorrelation) ? "representative trace" : "merged trace";

            if (betterCorrelation <= (mostIntensive ? 0.9 : 0.8)) {
                report.getItems().add(new QualityReport.Item(
                        String.format(Locale.US,
                                "%d-th isotope peak has a low correlation of %.2f in the %s",
                                k+1, betterCorrelation, betterCorrelationSource),
                        DataQuality.BAD,
                        weight
                ));
            } else {
                report.getItems().add(new QualityReport.Item(
                        String.format(Locale.US,
                                "%d-th isotope peak has a high correlation of %.2f in the %s",
                                k+1, betterCorrelation, betterCorrelationSource),
                        DataQuality.GOOD,
                        weight
                ));
            }

            if (correlation < 0.7 || reprCorrelation < 0.7) {
                report.getItems().add(new QualityReport.Item(
                        String.format(Locale.US,
                                "for the %d-th isotope peak the correlation between merged trace and representative trace differ a lot (%.2f vs. %.2f)",
                                k+1, correlation, reprCorrelation),
                        DataQuality.BAD,
                        weight
                ));
            }

            if (stability < 0) {
                report.getItems().add(new QualityReport.Item(
                        String.format(Locale.US,
                                "for the %d-th isotope peak the intensity ratios across samples differ a lot",
                                k+1),
                        DataQuality.BAD,
                        weight
                ));
            } else if (stability < 10) {
                report.getItems().add(new QualityReport.Item(
                        String.format(Locale.US,
                                "for the %d-th isotope peak the intensity ratios across samples differ slightly",
                                k+1),
                        DataQuality.DECENT,
                        weight
                ));
            } else {
                report.getItems().add(new QualityReport.Item(
                        String.format(Locale.US,
                                "for the %d-th isotope peak the intensity ratios across samples are very stable.",
                                k+1),
                        DataQuality.GOOD,
                        weight
                ));
            }


        }


    }
}
