package de.unijena.bioinf.fragmenter;

import org.openscience.cdk.interfaces.IBond;

public interface CombinatorialFragmenterScoring {

    double scoreBond(IBond bond, boolean direction);
    double scoreFragment(CombinatorialNode fragment);
    double scoreEdge(CombinatorialEdge edge);

}
