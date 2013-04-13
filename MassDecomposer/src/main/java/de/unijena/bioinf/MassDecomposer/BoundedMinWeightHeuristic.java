package de.unijena.bioinf.MassDecomposer;

import java.util.*;

/*
Is slower than MassDecomposer. Maybe it's possible to improve the heuristic, but currently it's useless.
 */
/* public */ class BoundedMinWeightHeuristic<T> extends MassDecomposer<T> {

    public BoundedMinWeightHeuristic(double precision, double errorPPM, double absMassError, Alphabet<T> alphabet) {
        super(precision, errorPPM, absMassError, /* new SubAlphabet<T>(*/alphabet/*)*/);
    }
    /*

    public List<int[]> decompose(double mass, Map<T, Interval> boundaries){
        init();
        final Interval minMassBoundary = boundaries.get(alphabet.minCharacter());
        if (mass == 0d) return Collections.emptyList();
        if (mass < 0d) throw new IllegalArgumentException("Expect positive mass for decomposition");
        final double absError = getErrorForMass(mass);
        final int[] minValues = new int[weights.size()];
        final int[] boundsarray = new int[weights.size()];
        boolean minAllZero = true;
        Arrays.fill(boundsarray, Integer.MAX_VALUE);
        if (boundaries!=null && !boundaries.isEmpty()) {
            for (int i = 0; i < boundsarray.length; i++) {
                T el = weights.get(i).getOwner();
                Interval range = boundaries.get(el);
                if (range != null) {
                    boundsarray[i] = toInt(range.getMax());
                    minValues[i] = toInt(range.getMin());
                    if (minValues[i] > 0) {
                        minAllZero = false;
                        mass -= weights.get(i).getMass() * range.getMin();
                    }
                    if (mass < 0) throw new IllegalArgumentException("Mass is not decomposable for given boundaries");
                }
            }
        }
        final ArrayList<int[]> results = new ArrayList<int[]>();
        final long minWeight = (long)(alphabet.minCharacterWeight()/precision);
        ArrayList<int[]> rawDecompositions = null;
        final Interval interval = integerBound(mass);              // TODO: Berücksichtige minWeight
        final double minCharWeight = alphabet.minCharacterWeight();
        for (long m = interval.getMin(); m < interval.getMax(); ++m) {
            for (int minCharacter=toInt(minMassBoundary.getMin()); minCharacter < minMassBoundary.getMax(); ++minCharacter) {
                final double realAdditionalMass = minCharacter * minCharWeight;
                final long adjustedMass = m - minCharacter * minWeight;
                rawDecompositions = integerDecompose(adjustedMass, boundsarray);
                for (int i=0; i < rawDecompositions.size(); ++i) {
                    final int[] decomp = rawDecompositions.get(i);
                    if (!minAllZero) {
                        for (int j=0; j < minValues.length; ++j) {
                            decomp[j] += minValues[j];
                        }
                    }
                    if (Math.abs(calcMass(decomp) + realAdditionalMass - mass) > absError) continue;
                    final int[] decompWithMin = new int[decomp.length+1];
                    System.arraycopy(decomp, 0, decompWithMin, 1, decomp.length);
                    decompWithMin[0] = minCharacter;
                    if ((validator == null) || validator.validate(decompWithMin, elements)) {
                        results.add(decompWithMin);
                    }
                }
            }
        }
        return results;
    }

    protected void computeErrors() {
        super.computeErrors();
        final double error = (precision * (long)(alphabet.minCharacterWeight()/precision) - alphabet.minCharacterWeight()) / alphabet.minCharacterWeight();
        minError = Math.min(minError, error);
        maxError = Math.max(maxError, error);
    }

    protected static class SubAlphabet<T> implements Alphabet<T> {
        private final Alphabet<T> alphabet;
        private final int minimalMassIndex;
        protected SubAlphabet(Alphabet<T> alphabet) {
            if (alphabet.size() == 0) throw new IllegalArgumentException("Expect alphabet with size > 0");
            double minMass = alphabet.weightOf(0);
            int minIndex = 0;
            // find character with minimal mass
            for (int i=1; i < alphabet.size(); ++i) {
                final double weight = alphabet.weightOf(i);
                if (weight < minMass) {
                    minIndex = i;
                    minMass = weight;
                }
            }
            this.alphabet = alphabet;
            this.minimalMassIndex = minIndex;
        }

        @Override
        public int size() {
            return alphabet.size() - 1;
        }

        @Override
        public double weightOf(int i) {
            return alphabet.weightOf(index(i));
        }

        @Override
        public T get(int i) {
            return alphabet.get(index(i));
        }

        @Override
        public int indexOf(T character) {
            final int index = alphabet.indexOf(character);
            if (index == minimalMassIndex) throw new NoSuchElementException();
            return index < minimalMassIndex ? index : index-1;
        }

        @Override
        public <S> Map<T, S> toMap() {
            return alphabet.toMap();
        }

        private final int index(int i) {
            return i < minimalMassIndex ? i : i+1;
        }

        public double minCharacterWeight() {
            return alphabet.weightOf(minimalMassIndex);
        }
        public T minCharacter() {
            return alphabet.get(minimalMassIndex);
        }
    }

    */

}
