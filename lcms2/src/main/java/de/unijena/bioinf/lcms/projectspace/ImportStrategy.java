package de.unijena.bioinf.lcms.projectspace;

import de.unijena.bioinf.ms.persistence.model.core.AlignedFeatures;

public interface ImportStrategy {

    public void importAlignedFeature(Object storage, AlignedFeatures alignedFeatures);

}
