package matching.algorithm;

import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * This interface represents the idea of an object which enables the comparison of two molecules.
 */
public interface Matcher {

    /**
     * This method calculates the distance/score between both given molecules and returns it.
     *
     * @return the distance/score between both molecules.
     */
    double compare();

    /**
     * Returns the distance/score between both given molecules.
     *
     * @return the distance/score between both molecules.
     */
    double getScore(); //TODO: change the name of this method because the return value can be a distance too

    /**
     * Returns one of the given molecules.
     *
     * @return a molecule which is compared to the other molecule
     */
    IAtomContainer getFirstMolecule();

    /**
     * Returns one of the given molecules.
     *
     * @return a molecules which is compared to the other molecule
     */
    IAtomContainer getSecondMolecule();

}
