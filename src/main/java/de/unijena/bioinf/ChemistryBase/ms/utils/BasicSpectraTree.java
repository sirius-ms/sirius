package de.unijena.bioinf.ChemistryBase.ms.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SpectraTree;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public class BasicSpectraTree<S extends Spectrum<P>, P extends Peak> implements
SpectraTree<BasicSpectraTree.Node<S,P>, S, P>{
	
	private Node<S,P> root;
	
	BasicSpectraTree(Node<S, P> root) {
		this.root = root;
	}
	
	public static class Node<S extends Spectrum<P>, P extends Peak> {
		private final double collisionEnergy;
		private final int msLevel;
		private final S spectrum;
		private final P precursor;
		private final double precursorMz;
		private final List<Node<S,P>> children;
		
		public Node(Node<S,P> n) {
			this(n.spectrum, n.precursor, n.precursorMz, n.collisionEnergy, n.msLevel);
			children.addAll(n.getChildren());
		}
		
		public Node(S spectrum, P precursor, double precursorMz, double collisionEnergy, int msLevel) {
			super();
			this.collisionEnergy = collisionEnergy;
			this.msLevel = msLevel;
			this.spectrum = spectrum;
			this.precursor = precursor;
			this.precursorMz = precursorMz;
			this.children = new ArrayList<Node<S,P>>();
		}
		public double getCollisionEnergy() {
			return collisionEnergy;
		}
		public int getMsLevel() {
			return msLevel;
		}
		public S getSpectrum() {
			return spectrum;
		}
		public P getPrecursor() {
			return precursor;
		}
		public double getPrecursorMz() {
			return precursorMz;
		}
		public List<Node<S,P>> getChildren() {
			return Collections.unmodifiableList(children);
		}
		void addChild(Node<S,P> n) {
			children.add(n);
		}
	}

	@Override
	public <T> T getProperty(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getProperty(String name, T defaultValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node<S,P> getRoot() {
		return root;
	}

	@Override
	public List<Node<S,P>> getChildrenOf(Node<S,P> vertex) {
		return vertex.getChildren();
	}

	@Override
	public S getSpectraOf(Node<S,P> vertex) {
		return vertex.getSpectrum();
	}

	@Override
	public double getCollisionEnergyOf(Node<S,P> vertex) {
		return vertex.getCollisionEnergy();
	}

	@Override
	public int getMsLevelOf(Node<S,P> vertex) {
		return vertex.getMsLevel();
	}

	@Override
	public P getPrecursorOf(Node<S,P> vertex) {
		return vertex.getPrecursor();
	}

	@Override
	public double getPrecursorMzOf(Node<S,P> vertex) {
		return vertex.getPrecursorMz();
	}

	@Override
	public boolean isMissing(Node<S,P> vertex) {
		return vertex.getSpectrum() == null;
	}
	
}
