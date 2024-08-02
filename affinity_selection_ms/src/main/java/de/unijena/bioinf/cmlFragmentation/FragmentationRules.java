package de.unijena.bioinf.cmlFragmentation;

import org.openscience.cdk.interfaces.IBond;

/**
 * A class implementing this interface defines a set of fragmentation rules regarding which bonds to cut.
 */
@FunctionalInterface
public interface FragmentationRules {

    /**
     * This method determines if {@code bond} matches the requirements to be cut
     * during in silico fragmentation.
     *
     * @param bond the bond for which it is determined if it will be cut
     * @return  {@code true} if {@code bond} fulfills the requirements to be cut;
     *          otherwise this method returns {@code false}
     */
    boolean match(IBond bond);

}