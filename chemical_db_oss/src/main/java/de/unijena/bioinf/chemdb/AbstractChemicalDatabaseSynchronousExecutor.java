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

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Marcus Ludwig on 15.11.16.
 */
public class AbstractChemicalDatabaseSynchronousExecutor implements AbstractChemicalDatabase,  Closeable {

    AbstractChemicalDatabase[] databases;
    AtomicBoolean[] isRunning;
    Executor executor;
    private volatile long filter = DataSource.ALL.flag;
    public AbstractChemicalDatabaseSynchronousExecutor(AbstractChemicalDatabase... chemicalDatabases) {
        this.databases = chemicalDatabases;
        this.isRunning = new AtomicBoolean[this.databases.length];
        for (int i = 0; i < isRunning.length; i++) {
            isRunning[i] = new AtomicBoolean(false);
        }
        executor = new Executor();
        Thread t = new Thread(executor);
        t.start();
    }

    public long getBioFilter() {
        return filter;
    }

    public synchronized void setFilter(long filter) {
        this.filter = filter;
    }

    @Override
    public String getName() {
        return Arrays.stream(this.databases).map(AbstractChemicalDatabase::getName).collect(Collectors.joining(","));
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        ChemicalDatabaseFuture<List<FormulaCandidate>> future = new ChemicalDatabaseFuture<>(executor, "lookupMolecularFormulas", new Class<?>[]{Long.class, double.class, Deviation.class, PrecursorIonType.class}, new Object[]{filter, mass, deviation, ionType});
        return future.get();
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        ChemicalDatabaseFuture<List<CompoundCandidate>> future = new ChemicalDatabaseFuture<>(executor, "lookupStructuresByFormula", new Class<?>[]{Long.class, MolecularFormula.class}, new Object[]{filter, formula});
        return future.get();
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        ChemicalDatabaseFuture<T> future = new ChemicalDatabaseFuture<>(executor, "lookupStructuresAndFingerprintsByFormula", new Class<?>[]{Long.class, MolecularFormula.class, Collection.class}, new Object[]{filter, formula, fingerprintCandidates});
        return future.get();
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        ChemicalDatabaseFuture<List<FingerprintCandidate>> future = new ChemicalDatabaseFuture<>(executor, "lookupFingerprintsByInchis", new Class<?>[]{Iterable.class}, new Object[]{inchi_keys});
        return future.get();
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        ChemicalDatabaseFuture<List<InChI>> future = new ChemicalDatabaseFuture<>(executor, "lookupManyInchisByInchiKeys", new Class<?>[]{Iterable.class}, new Object[]{inchi_keys});
        return future.get();
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
        ChemicalDatabaseFuture<List<FingerprintCandidate>> future = new ChemicalDatabaseFuture<>(executor, "lookupFingerprintsByInchi", new Class<?>[]{Iterable.class}, new Object[]{compounds});
        return future.get();
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws ChemicalDatabaseException {
        ChemicalDatabaseFuture future = new ChemicalDatabaseFuture<>(executor, "annotateCompounds", new Class<?>[]{List.class}, new Object[]{sublist});
        future.get();
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        ChemicalDatabaseFuture<List<InChI>> future = new ChemicalDatabaseFuture<>(executor, "findInchiByNames", new Class<?>[]{List.class}, new Object[]{names});
        return future.get();
    }

    @Override
    public String getChemDbDate() {
        return null;
    }

    @Override
    public long countAllFingerprints() throws ChemicalDatabaseException {
        int sum = 0;
        for (AbstractChemicalDatabase database : this.databases) {
            sum += database.countAllFingerprints();
        }
        return sum;
    }

    @Override
    public long countAllFormulas() throws ChemicalDatabaseException {
        long sum = 0;
        for (AbstractChemicalDatabase database : this.databases) {
            sum += database.countAllFormulas();
        }
        return sum;
    }

    @Override
    public boolean containsFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        ChemicalDatabaseFuture<Boolean> future = new ChemicalDatabaseFuture<>(executor, "containsFormula", new Class<?>[]{Long.class, MolecularFormula.class}, new Object[]{filter, formula});
        return future.get();
    }


    @Override
    public void close() {
        executor.close();
    }


    class Executor implements Runnable, Closeable {
        private Set<ChemicalDatabaseFuture> finished;
        private Set<ChemicalDatabaseFuture> runningFutures;

        private boolean running;
        private Queue<ChemicalDatabaseFuture> queue;

        private Executor() {
            finished = Collections.synchronizedSet(new HashSet<ChemicalDatabaseFuture>());
            runningFutures = Collections.synchronizedSet(new HashSet<ChemicalDatabaseFuture>());
            queue = new LinkedList<>();
        }

        /**
         * don't execute if your Task of interest is not still running!!!
         *
         * @return
         * @throws InterruptedException
         */
        public void waitForExit(ChemicalDatabaseFuture future) {
            if (finished.contains(future)) {
                finished.remove(future);
                return;
            }
            synchronized (this) {
                while (true) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (finished.contains(future)) {
                        finished.remove(future);
                        return;
                    }
                }
            }
        }

        public void addToQueue(ChemicalDatabaseFuture future) throws ChemicalDatabaseException {
            synchronized (this) {
                if (!running) throw new ChemicalDatabaseException("cannot add to queue. Executor is already stopped");
                queue.add(future);
                notifyAll();
            }
        }

        @Override
        public void close() {
            running = false;
            synchronized (this) {
                notifyAll();
            }
        }

        @Override
        public void run() {
            running = true;
            while (running) {
                synchronized (this) {
                    try {
                        while (!queue.isEmpty()) {
                            for (int i = 0; i < isRunning.length; i++) {
                                synchronized (isRunning[i]) {
                                    if (!isRunning[i].get()) {
                                        ChemicalDatabaseFuture future = queue.poll();
                                        if (future == null) continue;
                                        future.setDB(databases[i]);
                                        Thread t = new Thread(future);
                                        t.start();
                                        isRunning[i].set(true);
                                    }
                                }
                            }
                            wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            synchronized (this) {
                while (true) {
                    if (runningFutures.isEmpty()) break;
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            for (AbstractChemicalDatabase database : databases) {
                try {
                    database.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void finished(ChemicalDatabaseFuture chemicalDatabaseFuture, AbstractChemicalDatabase db) {
            synchronized (this) {
                for (int i = 0; i < databases.length; i++) {
                    final AbstractChemicalDatabase database = databases[i];
                    if (database == db) isRunning[i].set(false);
                }
                runningFutures.remove(chemicalDatabaseFuture);
                finished.add(chemicalDatabaseFuture);
                this.notifyAll();
            }

        }
    }

    protected class ChemicalDatabaseFuture<R> implements Runnable {

        private R result;
        private ChemicalDatabaseException exception;

        private String name;
        private Class<?>[] paramTypes;
        private Object[] paramValues;
        private AbstractChemicalDatabaseSynchronousExecutor.Executor executor;

        private AbstractChemicalDatabase db;

        ChemicalDatabaseFuture(AbstractChemicalDatabaseSynchronousExecutor.Executor executor, String name, Class<?>[] paramTypes, Object[] paramValues) throws ChemicalDatabaseException {
            this.name = name;
            this.paramValues = paramValues;
            this.paramTypes = paramTypes;
            this.executor = executor;
            executor.addToQueue(this);
        }


        void setDB(AbstractChemicalDatabase db) {
            this.db = db;
        }


        public R get() throws ChemicalDatabaseException {
            if (result != null) return result;
            executor.waitForExit(this);
            if (exception != null) throw exception;
            return result;
        }

        @Override
        public void run() {
            try {
                result = (R) db.getClass().getDeclaredMethod(name, paramTypes).invoke(db, paramValues);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                this.exception = new ChemicalDatabaseException(e.getCause());
            } catch (InvocationTargetException e) {
                e.getCause().printStackTrace();
                this.exception = new ChemicalDatabaseException(e.getCause());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                this.exception = new ChemicalDatabaseException(e.getCause());
            } catch (Exception e) {
                e.printStackTrace();
                this.exception = new ChemicalDatabaseException(e.getCause());
            }
            executor.finished(this, db);
        }
    }


}
