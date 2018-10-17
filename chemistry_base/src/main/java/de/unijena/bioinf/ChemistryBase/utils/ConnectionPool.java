package de.unijena.bioinf.ChemistryBase.utils;

import gnu.trove.procedure.TObjectProcedure;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class implements any kind of connection pool that manages some external resources (e.g. remote IO connections,
 * HTTP clients, SQL databases, ...).
 * <p>
 * The connection pool starts with a capacity that controls how many open connection are allowed to stay in the pool.
 * When first time a connection is requested, the connection pool will open a new connection and increase its size. When the connection is "closed", the connection pool will (instead of closing it) put the open connection into a list. When the next time a connection is requested, the connection pool will first empty its list before open a new connection. When the capacity of the pool is reached, the connection pool will block at every request until a free connection is available again.
 * <p>
 * Important note: The connection pool still requires that every requested connection is closed properly. The "real" closing of a connection is done when the connection pool itself is closed. For connections, that have a limited lifetime (e.g. SQL or HTTP connections), you might want to keep the connection pool in life only for a limited time, too, to avoid dying connection objects.
 *
 * <blockquote><pre>
 * {@code
 * ConnectionPool<Resource> pool = new ConnectionPool(resourceConnector, 10);
 * for (int i=0; i < 1000; ++i) {
 *   // even if 1000 threads order connections, there will be never more than 10 connection objects
 *   // ever created
 *   new Thread(new Runnable() {
 *       try (final Resource r = pool.orderConnection()) {
 *         // dome something with resource
 *         // inside this block, the resource is exclusively available for this thread. No other thread
 *         // can access this resource
 *       }
 *   }).start();
 * }
 * pool.close(); // will close all resources
 * }
 * </pre></blockquote>
 *
 * @param <T>
 */
public class ConnectionPool<T> implements Closeable, AutoCloseable {

    public interface Connection<T> {
        public T open() throws IOException;

        public void close(T connection) throws IOException;
    }

    protected final Connection<T> connector;
    protected final ConcurrentLinkedQueue<T> freeConnections;
    protected volatile boolean shutdown, forcedShutdown;
    protected final AtomicInteger size, sharedCounter;
    protected final int capacity;
    protected final Condition noFreeConnectionsLeft, noOpenConnections;
    protected final ReentrantLock connectionLock;
    protected final AtomicInteger waitingThreads;

    public ConnectionPool(Connection<T> connector, int capacity) {
        this.connector = connector;
        this.freeConnections = new ConcurrentLinkedQueue<>();
        this.shutdown = false;
        this.size = new AtomicInteger(0);
        this.capacity = capacity;
        this.connectionLock = new ReentrantLock();
        this.noFreeConnectionsLeft = connectionLock.newCondition();
        this.noOpenConnections = connectionLock.newCondition();
        this.waitingThreads = new AtomicInteger(0);
        this.sharedCounter = new AtomicInteger(1);
    }

    protected int getSize() {
        return size.get();
    }

    public ConnectionPool(Connection<T> connector) {
        this(connector, Integer.MAX_VALUE);
    }

    public PooledConnection<T> orderConnection() throws InterruptedException, IOException {
        if (shutdown) throw new IllegalStateException("Connection pool is closed and does not accept new requests.");
        waitingThreads.incrementAndGet();
        return orderConnectionDontIncreaseWaitingCount();
    }

    private PooledConnection<T> orderConnectionDontIncreaseWaitingCount() throws InterruptedException, IOException {
        while (true) {
            if (freeConnections.isEmpty()) {
                // try to open a new connection
                if (size.intValue() < capacity) {
                    if (size.incrementAndGet() >= capacity) {
                        // ooops, we have a problem
                        size.decrementAndGet();
                        return waitForNewConnectionComesIn();
                    } else {
                        return new PooledConnection<T>(this, connector.open());
                    }
                } else {
                    return waitForNewConnectionComesIn();
                }
            } else {
                final T connection = freeConnections.poll();
                if (connection != null) return new PooledConnection<T>(this, connection);
            }
        }
    }

    /**
     * This will close all idling connections. This method might be called regularly for e.g. database connections
     * to avoid that a database connection (that is idling for a long time) is dying.
     */
    public void closeAllIdlingConnections() throws IOException {
        while (!freeConnections.isEmpty()) {
            final T c = freeConnections.poll();
            if (c != null) {
                connector.close(c);
                size.decrementAndGet();
            }
        }
        connectionLock.lock();
        try {
            noFreeConnectionsLeft.signalAll(); // we might get new capacity free
        } finally {
            connectionLock.unlock();
        }
    }

    /**
     * This will refresh all idling connections. This method might be called regularly for e.g. database connections
     * to avoid that a database connection (that is idling for a long time) is dying.
     */
    public void refreshAllIdlingConnections(TObjectProcedure<T> refreshOperation) {
        final ArrayList<T> refreshedConnections = new ArrayList<>();
        while (!freeConnections.isEmpty()) {
            final T c = freeConnections.poll();
            if (c != null) {
                refreshOperation.execute(c);
                refreshedConnections.add(c);
            }
        }
        freeConnections.addAll(refreshedConnections);
        connectionLock.lock();
        try {
            noFreeConnectionsLeft.signalAll(); // we might get new capacity free
        } finally {
            connectionLock.unlock();
        }
    }

    private PooledConnection<T> waitForNewConnectionComesIn() throws InterruptedException, IOException {
        while (true) {
            connectionLock.lock();
            try {
                noFreeConnectionsLeft.await();
                if (forcedShutdown) {
                    throw new InterruptedException("Interrupted by shutdown of connection pool");
                }
                final T c = freeConnections.poll();
                if (c != null) {
                    return new PooledConnection<T>(this, c);
                }
            } finally {
                connectionLock.unlock();
            }
            if (size.get() < capacity) {
                return orderConnectionDontIncreaseWaitingCount();
            }
        }
    }

    void freeConnection(final PooledConnection<T> connection) throws IOException {
        if (connection == null) throw new NullPointerException();
        if (connection.closed) return; // already freed
        synchronized (connection) {
            if (connection.closed) return;
            connection.closed = true;
        }
        if (waitingThreads.decrementAndGet() <= 0) {
            connectionLock.lock();
            noOpenConnections.signalAll();
            connectionLock.unlock();
        }
        // else move connection into free list
        if (forcedShutdown) {
            connector.close(connection.connection);
            return;
        }
        freeConnections.add(connection.connection);
        connectionLock.lock();
        noFreeConnectionsLeft.signal();
        connectionLock.unlock();
    }

    public void shutdown() throws InterruptedException, IOException {
        if (sharedCounter.decrementAndGet() > 0) return;
        shutdown = true;
        while (true) {
            connectionLock.lock();
            if (waitingThreads.get() <= 0) {
                connectionLock.unlock();
                break;
            }
            noOpenConnections.await();
            connectionLock.unlock();
        }
        // close all connections
        while (!freeConnections.isEmpty()) {
            connector.close(freeConnections.poll());
        }
    }

    @Override
    public void close() throws IOException {
        // this method blocks until all requests are processed
        try {
            shutdown();
        } catch (InterruptedException e) {
            enforceShutdown();
        }
    }

    /**
     * This will terminate all running connections. However, it cannot
     * guarantee that all open connections are properly closed.
     */
    public void enforceShutdown() throws IOException {
        if (sharedCounter.decrementAndGet() > 0) return;
        shutdown = true;
        forcedShutdown = true;
        while (!freeConnections.isEmpty()) {
            connector.close(freeConnections.poll());
        }
        connectionLock.lock();
        try {
            noFreeConnectionsLeft.signalAll(); // we might get new capacity free
        } finally {
            connectionLock.unlock();
        }
    }

    public ConnectionPool<T> newSharedConnectionPool() {
        sharedCounter.incrementAndGet();
        return this;
    }

    public int getCapacity() {
        return capacity;
    }
    public int getNumberOfIdlingConnections() {
        return freeConnections.size();
    }
}
