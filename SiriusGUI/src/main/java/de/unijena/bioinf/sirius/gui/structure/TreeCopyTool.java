package de.unijena.bioinf.sirius.gui.structure;

import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeNode;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;

public class TreeCopyTool {

    public static TreeNode copyTree(TreeNode root) {

        DefaultTreeNode newRoot = new DefaultTreeNode();
        copyNode(root, newRoot);

        copyTreeRek(root, newRoot);

        return newRoot;
    }

    private static void copyTreeRek(TreeNode node, TreeNode newNode) {
        for (TreeEdge edge : node.getOutEdges()) {
            TreeNode child = edge.getTarget();
            DefaultTreeNode newChild = new DefaultTreeNode();
            copyNode(child, newChild);

            DefaultTreeEdge newEdge = new DefaultTreeEdge();
            newEdge.setSource(newNode);
            newEdge.setTarget(newChild);
            newEdge.setLossFormula(edge.getLossFormula());
            newEdge.setLossMass(edge.getLossMass());
            newEdge.setScore(edge.getScore());

            newNode.addOutEdge(newEdge);
            newChild.setInEdge(newEdge);

            copyTreeRek(child, newChild);
        }
    }

    private static void copyNode(TreeNode node, TreeNode newNode) {
        newNode.setMolecularFormula(node.getMolecularFormula());
        newNode.setMolecularFormulaMass(node.getMolecularFormulaMass());
        newNode.setPeakMass(node.getPeakMass());
        newNode.setPeakAbsoluteIntenstiy(node.getPeakAbsoluteIntensity());
        newNode.setPeakRelativeIntensity(node.getPeakRelativeIntensity());
        newNode.setScore(node.getScore());
    }

}
