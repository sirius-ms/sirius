/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fingerid.connection_pooling;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PooledDBReconnector<T extends PooledDB> implements Closeable, AutoCloseable {
    public final static long NUMBER_OF_MILLISECONDS_TEN_SECONDS = 1000L * 10L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PooledDBReconnector.class);

    private T pooledDB;
    private final AtomicBoolean reconnectingDB;
    private final Class<T> source;

    public PooledDBReconnector(T pooledDB) {
        this.reconnectingDB = new AtomicBoolean(false);
        this.pooledDB = pooledDB;
        source = (Class<T>) this.pooledDB.getClass();
    }

    public void reconnect() {
        if (reconnectingDB.compareAndSet(false, true)) {
            SiriusJobs.runInBackground(new TinyBackgroundJJob<T>() {
                @Override
                protected T compute() throws Exception {
                    LOGGER.warn("Thread " + Thread.currentThread().getName() + " is reconnecting " + source.getName() + "."
                            + System.lineSeparator() +
                            pooledDB.getNumberOfIdlingConnections() + " Idle Connections to close."
                    );
                    while (reconnectingDB.get()) {
                        checkForInterruption();
                        try {
                            pooledDB.refresh();
                            try {
                                LOGGER.info("Checking RECONNECT!" + pooledDB.getNumberOfIdlingConnections() + " Idle Connections after Reconnect of  " + source.getName() + ".");
                                if (pooledDB.hasConnection(10)) {
                                    LOGGER.info("Reconnect SUCCESSFUL!" + pooledDB.getNumberOfIdlingConnections() + " Idle Connections after Reconnect of  " + source.getName() + ".");
                                    reconnectingDB.set(false);
                                    synchronized (PooledDBReconnector.this) {
                                        PooledDBReconnector.this.notifyAll();
                                    }
                                    return pooledDB;
                                } else {
                                    LOGGER.warn("Reconnecting of " + source.getName() + " on thread " + Thread.currentThread().getName() + " failed, Retry in 10s ...");
                                }
                            } catch (SQLException e) {
                                LOGGER.warn("Reconnecting of " + source.getName() + " on thread " + Thread.currentThread().getName() + " failed by exception, Retry in 10s ...", e);
                            }
                            waiting(NUMBER_OF_MILLISECONDS_TEN_SECONDS);

                        } catch (Exception e) {
                            checkForInterruption();
                            //this mean something with the object is wrong
                            LOGGER.error("Error during Reconnection process of " + source.getName() + " on thread " + Thread.currentThread().getName() + ". Try creating a new instance in 10s!", e);
                            try {
                                pooledDB.close();
                            } catch (IOException e1) {
                                LOGGER.error("Could not close " + source.getName() + " DB connection on thread " + Thread.currentThread().getName() + ".", e1);
                            }
                            waiting(NUMBER_OF_MILLISECONDS_TEN_SECONDS);
                            pooledDB = source.newInstance();
                        }
                    }
                    return pooledDB;
                }

                private void waiting(final long watingInMillis) throws InterruptedException {
                    final long waitUntil = System.currentTimeMillis() + watingInMillis;
                    while (System.currentTimeMillis() < waitUntil) {
                        try {
                            Thread.sleep(waitUntil - System.currentTimeMillis());
                        } catch (InterruptedException ignore) {
                        }
                    }
                }
            });
        }

        LOGGER.warn("Thread " + Thread.currentThread().getName() + " is waiting for Reconnect of " + source.getName() + " when trying to reconnect.");
        while (reconnectingDB.get()) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public T get() {
        if (reconnectingDB.get()) { //just to get the message only once
            LOGGER.warn("Thread " + Thread.currentThread().getName() + " is waiting for Reconnect of " + source.getName() + " when ordering Connection.");
            while (reconnectingDB.get()) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
        return pooledDB;
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Thread " + Thread.currentThread().getName() + " is closing/shutting down " + source.getName() + ".");
        pooledDB.close();
    }

}
