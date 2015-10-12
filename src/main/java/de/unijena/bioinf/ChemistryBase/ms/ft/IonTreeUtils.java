package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies all in-source fragmentations and adducts to the tree. Set a PrecursorIonType annotation object to each vertex
 * of the tree
 */
public class IonTreeUtils {

    /**
     * Takes a computed tree as input with a certain PrecursorIonType. Resolve the PrecursorIonType, such that insource
     * fragmentations are reflected in the tree and all vertices in the tree have a neutral molecular formula excluding
     * the adduct and ionization. Annotates each vertex with a PrecursorIonType.
     *
     * This modifications might be in-place. So the caller have to ensure to copy the tree if he do not want to change it.
     */
    public FTree treeToNeutralTree(FTree tree) {
        PrecursorIonType ion = tree.getAnnotationOrThrow(PrecursorIonType.class);

        if (ion.getInSourceFragmentation().atomCount() > 0) {
            // add the in-source fragmentation to the tree
            tree.addRoot(ion.getInSourceFragmentation().add(tree.getRoot().getFormula()));
            tree.getOrCreateLossAnnotation(InsourceFragmentation.class).set(tree.getRoot().getOutgoingEdge(0), new InsourceFragmentation(true));
            ion = ion.withoutInsource();
        }
        if (ion.getAdduct().atomCount() > 0) {
            // remove the adduct from all fragments
            reduceTree(tree, ion, ion.getAdduct());
        } else {
            setPrecursorToEachNode(tree, tree.getRoot(), ion);
        }
        return tree;
    }

    /**
     * Insert the ionization into every vertex, such that each vertex is ionized with [M+?]+ and is labeled with the
     * molecular formula of the ion. Resolve also insource fragmentations.
     *
     * This modifications might be in-place. So the caller have to ensure to copy the tree if he do not want to change it.
     */
    public FTree treeToIonTree(FTree tree) {
        PrecursorIonType ion = tree.getAnnotationOrThrow(PrecursorIonType.class);
        final FragmentAnnotation<PrecursorIonType> fa = tree.getOrCreateFragmentAnnotation(PrecursorIonType.class);
        if (ion.getInSourceFragmentation().atomCount() > 0) {
            // add the in-source fragmentation to the tree
            tree.addRoot(ion.getInSourceFragmentation().add(tree.getRoot().getFormula()));
            tree.getOrCreateLossAnnotation(InsourceFragmentation.class).set(tree.getRoot().getOutgoingEdge(0), new InsourceFragmentation(true));
            ion = ion.withoutInsource();
        }
        final PrecursorIonType empty = PeriodicTable.getInstance().getUnknownPrecursorIonType(ion.getCharge());
        for (Fragment f : tree) {
            f.setFormula(ion.neutralMoleculeToPrecursorIon(f.getFormula()));
            fa.set(f, empty);
        }
        tree.setAnnotation(PrecursorIonType.class, empty);
        return tree;
    }

    private void reduceTree(final FTree tree, PrecursorIonType iontype, final MolecularFormula adduct) {
        reduceSubtree(tree, iontype, adduct, tree.getRoot());
    }

    private void reduceSubtree(final FTree tree, PrecursorIonType iontype, final MolecularFormula adduct, Fragment vertex) {
        if (vertex.isDeleted()) {
            System.out.println("????");
        };
        final FragmentAnnotation<PrecursorIonType> fa = tree.getOrCreateFragmentAnnotation(PrecursorIonType.class);
        // if adduct is lost: contract loss
        if (vertex.getInDegree() > 0 && vertex.getIncomingEdge().getFormula().equals(adduct)) {
            final PrecursorIonType newIonType = iontype.withoutAdduct();
            // delete this vertex
            final List<Fragment> children = new ArrayList<Fragment>(vertex.getChildren());
            for (Fragment f : children) tree.swapLoss(f, vertex.getParent());
            tree.deleteVertex(vertex);
            for (Fragment f : children) {
                setPrecursorToEachNode(tree, f, newIonType);
            }
            // finished
            return;
        }
        // if adduct is still part of the fragment: remove it from fragment
        final MolecularFormula f = vertex.getFormula().subtract(adduct);
        if (f.isAllPositiveOrZero() && !f.isEmpty()) {
            vertex.setFormula(f);
            fa.set(vertex, iontype);
            final ArrayList<Fragment> childs = new ArrayList<Fragment>(vertex.getChildren());
            for (Fragment g : childs) {
                reduceSubtree(tree, iontype, adduct, g);
            }
        } else {
            // if adduct is part of the loss, remove it from loss
            final MolecularFormula l = vertex.getIncomingEdge().getFormula().subtract(adduct);
            if (l.isAllPositiveOrZero()) {
                vertex.getIncomingEdge().setFormula(l);
                setPrecursorToEachNode(tree, vertex, iontype.withoutAdduct());
            } else {
                // otherwise: delete whole subtree
                tree.deleteSubtree(vertex);
            }
        }
    }

    private void setPrecursorToEachNode(FTree tree, Fragment f, final PrecursorIonType ionType) {
        final FragmentAnnotation<PrecursorIonType> fa = tree.getOrCreateFragmentAnnotation(PrecursorIonType.class);
        new PostOrderTraversal<Fragment>(tree.getCursor(f)).run(new PostOrderTraversal.Run<Fragment>() {
            @Override
            public void run(Fragment vertex, boolean isRoot) {
                fa.set(vertex, ionType);
            }
        });
    }
}
