package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;
import org.openscience.cdk.graph.AllPairsShortestPaths;
import org.openscience.cdk.interfaces.IAtom;

public class ShapeKernel implements MoleculeKernel<ShapeKernel.Prepared> {

    @Override
    public Prepared prepare(PredictableCompound compound) {
        return new Prepared(compound);
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, Prepared preparedLeft, Prepared preparedRight) {
        double cosine;
        if (preparedLeft.histogram.length>preparedRight.histogram.length)
            cosine = calc(preparedRight.histogram, preparedLeft.histogram);
        else cosine = calc(preparedLeft.histogram, preparedRight.histogram);

        // longest path
        double distance = (preparedLeft.histogram.length-preparedRight.histogram.length);
        distance = distance*distance;
        distance = Math.exp(-distance/5d);
        return (cosine + distance + cosine*distance)/3d;
    }

    private double calc(double[] a, double[] b) {
        double uv=0d,uu=0d,vv=0d;
        for (int i=0; i < a.length; ++i) {
            uv += a[i]*b[i];
            uu+=a[i]*a[i];
            vv+=b[i]*b[i];
        }
        for (int i=a.length; i < b.length; ++i) {
            vv+=b[i]*b[i];
        }
        return uv / Math.sqrt(uu*vv);
    }

    public static class Prepared {
        private double[] histogram;

        public Prepared(PredictableCompound c) {
            AllPairsShortestPaths paths = new AllPairsShortestPaths(c.getMolecule());
            int[] bins = new int[c.getMolecule().getAtomCount()];
            for (IAtom a : c.getMolecule().atoms()) {
                for (IAtom b : c.getMolecule().atoms()) {
                    if (a!=b) {
                        bins[paths.from(a).distanceTo(b)] += 1;
                    }
                }
            }
            int nonzero = bins.length-1;
            for (; nonzero >= 0; --nonzero) {
                if (bins[nonzero]>0) break;
            }
            ++nonzero;
            this.histogram = new double[nonzero];
            int total=0;
            for (int k=0; k < histogram.length; ++k) {
                histogram[k] = bins[k];
                total += bins[k];
            }
            for (int k=0; k < histogram.length; ++k) {
                histogram[k] /= total;
            }
        }

        public double[] getHistogram() {
            return histogram;
        }
    }

}
