
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
import java.util.Collections;

import static java.lang.Math.log;

class IsotopologueTable implements Isotopologues {

    private final Element element;
    private final int numberOfAtoms;
    private final Isotopologue[] sortedIsotopologues;

    public IsotopologueTable(Element element, int numberOfAtoms, IsotopicDistribution distribution) {
        this.element = element;
        this.numberOfAtoms = numberOfAtoms;
        final Isotopes isotopes = distribution.getIsotopesFor(element);
        final int k = isotopes.getNumberOfIsotopes() - 1;
        final int numberOfIsotopologues = binomialCoefficient(numberOfAtoms + k, k);
        this.sortedIsotopologues = new Isotopologue[numberOfIsotopologues];
        // fill array
        final short[] vector = new short[k + 1];
        final double[] masses = new double[k + 1];
        final double[] logAbundances = new double[k + 1];

        for (int i = 0; i <= k; ++i) {
            logAbundances[i] = log(isotopes.getAbundance(i));
            masses[i] = isotopes.getMassDifference(i);
        }
        final int c = generateIsotopologues(numberOfAtoms, sortedIsotopologues, 0, vector, numberOfAtoms, masses, logAbundances, 0d, element.getMass() * numberOfAtoms, 0);
        assert c == sortedIsotopologues.length : "expect " + sortedIsotopologues.length + " but " + c + " given";
        Arrays.sort(sortedIsotopologues, Collections.reverseOrder());
        double x = 0.0d;
        for (int i = 0; i < sortedIsotopologues.length; ++i) {
            x += Math.exp(sortedIsotopologues[i].logAbundance);
        }
    }

    /*
    1  wenn k = 0 dann rückgabe 1
2  wenn 2k{@literal >}n
3      dann führe aus ergebnis \leftarrow binomialkoeffizient(n, n-k)
4  sonst führe aus ergebnis \leftarrow n-k+1
5          von i \leftarrow 2 bis k
6              führe aus ergebnis \leftarrow ergebnis \cdot (n - k + i)
7                        ergebnis \leftarrow ergebnis : i
8  rückgabe ergebnis
     */

    private static int binomialCoefficient(int n, int k) {
        if (k == 0) return 1;
        if (2 * k > n) {
            return binomialCoefficient(n, n - k);
        } else {
            int x = 1;
            for (int i = 1; i <= k; ++i) {
                x *= (n - k + i);
                x /= i;
            }
            return x;
        }
    }
    /*
    private static int generateIsotopologues(Element element, Isotopologue[] list, int relative, short[] vector, int maximalAmount, double[] isoMasses, double[] isoProbabilities, double currentMass, double currentProbability) {
        for (int i = vector.length - 1; i >= 0; --i) {
            // increase vector[i]
            ++vector[i];
            --maximalAmount;
            currentMass += isoMasses[i];
            currentProbability += isoProbabilities[i];
            // add to list
            list[relative++] = new Isotopologue(vector.clone(), currentMass, currentProbability);
            // recursive call
            if (maximalAmount > 0) {
                relative = generateIsotopologues(element, list, relative, vector, maximalAmount, isoMasses, isoProbabilities, currentMass, currentProbability);
            }
            // redo
            --vector[i];
            ++maximalAmount;
            currentMass -= isoMasses[i];
            currentProbability -= isoProbabilities[i];
            if (vector[i] != 0) break;
        }
        return relative;
    }
    */

    private static int generateIsotopologues(final int N, Isotopologue[] list, int c, short[] vector, int maximalAmount, double[] isoMasses, double[] isoProbabilities, double currentProb, double currentMass, int k) {
        final int nextK = k + 1;
        while (maximalAmount > 0) {
            if (nextK < isoMasses.length && maximalAmount > 0) {
                c = generateIsotopologues(N, list, c, vector, maximalAmount, isoMasses, isoProbabilities, currentProb, currentMass, nextK);
            }
            --maximalAmount;
            ++vector[k];
            currentProb += isoProbabilities[k] + Math.log(N - maximalAmount) - Math.log(vector[k]);
            currentMass += isoMasses[k];
        }
        list[c++] = new Isotopologue(vector.clone(), currentMass, currentProb);
        //check(list[relative-1], isoProbabilities);
        vector[k] = 0;
        return c;
    }

    private static void check(Isotopologue isotopologue, double[] isoProbabilities/*, int atomNumber*/) {
        double x = 0d;
        int c = 0;
        for (int i = 0; i < isoProbabilities.length; ++i) {
            x += isotopologue.amounts[i] * isoProbabilities[i];
            c += isotopologue.amounts[i];
        }
        //assert relative == atomNumber : "expect " + atomNumber + " atoms but " + relative + " is given";
        assert Math.abs(x - isotopologue.logAbundance) < 1e-14 : "expect " + x + " but " + isotopologue.logAbundance + " given";
    }

    @Override
    public double logAbundance(int i) {
        return sortedIsotopologues[i].logAbundance;
    }

    @Override
    public double mass(int i) {
        return sortedIsotopologues[i].mass;
    }

    @Override
    public int size() {
        return sortedIsotopologues.length;
    }
}
