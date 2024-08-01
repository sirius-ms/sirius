package de.unijena.bioinf.lcms.adducts.assignment;

import de.unijena.bioinf.lcms.adducts.AdductNode;

public interface SubnetworkResolver {

    public AdductAssignment[] resolve(AdductNode[] subnetwork, int charge);

}
