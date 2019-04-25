package de.unijena.bioinf.passatutto;

import gnu.trove.set.hash.TIntHashSet;

import java.util.List;
import java.util.TreeMap;

public class ConditionalPeaks {

    /**
     * collect all peaks from spectral library
     * use a reversed index to remember for each peak in which spectra it is contained
     */
    protected static class DecoyPool {

        protected final ReferenceCompound[] compounds;

        protected final TreeMap<Double, int[]> index;

        public DecoyPool(List<ReferenceCompound> compounds) {
            this.compounds = compounds.toArray(new ReferenceCompound[compounds.size()]);
            this.index = new TreeMap<>();
            buildIndex();
        }

        private void buildIndex() {

        }

    }

}
