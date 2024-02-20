package de.unijena.bioinf.lcms.projectspace;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;

import java.io.IOException;
import java.util.Iterator;

public interface ImportStrategy {

    void importTrace(AbstractTrace trace) throws IOException;

    void importAlignedFeature(AlignedFeatures alignedFeatures) throws IOException;

}
