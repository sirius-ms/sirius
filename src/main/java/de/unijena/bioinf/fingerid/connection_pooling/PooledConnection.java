package de.unijena.bioinf.fingerid.connection_pooling;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;

public final class PooledConnection<T> implements Closeable, AutoCloseable{
    public final T connection;
    volatile boolean closed;
    private final ConnectionPool<T> pool;

    PooledConnection(ConnectionPool<T> pool, T connection) {
        if (connection==null) throw new NullPointerException();
        this.pool = pool;
        this.connection = connection;
        this.closed = false;
    }

    @Override
    public void close() throws IOException {
        pool.freeConnection(this);
    }
}
