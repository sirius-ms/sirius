
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

package de.unijena.bioinf.IsotopePatternAnalysis.generation;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;

import java.util.Arrays;

/**
 * Created by kaidu on 17.11.2014.
 */
class SimpleIsotopologues implements Isotopologues {

    private final SimpleIsotopologue[] isotopologues;
    private final int size;

    public SimpleIsotopologues(Element e, IsotopicDistribution distr, int numberOfAtoms) {
        final Isotopes iso = distr.getIsotopesFor(e);
        final double monoMass = iso.getMass(0);
        final double otherMass = iso.getMass(1);
        final double monoLogAbundance = Math.log(iso.getAbundance(0));
        final double otherLogAbundance = Math.log(iso.getAbundance(1));
        this.isotopologues = new SimpleIsotopologue[numberOfAtoms + 1];

        int n = numberOfAtoms;
        int k = 0;
        double kfakTop = Math.log(1);
        double kfakBot = Math.log(1);
        double m1 = monoMass * numberOfAtoms, m2 = 0, a1 = monoLogAbundance * numberOfAtoms, a2 = 0;
        for (int i = 0; i < isotopologues.length; ++i) {
            isotopologues[i] = new SimpleIsotopologue(m1 + m2, a1 + a2 + kfakTop - kfakBot);
            if (i + 1 >= isotopologues.length) break;
            m1 -= monoMass;
            m2 += otherMass;
            a1 -= monoLogAbundance;
            a2 += otherLogAbundance;
            ++k;
            kfakTop += Math.log(isotopologues.length - k);
            kfakBot += Math.log(k);
        }
        Arrays.sort(isotopologues);
        this.size = numberOfAtoms + 1;
    }

    private long fak(int numberOfAtoms) {
        long n = 1;
        for (int k = 2; k <= numberOfAtoms; ++k) {
            n *= k;
        }
        return n;
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
