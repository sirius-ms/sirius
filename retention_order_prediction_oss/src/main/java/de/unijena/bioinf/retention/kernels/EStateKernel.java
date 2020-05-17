package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.descriptors.molecular.KierHallSmartsDescriptor;
import org.openscience.cdk.qsar.result.IntegerArrayResult;

public class EStateKernel implements MoleculeKernel<int[]> {

    @Override
    public int[] prepare(PredictableCompound compound) {
        final KierHallSmartsDescriptor descr = new KierHallSmartsDescriptor();
        try {
            Aromaticity.cdkLegacy().apply(compound.getMolecule());
            final DescriptorValue calculate = descr.calculate(compound.getMolecule());
            final IntegerArrayResult value = (IntegerArrayResult) calculate.getValue();
            final int[] counts = new int[value.length()];
            for (int i=0; i < counts.length; ++i) {
                counts[i] = value.get(i);
            }
            return counts;
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, int[] preparedLeft, int[] preparedRight) {
        int mn=0,mx=0;
        for (int i=0; i < preparedLeft.length; ++i) {
            mn += Math.min(preparedLeft[i],preparedRight[i]);
            mx += Math.max(preparedLeft[i],preparedRight[i]);
        }
        return ((double)mn)/mx;
    }
}
