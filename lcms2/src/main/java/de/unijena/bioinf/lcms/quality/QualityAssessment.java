package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.lcms.adducts.TraceProvider;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;

import java.io.IOException;
import java.util.List;

public class QualityAssessment implements FeatureQualityChecker {

    protected List<FeatureQualityChecker> checkerList;

    public QualityAssessment() {
        this(List.of(
                new CheckPeakQuality(),
                new CheckAlignmentQuality(),
                new CheckIsotopeQuality(),
                new CheckMs2Quality(),
                new CheckAdductQuality()
        ));
    }

    public QualityAssessment(List<FeatureQualityChecker> checkerList) {
        this.checkerList = checkerList;
    }

    @Override
    public void addToReport(QualityReport report, MergedLCMSRun run, AlignedFeatures feature, TraceProvider traceProvider) throws IOException {
        // 1. make checks
        for (FeatureQualityChecker c : checkerList) {
            c.addToReport(report,run,feature,traceProvider);
        }

        // 2. do scoring
        float score = 0f; int count=0;
        for (QualityReport.Category c : report.getCategories().values()) {
            c.setOverallQuality(scoreCategory(c));
            if (c.getOverallQuality()!=DataQuality.NOT_APPLICABLE) {
                score += c.getOverallQuality().getScore();
                ++count;
            }
        }
        float c = report.getCategories().get(QualityReport.MS2_QUALITY).getOverallQuality().getScore();
        if (count==0) {
            report.setOverallQuality(DataQuality.NOT_APPLICABLE);
        } else {
            score /= count;
            if (score >= 2.5 && c>=2) {
                report.setOverallQuality(DataQuality.GOOD);
            } else if (score >= 1.6) {
                report.setOverallQuality(DataQuality.DECENT);
            } else if (score >= 0.6) {
                report.setOverallQuality(DataQuality.BAD);
            } else report.setOverallQuality(DataQuality.LOWEST);
        }
        // we add some additional rules:
        // - whenever we have a good MS2 we KEEP the data. ALWAYS.
        // - if almost everything is bad (including the spectra) we downgrade the quality level to lowest
        //   this essentially marks a compound for "deletion".
        float c2 = report.getCategories().get(QualityReport.PEAK_QUALITY).getOverallQuality().getScore();
        float c3 = report.getCategories().get(QualityReport.ADDUCT_QUALITY).getOverallQuality().getScore();
        float c4 = report.getCategories().get(QualityReport.ISOTOPE_QUALITY).getOverallQuality().getScore();
        if (report.getOverallQuality().getScore() > 1 || c >= 2 || Math.max(c2,Math.max(c3,c4))>=3 || (c2+c3+c4) >= 5 ) {
            if (report.getOverallQuality().getScore()<0) report.setOverallQuality(DataQuality.BAD);
        } else {
            report.setOverallQuality(DataQuality.LOWEST);
        }

    }


    /**
     * Take the average between best and worst score of majors and use minors as tie breaker
     */
    public static DataQuality scoreCategory(QualityReport.Category category) {
        if (category.getItems().isEmpty()) return DataQuality.NOT_APPLICABLE;
        float minScore= DataQuality.GOOD.getScore(), maxScore = DataQuality.LOWEST.getScore();
        float flexibleScore = 0, count = 0;
        for (QualityReport.Item item : category.getItems()) {
            if (item.getWeight()== QualityReport.Weight.MAJOR) {
                minScore = Math.min(minScore, item.getQuality().getScore());
                maxScore = Math.max(maxScore, item.getQuality().getScore());
                flexibleScore += item.getQuality().getScore();
                count+=1;
            } else {
                flexibleScore += item.getQuality().getScore()*0.2f;
                count += 0.2f;
            }
        }
        float score = Math.min(minScore+1, flexibleScore/count);
        if (count==0) {
            return DataQuality.NOT_APPLICABLE;
        } else {
            if (score >= 2.66) {
                return DataQuality.GOOD;
            } else if (score >= 1.66) {
                return DataQuality.DECENT;
            } else if (score >= 0.66) {
                return DataQuality.BAD;
            } else return DataQuality.LOWEST;
        }
    }
}
