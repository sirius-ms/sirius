package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import java.util.BitSet;

public class MACCSFingerprinter implements MoleculeKernel<BitSet> {
    @Override
    public BitSet prepare(PredictableCompound compound) {
        try {
            return new org.openscience.cdk.fingerprint.MACCSFingerprinter(SilentChemObjectBuilder.getInstance()).getBitFingerprint(compound.getMolecule()).asBitSet();
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, BitSet preparedLeft, BitSet preparedRight) {
        final BitSet copy = (BitSet) preparedLeft.clone();
        copy.and(preparedRight);
        return ((double)copy.cardinality()) / ((double)preparedLeft.cardinality()+preparedRight.cardinality()-copy.cardinality());
    }
}
