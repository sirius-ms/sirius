package de.unijena.bioinf.IsotopePatternAnalysis.generation;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.NormalizationMode;
import de.unijena.bioinf.ChemistryBase.ms.Peak;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

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
        final FinestructureGenerator gen = new FinestructureGenerator(PeriodicTable.getInstance().getDistribution(), PeriodicTable.getInstance().ionByName("[M]+"),
                Normalization.Sum(100d));
        final Iterator iter = gen.iterator(MolecularFormula.parse("C6H12O6"));
        while (iter.hasNext()) {
            iter.next();
            System.out.println(iter.getPeak());
        }
    }

    public Iterator iterator(MolecularFormula formula) {
        return new Iterator(cache, formula, ion, mode);
    }

    public final class Iterator {

        private final MolecularFormula formula;
        private final Normalization mode;
        private final double scale, logScale;
        private final PriorityQueue<Isotopologue> heap;
        private Element[] isotopicElements;
        private double baseMass;
        private Isotopologues[] isotopologues;
        private Isotopologue currentIsotopologue;

        protected Iterator(final CachedIsoTable cache, MolecularFormula formula, Ionization ion, Normalization mode) {
            final MolecularFormula adduct = ion.getAtoms();
            if (adduct != null) {
                this.formula = formula.add(adduct);
                baseMass -= adduct.getMass();
            } else {
                this.formula = formula;
            }
            this.mode = mode;
            this.heap = new PriorityQueue<Isotopologue>();
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
                logScale = basePeak.logAbundance + Math.log(mode.getBase());
                scale = Math.exp(basePeak.logAbundance) * mode.getBase();
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

        public double getLogAbundance() {
            return currentIsotopologue.logAbundance;
        }

        public double getAbundance() {
            return Math.exp(currentIsotopologue.logAbundance);
        }

    }
}
