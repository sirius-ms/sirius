package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

public class DeNovoStructureUtils {

    // needed to perceive aromaticity
    final CDKHydrogenAdder hydrogenAdder = CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance());
    final CycleFinder cycles = Cycles.or(Cycles.all(), Cycles.all(6));
    final Aromaticity aromaticity = new Aromaticity(ElectronDonation.daylight(), cycles);

    public IAtomContainer perceiveAromaticityOnSMILES(@NotNull String smiles) {
        IAtomContainer molecule = FixedFingerprinter.parseStructureFromStandardizedSMILES(smiles);
        try {
            aromaticity.apply(molecule);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
            hydrogenAdder.addImplicitHydrogens(molecule);
        } catch (CDKException e) {
            e.printStackTrace();
            return null;
        }
        return molecule;
    }
}
