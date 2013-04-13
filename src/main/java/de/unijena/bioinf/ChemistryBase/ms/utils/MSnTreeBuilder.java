package de.unijena.bioinf.ChemistryBase.ms.utils;

import java.util.ArrayList;

import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.IntList;

import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.BasicSpectraTree.Node;
import static de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.*;

public class MSnTreeBuilder<S extends Spectrum<P>, P extends Peak> {

	private final ArrayList<IntList> msLevels;
	private final ArrayList<Node<S,P>> vertices;
	private final ArrayList<S> parents;
	
	public MSnTreeBuilder()  {
		this.vertices = new ArrayList<Node<S,P>>();
		this.parents = new ArrayList<S>();
		this.msLevels = new ArrayList<IntList>();
	}
	
	public void addRoot(S spectrum) {
		final Node<S,P> root = new Node<S,P>(spectrum, null, 0, 0, 1);
		addTo(0, root, null);
	}
	
	public void add(S spectrum, S precursorSpectrum, int precursorPosition, double collisionEnergy, int msLevel) {
		final P precursor = precursorSpectrum.getPeakAt(precursorPosition);
		addTo(msLevel-1, new Node<S,P>(spectrum, precursor, precursor.getMass(), collisionEnergy, msLevel), precursorSpectrum);
	}
	
	public void add(S spectrum, P precursor, double collisionEnergy, int msLevel) {
		addTo(msLevel-1, new Node<S,P>(spectrum, precursor, precursor.getMass(), collisionEnergy, msLevel), null);
	}
	public void add(S spectrum, P precursor, int msLevel) {
		add(spectrum, precursor, 0, msLevel);
	}
	
	public void add(S spectrum, double precursorMz, double collisionEnergy, int msLevel) {
		addTo(msLevel-1, new Node<S,P>(spectrum, null, precursorMz, collisionEnergy, msLevel), null);
	}
	public void add(S spectrum, double precursorMz,int msLevel) {
		add(spectrum, precursorMz, 0, msLevel);
	}
	
	// todo: add support for missing msi spectra
	public BasicSpectraTree<S, P> buildTree(Deviation precursorDeviation) throws InvalidInputData {
		if (vertices.size() == 0) return null;
		final ArrayList<Node<S,P>> vertices = deepCopy();
		final Node<S,P> root;
		if (msLevels.get(0).isEmpty()) {
			root = new Node<S,P>(null, null, 0, 0, 1);
		} else {
			root = vertices.get(msLevels.get(0).get(0));
		}
		// all MS2 Spectra are children of the MS1 spectrum
		for (int i=0; i < msLevels.get(1).size(); ++i) {
			root.addChild(vertices.get(msLevels.get(1).get(i)));
		}
		// each msn spectrum is a child of one ms(n-1) spectrum
		for (int i=2; i < vertices.size(); ++i) {
			final IntList ids = msLevels.get(i);
			found:
			for (int j = 0; j < ids.size(); ++j) {
				final Node<S,P> msi = vertices.get(ids.get(j));
				final S parent = parents.get(ids.get(j));
				// search for parent spectrum
				
				final IntList ids2 = msLevels.get(i-1);
				for (int k = 0; k < ids2.size(); ++k) {
					final Node<S,P> n = vertices.get(ids2.get(k));
					if (parent != null) {
						if (n.getSpectrum() == parent) {
							n.addChild(msi);
							continue found;
						} else {
							continue;
						}
					} else if (n.getSpectrum() != null) {
						if (msi.getPrecursor() != null) {
							if (indexOfPeak(n.getSpectrum(), msi.getPrecursor()) >= 0) {
								n.addChild(msi);
								continue found;
							}
						} else if (msi.getPrecursorMz() != 0) {
							final int pos = search(n.getSpectrum(), msi.getPrecursorMz(), precursorDeviation);
							if (pos >= 0) {
								n.addChild(new Node<S,P>(msi.getSpectrum(), n.getSpectrum().getPeakAt(pos),
										msi.getPrecursorMz(), msi.getCollisionEnergy(), msi.getMsLevel()));
								continue found;
							}
						} else {
							throw new InvalidInputData("Spectrum without precursor information: '" + msi + "'");
						}
					}
				}
				throw new InvalidInputData("Precursor for spectrum not found: '" + msi + "'");
			}
		}
		return new BasicSpectraTree<S,P>(root);
	}
	
	private synchronized ArrayList<Node<S,P>> deepCopy() {
		final ArrayList<Node<S,P>> copy = new ArrayList<Node<S,P>>(vertices);
		for (int i=0; i < copy.size(); ++i) {
			copy.set(i, new Node<S,P>(vertices.get(i)));
		}
		return copy;
	}
	
	private synchronized int addTo(int i, Node<S,P> n, S parent) {
		while (msLevels.size() <= i) {
			msLevels.add(new ArrayIntList());
		}
		if (i == 0 && !msLevels.get(i).isEmpty()) 
			throw new RuntimeException("MSnTreeBuilder supports only one MS1 spectrum per tree");
		vertices.add(n);
		parents.add(parent);
		msLevels.get(i).add(vertices.size()-1);
		return vertices.size()-1;
	}
	
}
