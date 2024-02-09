package matching.algorithm;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.isomorphism.VentoFoggia;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public abstract class EDIC implements Matcher {

    /**
     * The {@link IAtomContainer} object representing the molecule that will be compared to {@link EDIC#molecule2}.
     */
    private final IAtomContainer molecule1;
    /**
     * The {@link IAtomContainer} object representing the molecule that will be compared to {@link EDIC#molecule1}.
     */
    private final IAtomContainer molecule2;
    /**
     * The calculated score between both molecules - {@link EDIC#molecule1} and {@link EDIC#molecule2}.<br>
     * <p>
     * The score can be 0 or 1. If the score is 1, molecule1 can be transformed into molecule2 (or or vice versa)
     * by deleting and inserting a bond. If this transformation is not possible, the score will be 0.
     * Note that a bond deletion is defined as decrementing its edge label.<br>
     * E.g.: A double bond will be a single bond after one deletion. A single bond will be completely removed.
     */
    protected double score;

    public EDIC(IAtomContainer molecule1, IAtomContainer molecule2) throws CDKException {
        // Initialisation of this.molecule1 and this.molecule2.
        // Detect all aromatic bonds in both molecules and convert implicit hydrogens into explicit hydrogen atoms.
        final CDKHydrogenAdder hydrogenAdder = CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.or(Cycles.all(), Cycles.all(6));
        Aromaticity aromaticity = new Aromaticity(ElectronDonation.daylight(), cycles);

        this.molecule1 = this.setUpMolecule(molecule1, hydrogenAdder, aromaticity);
        this.molecule2 = this.setUpMolecule(molecule2, hydrogenAdder, aromaticity);
        this.score = -1d;
    }

    private IAtomContainer setUpMolecule(IAtomContainer molecule, CDKHydrogenAdder hydrogenAdder, Aromaticity aromaticity) throws CDKException {
        aromaticity.apply(molecule);
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
        hydrogenAdder.addImplicitHydrogens(molecule);
        AtomContainerManipulator.convertImplicitToExplicitHydrogens(molecule);
        return molecule;
    }


    @Override
    public double getScore() {
        if (this.score != -1) {
            return this.score;
        } else {
            return this.compare();
        }
    }

    @Override
    public IAtomContainer getFirstMolecule() {
        return this.molecule1;
    }

    @Override
    public IAtomContainer getSecondMolecule() {
        return this.molecule2;
    }
}
