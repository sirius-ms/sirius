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

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ConfidenceScore.PredictionException;
import de.unijena.bioinf.ConfidenceScore.QueryPredictor;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.DatabaseException;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.RESTDatabase;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.sirius.gui.compute.JobLog;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import gnu.trove.list.array.TIntArrayList;

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

    protected int[] fingerprintIndizes;
    protected double[] fscores;
    protected PredictionPerformance[] performances;
    protected MaskedFingerprintVersion fingerprintVersion;
    protected HashMap<String, Compound> compounds;
    protected ConcurrentHashMap<MolecularFormula, List<Compound>> compoundsPerFormula;
    protected RESTDatabase restDatabase;
    protected boolean configured = false;
    protected File directory;
    protected Callback callback;

    protected QueryPredictor confidenceScorePredictor;

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
        this.restDatabase = WebAPI.DEBUG ? new RESTDatabase(directory, BioFilter.ALL, "http://localhost:8080/frontend") :  new RESTDatabase(directory, BioFilter.ALL);

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

    public MaskedFingerprintVersion getFingerprintVersion() {
        return fingerprintVersion;
    }

    public double[] getFScores() {
        return fscores;
    }

    private void loadStatistics(WebAPI webAPI) throws IOException {
        final TIntArrayList list = new TIntArrayList(4096);
        this.performances = webAPI.getStatistics(list);
        this.confidenceScorePredictor = webAPI.getConfidenceScore();

        final MaskedFingerprintVersion.Builder v = MaskedFingerprintVersion.buildMaskFor(CdkFingerprintVersion.getDefault());
        v.disableAll();

        this.fingerprintIndizes = list.toArray();

        for (int index : fingerprintIndizes) {
            v.enable(index);
        }

        this.fingerprintVersion = v.toMask();

        fscores = new double[fingerprintIndizes.length];
        for (int k=0; k < performances.length; ++k)
            fscores[k] = performances[k].getF();
    }

    private FingerIdData blast(SiriusResultElement elem, ProbabilityFingerprint plattScores) {
        final MolecularFormula formula = elem.getMolecularFormula();
        final Fingerblast blaster = new Fingerblast(restDatabase);
        restDatabase.setBioFilter(isEnforceBio() ? BioFilter.ONLY_BIO : BioFilter.ALL);
        blaster.setScoring(new CSIFingerIdScoring(performances));
        try {
            List<Scored<FingerprintCandidate>> candidates = blaster.search(formula, plattScores);
            final double[] scores = new double[candidates.size()];
            final Compound[] comps = new Compound[candidates.size()];
            int k=0;
            for (Scored<FingerprintCandidate> candidate : candidates) {
                scores[k] = candidate.getScore();
                comps[k] = this.compounds.get(candidate.getCandidate().getInchiKey2D());
                if (comps[k]==null) comps[k] = new Compound(candidate.getCandidate());
                ++k;
            }
            return new FingerIdData(comps, scores, plattScores);
        } catch (DatabaseException e) {
            throw new RuntimeException(e); // TODO: handle
        }
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
        final File dir = new File(directory, bio ? "bio" : "not-bio");
        if (!dir.exists()) dir.mkdirs();
        final File mfile = new File(dir, formula.toString() + ".json.gz");
        final List<Compound> compounds;
        if (mfile.exists()) {
            try (final JsonParser parser = Json.createParser(new GZIPInputStream(new FileInputStream(mfile)))) {
                compounds = new ArrayList<>();
                Compound.parseCompounds(fingerprintVersion, compounds, parser);
            }
        } else {
            if (webAPI==null) {
                try (final WebAPI webAPI2 = new WebAPI()) {
                    compounds = webAPI2.getCompoundsFor(formula, mfile, fingerprintVersion, bio);
                }
            } else {
                compounds = webAPI.getCompoundsFor(formula, mfile, fingerprintVersion, bio);
            }
        }
        for (Compound c : compounds) {
            this.compounds.put(c.inchi.key2D(), c);
        }
        compoundsPerFormula.put(formula, compounds);
        for (Compound c : compounds) c.calculateXlogP();
        return compounds;
    }

    public Compound getCompound(String inchiKey2D) {
        return compounds.get(inchiKey2D); // TODO: probably we have to implement a cache here
    }

    public void setDirectory(File directory) {
        this.directory = directory;
        this.compounds = new HashMap<>();
    }

    protected static List<SiriusResultElement> getTopSiriusCandidates(ExperimentContainer container) {
        final ArrayList<SiriusResultElement> elements = new ArrayList<>();
        if (container==null || !container.isComputed() || container.getResults()==null) return elements;
        final SiriusResultElement top = container.getResults().get(0);
        elements.add(top);
        final double threshold = Math.max(top.getScore(),0) -  Math.max(5, top.getScore()*0.25);
        for (int k=1; k < container.getResults().size(); ++k) {
            SiriusResultElement e = container.getResults().get(k);
            if (e.getScore() < threshold) break;
            elements.add(e);
        }
        return elements;
    }

    public void computeAll(Enumeration<ExperimentContainer> compounds) {
        final ArrayList<FingerIdTask> tasks = new ArrayList<>();
        while (compounds.hasMoreElements()) {
            final ExperimentContainer c = compounds.nextElement();
            for (SiriusResultElement e : getTopSiriusCandidates(c)) {
                tasks.add(new FingerIdTask(c, e));
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
        // second: push job to cluster
        final double[] prediction;
        FingerIdJob job = null;
        if (jobWorker.jobs.containsKey(task)) {
            job = jobWorker.jobs.get(task);
        }
        try {
            // first read statistics
            globalLock.lock();
            try {
                if (performances==null || this.fingerprintVersion==null) loadStatistics(webAPI);
                globalCondition.signalAll();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                globalLock.unlock();
            }
            if (job == null) {
                job = webAPI.submitJob(SiriusDataConverter.experimentContainerToSiriusExperiment(task.experiment), resultElement.getRawTree(), this.fingerprintVersion);
            }
            final List<Compound> compounds;

            if (compoundsPerFormula.containsKey(resultElement.getMolecularFormula()))
                compounds = compoundsPerFormula.get(resultElement.getMolecularFormula());
            else
                compounds = loadCompoundsForGivenMolecularFormula(null, resultElement.getMolecularFormula());

            ProbabilityFingerprint platts=null;
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

    protected void refreshConfidence(ExperimentContainer exp) {
        if (exp.getResults()==null || exp.getResults().isEmpty()) return;
        SiriusResultElement best = null;
        for (SiriusResultElement elem : exp.getResults()) {
            if (elem!= null && elem.getFingerIdData()!=null) {
                if (best==null) best = elem;
                else if (elem.getFingerIdData().scores.length>0 && elem.getFingerIdData().topScore > best.getFingerIdData().topScore) {
                    best = elem;
                }
            }
        }

        if (best!=null) {
            if (Double.isNaN(best.getFingerIdData().confidence)) recomputeConfidence(best);
        }
        exp.setBestHit(best);
    }

    private void recomputeConfidence(SiriusResultElement best) {
        try {
            globalLock.lock();
            if (performances==null) {
                final WebAPI webAPI = new WebAPI();
                loadStatistics(webAPI);
                webAPI.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
            globalLock.unlock();
        }

        if (confidenceScorePredictor!=null) {
            final FingerIdData data = best.getFingerIdData();
            if (data.compounds.length==0) return;
            CompoundWithAbstractFP<ProbabilityFingerprint> query = data.compounds[0].asQuery(data.platts);
            CompoundWithAbstractFP<Fingerprint>[] candidates = new CompoundWithAbstractFP[data.compounds.length];
            for (int k=0; k < candidates.length; ++k) {
                candidates[k] = data.compounds[k].asCandidate();
            }
            try {
                data.confidence = confidenceScorePredictor.estimateProbability(query, candidates);
            } catch (PredictionException e) {
                data.confidence = Double.NaN;
                e.printStackTrace();
            }
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
                if (performances==null) loadStatistics(webAPI);
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
                    final MolecularFormula formula = container.result.getMolecularFormula();
                    final JobLog.Job job = JobLog.getInstance().submit(container.experiment.getGUIName(), "Download " + formula.toString());
                    try {
                        loadCompoundsForGivenMolecularFormula(webAPI, container.result.getMolecularFormula());
                        synchronized (blastWorker) {blastWorker.notifyAll();}

                        job.done();
                    } catch (IOException e) {
                        job.error(e.getMessage(), e);
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
                        container.job = JobLog.getInstance().submit(container.experiment.getGUIName(), "Predict fingerprint");
                        try {
                            final FingerIdJob job = webAPI.submitJob(SiriusDataConverter.experimentContainerToSiriusExperiment(container.experiment), container.result.getRawTree(), fingerprintVersion);
                            jobs.put(container, job);
                        } catch (IOException e) {
                            jobQueue.add(container);
                            container.job.error(e.getMessage(), e);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                            container.job.error(e.getMessage(), e);
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
                            entry.getKey().job.done();
                            synchronized (blastWorker) {
                                blastWorker.notifyAll();
                            }
                        } else if (entry.getValue().state=="CRASHED"){
                            iter.remove();
                            entry.getKey().job.error("Error on server side", null);
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
                    // blast this compound
                    container.job = JobLog.getInstance().submit(container.experiment.getGUIName(), "Search in structure database");
                    final FingerIdData data = blast(container.result, container.prediction);
                    final ExperimentContainer experiment = container.experiment;
                    final SiriusResultElement resultElement = container.result;
                    resultElement.setFingerIdData(data);
                    resultElement.fingerIdComputeState = ComputingStatus.COMPUTED;
                    refreshConfidence(experiment);
                    callback.computationFinished(experiment, resultElement);
                    container.job.done();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        }
    }
}
