package de.unijena.bioinf.IsotopePatternAnalysis.generation;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;

import java.util.Arrays;
import java.util.Collections;

import static java.lang.Math.log;

public class IsotopologueTable implements Isotopologues {

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
        final short[] vector = new short[k];
        final double[] masses = new double[k];
        final double[] logAbundances = new double[k];

        final double monoMass = isotopes.getMass(0);
        final int N = isotopes.getNumberOfIsotopes() - 1;

        final double monoAbundance = log(isotopes.getAbundance(0));

        for (int i = 0; i < k; ++i) {
            logAbundances[i] = log(isotopes.getAbundance(i + 1)) - monoAbundance;
            masses[i] = isotopes.getMass(i + 1) - monoMass;
        }
        final double baseMass = monoMass * numberOfAtoms;
        final double baseProbability = monoAbundance * numberOfAtoms;
        sortedIsotopologues[0] = new Isotopologue(vector.clone(), baseMass, baseProbability);
        final int c = generateIsotopologues(element, sortedIsotopologues, 1, vector, numberOfAtoms, masses, logAbundances, baseMass, baseProbability);
        assert c == sortedIsotopologues.length : "expect " + sortedIsotopologues.length + " but " + c + " given";
        Arrays.sort(sortedIsotopologues, Collections.reverseOrder());
    }

    /*
    1  wenn k = 0 dann rückgabe 1
2  wenn 2k > n
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

    private static int generateIsotopologues(Element element, Isotopologue[] list, int c, short[] vector, int maximalAmount, double[] isoMasses, double[] isoProbabilities, double currentMass, double currentProbability) {
        for (int i = vector.length - 1; i >= 0; --i) {
            // increase vector[i]
            ++vector[i];
            --maximalAmount;
            currentMass += isoMasses[i];
            currentProbability += isoProbabilities[i];
            // add to list
            list[c++] = new Isotopologue(vector.clone(), currentMass, currentProbability);
            // recursive call
            if (maximalAmount > 0) {
                c = generateIsotopologues(element, list, c, vector, maximalAmount, isoMasses, isoProbabilities, currentMass, currentProbability);
            }
            // redo
            --vector[i];
            ++maximalAmount;
            currentMass -= isoMasses[i];
            currentProbability -= isoProbabilities[i];
            if (vector[i] != 0) break;
        }
        return c;
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
