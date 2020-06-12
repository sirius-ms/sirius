package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.procedure.TLongLongProcedure;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.AllPairsShortestPaths;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.HashMap;

public class SubstructurePathKernel implements MoleculeKernel<SubstructurePathKernel.Prepared> {

    protected int diameter;

    public SubstructurePathKernel(int diameter) {
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

    public double compareTanimoto(TLongLongHashMap left, TLongLongHashMap right) {
        long[] count = new long[2];
        left.forEachEntry(new TLongLongProcedure() {
            @Override
            public boolean execute(long a, long b) {
                long s = right.get(a)&b;
                count[0] += Long.bitCount(s);
                count[1] += Long.bitCount(b);
                return true;
            }
        });
        right.forEachEntry(new TLongLongProcedure() {
            @Override
            public boolean execute(long a, long b) {
                long s = left.get(a)&b;
                count[0] += Long.bitCount(s);
                count[1] += Long.bitCount(b);
                return true;
            }
        });
        count[0] /= 2;
        double intersection = count[0];
        double union = count[1]-count[0];
        return intersection/union;
    }

    public double compareLinear(TLongLongHashMap left, TLongLongHashMap right) {
        long[] count = new long[1];
        left.forEachEntry(new TLongLongProcedure() {
            @Override
            public boolean execute(long a, long b) {
                long s = right.get(a)&b;
                count[0] += Long.bitCount(s);
                return true;
            }
        });
        right.forEachEntry(new TLongLongProcedure() {
            @Override
            public boolean execute(long a, long b) {
                long s = left.get(a)&b;
                count[0] += Long.bitCount(s);
                return true;
            }
        });
        return count[0];
    }

    static TLongLongHashMap makeExplicitMap(int[] identities, AllPairsShortestPaths paths) {
        final TLongLongHashMap map = new TLongLongHashMap(32,0.75f,0,0);
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

                long set = map.get(X);
                int pathSize = Math.min(63, paths.from(i).distanceTo(j));
                set |= (1L<<pathSize);
                map.put(X, set);
            }
        }
        return map;
    }

    public static class Prepared {
        protected final int[] identities;
        protected HashMap<Integer, int[]> identity2atoms;
        protected AllPairsShortestPaths shortestPaths;
        protected TLongLongHashMap explicitMap;
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
            fp.storeIdentitesPerIteration = false;
            fp.calculate(molecule);
            this.identities = fp.identity;
            this.identity2atoms = new HashMap<>();
            final int[] empty = new int[0];
            for (int i=0; i < identities.length; ++i) {
                int[] ary = identity2atoms.getOrDefault(identities[i], empty);
                int[] copy = Arrays.copyOf(ary, ary.length+1);
                copy[copy.length-1] = i;
                identity2atoms.put(identities[i],copy);
            }
            this.shortestPaths = new AllPairsShortestPaths(molecule);
            this.explicitMap = makeExplicitMap(identities,shortestPaths);
        }
    }



}
