
package de.unijena.bioinf.counting;

import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.treealign.Tree;

import java.util.ArrayList;
import java.util.List;

public class CompleteTreeDecorator<T> extends PostOrderTraversal.Call<T, Tree<T>> {
    private final ArrayList<Tree<T>> list;
    public int maxDegree;
    private int index;

    public CompleteTreeDecorator(ArrayList<Tree<T>> list) {
        this.index = 0;
        this.list = list;
    }

    public Tree<T> call(T vertex, List<Tree<T>> values, boolean isRoot) {
        assert isRoot == (getCursor().getDepth() == 0);
        final int i = index++;
        final int num = (isRoot ? -1 : getCursor().getCurrentSiblingIndex());
        final Tree<T> node = new Tree<T>(vertex, i, num, getCursor().getDepth(), values);
        this.maxDegree = Math.max(maxDegree, getCursor().getAdapter().getDegreeOf(vertex));
        list.add(node);
        for (Tree<T> n : values) n.setParent(node);
        return node;
    }
}
