
package de.unijena.bioinf.treealign;

import java.util.*;

/**
 * @author Kai Dührkop
 */
public class StackedBacktrace<T> implements Backtrace<T> {

    private final List<Backtrace<T>> stack;

    public StackedBacktrace() {
        this.stack = new ArrayList<Backtrace<T>>();
    }

    public StackedBacktrace(Backtrace<T>... tracers) {
        this();
        pushAll(tracers);
    }

    @SuppressWarnings("unchecked")
    public <S extends Backtrace<T>> S get(Class<S> klass) {
        for (Backtrace<T> trace : stack) {
            if (klass.isInstance(trace)) return (S) trace;
        }
        return null;
    }

    public void pushAll(Backtrace<T>... tracers) {
        stack.addAll(Arrays.asList(tracers));
    }

    public void push(Backtrace<T> trace) {
        stack.add(trace);
    }

    public void pop() {
        stack.remove(stack.size() - 1);
    }

    @Override
    public void join(float score, Iterator<T> left, Iterator<T> right, int leftNumber, int rightNumber) {
        if (stack.size() == 1) stack.get(0).join(score, left, right, leftNumber, rightNumber);
        else {
            List<T> leftList = new LinkedList<T>();
            while (left.hasNext()) {
                leftList.add(left.next());
            }
            List<T> rightList = new LinkedList<T>();
            while (right.hasNext()) {
                rightList.add(right.next());
            }

            for (Backtrace<T> tracer : stack) {
                tracer.join(score, leftList.iterator(), rightList.iterator(), leftNumber, rightNumber);
            }
        }
    }

    @Override
    public void deleteLeft(float score, T node) {
        for (Backtrace<T> tracer : stack) {
            tracer.deleteLeft(score, node);
        }
    }

    @Override
    public void deleteRight(float score, T node) {
        for (Backtrace<T> tracer : stack) {
            tracer.deleteRight(score, node);
        }
    }

    @Override
    public void match(float score, T left, T right) {
        for (Backtrace<T> tracer : stack) {
            tracer.match(score, left, right);
        }
    }

    @Override
    public void innerJoinLeft(T node) {
        for (Backtrace<T> tracer : stack) {
            tracer.innerJoinLeft(node);
        }
    }

    @Override
    public void innerJoinRight(T node) {
        for (Backtrace<T> tracer : stack) {
            tracer.innerJoinRight(node);
        }
    }

    @Override
    public void matchVertices(float score, T left, T right) {
        for (Backtrace<T> tracer : stack) {
            tracer.matchVertices(score, left, right);
        }
    }
}
