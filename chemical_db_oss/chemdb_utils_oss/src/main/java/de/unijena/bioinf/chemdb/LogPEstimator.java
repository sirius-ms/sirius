package de.unijena.bioinf.chemdb;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.descriptors.molecular.JPlogPDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

public class LogPEstimator {
    final private CDKHydrogenAdder hd;
    final private CycleFinder cycles;
    final private Aromaticity aromaticity;
    final private JPlogPDescriptor logPDescriptor;

    public LogPEstimator() {
        hd = CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance());
        cycles      = Cycles.or(Cycles.all(), Cycles.all(6));
        aromaticity = new Aromaticity(ElectronDonation.daylight(), cycles);
        logPDescriptor = new JPlogPDescriptor();
    }

    /**
     * Computes pLogP value. Preparation step will change the IAtomContainer
     * @param mol
     * @return logP estimate
     * @throws CDKException
     */
    public double prepareMolAndComputeLogP(IAtomContainer mol) throws CDKException {
        prepare(mol);
        double lopPEstimate = computeLogP(mol);
        return lopPEstimate;
    }


    private void prepare(IAtomContainer mol) throws CDKException {
        aromaticity.apply(mol);
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        hd.addImplicitHydrogens(mol);
        AtomContainerManipulator.convertImplicitToExplicitHydrogens(mol);
    }

    private double computeLogP(IAtomContainer mol) {
        final double plogp = ((DoubleResult) logPDescriptor.calculate(mol).getValue()).doubleValue();
        return plogp;
    }
}
