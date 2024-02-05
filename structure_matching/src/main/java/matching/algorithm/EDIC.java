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

public class EDIC implements Matcher {

    /**
     * The {@link IAtomContainer} object representing the molecule that will be compared to {@link EDIC#molecule2}.
     */
    private IAtomContainer molecule1;
    /**
     * The {@link IAtomContainer} object representing the molecule that will be compared to {@link EDIC#molecule1}.
     */
    private IAtomContainer molecule2;
    /**
     * The calculated score between both molecules - {@link EDIC#molecule1} and {@link EDIC#molecule2}.<br>
     * <p>
     * The score can be 0 or 1. If the score is 1, molecule1 can be transformed into molecule2 (or or vice versa)
     * by deleting and inserting a bond. If this transformation is not possible, the score will be 0.
     * Note that a bond deletion is defined as decrementing its edge label.<br>
     * E.g.: A double bond will be a single bond after one deletion. A single bond will be completely removed.
     */
    private double score;

    public EDIC(IAtomContainer molecule1, IAtomContainer molecule2) throws CDKException {
        // Initialisation of this.molecule1 and this.molecule2.
        // Detect all aromatic bonds in both molecules and convert implicit hydrogens into explicit hydrogen atoms.
        final CDKHydrogenAdder hydrogenAdder = CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.or(Cycles.all(), Cycles.all(6));
        Aromaticity aromaticity = new Aromaticity(ElectronDonation.daylight(), cycles);

        this.molecule1 = this.setUpMolecule(molecule1, hydrogenAdder, aromaticity);
        this.molecule2 = this.setUpMolecule(molecule2, hydrogenAdder, aromaticity);
        this.score = -1;
    }

    private IAtomContainer setUpMolecule(IAtomContainer molecule, CDKHydrogenAdder hydrogenAdder, Aromaticity aromaticity) throws CDKException {
        aromaticity.apply(molecule);
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
        hydrogenAdder.addImplicitHydrogens(molecule);
        AtomContainerManipulator.convertImplicitToExplicitHydrogens(molecule);
        return molecule;
    }

    @Override
    public double compare() {
        if (this.score != -1) {
            return this.score;
        } else {
            // 1. STEP: Test if both molecules have the same molecular formula
            if (this.haveSameMolecularFormula()) {
                // 2. STEP: Create for each molecule one copy with removed hydrogens:
                IAtomContainer mol1 = AtomContainerManipulator.removeHydrogens(this.molecule1);
                IAtomContainer mol2 = AtomContainerManipulator.removeHydrogens(this.molecule2);

                /* 3. STEP: ITERATION THROUGH (mol1.bonds() x mol2.bonds())
                 * For each pair (e1,e2) of edges in mol1 and mol2, check if both molecule graphs are isomorphic
                 * after deleting both edges.
                 * If a pair (e1,e2) is found that meets this requirement, stop this comparison and return 1.
                 *
                 * NOTE: an edge deletion is defined as decrementing its edge label, and
                 * aromatic bonds are not considered.
                 * E.g., the label of an double bond is 2. Decrementing it would result in a single bond with label 1.
                 * If an edge label is 0, the corresponding edge will be removed.
                 */
                Pattern pattern;

                try {
                    for (int idxBond1 = 0; idxBond1 < mol1.getBondCount(); idxBond1++) {
                        if (!mol1.getBond(idxBond1).isAromatic()) {
                            IAtomContainer clonedMol1 = mol1.clone();
                            this.deleteEdge(clonedMol1, clonedMol1.getBond(idxBond1));
                            pattern = VentoFoggia.findIdentical(clonedMol1);

                            for (int idxBond2 = 0; idxBond2 < mol2.getBondCount(); idxBond2++) {
                                if (!mol2.getBond(idxBond2).isAromatic()) {
                                    IAtomContainer clonedMol2 = mol2.clone();
                                    this.deleteEdge(clonedMol2, clonedMol2.getBond(idxBond2));

                                    int[] mapping = pattern.match(clonedMol2);

                                    if (mapping.length > 0) {
                                        this.score = 1;
                                        return this.score;
                                    }
                                }
                            }
                        }
                    }
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
                this.score = 0;
                return this.score;
            } else {
                // Both molecules - this.molecule1 and this.molecule2 - don't have the same molecular formula.
                this.score = 0;
                return this.score;
            }
        }
    }

    private void addRemovedEdge(IAtomContainer molecule, IBond e, IBond.Order order) {
        if (order == IBond.Order.SINGLE || order == IBond.Order.UNSET) {
            molecule.addBond(e);
        } else {
            e.setOrder(order);
        }
    }

    private void deleteEdge(IAtomContainer molecule, IBond e) {
        switch (e.getOrder()) {
            case SINGLE:
            case UNSET:
                molecule.removeBond(e);
                break;
            case DOUBLE:
                e.setOrder(IBond.Order.SINGLE);
                break;
            case TRIPLE:
                e.setOrder(IBond.Order.DOUBLE);
                break;
            case QUADRUPLE:
                e.setOrder(IBond.Order.TRIPLE);
                break;
            case QUINTUPLE:
                e.setOrder(IBond.Order.QUADRUPLE);
                break;
            case SEXTUPLE:
                e.setOrder(IBond.Order.QUINTUPLE);
                break;
        }
    }

    private boolean haveSameMolecularFormula() {
        //Erzeuge als erstes eine Summenformel für molecule1:
        MolecularFormula molForm1 = new MolecularFormula();
        for (IAtom atom : this.molecule1.atoms()) {
            molForm1.addIsotope(atom);
        }

        //Erzeuge eine Summenformel für molecule2:
        MolecularFormula molForm2 = new MolecularFormula();
        for (IAtom atom : this.molecule2.atoms()) {
            molForm2.addIsotope(atom);
        }

        return MolecularFormulaManipulator.compare(molForm1, molForm2);
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
