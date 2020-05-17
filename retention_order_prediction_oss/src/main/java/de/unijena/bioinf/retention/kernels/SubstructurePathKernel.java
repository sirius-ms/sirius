package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.AllPairsShortestPaths;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.Arrays;
import java.util.HashMap;

public class SubstructurePathKernel implements MoleculeKernel<SubstructurePathKernel.Prepared> {

    protected int diameter;

    protected final static boolean SMOOTH = false;

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
        final int[] shared = sharedKeys(l, r);
        final TShortArrayList distancesL = new TShortArrayList(), distancesR = new TShortArrayList();
        double matches = 0d;
        for (int i=0; i < shared.length; ++i) {
            for (int j=i; j < shared.length; ++j) {
                final int A = shared[i];
                final int B = shared[j];
                distancesL.clearQuick(); distancesR.clearQuick();
                double m = scorePaths(getDistances(l, distancesL, A, B), getDistances(r, distancesR, A, B));
                matches += m;

            }
        }
        return matches;
    }


    private double smoothPaths(TShortArrayList left, TShortArrayList right) {
        double score=0d;
        for (int i=0; i < left.size(); ++i) {
            for (int j=0; j<right.size(); ++j) {
                final int distance = Math.abs(left.get(i)-right.get(j));
                if (distance<smoothDist.length) {
                    score += smoothDist[distance];
                }
            }
        }
        return score;
    }

    private double scorePaths(TShortArrayList left, TShortArrayList right) {
        if (SMOOTH) return smoothPaths(left,right);
        else return countMatchingPaths(left,right);
    }

    private double countMatchingPaths(TShortArrayList left, TShortArrayList right) {
        left.sort();
        right.sort();
        int i=0,j=0;
        int matches = 0;
        while (i < left.size() && j < right.size()) {
            short a = left.get(i);
            short b = right.get(j);
            if (a==b) {
                ++matches;
                ++i;
                ++j;
            } else if (a<b) {
                ++i;
            } else {
                ++j;
            }
        }
        return matches;
    }

    private static double[] smoothDist = new double[20];
    static {
        for (int i=0; i < smoothDist.length; ++i) {
            smoothDist[i] = Math.exp(-i/2d);
        }
    }

    private TShortArrayList getDistances(Prepared l, TShortArrayList distances, int a, int b) {
        final int[] ds = l.identity2atoms.get(a);
        final int[] ds2 = l.identity2atoms.get(b);
        for (int p=0; p < ds.length; ++p) {
            for (int q=0; q < ds2.length; ++q) {
                if (ds[p]!=ds2[q]) {
                    distances.add((short) l.shortestPaths.from(ds[p]).distanceTo(ds2[q]));
                }
            }
        }
        return distances;
    }

    private int[] sharedKeys(Prepared l, Prepared r) {
        final TIntArrayList sharedKeys = new TIntArrayList();
        for (Integer key : l.identity2atoms.keySet()) {
            if (r.identity2atoms.containsKey(key)) {
                sharedKeys.add(key);
            }
        }
        return sharedKeys.toArray();
    }

    public static class Prepared {
        protected final int[] identities;
        protected HashMap<Integer, int[]> identity2atoms;
        protected AllPairsShortestPaths shortestPaths;
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
        }
    }



}
