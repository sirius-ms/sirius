package de.unijena.bioinf.fragmenter;

import org.openscience.cdk.interfaces.IBond;

public interface CombinatorialFragmenterScoring {

    public double scoreBond(IBond bond, boolean direction);
    public double scoreFragment(CombinatorialFragment fragment);

}
