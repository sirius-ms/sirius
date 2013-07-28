package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.algorithm.ImmutableParameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

import java.util.Iterator;

public class Isotopes implements ImmutableParameterized<Isotopes> {

    private final double[] masses;
    private final double[] abundances;

    public Isotopes() {
        this(new double[0], new double[0]);
    }

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


    @Override
    public <G, D, L> Isotopes readFromParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final L abu = document.getListFromDictionary(dictionary, "abundances");
        final L ms = document.getListFromDictionary(dictionary, "masses");
        final int n = document.sizeOfList(abu);
        if (n != document.sizeOfList(ms)) throw new RuntimeException("Number of masses and abundances have to be equal!");
        final double[] ab = new double[n];
        final double[] mas = new double[n];
        for (int i=0; i < n; ++i) {
            ab[i] = document.getDoubleFromList(abu, i);
            mas[i] = document.getDoubleFromList(ms, i);
        }
        return new Isotopes(mas, ab);
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final L abu = document.newList();
        for (double d : abundances) document.addToList(abu, d);
        final L ms = document.newList();
        for (double d : masses) document.addToList(ms, d);
        document.addListToDictionary(dictionary, "abundances", abu);
        document.addListToDictionary(dictionary, "masses", ms);
    }
}
