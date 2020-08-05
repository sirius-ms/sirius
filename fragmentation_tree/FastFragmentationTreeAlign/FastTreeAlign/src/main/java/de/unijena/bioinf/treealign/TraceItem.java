
package de.unijena.bioinf.treealign;

import java.util.List;

public class TraceItem<T> {
    public final int A;
    public final int B;
    public final Tree<T> u;
    public final Tree<T> v;

    public TraceItem(Tree<T> u, Tree<T> v, int a, int b) {
        A = a;
        B = b;
        this.u = u;
        this.v = v;
    }

    public TraceItem(Tree<T> u, Tree<T> v, List<Tree<T>> a, List<Tree<T>> b) {
        this(u, v, (1<<a.size())-1, (1<<b.size())-1);
    }

    public TraceItem(Tree<T> u, Tree<T> v) {
        this(u, v, u.children(), v.children());

    }
}
