package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;
import gnu.trove.map.hash.TIntIntHashMap;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;

public class SubstructureLinearKernel implements MoleculeKernel<SubstructureLinearKernel.Prepared> {


    @Override
    public Prepared prepare(PredictableCompound compound) {
        final CircularFingerprinter fp = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP6);
        try {
            fp.calculate(compound.getMolecule());
            return new Prepared(fp);
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, Prepared preparedLeft, Prepared preparedRight) {
        int[] dot = new int[]{0,0,0};
        preparedLeft.fps.forEachEntry((key,value)->{
            final int value2 = preparedRight.fps.get(key);
            dot[0] += value*value2;
            dot[1] += value*value;
            dot[2] += value2*value2;
            return true;
        });
        preparedRight.fps.forEachEntry((key,value)->{
            final int value2 = preparedLeft.fps.get(key);
            if (value2 == 0) {
                dot[2] += value*value;
            }
            return true;
        });
        return ((double)dot[0])/Math.sqrt((double)dot[1]*dot[2]);
    }

    public static class Prepared {
        protected final TIntIntHashMap fps;
        public Prepared(CircularFingerprinter fp) {
            this.fps = new TIntIntHashMap(125,0.75f,0,0);
            for (int i=0; i < fp.getFPCount(); ++i) {
                this.fps.adjustOrPutValue(fp.getFP(i).hashCode,1,1);
            }
        }
    }



}
