package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;
import gnu.trove.map.hash.TLongShortHashMap;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.AllPairsShortestPaths;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class LongestPathKernel implements MoleculeKernel<LongestPathKernel.Prepared> {

    protected int diameter;

    protected final static boolean SMOOTH = false;

    public LongestPathKernel(int diameter) {
        this.diameter = diameter;
    }

    @Override
    public Prepared prepare(PredictableCompound compound) {
        try {
            return new Prepared(compound.getMolecule(),diameter);
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, Prepared l, Prepared r) {
        return compareLinear(l.explicitMap,r.explicitMap);
    }

    public double compareLinear(TLongShortHashMap left, TLongShortHashMap right) {
        long[] count = new long[1];
        left.forEachEntry((a, b) -> {
            long s = right.get(a);
            count[0] += b*s;
            return true;
        });
        return count[0];
    }

    static TLongShortHashMap makeExplicitMap(int[] identities, AllPairsShortestPaths paths) {
        final TLongShortHashMap map = new TLongShortHashMap(32,0.75f,0,(short)0);
        final ByteBuffer buffer = ByteBuffer.allocate(8);
        final LongBuffer aslong = buffer.asLongBuffer();
        final IntBuffer asInt = buffer.asIntBuffer();
        for (int i=0; i < identities.length; ++i) {
            for (int j=0; j < identities.length; ++j) {
                int a = identities[i];
                int b = identities[j];
                asInt.put(a);
                asInt.put(b);
                asInt.rewind();
                long X = aslong.get(0);

                short set = map.get(X);
                int pathSize = Math.max(set, paths.from(i).distanceTo(j));
                map.put(X, (short)pathSize);
            }
        }
        return map;
    }

    public static class Prepared {
        protected TLongShortHashMap explicitMap;
        public Prepared(IAtomContainer molecule, int diameter) throws CDKException {
            int st;
            switch (diameter) {
                case 0: st=CircularFingerprinter.CLASS_ECFP0;break;
                case 2: st=CircularFingerprinter.CLASS_ECFP2;break;
                case 4: st=CircularFingerprinter.CLASS_ECFP4;break;
                case 6: st=CircularFingerprinter.CLASS_ECFP6;break;
                default: throw new IllegalArgumentException("Unsupported diameter");
            }
            CircularFingerprinter fp = new CircularFingerprinter(st);
            fp.calculate(molecule);
            this.explicitMap = makeExplicitMap(fp.identity,new AllPairsShortestPaths(molecule));
        }
    }



}
