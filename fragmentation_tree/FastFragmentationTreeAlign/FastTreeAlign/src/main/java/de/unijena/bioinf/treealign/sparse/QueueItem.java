
package de.unijena.bioinf.treealign.sparse;

/**
 * @author Kai Dührkop
 */
public final class QueueItem {
    public final int A;
    public final int B;
    public QueueItem(int A, int B) {
        this.A = A;
        this.B = B;
    }
}
