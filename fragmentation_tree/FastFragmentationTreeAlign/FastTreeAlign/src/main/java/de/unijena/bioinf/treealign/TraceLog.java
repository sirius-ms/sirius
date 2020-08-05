
package de.unijena.bioinf.treealign;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Iterator;

public class TraceLog<T> extends AbstractBacktrace<T> {

    private final PrintStream out;

    public TraceLog(PrintStream out) {
        this.out = out;
    }

    public TraceLog() {
        this.out = System.out;
    }

    @Override
    public void deleteLeft(float score, T node) {
        out.println("DELETE LEFT " + node + " WITH SCORE "+ score);
    }

    @Override
    public void deleteRight(float score, T node) {
        out.println("DELETE RIGHT " + node + " WITH SCORE "+ score);
    }

    @Override
    public void match(float score, T left, T right) {
        out.println("MATCH " + left + " WITH: " + right + " WITH SCORE "+ score);
    }

    @Override
    public void matchVertices(float score, T left, T right) {
        out.println("MATCH FRAGMENTS OF " + left + " WITH: " + right + " WITH SCORE "+ score);
    }
    
    @Override
    public void join(float score, Iterator<T> left, Iterator<T> right, int leftNumber, int rightNumber) {
        final ArrayDeque<T> lefts = new ArrayDeque<T>(leftNumber);
        final ArrayDeque<T> rights = new ArrayDeque<T>(rightNumber);
        while (left.hasNext()) lefts.offerFirst(left.next());
        while (right.hasNext()) rights.offerFirst(right.next());
        out.print("JOIN (" + lefts.removeFirst() + (lefts.isEmpty() ? ")" :  " WITH "));
        while (!lefts.isEmpty()) {
            out.print(lefts.removeFirst() + (lefts.isEmpty() ? ")" :  " WITH "));
        }
        out.print(" MATCHING: (" + rights.removeFirst() + (rights.isEmpty() ? "" :  " WITH "));
        while (!rights.isEmpty()) {
            out.print(rights.removeFirst() + (rights.isEmpty() ? "" :  " WITH "));
        }
        out.println(" WITH SCORE " + score);
    }
}
