package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.qsar.descriptors.bond.BondPartialTChargeDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;

public class ElectroPathKernel {

    public static class Prepared {

        public Prepared(PredictableCompound c) {
            BondPartialTChargeDescriptor descr = new BondPartialTChargeDescriptor();
            for (IBond b  : c.getMolecule().bonds()) {
                final IAtom left = b.getAtom(0);
                final IAtom right = b.getAtom(1);
                String l = left.getSymbol(), r= right.getSymbol();
                String bond;
                switch (b.getOrder()) {
                    case SINGLE: bond="-";break;
                    case DOUBLE: bond="="; break;
                    case TRIPLE: bond="#"; break;
                    default: bond="?";
                }
                if (b.isAromatic()) bond = ":";
                double value = ((DoubleResult)descr.calculate(b,c.getMolecule()).getValue()).doubleValue();
                System.out.println(l+bond+r+"\t"+value);
            }
        }

    }

}
