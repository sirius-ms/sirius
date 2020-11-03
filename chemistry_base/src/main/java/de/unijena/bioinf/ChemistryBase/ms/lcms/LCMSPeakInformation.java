package de.unijena.bioinf.ChemistryBase.ms.lcms;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * stores all chromatographic information about a certain compound
 */
public class LCMSPeakInformation implements DataAnnotation  {

    public final CoelutingTraceSet[] traceSet;

    public LCMSPeakInformation(CoelutingTraceSet[] traceSet) {
        this.traceSet = traceSet;
    }

    public TObjectDoubleHashMap<String> getQuantificationAsMap() {
        final TObjectDoubleHashMap<String> map = new TObjectDoubleHashMap<>();
        for (CoelutingTraceSet set : traceSet) {
            map.put(set.sampleName, set.getIonTrace().getMonoisotopicPeak().getApexIntensity());
        }
        return map;
    }

    public double[] getQuantificationAsVector(TObjectIntHashMap<String> sample2index) {
        int maxIndex = 0;
        for (int v : sample2index.values()) maxIndex = Math.max(maxIndex,v);
        final double[] vector = new double[maxIndex+1];
        for (CoelutingTraceSet set : traceSet) {
            if (sample2index.containsKey(set.sampleName)) {
                vector[sample2index.get(set.sampleName)] = set.getIonTrace().getMonoisotopicPeak().getApexIntensity();
            }
        }
        return vector;
    }
}
