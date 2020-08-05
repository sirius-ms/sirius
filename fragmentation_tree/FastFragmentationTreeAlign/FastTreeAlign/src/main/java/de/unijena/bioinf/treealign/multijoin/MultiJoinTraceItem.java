
package de.unijena.bioinf.treealign.multijoin;

import de.unijena.bioinf.treealign.Set;
import de.unijena.bioinf.treealign.TraceItem;
import de.unijena.bioinf.treealign.Tree;


/**
 * @author Kai Dührkop
 */
class MultiJoinTraceItem<T> extends TraceItem<T> {
    
    final short l;
    final short r;

    MultiJoinTraceItem(Tree<T> u, Tree<T> v, int a, int b, short l, short r) {
        super(u, v, a, b);
        this.l = l;
        this.r = r;
    }

    MultiJoinTraceItem(Tree<T> u, Tree<T> v, int a, int b) {
        this(u, v, a, b, (short) 0, (short) 0);
    }

    MultiJoinTraceItem(Tree<T> u, Tree<T> v) {
        this(u, v, Set.of(u.children()), Set.of(v.children()));
    }
}
