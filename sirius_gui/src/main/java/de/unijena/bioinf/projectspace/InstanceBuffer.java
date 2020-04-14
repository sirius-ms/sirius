package de.unijena.bioinf.projectspace;

import java.util.LinkedHashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InstanceBuffer {
    Lock lock = new ReentrantLock();
    private final int maxSize;
    private final LinkedHashSet<InstanceBean> buffer;

    public InstanceBuffer(int maxSize) {
        this.maxSize = maxSize;
        buffer = new LinkedHashSet<>(maxSize + 1);
    }

    public void add(InstanceBean instanceBean) {
        lock.lock();
        try {
            buffer.remove(instanceBean);
            buffer.add(instanceBean);
            if (buffer.size() > maxSize) //remove the oldest instance
                remove(buffer.iterator().next());
        } finally {
            lock.unlock();
        }
    }

    public boolean remove(InstanceBean toRemove) {
        lock.lock();
        try {
            if (buffer.remove(toRemove)) {
                System.out.println("+++++++ Removing from cache: " + toRemove.toString());
                toRemove.clearFormulaResultsCache();
                toRemove.clearCompoundCache();
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}
