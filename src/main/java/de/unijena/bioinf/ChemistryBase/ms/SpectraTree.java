package de.unijena.bioinf.ChemistryBase.ms;

import java.util.List;

import de.unijena.bioinf.ChemistryBase.ms.utils.PropertySet;

/**
 * spectra tree for tandem ms
 *
 * @param <N> node type
 * @param <S> spectra type
 * @param <P> peak type
 */
public interface SpectraTree<N, S extends Spectrum<P>, P extends Peak> extends PropertySet {
	
	/**
	 * @return the root of the tree, which should be a MS1 spectrum
	 */
	public N getRoot();
	
	/**
	 * @return all sub-spectra of the spectra node
	 */
	public List<N> getChildrenOf(N vertex);
	
	/**
	 * @return the spectrum which belongs to the vertex or {{@link #isMissing} returns true
	 */
	public S getSpectraOf(N vertex);
	
	/**
	 * @return the collision energy which is used to fragment the spectrum or 0 if the 
	 * energy is unknown
	 */
	public double getCollisionEnergyOf(N vertex);
	
	/**
	 * Usually the ms level is the depth of the node in the tree, but this is not forced.
	 * @return the ms level (number of fragmentation steps) of the vertex or 0 if the level is unknown.
	 */
	public int getMsLevelOf(N vertex);
	
	/**
	 * @return the precursor peak of the spectrum or null if the precursor does not exist, is unknown or
	 * can not be mapped directly
	 */
	public P getPrecursorOf(N vertex);
	
	/**
	 * If there is exactly one peak which can be defined as precursor, this method should return the
	 * mz value of {@link #getPrecursorOf}. But sometimes there are more candidate peaks for the precursor,
	 * or the precursor is missing in the precursor spectrum. In this cases, this method should return
	 * the theoretical mz value of the precursor. 
	 * If the precursor does not exist (in MS1 spectra) this method should return 0.
	 * @param vertex
	 * @return
	 */
	public double getPrecursorMzOf(N vertex);
	
	/**
	 * sometimes a vertex/spectrum is missing/not saved. This is often the case for MS1 spectra.
	 * @return true if the spectrum which belongs to the given vertex is missing/unknown
	 */
	public boolean isMissing(N vertex);
	
}
