package de.unijena.bioinf.fingerid.fingerprints;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;

public class FPKernel {


    protected static class ECFPKernel {

        public ECFPKernel() {

        }

        public double[] oneAgainstAll(IAtomContainer a, List<IAtomContainer> rest) throws CDKException {
            final double[] kernel = new double[rest.size()];

            final CircularFingerprinter circularFingerprinter = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP6);
            circularFingerprinter.calculate(a);
            final TIntIntHashMap LEFT = new TIntIntHashMap(100,0.75f,-1,0), RIGHT = new TIntIntHashMap(100,0.75f,-1,0);
            final TIntHashSet keys = new TIntHashSet(100,0.75f, -1);
            for (int k=0; k < circularFingerprinter.getFPCount(); ++k) {
                final CircularFingerprinter.FP fp = circularFingerprinter.getFP(k);
                LEFT.adjustOrPutValue(fp.hashCode, 1, 1);
            }
            final CircularFingerprinter right = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP6);
            for (int i=0; i < rest.size(); ++i) {
                RIGHT.clear();
                keys.clear();
                keys.addAll(LEFT.keys());
                right.calculate(rest.get(i));
                for (int k=0; k < right.getFPCount(); ++k) {
                    final CircularFingerprinter.FP fp = right.getFP(k);
                    RIGHT.adjustOrPutValue(fp.hashCode, 1, 1);
                    keys.add(fp.hashCode);
                }

                for (int key : keys.toArray()) {
                    kernel[i] += Math.min(LEFT.get(key), RIGHT.get(key)) / (double)Math.max(LEFT.get(key), RIGHT.get(key));
                }
            }
            return kernel;
        }

    }

    protected static class ShortestPathKernel {

        public ShortestPathKernel() {

        }

        public double[] oneAgainstAll(IAtomContainer a, List<IAtomContainer> rest) throws CDKException {
            return null;
        }


    }

}
