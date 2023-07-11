package de.unijena.bioinf.cmlFragmentation;

import org.openscience.cdk.interfaces.IBond;

@FunctionalInterface
public interface FragmentationRules {

    boolean match(IBond bond);

}
