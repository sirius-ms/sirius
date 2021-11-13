
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.chem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;



class TableSelectionCache {
	
	final static int DEFAULT_MAX_COMPOMERE_SIZE = 12;
	final static String DEFAULT_ALPHABET = "CHNOPS";
	final static String START_SET = "HCNO";
    final static String[] START_SET_ARRAY = new String[]{"H", "C", "N", "O"};
	
	private final ArrayList<TableSelection> cache;
	private final int defaultCompomereSize;
	private final PeriodicTable table;
	private final ReadWriteLock lock;
	private int modificationCount;
	
	
	TableSelectionCache(PeriodicTable table, int defaultCompomereSize) {
		this.cache = new ArrayList<TableSelection>();
		this.defaultCompomereSize = defaultCompomereSize;
		this.table = table;
		this.lock = new ReentrantReadWriteLock();
		this.modificationCount = 0;
	}

    void addDefaultAlphabet() {
        cache.add(TableSelection.fromString(table, DEFAULT_ALPHABET));
    }
	
	void clearCache() {
		cache.clear();
		cache.add(TableSelection.fromString(table, DEFAULT_ALPHABET));
	}
	
	TableSelection getSelectionFor(BitSet bitset) {
		// naive implementation
		lock.readLock().lock();
		final int mod = modificationCount;
		TableSelection best;
		try {
			final SearchResult result = searchForSelection(bitset);
			if (result.optimal) return result.selection;
			best = result.selection;
		} finally {
			lock.readLock().unlock();
		}
		lock.writeLock().lock();
		try {
			if (mod != modificationCount) {
				final SearchResult result = searchForSelection(bitset);
				if (result.optimal) return result.selection;
				best = result.selection;
			}
			if (best == null) return addToCache(bitset);
			else return extendCache(bitset, best);
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private SearchResult searchForSelection(BitSet bitset) {
		int min2add = Integer.MAX_VALUE;
		TableSelection best = null;
		for (TableSelection selection : cache) {
			final BitSet mask = selection.bitmask;
			final BitSet diff = (BitSet)bitset.clone();
			diff.andNot(mask);
			final int toAdd = diff.cardinality();
			if (toAdd == 0) return new SearchResult(selection, true);
			if (toAdd + mask.cardinality() <= defaultCompomereSize && toAdd < min2add) {
				min2add = toAdd;
				best = selection;
			}
		}
		return new SearchResult(best, false);
	}
	
	private final static class SearchResult {
		private final TableSelection selection;
		private final boolean optimal;
		private SearchResult(TableSelection selection, boolean optimal) {
			this.selection = selection;
			this.optimal = optimal;
		}
	}

	TableSelection addToCache(TableSelection selection) {
		lock.writeLock().lock();
		++modificationCount;
		TableSelection best;
		try {
			final SearchResult result = searchForSelection(selection.bitmask);
			if (result.selection!=null && result.selection.isSubsetOf(selection)) {
				result.selection.replace(selection);
				return result.selection;
			} else {
				cache.add(selection);
				return selection;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private TableSelection addToCache(BitSet bitset) {
        final boolean commonPt = !(Arrays.asList(table.getAllByName(START_SET_ARRAY)).contains(null));
		final TableSelection newSelection = TableSelection.fromString(table, commonPt ? START_SET : "");
		extendCache(bitset, newSelection);
		cache.add(newSelection);
		return newSelection;
	}
	
	private TableSelection extendCache(BitSet bitset, TableSelection selection) {
		++modificationCount;
		final BitSet toAdd = ((BitSet)bitset.clone());
		toAdd.andNot(selection.bitmask);
		final Element[] elements = new Element[toAdd.cardinality()];
		int k=0;
		for (int i = toAdd.nextSetBit(0); i >= 0; i = toAdd.nextSetBit(i+1)) {
		     elements[k++] = table.get(i);
		}
		selection.extendElements(elements);
		return selection;
	}
	
	
}
