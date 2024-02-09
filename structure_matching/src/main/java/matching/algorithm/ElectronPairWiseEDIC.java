package matching.algorithm;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.isomorphism.VentoFoggia;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class ElectronPairWiseEDIC extends EDIC{

    private final MolecularFormula molFormula1;
    private final MolecularFormula molFormula2;

    public ElectronPairWiseEDIC(IAtomContainer molecule1, IAtomContainer molecule2) throws CDKException {
        super(molecule1, molecule2);
        this.molFormula1 = this.getMolecularFormula(this.getFirstMolecule());
        this.molFormula2 = this.getMolecularFormula(this.getSecondMolecule());
    }

    private MolecularFormula getMolecularFormula(IAtomContainer molecule){
        MolecularFormula mf = new MolecularFormula();
        molecule.atoms().forEach(mf::addIsotope);
        return mf;
    }

    @Override
    public double compare() {
        if (this.score == -1d) {
            // 1. STEP: Test if both molecules have the same molecular formula
            if (MolecularFormulaManipulator.compare(this.molFormula1, this.molFormula2)) {
                // 2. STEP: Create for each molecule one copy with removed hydrogens:
                IAtomContainer mol1 = AtomContainerManipulator.removeHydrogens(this.getFirstMolecule());
                IAtomContainer mol2 = AtomContainerManipulator.removeHydrogens(this.getSecondMolecule());

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
                                        this.score = 1d;
                                        return this.score;
                                    }
                                }
                            }
                        }
                    }
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
            this.score = 0d;
        }
        return this.score;
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
}
