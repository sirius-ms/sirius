package de.unijena.bioinf.lcms.adducts.assignment;

import de.unijena.bioinf.lcms.adducts.AdductNetwork;
import de.unijena.bioinf.lcms.adducts.AdductNode;

public class CompoundGroup {

    AdductAssignment[] assignments;
    AdductNode[] nodes;



    public CompoundGroup(AdductAssignment[] assignments, AdductNode[] nodes) {
        this.assignments = assignments;
        this.nodes = nodes;
    }
}
