package de.unijena.bioinf.lcms.adducts.assignment;

import de.unijena.bioinf.lcms.adducts.AdductManager;
import de.unijena.bioinf.lcms.adducts.AdductNode;

public interface SubnetworkResolver {

    public AdductAssignment[] resolve(AdductManager manager, AdductNode[] subnetwork, int charge);

}
