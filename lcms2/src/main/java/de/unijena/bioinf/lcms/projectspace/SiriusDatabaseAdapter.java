package de.unijena.bioinf.lcms.projectspace;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;

import java.io.IOException;

public interface SiriusDatabaseAdapter {

    void importRun(LCMSRun run) throws IOException;

    void updateRun(LCMSRun run) throws IOException;

    void importMergedRun(MergedLCMSRun mergedRun) throws IOException;

    void importScan(Scan scan) throws IOException;

    void importMSMSScan(MSMSScan scan) throws IOException;

    void importTrace(AbstractTrace trace) throws IOException;

    void importAlignedFeature(AlignedFeatures alignedFeatures) throws IOException;

    void importRetentionTimeAxis(RetentionTimeAxis axis) throws IOException;
}
