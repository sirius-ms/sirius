package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.lcms.adducts.TraceProvider;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class QualityAssessment implements FeatureQualityChecker {

    List<FeatureQualityChecker> checkerList = Arrays.asList(
      new CheckPeakQuality(),
      new CheckAlignmentQuality(),
      new CheckIsotopeQuality(),
      new CheckMs2Quality(),
            new CheckAdductQuality()
    );

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
        if (count==0) {
            report.setOverallQuality(DataQuality.NOT_APPLICABLE);
        } else {
            score /= count;
            if (score >= 2.66) {
                report.setOverallQuality(DataQuality.GOOD);
            } else if (score >= 1.66) {
                report.setOverallQuality(DataQuality.DECENT);
            } else if (score >= 0.66) {
                report.setOverallQuality(DataQuality.BAD);
            } else report.setOverallQuality(DataQuality.LOWEST);
        }

    }


    /**
     * Take the average between best and worst score of majors and use minors as tie breaker
     */
    public static DataQuality scoreCategory(QualityReport.Category category) {
        if (category.getItems().isEmpty()) return DataQuality.NOT_APPLICABLE;
        float minScore= DataQuality.GOOD.getScore(), maxScore = DataQuality.LOWEST.getScore();
        float tiebreaker = 0f; int tiebreakcount=0;
        for (QualityReport.Item item : category.getItems()) {
            if (item.getWeight()== QualityReport.Weight.MAJOR) {
                minScore = Math.min(minScore, item.getQuality().getScore());
                maxScore = Math.max(maxScore, item.getQuality().getScore());
            } else {
                tiebreakcount++;
                tiebreaker += item.getQuality().getScore();
            }
        }
        if (tiebreakcount==0) tiebreaker = 1;
        else tiebreaker/=tiebreakcount;
        float score = (maxScore+minScore)/2f;
        if (score <= 0) {
            return DataQuality.LOWEST;
        } else if (score < 1) {
            return tiebreaker >= 1 ? DataQuality.BAD : DataQuality.LOWEST;
        } else if (score == 1) {return DataQuality.BAD;
        } else if (score < 2) {
            return tiebreaker >= 2 ? DataQuality.DECENT : DataQuality.BAD;
        } else if (score == 2) {
            return DataQuality.DECENT;
        } else if (score < 3) {
            return tiebreaker >= 3 ? DataQuality.GOOD : DataQuality.DECENT;
        } else return DataQuality.GOOD;
    }
}
