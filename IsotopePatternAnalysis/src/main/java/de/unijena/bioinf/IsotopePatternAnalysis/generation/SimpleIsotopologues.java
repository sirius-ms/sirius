package de.unijena.bioinf.IsotopePatternAnalysis.generation;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;

import java.util.Arrays;

/**
 * Created by kaidu on 17.11.2014.
 */
public class SimpleIsotopologues implements Isotopologues {

    private final SimpleIsotopologue[] isotopologues;
    private final int size;

    public SimpleIsotopologues(Element e, IsotopicDistribution distr, int numberOfAtoms) {
        final Isotopes iso = distr.getIsotopesFor(e);
        final double monoMass = iso.getMass(0);
        final double otherMass = iso.getMass(1);
        final double monoLogAbundance = Math.log(iso.getAbundance(0));
        final double otherLogAbundance = Math.log(iso.getAbundance(1));
        this.isotopologues = new SimpleIsotopologue[numberOfAtoms];

        double m1 = monoMass * numberOfAtoms, m2 = 0, a1 = monoLogAbundance * numberOfAtoms, a2 = 0;
        for (int i = 0; i < isotopologues.length; ++i) {
            isotopologues[i] = new SimpleIsotopologue(m1 + m2, a1 + a2);
            m1 -= monoMass;
            m2 += otherMass;
            a1 -= monoLogAbundance;
            a2 += otherLogAbundance;
        }
        Arrays.sort(isotopologues);
        this.size = numberOfAtoms;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public double logAbundance(int i) {
        return isotopologues[i].logAbundance;
    }

    @Override
    public double mass(int i) {
        return isotopologues[i].mass;
    }

    private final static class SimpleIsotopologue implements Comparable<SimpleIsotopologue> {
        private final double mass;
        private final double logAbundance;

        private SimpleIsotopologue(double mass, double logAbundance) {
            this.mass = mass;
            this.logAbundance = logAbundance;
        }

        @Override
        public int compareTo(SimpleIsotopologue o) {
            return Double.compare(o.logAbundance, logAbundance);
        }
    }
}
