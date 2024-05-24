package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.lcms.adducts.TraceProvider;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CheckAdductQuality implements FeatureQualityChecker{
    @Override
    public void addToReport(QualityReport report, MergedLCMSRun run, AlignedFeatures feature, TraceProvider provider) {
        // majors
        QualityReport.Category peakQuality = new QualityReport.Category(QualityReport.ADDUCT_QUALITY);
        List<PrecursorIonType> allAdducts = feature.getDetectedAdducts().getAllAdducts();
        int size = allAdducts.size();

        boolean unsure = allAdducts.stream().anyMatch(PrecursorIonType::isIonizationUnknown);
        if (size == 1 && !unsure) {
            peakQuality.getItems().add(new QualityReport.Item("There is an unambigious adduct assignment of "
            + allAdducts.get(0) + " for this feature", DataQuality.GOOD, QualityReport.Weight.MAJOR));
        } else if (size<=2 && unsure) {
            peakQuality.getItems().add(new QualityReport.Item("There is an unambigious adduct assignment of "
                    + allAdducts.get(0) + " for this feature, but it's confidence is very low.", DataQuality.DECENT, QualityReport.Weight.MAJOR));
        } else if (size>=2) {
            peakQuality.getItems().add(new QualityReport.Item("There are multiple possible assignment for this feature: "
                    + allAdducts.stream().map(PrecursorIonType::toString).collect(Collectors.joining(", ")),
                    DataQuality.DECENT, QualityReport.Weight.MAJOR));
        } else  {
            peakQuality.getItems().add(new QualityReport.Item("There is no adduct assignment for this feature.",
                    DataQuality.BAD, QualityReport.Weight.MAJOR));
        }
        report.addCategory(peakQuality);
    }
}
