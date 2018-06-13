package de.unijena.bioinf.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.ExtendedConnectivityProperty;
import gnu.trove.map.hash.TLongIntHashMap;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.*;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class ECFPFingerprinter extends AbstractFingerprinter implements IFingerprinter {


    private final static TLongIntHashMap HASH_SET = new TLongIntHashMap(1024, 0.75f, Long.MIN_VALUE, -1);

    static {
        for (int i = 0; i < ExtendedConnectivityProperty.getFingerprintLength(); ++i) {
            HASH_SET.put(ExtendedConnectivityProperty.getHashValue(i), i);
        }
    }

    private final CircularFingerprinter circularFingerprinter = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP6);

    @Deprecated
    public CircularFingerprinter.FP[] getRelevantFingerprintDetails() {
        final ArrayList<CircularFingerprinter.FP> list = new ArrayList<>();
        for (int k=0; k < circularFingerprinter.getFPCount(); ++k) {
            final CircularFingerprinter.FP fp = circularFingerprinter.getFP(k);
            if (HASH_SET.containsKey(fp.hashCode)) {
                list.add(fp);
            }
        }
        Collections.sort(list, new Comparator<CircularFingerprinter.FP>() {
            @Override
            public int compare(CircularFingerprinter.FP o1, CircularFingerprinter.FP o2) {
                return Integer.compare(o1.hashCode, o2.hashCode);
            }
        });
        return list.toArray(new CircularFingerprinter.FP[list.size()]);
    }

    /*
    returns an array with the size of the ECFP fingerprint where all entries
    that are set contain the details and all missing properties are null
     */
    public CircularFingerprinter.FP[] getFingerprintDetails() {
        final CircularFingerprinter.FP[] ary = new CircularFingerprinter.FP[HASH_SET.size()];
        for (int k=0; k < circularFingerprinter.getFPCount(); ++k) {
            final CircularFingerprinter.FP fp = circularFingerprinter.getFP(k);
            if (HASH_SET.containsKey(fp.hashCode)) {
                ary[HASH_SET.get(fp.hashCode)] = fp;
            }
        }
        return ary;
    }

    @Override
    public IBitFingerprint getBitFingerprint(IAtomContainer container) throws CDKException {
        final BitSetFingerprint bf = new BitSetFingerprint(getSize());
        final ICountFingerprint icfp = circularFingerprinter.getCountFingerprint(container);
        for (int k=0, n = icfp.numOfPopulatedbins(); k < n; ++k) {
            final int index = HASH_SET.get(icfp.getHash(k));
            if (index >= 0) {
                if (icfp.getCount(k)>0) bf.set(index, true);
            }
        }
        return bf;

    }

    @Override
    public ICountFingerprint getCountFingerprint(IAtomContainer container) throws CDKException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Integer> getRawFingerprint(IAtomContainer container) throws CDKException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSize() {
        return ExtendedConnectivityProperty.getFingerprintLength();
    }
}
