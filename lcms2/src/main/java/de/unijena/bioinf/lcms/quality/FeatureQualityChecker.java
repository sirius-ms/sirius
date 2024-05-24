package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.lcms.adducts.TraceProvider;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;

import java.io.IOException;

public interface FeatureQualityChecker {

    public void addToReport(QualityReport report, MergedLCMSRun run, AlignedFeatures feature, TraceProvider traceProvider) throws IOException;


}
