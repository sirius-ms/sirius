package de.unijena.bioinf.ChemistryBase.ms.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SpectraTree;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public abstract class SpectraTrees {
	
	public static <N, S extends Spectrum<P>, P extends Peak> Iterator<N> preOrder(final SpectraTree<N,S,P> tree) {
		final ArrayDeque<Iterator<N>> stack = new ArrayDeque<Iterator<N>>();
		stack.add(Collections.singletonList(tree.getRoot()).iterator());
		return new Iterator<N>() {

			@Override
			public boolean hasNext() {
				return !stack.isEmpty();
			}

			@Override
			public N next() {
				if (!hasNext()) throw new NoSuchElementException();
				final Iterator<N> siblings = stack.peek();
				final N vertex = siblings.next();
				if (isLeaf(tree, vertex)) {
					while (!stack.peek().hasNext()) stack.pop();
				} else {
					stack.add(tree.getChildrenOf(vertex).iterator());
				}
				return vertex;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	public static <N, S extends Spectrum<P>, P extends Peak> boolean isLeaf(SpectraTree<N,S,P> tree, N node) {
		return tree.getChildrenOf(node).isEmpty();
	}
	
	public static <N, S extends Spectrum<P>, P extends Peak> List<S> getSpectra(SpectraTree<N,S,P> tree, List<N> nodes) {
		final ArrayList<S> spectra = new ArrayList<S>(nodes.size());
		for (N node : nodes) spectra.add(tree.getSpectraOf(node));
		return spectra;
	}
	
	public static <N, S extends Spectrum<P>, P extends Peak> List<S> getSpectra(SpectraTree<N,S,P> tree, Iterator<N> nodes) {
		final ArrayList<S> spectra = new ArrayList<S>();
		while (nodes.hasNext()) spectra.add(tree.getSpectraOf(nodes.next()));
		return spectra;
	}
	
	public static <N, S extends Spectrum<P>, P extends Peak> List<N> getAllSubspectra(SpectraTree<N,S,P> tree) {
		final Iterator<N> preorder = preOrder(tree);
		final ArrayList<N> list = new ArrayList<N>();
		while (preorder.hasNext())  {
			final N u = preorder.next();
			if (tree.getMsLevelOf(u) != 1) {
				list.add(u);
			}
		}
		return list;
	}
	
}
