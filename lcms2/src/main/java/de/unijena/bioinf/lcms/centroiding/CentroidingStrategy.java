package de.unijena.bioinf.lcms.centroiding;

import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;

public interface CentroidingStrategy {

    public void centroidMsScan(Scan scan);
    public void centroidMsMsScan(MSMSScan scan);

}
