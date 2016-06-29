package de.unijena.bioinf.ChemistryBase.algorithm;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/*
    Simple producer/consumer scenario:
    - producer runs in a separate thread and inserts entries into the queue
    - as soon as the queue is full, the producer stops producing
        -> this allows some kind of memory control, e.g. a process is computing some intermediate values while another
            process is using these values. If the producer is much faster than the consumer, it will take breaks
    - the consumer runs in a separate thread and takes entries from the queue
    - at some point the producer is finished producing and runs the done() method. From this point, all consumers can
      remove what is still left in the queue and afterwards won't get anything new.
 */
public class Async<T> {

    protected final Object[] entries;
    protected int readFrom, writeTo, size;
    protected final Condition notFull, notEmpty;
    protected final ReentrantLock lock;
    protected int producers;


    public Async(int capacity) {
        this.entries = new Object[capacity];
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        this.notFull = lock.newCondition();
        this.size = 0;
        this.producers = 0;
    }

    public Producer newProducer() {
        lock.lock();
        try {
            ++producers;
            return new Producer();
        } finally {
            lock.unlock();
        }
    }

    public void newProducerThread(final Function<Producer,?> runnable) {
        final Producer producer = newProducer();
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.apply(producer);
                } finally {
                    if (producer.active) producer.done();
                }
            }
        });
        t.start();
    }

    protected boolean noProducing() {
        lock.lock();
        try {
            return producers<=0;
        } finally {
            lock.unlock();
        }
    }

    public Consumer newConsumer() {
        return new Consumer();
    }

    protected void releaseConsumer() {
        lock.lock();
        try {
            --producers;
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }

    }

    protected void produce(T item) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (size >= entries.length) {
                notFull.await();
            }
            push(item);
        } finally {
            lock.unlock();
        }
    }

    protected Optional<T> consume() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (size <= 0) {
                if (producers<=0) return Optional.absent();
                notEmpty.await();
            }
            return Optional.of(poll());
        } finally {
            lock.unlock();
        }
    }

    protected void push(T item) {
        entries[writeTo] = item;
        if (++writeTo == entries.length) {
            writeTo = 0;
        }
        ++size;
        notEmpty.signal();
    }
    protected T poll() {
        final T entry = (T)entries[readFrom];
        entries[readFrom] = null;
        --size;
        if (++readFrom == entries.length) {
            readFrom = 0;
        }
        notFull.signal();
        return entry;
    }

    public class Producer {

        private boolean active = true;

        public void produce(T entry) throws InterruptedException {
            if (!active) throw new IllegalStateException("Producer has already finished producing");
            Async.this.produce(entry);
        }

        public void done() {
            if (!active) throw new IllegalStateException("Producer has already finished producing");
            active=false;
            Async.this.releaseConsumer();
        }

    }

    public class Consumer implements Iterable<T> {

        public Optional<T> consume() throws InterruptedException {
            return Async.this.consume();
        }

        public boolean isDone() {
            return Async.this.noProducing();
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                private Optional<T> nextEntry;

                @Override
                public boolean hasNext() {
                    if (nextEntry==null) {
                        try {
                            nextEntry = consume();
                        } catch (InterruptedException e) {
                            nextEntry=null;
                        }
                    }
                    return nextEntry!=null && nextEntry.isPresent();
                }

                @Override
                public T next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    final T item = nextEntry.get();
                    nextEntry=null;
                    return item;
                }
            };
        }
    }

}
