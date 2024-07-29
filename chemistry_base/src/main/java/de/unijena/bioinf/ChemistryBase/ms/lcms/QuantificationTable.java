package de.unijena.bioinf.ChemistryBase.ms.lcms;

import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.HashMap;
import java.util.Optional;

public interface QuantificationTable {

    public String getName(int i);

    public double getAbundance(int i);

    /**
     * returns abundance or 0.0 if name is unknown
     */
    public double getAbundance(String name);

    /**
     * returns abundance or not if name is unknown
     */
    public Optional<Double> mayGetAbundance(String name);

    public int length();

    public QuantificationMeasure getMeasure();

    public default double[] getAsVector() {
        final double[] vec = new double[length()];
        for (int i=0; i < vec.length; ++i) vec[i] = getAbundance(i);
        return vec;
    }
    public default TObjectDoubleHashMap<String> getAsMap() {
        final TObjectDoubleHashMap<String> map = new TObjectDoubleHashMap<>(length());
        for (int i=0,n=length(); i < n; ++i) map.put(getName(i),getAbundance(i));
        return map;
    }
    public default HashMap<String,Double> getAsJavaMap() {
        final HashMap<String,Double> map = new HashMap<>(length());
        for (int i=0,n=length(); i < n; ++i) map.put(getName(i),getAbundance(i));
        return map;
    }

}
