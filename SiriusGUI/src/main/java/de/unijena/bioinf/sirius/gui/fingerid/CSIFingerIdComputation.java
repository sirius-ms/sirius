/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.fingerid.*;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import javax.json.Json;
import javax.json.stream.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;

/**
 * keeps all compounds in memory
 */
public class CSIFingerIdComputation {

    public interface Callback {
        public void computationFinished(ExperimentContainer container, SiriusResultElement element);
    }

    protected FingerprintStatistics statistics;
    protected int[] fingerprintIndizes;
    protected TIntObjectHashMap<String> absoluteIndex2Smarts;
    protected String[] relativeIndex2Smarts;
    protected String[] relativeIndex2Comments;
    protected HashMap<String, Compound> compounds;
    protected ConcurrentHashMap<MolecularFormula, List<Compound>> compoundsPerFormula;
    protected boolean configured = false;
    protected MarvinsScoring scoring = new MarvinsScoring();
    protected File directory;
    protected Callback callback;

    protected boolean enforceBio;

    protected final Thread blastThread, formulaThread, jobThread;
    protected final BackgroundThreadBlast blastWorker;
    protected final BackgroundThreadFormulas formulaWorker;
    protected final BackgroundThreadJobs jobWorker;

    protected final ReentrantLock globalLock;
    protected final Condition globalCondition;

    private final ConcurrentLinkedQueue<FingerIdTask> formulaQueue, jobQueue, blastQueue;

    public CSIFingerIdComputation(Callback callback) {
        setDirectory(getDefaultDirectory());
        globalLock = new ReentrantLock();
        globalCondition = globalLock.newCondition();
        this.compounds = new HashMap<>(32768);
        this.compoundsPerFormula = new ConcurrentHashMap<>(128);
        absoluteIndex2Smarts=new TIntObjectHashMap<>(4096);
        this.formulaQueue = new ConcurrentLinkedQueue<>();
        this.blastQueue = new ConcurrentLinkedQueue<>();
        this.jobQueue = new ConcurrentLinkedQueue<>();
        this.callback = callback;
        this.blastWorker = new BackgroundThreadBlast();
        this.blastThread = new Thread(blastWorker);
        blastThread.start();

        this.formulaWorker = new BackgroundThreadFormulas();
        this.formulaThread = new Thread(formulaWorker);
        formulaThread.start();

        this.jobWorker = new BackgroundThreadJobs();
        this.jobThread = new Thread(jobWorker);
        jobThread.start();
    }

    public double[] getFScores() {
        return statistics.f;
    }

    private void loadStatistics(WebAPI webAPI) throws IOException {
        final TIntArrayList list = new TIntArrayList(4096);
        ArrayList<String> cs = new ArrayList<>(4096);
        ArrayList<String> sm = new ArrayList<>(4096);
        this.statistics = webAPI.getStatistics(list, sm, cs);
        this.relativeIndex2Comments = cs.toArray(new String[cs.size()]);
        this.relativeIndex2Smarts = sm.toArray(new String[sm.size()]);
        this.fingerprintIndizes = list.toArray();
        for (int i=0; i < fingerprintIndizes.length; ++i) {
            absoluteIndex2Smarts.put(fingerprintIndizes[i], sm.get(i));
        }
    }

    private FingerIdData blast(SiriusResultElement elem, double[] plattScores) {
        final MolecularFormula formula = elem.getMolecularFormula();
        final List<Compound> compounds = compoundsPerFormula.get(formula);
        final Scorer scorer = scoring.getScorer(statistics);
        final TreeMap<Double, Compound> map = new TreeMap<>();
        final Query query = new Query("_query_", null, plattScores);
        scorer.preprocessQuery(query, statistics);
        for (Compound c : compounds) {
            map.put(scorer.score(query, new Candidate(c.inchi.in2D, c.fingerprint), statistics), c);
        }
        final double[] scores = new double[map.size()];
        final Compound[] comps = new Compound[map.size()];
        int k=0;
        for (Map.Entry<Double, Compound> entry : map.descendingMap().entrySet()) {
            scores[k] = entry.getKey();
            comps[k] = entry.getValue();
            ++k;
        }
        return new FingerIdData(comps, scores, plattScores);
    }


    public File getDirectory() {
        return directory;
    }

    public File getDefaultDirectory() {
        final String val = System.getenv("CSI_FINGERID_STORAGE");
        if (val!=null) return new File(val);
        return new File(System.getProperty("user.home"), "csi_fingerid_cache");
    }

    public boolean isEnforceBio() {
        return enforceBio;
    }

    public void setEnforceBio(boolean enforceBio) {
        this.enforceBio = enforceBio;
    }

    private List<MolecularFormula> getFormulasForDifferentIonizationVariants(MolecularFormula formula, PrecursorIonType... variants) {
        final ArrayList<MolecularFormula> formulas = new ArrayList<>();
        for (PrecursorIonType ionType : variants) {
            final MolecularFormula neutralFormula = ionType.precursorIonToNeutralMolecule(formula);
            if (!neutralFormula.isAllPositiveOrZero()) continue;
            formulas.add(neutralFormula);
        }
        return formulas;
    }

    private List<Compound> loadCompoundsForGivenMolecularFormula(WebAPI webAPI, MolecularFormula formula) throws IOException {
        if (compoundsPerFormula.containsKey(formula)) return compoundsPerFormula.get(formula);
        final List<Compound> compounds;
        try {
            globalLock.lock();
            compounds = loadCompoundsForGivenMolecularFormula(webAPI, formula, true);
            if (!enforceBio) compounds.addAll(loadCompoundsForGivenMolecularFormula(webAPI, formula, false));
        } finally {
            globalLock.unlock();
        }
        return compounds;
    }

    private List<Compound> loadCompoundsForGivenMolecularFormula(WebAPI webAPI, MolecularFormula formula, boolean bio) throws IOException {
        final File dir = new File(directory, "biodb");
        if (!dir.exists()) dir.mkdirs();
        final File mfile = new File(dir, formula.toString() + ".json.gz");
        final List<Compound> compounds;
        if (mfile.exists()) {
            try (final JsonParser parser = Json.createParser(new GZIPInputStream(new FileInputStream(mfile)))) {
                compounds = new ArrayList<>();
                Compound.parseCompounds(fingerprintIndizes, compounds, parser);
            }
        } else {
            if (webAPI==null) {
                try (final WebAPI webAPI2 = new WebAPI()) {
                    compounds = webAPI2.getCompoundsFor(formula, mfile, fingerprintIndizes, bio);
                }
            } else {
                compounds = webAPI.getCompoundsFor(formula, mfile, fingerprintIndizes, bio);
            }
        }
        for (Compound c : compounds) {
            this.compounds.put(c.inchi.key2D(), c);
        }
        compoundsPerFormula.put(formula, compounds);
        return compounds;
    }

    public Compound getCompound(String inchiKey2D) {
        return compounds.get(inchiKey2D); // TODO: probably we have to implement a cache here
    }

    public void setDirectory(File directory) {
        this.directory = directory;
        this.compounds = new HashMap<>();
    }

    public void computeAll(Enumeration<ExperimentContainer> compounds) {
        final ArrayList<FingerIdTask> tasks = new ArrayList<>();
        while (compounds.hasMoreElements()) {
            final ExperimentContainer c = compounds.nextElement();
            if (c != null && c.isComputed() && c.getResults()!=null && c.getResults().size()>0) {
                tasks.add(new FingerIdTask(c, c.getResults().get(0)));
            }
        }
        computeAll(tasks);
    }

    public void computeAll(Collection<FingerIdTask> compounds) {
        for (FingerIdTask task : compounds) {
            final ComputingStatus status = task.result.fingerIdComputeState;
            if (status == ComputingStatus.UNCOMPUTED || status == ComputingStatus.FAILED) {
                task.result.fingerIdComputeState = ComputingStatus.COMPUTING;
                formulaQueue.add(task);
                jobQueue.add(task);
            }
        }
        synchronized (formulaWorker) {formulaWorker.notifyAll();};
        synchronized (jobWorker) {jobWorker.notifyAll();};
    }

    public void shutdown() {
        formulaWorker.shutdown=true;
        blastWorker.shutdown=true;
        jobWorker.shutdown=true;
        formulaQueue.clear(); blastQueue.clear(); jobQueue.clear();
        synchronized (formulaWorker) {
            formulaWorker.notifyAll();
        }
        synchronized (blastWorker) {
            blastWorker.notifyAll();
        }
        synchronized (jobWorker) {
            jobWorker.notifyAll();
        }
        try {
            if (globalLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                globalCondition.signalAll();
                globalLock.unlock();
            }
        } catch (InterruptedException e) {
            // just give up
        }
    }

    public boolean synchronousPredictionTask(ExperimentContainer container, SiriusResultElement resultElement) throws IOException {
        // first: delete this job from all queues
        final FingerIdTask task = new FingerIdTask(container, resultElement);
        formulaQueue.remove(task);
        jobQueue.remove(task);
        blastQueue.remove(task);
        final WebAPI webAPI = new WebAPI();
        if (statistics==null) {
            globalLock.lock();
            if (statistics==null) {
                loadStatistics(webAPI);
                globalCondition.signalAll();
            }
            globalLock.unlock();
        }
        // second: push job to cluster
        final double[] prediction;
        FingerIdJob job = null;
        if (jobWorker.jobs.containsKey(task)) {
            job = jobWorker.jobs.get(task);
        }
        try {
            if (job == null) {
                job = webAPI.submitJob(SiriusDataConverter.experimentContainerToSiriusExperiment(task.experiment), resultElement.getRawTree());
            }
            final List<Compound> compounds;

            if (compoundsPerFormula.containsKey(resultElement.getMolecularFormula()))
                compounds = compoundsPerFormula.get(resultElement.getMolecularFormula());
            else
                compounds = loadCompoundsForGivenMolecularFormula(null, resultElement.getMolecularFormula());

            double[] platts=null;
            for (int k=0; k < 60; ++k) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (webAPI.updateJobStatus(job)) {
                    platts=job.prediction;
                    break;
                }
            }
            if (platts==null) {
                return false;
            }
            final FingerIdData data = blast(resultElement, platts);
            resultElement.setFingerIdData(data); // todo: etwas kritisch... dürfte aber keine Probleme geben... oder?
            return true;
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    protected class BackgroundThreadFormulas implements Runnable {

        protected volatile boolean shutdown;

        @Override
        public void run() {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            final WebAPI webAPI = new WebAPI();
            // first read statistics
            globalLock.lock();
            try {
                if (statistics==null) loadStatistics(webAPI);
                globalCondition.signalAll();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                globalLock.unlock();
            }
            while ((!shutdown)) {
                final FingerIdTask container = formulaQueue.poll();
                if (container==null) {
                    try {
                        synchronized (this) {
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    // download molecular formulas
                    try {
                        final MolecularFormula formula = container.result.getMolecularFormula();
                        loadCompoundsForGivenMolecularFormula(webAPI, container.result.getMolecularFormula());
                        synchronized (blastWorker) {blastWorker.notifyAll();}
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected class BackgroundThreadJobs implements Runnable {

        protected volatile boolean shutdown;

        protected final ConcurrentHashMap<FingerIdTask, FingerIdJob> jobs;

        public BackgroundThreadJobs() {
            this.jobs = new ConcurrentHashMap<>();
        }

        @Override
        public void run() {
            // wait until statistics are loaded
            globalLock.lock();
            try {
                globalCondition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                globalLock.unlock();
            }
            WebAPI webAPI = new WebAPI();
            while ((!shutdown)) {
                boolean nothingToDo = true;
                for(int c=0; c < 20; ++c) {
                    final FingerIdTask container = jobQueue.poll();
                    if (container!=null) {
                        nothingToDo=false;
                        try {
                            final FingerIdJob job = webAPI.submitJob(SiriusDataConverter.experimentContainerToSiriusExperiment(container.experiment), container.result.getRawTree());
                            jobs.put(container, job);
                        } catch (IOException e) {
                            jobQueue.add(container);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                final Iterator<Map.Entry<FingerIdTask, FingerIdJob>> iter = jobs.entrySet().iterator();
                while (iter.hasNext()) {
                    nothingToDo=false;
                    final Map.Entry<FingerIdTask, FingerIdJob> entry = iter.next();
                    try {
                        if (webAPI.updateJobStatus(entry.getValue())) {
                            entry.getKey().prediction = entry.getValue().prediction;
                            iter.remove();
                            blastQueue.add(entry.getKey());
                            synchronized (blastWorker) {
                                blastWorker.notifyAll();
                            }
                        } else if (entry.getValue().state=="CRASHED"){
                            iter.remove();
                        }
                    } catch (URISyntaxException e) {
                        iter.remove();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (nothingToDo) {
                    try {
                        synchronized (this) {wait();};
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    protected class BackgroundThreadBlast implements Runnable {

        protected volatile boolean shutdown;

        @Override
        public void run() {
            // wait until statistics are loaded
            globalLock.lock();
            try {
                globalCondition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                globalLock.unlock();
            }
            while ((!shutdown)) {
                final FingerIdTask container = blastQueue.poll();
                try {
                    if (container==null) {
                        synchronized (this) {this.wait();}
                        continue;
                    } else if (!compoundsPerFormula.containsKey(container.result.getMolecularFormula())) {
                        final boolean queueIsEmpty = blastQueue.isEmpty();
                        blastQueue.add(container);
                        if (queueIsEmpty) {
                            synchronized (this) {
                                this.wait();
                            }
                        }
                        continue;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // blast this compound
                final FingerIdData data = blast(container.result, container.prediction);
                final ExperimentContainer experiment = container.experiment;
                final SiriusResultElement resultElement = container.result;
                resultElement.setFingerIdData(data);
                resultElement.fingerIdComputeState = ComputingStatus.COMPUTED;
                callback.computationFinished(experiment, resultElement);

            }
        }
    }
}
