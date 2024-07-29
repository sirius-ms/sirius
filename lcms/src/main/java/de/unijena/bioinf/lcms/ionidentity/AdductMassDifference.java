package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeMap;

public class AdductMassDifference {

    private final double mzDifference;
    private final long key;
    private final PrecursorIonType[] leftTypes, rightTypes;

    public static long getkey(double mzDiff) {
        return Math.round(mzDiff*100);
    }

    public static TreeMap<Long, AdductMassDifference> getAllDifferences(Set<PrecursorIonType> detectableIonTypes) {
        final TreeMap<Long, AdductMassDifference> map = new TreeMap<>();
        final PrecursorIonType[] types = detectableIonTypes.toArray(PrecursorIonType[]::new);
        Arrays.sort(types, Comparator.comparingDouble(PrecursorIonType::getModificationMass));
        for (int i=0; i < types.length; ++i) {
            final PrecursorIonType l = types[i];
            for (int j=i+1; j < types.length; ++j) {
                final PrecursorIonType r = types[j];
                final double mzdiff = r.getModificationMass()-l.getModificationMass();
                final long key = getkey(mzdiff);
                //


                if (Math.abs(key)>getkey(0.1)) {
                    map.compute(key, (k,v)->v==null ? new AdductMassDifference(mzdiff, new PrecursorIonType[]{l}, new PrecursorIonType[]{r}) : v.extend(l,r));
                }
            }
        }
        // add inverted pairs, too
        final AdductMassDifference[] diffs = map.values().toArray(AdductMassDifference[]::new);
        for (AdductMassDifference d : diffs) {
            map.put(-d.key, new AdductMassDifference(-d.mzDifference, d.rightTypes, d.leftTypes));
        }
        return map;
    }

    AdductMassDifference(double mzDifference, PrecursorIonType[] leftTypes, PrecursorIonType[] rightTypes) {
        this.key = getkey(mzDifference);
        this.mzDifference = mzDifference;
        this.leftTypes = leftTypes;
        this.rightTypes = rightTypes;
    }

    AdductMassDifference extend(PrecursorIonType left, PrecursorIonType right) {
        PrecursorIonType[] ll = Arrays.copyOf(leftTypes,leftTypes.length+1);
        PrecursorIonType[] rr = Arrays.copyOf(rightTypes,rightTypes.length+1);
        ll[leftTypes.length] = left;
        rr[rightTypes.length] = right;
        return new AdductMassDifference(mzDifference, ll, rr);
    }

    public int size() {
        return leftTypes.length;
    }

    public boolean isAmbigous() {
        return leftTypes.length > 1 || rightTypes.length>1;
    }

    public PrecursorIonType getLeftAt(int i) {
        return leftTypes[i];
    }
    public PrecursorIonType getRightAt(int i) {
        return rightTypes[i];
    }

    public PrecursorIonType[] getAllLeftTypes() {
        return leftTypes;
    }
    public PrecursorIonType[] getAllRightTypes() {
        return rightTypes;
    }

    public double getDeltaMass() {
        return mzDifference;
    }
}
