package de.unijena.bioinf.IsotopePatternAnalysis.generation;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.NormalizationMode;
import de.unijena.bioinf.ChemistryBase.ms.Peak;

import java.util.*;

public class FinestructureGenerator {

    private final IsotopicDistribution distribution;
    private final CachedIsoTable cache;
    private final Normalization mode;
    private final Ionization ion;

    public FinestructureGenerator(IsotopicDistribution dist, Ionization ion, Normalization mode) {
        this(dist, ion, mode, new CachedIsoTable(dist));
    }

    FinestructureGenerator(IsotopicDistribution dist, Ionization ion, Normalization mode, CachedIsoTable cache) {
        this.distribution = dist;
        this.ion = ion;
        this.mode = mode;
        this.cache = cache;
        if (ion == null || mode == null || distribution == null)
            throw new NullPointerException("Expect non null parameters");
    }

    public static void main(String[] args) {
        final Ionization ion = PeriodicTable.getInstance().ionByName("[M]+");
        final MolecularFormula formula = MolecularFormula.parse("C1000H22NO16");
        final FinestructureGenerator gen = new FinestructureGenerator(PeriodicTable.getInstance().getDistribution(), ion,
                Normalization.Sum(1d));
        {
            final java.util.Iterator<Peak> piter = gen.iterator(formula).toPeakIterator();
            double sz = 0.0d;
            while (piter.hasNext()) {
                final Peak p = piter.next();
                sz += p.getIntensity();
            }
        }
        System.exit(0);
        final Iterator iter = gen.iteratorSumingUpTo(formula, 0.99d);
        final double[] mzs = new FineStructureMerger(6500).mergeMasses(iter, ion.addToMass(formula.getMass()));
        System.out.println(Arrays.toString(mzs));
    }

    public RawIterator iterator(MolecularFormula formula) {
        return new RawIterator(cache, formula, ion, mode, distribution);
    }

    public Iterator iteratorWithPeakLimit(MolecularFormula formula, final int maxNumberOfPeaks) {
        return new PredicatedIterator(iterator(formula)) {
            int counter = 0;

            @Override
            protected boolean shouldStop() {
                return ++counter >= maxNumberOfPeaks;
            }
        };
    }

    public Iterator iteratorWithIntensityThreshold(MolecularFormula formula, final double intensity) {
        final double logIntensity = Math.log(intensity);
        return new PredicatedIterator(iterator(formula)) {
            @Override
            protected boolean shouldStop() {
                return getLogAbundance() < logIntensity;
            }
        };
    }

    public Iterator iteratorSumingUpTo(MolecularFormula formula, double sumIntensity) {
        final double threshold = mode.getBase() - sumIntensity;
        return new PredicatedIterator(iterator(formula)) {
            double intensitySum = mode.getBase();

            @Override
            protected boolean shouldStop() {
                intensitySum -= getAbundance();
                return intensitySum <= threshold;
            }
        };
    }


    public abstract static class Iterator {

        public abstract void next();

        public abstract boolean hasNext();

        public abstract double getMass();

        public abstract double getAbundance();

        public abstract double getLogAbundance();

        public abstract double getProbability();

        public abstract double getLogProbability();

        public java.util.Iterator<Peak> toPeakIterator() {
            return new java.util.Iterator<Peak>() {

                @Override
                public boolean hasNext() {
                    return Iterator.this.hasNext();
                }

                @Override
                public Peak next() {
                    Iterator.this.next();
                    return new Peak(getMass(), getAbundance());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    abstract static class PredicatedIterator extends Iterator {

        private final RawIterator iterator;
        private boolean stop;

        PredicatedIterator(RawIterator iterator) {
            this.iterator = iterator;
            this.stop = false;
        }

        @Override
        public void next() {
            if (!hasNext()) throw new NoSuchElementException();
            iterator.next();
            this.stop = shouldStop();
        }

        protected abstract boolean shouldStop();

        @Override
        public double getProbability() {
            return iterator.getProbability();
        }

        @Override
        public double getLogProbability() {
            return iterator.getLogProbability();
        }

        @Override
        public boolean hasNext() {
            return !stop && iterator.hasNext();
        }

        @Override
        public double getMass() {
            return iterator.getMass();
        }

        @Override
        public double getAbundance() {
            return iterator.getAbundance();
        }

        @Override
        public double getLogAbundance() {
            return iterator.getLogAbundance();
        }
    }

    final static class RawIterator extends Iterator {

        private final MolecularFormula formula;
        private final Normalization mode;
        private final double scale, logScale;
        private final PriorityQueue<Isotopologue> heap;
        private Element[] isotopicElements;
        private double baseMass;
        private Isotopologues[] isotopologues;
        private Isotopologue currentIsotopologue;

        protected RawIterator(final CachedIsoTable cache, MolecularFormula formula, Ionization ion, Normalization mode, final IsotopicDistribution distribution) {
            final MolecularFormula adduct = ion.getAtoms();
            if (adduct != null) {
                this.formula = formula.add(adduct);
                baseMass -= adduct.getMass();
            } else {
                this.formula = formula;
            }
            this.mode = mode;
            this.heap = new PriorityQueue<Isotopologue>(10, Collections.reverseOrder());
            final ArrayList<Element> isoEls = new ArrayList<Element>();
            final ArrayList<Isotopologues> isoL = new ArrayList<Isotopologues>();
            this.baseMass = ion.getMass();
            formula.visit(new FormulaVisitor<Object>() {
                @Override
                public Object visit(Element element, int amount) {
                    if (amount > 0) {
                        final Isotopes iso = distribution.getIsotopesFor(element);
                        if (iso != null && iso.getNumberOfIsotopes() > 1) {
                            isoEls.add(element);
                            if (iso.getNumberOfIsotopes() == 2) {
                                isoL.add(new SimpleIsotopologues(element, distribution, amount));
                            } else {
                                isoL.add(cache.getIsotopologuesFor(element, amount));
                            }
                        } else {
                            baseMass += element.getMass() * amount;
                        }
                    }
                    return null;
                }
            });
            this.isotopologues = isoL.toArray(new Isotopologues[isoL.size()]);
            this.isotopicElements = isoEls.toArray(new Element[isoEls.size()]);
            // add zero vector to heap
            addZeroVector();
            if (mode.getMode() == NormalizationMode.MAX) {
                final Isotopologue basePeak = heap.peek();
                logScale = Math.log(mode.getBase()) - basePeak.logAbundance;
                scale = mode.getBase() / Math.exp(basePeak.logAbundance);
            } else {
                logScale = Math.log(mode.getBase());
                scale = mode.getBase();
            }
            currentIsotopologue = null;
        }

        private void addZeroVector() {
            final short[] vector = new short[isotopologues.length];
            double mass = baseMass;
            double logAbundance = 0d;
            for (int i = 0; i < vector.length; ++i) {
                mass += isotopologues[i].mass(0);
                logAbundance += isotopologues[i].logAbundance(0);
            }
            final Isotopologue iso = new Isotopologue(vector, mass, logAbundance);
            heap.offer(iso);
        }

        public boolean hasNext() {
            return !heap.isEmpty();
        }

        public void next() {
            if (!hasNext()) throw new NoSuchElementException();
            currentIsotopologue = heap.poll();
            final short[] base = currentIsotopologue.amounts;
            // add corresponding isotopologues to the heap
            for (int i = isotopologues.length - 1; i >= 0; --i) {
                if (base[i] + 1 < isotopologues[i].size()) {
                    final short[] vector = base.clone();
                    final double mz = currentIsotopologue.mass - isotopologues[i].mass(vector[i]);
                    final double ab = currentIsotopologue.logAbundance - isotopologues[i].logAbundance(vector[i]);
                    ++vector[i];
                    heap.offer(new Isotopologue(vector, mz + isotopologues[i].mass(vector[i]), ab + isotopologues[i].logAbundance(vector[i])));
                }
                if (base[i] != 0) break;
            }
        }

        public Peak getPeak() {
            return new Peak(getMass(), getAbundance());
        }

        public double getMass() {
            return currentIsotopologue.mass;
        }

        public double getProbability() {
            return Math.exp(currentIsotopologue.logAbundance);
        }

        public double getLogProbability() {
            return currentIsotopologue.logAbundance;
        }

        public double getLogAbundance() {
            return currentIsotopologue.logAbundance + logScale;
        }

        public double getAbundance() {
            return Math.exp(currentIsotopologue.logAbundance) * scale;
        }

    }
}
