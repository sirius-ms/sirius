package de.unijena.bioinf.lcms.projectspace;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.Run;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;

import java.io.IOException;

public interface ImportStrategy {

    void importRun(Run run) throws IOException;

    void updateRun(Run run) throws IOException;

    void importScan(Scan scan) throws IOException;

    void importMSMSScan(MSMSScan scan) throws IOException;

    void importTrace(AbstractTrace trace) throws IOException;

    void importAlignedFeature(AlignedFeatures alignedFeatures) throws IOException;

}
