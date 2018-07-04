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

/**
 * Created by Marcus Ludwig on 15.11.16.
 */
public class AbstractChemicalDatabaseSynchronousExecutor extends AbstractChemicalDatabase implements Closeable {

    AbstractChemicalDatabase[] databases;
    AtomicBoolean[] isRunning;
    Executor executor;
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

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws DatabaseException {
        ChemicalDatabaseFuture<List<FormulaCandidate>> future = new ChemicalDatabaseFuture<>(executor, "lookupMolecularFormulas", new Class<?>[]{double.class, Deviation .class, PrecursorIonType.class}, new Object[]{mass, deviation, ionType});
        return future.get();
    }
    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws DatabaseException {
        ChemicalDatabaseFuture<List<CompoundCandidate>> future = new ChemicalDatabaseFuture<>(executor, "lookupStructuresByFormula", new Class<?>[]{MolecularFormula.class}, new Object[]{formula});
        return future.get();
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws DatabaseException {
        ChemicalDatabaseFuture<T> future = new ChemicalDatabaseFuture<>(executor, "lookupStructuresAndFingerprintsByFormula", new Class<?>[]{MolecularFormula.class, Collection.class}, new Object[]{formula, fingerprintCandidates});
        return future.get();
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws DatabaseException {
        ChemicalDatabaseFuture<List<FingerprintCandidate>> future = new ChemicalDatabaseFuture<>(executor, "lookupFingerprintsByInchis", new Class<?>[]{Iterable.class}, new Object[]{inchi_keys});
        return future.get();
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws DatabaseException {
        ChemicalDatabaseFuture<List<InChI>> future = new ChemicalDatabaseFuture<>(executor, "lookupManyInchisByInchiKeys", new Class<?>[]{Iterable.class}, new Object[]{inchi_keys});
        return future.get();
    }

    @Override
    public List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> inchi_keys) throws DatabaseException {
        ChemicalDatabaseFuture<List<FingerprintCandidate>> future = new ChemicalDatabaseFuture<>(executor, "lookupManyFingerprintsByInchis", new Class<?>[]{Iterable.class}, new Object[]{inchi_keys});
        return future.get();
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws DatabaseException {
        ChemicalDatabaseFuture<List<FingerprintCandidate>> future = new ChemicalDatabaseFuture<>(executor, "lookupFingerprintsByInchi", new Class<?>[]{Iterable.class}, new Object[]{compounds});
        return future.get();
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws DatabaseException {
        ChemicalDatabaseFuture future = new ChemicalDatabaseFuture<>(executor, "annotateCompounds", new Class<?>[]{List.class}, new Object[]{sublist});
        future.get();
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws DatabaseException {
        ChemicalDatabaseFuture<List<InChI>> future = new ChemicalDatabaseFuture<>(executor, "findInchiByNames", new Class<?>[]{List.class}, new Object[]{names});
        return future.get();
    }


    @Override
    public void close() {
        executor.close();
    }


    class Executor implements Runnable, Closeable{
        private Set<ChemicalDatabaseFuture> finished;
        private Set<ChemicalDatabaseFuture> runningFutures;

        private boolean running;
        private Queue<ChemicalDatabaseFuture> queue;

        private Executor(){
            finished = Collections.synchronizedSet(new HashSet<ChemicalDatabaseFuture>());
            runningFutures = Collections.synchronizedSet(new HashSet<ChemicalDatabaseFuture>());
            queue = new LinkedList<>();
        }

        /**
         * don't execute if your Task of interest is not still running!!!
         * @return
         * @throws InterruptedException
         */
        public void waitForExit(ChemicalDatabaseFuture future) {
            if (finished.contains(future)){
                finished.remove(future);
                return;
            }
            synchronized (this) {
                while (true){
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (finished.contains(future)){
                        finished.remove(future);
                        return;
                    }
                }
            }
        }

        public void addToQueue(ChemicalDatabaseFuture future) throws DatabaseException {
            synchronized (this){
                if (!running) throw new DatabaseException("cannot add to queue. Executor is already stopped");
                queue.add(future);
                notifyAll();
            }
        }

        @Override
        public void close() {
            running = false;
            synchronized (this){
                notifyAll();
            }
        }

        @Override
        public void run() {
            running = true;
            while (running){
                synchronized (this){
                    try {
                        while (!queue.isEmpty()){
                            for (int i = 0; i < isRunning.length; i++) {
                                synchronized (isRunning[i]){
                                    if (!isRunning[i].get()){
                                        ChemicalDatabaseFuture future = queue.poll();
                                        if (future==null) continue;
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
            synchronized (this){
                while (true){
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
        private DatabaseException exception;

        private String name;
        private Class<?>[] paramTypes;
        private Object[] paramValues;
        private AbstractChemicalDatabaseSynchronousExecutor.Executor executor;

        private AbstractChemicalDatabase db;

        ChemicalDatabaseFuture(AbstractChemicalDatabaseSynchronousExecutor.Executor executor, String name, Class<?>[] paramTypes, Object[] paramValues) throws DatabaseException {
            this.name = name;
            this.paramValues = paramValues;
            this.paramTypes = paramTypes;
            this.executor = executor;
            executor.addToQueue(this);
        }


        void setDB(AbstractChemicalDatabase db){
            this.db = db;
        }


        public R get() throws DatabaseException {
            if (result!=null) return result;
            executor.waitForExit(this);
            if (exception!=null) throw exception;
            return result;
        }

        @Override
        public void run() {
            try {
                result = (R)db.getClass().getDeclaredMethod(name, paramTypes).invoke(db, paramValues);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                this.exception = new DatabaseException(e.getCause());
            } catch (InvocationTargetException e) {
                e.getCause().printStackTrace();
                this.exception = new DatabaseException(e.getCause());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                this.exception = new DatabaseException(e.getCause());
            } catch (Exception e) {
                e.printStackTrace();
                this.exception = new DatabaseException(e.getCause());
            }
            executor.finished(this, db);
        }
    }

}
