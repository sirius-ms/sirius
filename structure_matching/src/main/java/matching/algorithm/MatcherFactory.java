package matching.algorithm;

import matching.algorithm.Matcher;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

@FunctionalInterface
public interface MatcherFactory<M extends Matcher> {
    M newInstance(IAtomContainer molecule1, IAtomContainer molecule2) throws CDKException;
}
