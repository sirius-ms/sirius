package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;
import gnu.trove.map.hash.TIntIntHashMap;
import org.openscience.cdk.exception.CDKException;

public class SubstructureKernel implements MoleculeKernel<SubstructureKernel.Prepared> {

    private final int diameter;

    public SubstructureKernel(int diameter) {
        this.diameter = diameter;
    }

    public SubstructureKernel() {
        this(CircularFingerprinter.CLASS_ECFP6);
    }

    @Override
    public Prepared prepare(PredictableCompound compound) {
        final CircularFingerprinter fp = new CircularFingerprinter(diameter);
        fp.setPerceiveStereo(true);
        try {
            fp.calculate(compound.getMolecule());
            return new Prepared(fp);
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, Prepared preparedLeft, Prepared preparedRight) {
        int[] minMax = new int[]{0,0};
        preparedLeft.fps.forEachEntry((key,value)->{
            final int value2 = preparedRight.fps.get(key);
            minMax[0] += Math.min(value,value2);
            minMax[1] += Math.max(value,value2);
            return true;
        });
        preparedRight.fps.forEachEntry((key,value)->{
            final int value2 = preparedLeft.fps.get(key);
            if (value2 == 0)
                minMax[1] += value;
            return true;
        });
        return ((double)minMax[0])/minMax[1];
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
