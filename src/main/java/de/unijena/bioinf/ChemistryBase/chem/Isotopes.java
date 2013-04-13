package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;


public class Isotopes {

    private final double[] masses;
    private final double[] abundances;

    public Isotopes(double[] masses, double[] abundances) {
        this.masses = masses.clone();
        this.abundances = abundances.clone();
        if (!isOrdered(masses, abundances)) reorder(masses, abundances);
    }

    public int getIntegerMass(int index) {
        return (int)(Math.round(masses[index]));
    }

    public double getMass(int index) {
        return masses[index];
    }

    public double getAbundance(int index) {
        return abundances[index];
    }

    public int getNumberOfIsotopes() {
        return masses.length;
    }

    private static void reorder(double[] ms, double[] is) {
        // insertion-sort, because the arrays are usually very small
        for (int i=0; i < ms.length; ++i) {
            int mindex = i;
            for (int j=i+1; j < ms.length; ++j) {
                if (ms[j] < ms[mindex]) {
                    mindex = j;
                }
            }
            if (i != mindex) {
                double z = ms[i];
                ms[i] = ms[mindex];
                ms[mindex] = z;
                z = is[i];
                is[i] = is[mindex];
                is[mindex] = z;
            }
        }
        assert isOrdered(ms, is);
    }

    private static boolean isOrdered(double[] ms, double[] is) {
        for (int i=1; i < ms.length; ++i) {
            if (ms[i] <= ms[i-1]) {
                return false;
            }
        }
        return true;

    }


}
