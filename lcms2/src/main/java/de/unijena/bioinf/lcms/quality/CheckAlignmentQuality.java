package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.lcms.adducts.TraceProvider;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AbstractFeature;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;

import java.util.Arrays;
import java.util.List;

public class CheckAlignmentQuality implements FeatureQualityChecker{
    @Override
    public void addToReport(QualityReport report, MergedLCMSRun run, AlignedFeatures feature, TraceProvider provider) {
        // majors
        QualityReport.Category peakQuality = new QualityReport.Category(QualityReport.ALIGNMENT_QUALITY);

        // 1. number of alignments
        final int medianAl = Math.max((int)(run.getRuns().map(List::size).orElse(0) * 0.15), run.getSampleStats().getMedianNumberOfAlignments());
        int minimumNumber = (int)Math.max(2, medianAl * 0.1);

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

        // minors
        // check if there is a minimum number of intensive features
        double[] ints = feature.getFeatures().stream().flatMap(List::stream).mapToDouble(AbstractFeature::getApexIntensity).toArray();
        double max = Arrays.stream(ints).max().orElse(1d);
        int intensiveFeatures = (int)Arrays.stream(ints).filter(x->x>max*0.33d).count();
        if (intensiveFeatures < minimumNumber) {
            peakQuality.getItems().add(new QualityReport.Item(
                    "feature alignment is very imbalanced with only " + intensiveFeatures + " have a high apex intensity.", DataQuality.BAD, QualityReport.Weight.MINOR
            ));
        } else {
            peakQuality.getItems().add(new QualityReport.Item(
                    "feature alignment is decently balanced with" + intensiveFeatures + " have a high apex intensity.", DataQuality.GOOD, QualityReport.Weight.MINOR
            ));
        }
        report.addCategory(peakQuality);
    }
}
