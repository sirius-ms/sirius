/*
\\@Kai :-) todo make compatible again
package de.unijena.bioinf.ChemistryBase.utils;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ConnectionPoolTest {

    protected static class Resource {
        private final int id;
        private volatile boolean closed;
        private AtomicInteger inUse;
        public double someValue;

        public Resource(int id) {
            this.id = id;
            closed = false;
            this.inUse = new AtomicInteger();
        }

        public void use() {
            assertFalse(closed);
            assertEquals("Resource is used in two threads in parallel!", 1, this.inUse.incrementAndGet() );
        }

        public void unuse() {
            assertFalse(closed);
            assertEquals("Resource is used in two threads in parallel!", 0, this.inUse.decrementAndGet() );
        }


    }

    protected static class ResourceConnector implements ConnectionPool.Connection<Resource> {

        private AtomicInteger ID = new AtomicInteger(0);

        @Override
        public Resource open() {
            return new Resource(ID.incrementAndGet());
        }

        @Override
        public void close(Resource connection) {
            connection.closed = true;
        }
    }

    @Test
    public void testConnectionPool() {

        final ConnectionPool<Resource> resourcePool = new ConnectionPool<>(new ResourceConnector(), 10);

        final ExecutorService service = Executors.newFixedThreadPool(40);

        for (int i=0; i < 1000; ++i) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    try (final ConnectionPool<Resource>.PooledConnection c = resourcePool.orderConnection()) {
                        c.connection.use();
                        System.out.println("Do something with " + c.connection.id);
                        assertTrue("id is <= 10: " + c.connection.id, c.connection.id <= 10 );
                        double v = 0d;
                        for (int i=1; i < 10000; ++i) {
                            v += i % 2 == 0 ? Math.exp(-i) : -Math.exp(-i);
                        }
                        c.connection.someValue = v;
                        c.connection.unuse();
                    } catch (InterruptedException|IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        service.shutdown();
        try {
            service.awaitTermination(100, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            resourcePool.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConnectionPoolShutdown() {

        final ConnectionPool<Resource> resourcePool = new ConnectionPool<>(new ResourceConnector(), 10);

        final ExecutorService service = Executors.newFixedThreadPool(150);
        final AtomicInteger counter = new AtomicInteger(0), counter2=new AtomicInteger(0);
        for (int i=0; i < 100; ++i) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    counter2.incrementAndGet();
                    try (final ConnectionPool<Resource>.PooledConnection c = resourcePool.orderConnection()) {
                        c.connection.use();
                        System.out.println("Do something with " + c.connection.id);
                        assertTrue("id is <= 10: " + c.connection.id, c.connection.id <= 10 );
                        double v = 0d;
                        for (int i=1; i < 500000; ++i) {
                            v += i % 2 == 0 ? Math.exp(-i) : -Math.exp(-i);
                        }
                        c.connection.someValue = v;
                        c.connection.unuse();
                        counter.incrementAndGet();
                    } catch (InterruptedException|IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        service.shutdown();
        try {
            resourcePool.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(100, counter2.get());
        assertEquals(100, counter.get());
    }
}
*/
