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
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ConfidenceScore.PredictionException;
import de.unijena.bioinf.ConfidenceScore.QueryPredictor;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.fingerprints.ECFPFingerprinter;
import de.unijena.bioinf.sirius.gui.compute.JobLog;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.stream.JsonParser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    private Logger logger = LoggerFactory.getLogger(CSIFingerIdComputation.class);

    private VersionsInfo versionNumber;

    public void setVersionNumber(VersionsInfo versionNumber) {
        this.versionNumber = versionNumber;
    }

    public VersionsInfo getVersionNumber() {
        return versionNumber;
    }

    public interface Callback {
        void computationFinished(ExperimentContainer container, SiriusResultElement element);
    }

    protected int[] fingerprintIndizes;
    protected double[] fscores;
    protected PredictionPerformance[] performances;
    protected MaskedFingerprintVersion fingerprintVersion;
    protected final HashMap<String, Compound> compounds;
    protected final HashMap<MolecularFormula, List<Compound>> compoundsPerFormulaBio, compoundsPerFormulaNonBio;
    protected RESTDatabase restDatabase;
    protected boolean configured = false;
    private File directory;
    private boolean enabled;
    protected List<Runnable> enabledListeners=new ArrayList<>();

    protected QueryPredictor pubchemConfidenceScorePredictor, bioConfidenceScorePredictor;

    protected boolean enforceBio, checkedCache;

    protected final Thread formulaThread, jobThread;
    protected final Thread[] blastThreads;
    protected final BackgroundThreadBlast blastWorker;
    protected final BackgroundThreadFormulas formulaWorker;
    protected final BackgroundThreadJobs jobWorker;


    protected final ReentrantLock globalLock;
    protected final Condition globalCondition;

    private final ConcurrentLinkedQueue<FingerIdTask> formulaQueue, jobQueue, blastQueue;

    public CSIFingerIdComputation() {
        globalLock = new ReentrantLock();
        globalCondition = globalLock.newCondition();
        this.compounds = new HashMap<>(32768);
        this.compoundsPerFormulaBio = new HashMap<>(128);
        this.compoundsPerFormulaNonBio = new HashMap<>(128);
        setDirectory(getDefaultDirectory());
        this.restDatabase = WebAPI.DEBUG ? new RESTDatabase(directory, BioFilter.ALL, "http://localhost:8080/frontend") :  new RESTDatabase(directory, BioFilter.ALL);

        this.formulaQueue = new ConcurrentLinkedQueue<>();
        this.blastQueue = new ConcurrentLinkedQueue<>();
        this.jobQueue = new ConcurrentLinkedQueue<>();

        final int numberOfBlastThreads = Runtime.getRuntime().availableProcessors()-1;

        this.blastWorker = new BackgroundThreadBlast();
        this.blastThreads = new Thread[numberOfBlastThreads];
        for (int k=0; k < numberOfBlastThreads; ++k) {
            blastThreads[k] = new Thread(blastWorker);
            blastThreads[k].start();

        }
        this.enforceBio = true;

        this.formulaWorker = new BackgroundThreadFormulas();
        this.formulaThread = new Thread(formulaWorker);
        formulaThread.start();

        this.jobWorker = new BackgroundThreadJobs();
        this.jobThread = new Thread(jobWorker);
        jobThread.start();

    }

    public List<Runnable> getEnabledListeners() {
        return enabledListeners;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        for (Runnable r : enabledListeners) r.run();
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
        this.pubchemConfidenceScorePredictor = webAPI.getConfidenceScore(false);
        this.bioConfidenceScorePredictor = webAPI.getConfidenceScore(true);

        final CdkFingerprintVersion version = (CdkFingerprintVersion) webAPI.getFingerprintVersion();

        final MaskedFingerprintVersion.Builder v = MaskedFingerprintVersion.buildMaskFor(version);
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

    private FingerIdData blast(SiriusResultElement elem, ProbabilityFingerprint plattScores, boolean bio) {
        final MolecularFormula formula = elem.getMolecularFormula();
        final Fingerblast blaster = new Fingerblast(new InMemoryCacheDatabase(bio));
        blaster.setScoring(new CSIFingerIdScoring(performances));
        try {
            List<Scored<FingerprintCandidate>> candidates = blaster.search(formula, plattScores);
            final double[] scores = new double[candidates.size()];
            final Compound[] comps = new Compound[candidates.size()];
            int k=0;
            final HashMap<String, Compound> compounds;
            synchronized(this.compounds) {
                compounds = this.compounds;
            }
            for (Scored<FingerprintCandidate> candidate : candidates) {
                scores[k] = candidate.getScore();
                comps[k] = compounds.get(candidate.getCandidate().getInchiKey2D());
                if (comps[k]==null) {
                    comps[k] = new Compound(candidate.getCandidate());
                }
                ++k;
            }
            return new FingerIdData(bio, comps, scores, plattScores);
        } catch (DatabaseException e) {
            throw new RuntimeException(e); // TODO: handle
        }
    }


    public File getDirectory() {
        return directory;
    }

    public File getDefaultDirectory() {
        final String val = System.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        return Paths.get(val).toFile();
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

    private List<Compound> loadCompoundsForGivenMolecularFormula(WebAPI webAPI, MolecularFormula formula, boolean bioFlag) throws IOException {
        final List<Compound> compounds = new ArrayList<>();
        try {
            globalLock.lock();
            destroyCacheIfNecessary();
            compounds.addAll(internalLoadCompoundsForGivenMolecularFormula(webAPI, formula, true));
            if (!bioFlag) compounds.addAll(internalLoadCompoundsForGivenMolecularFormula(webAPI, formula, false));
        } finally {
            globalLock.unlock();
        }
        return compounds;
    }

    private boolean cacheHasToBeDestroyed() {
        checkedCache = true;
        final File f = new File(directory, "version");
        if (f.exists()) {
            try {
                final List<String> content = Files.readAllLines(f.toPath(), Charset.forName("UTF-8"));
                if (content.size()>0 && !versionNumber.databaseOutdated(content.get(0)) ) return false;
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
            }
        }
        return true;
    }

    private void destroyCacheIfNecessary() {
        if (checkedCache || !cacheHasToBeDestroyed()) return;
        try {
            destroyCache();
        } catch (IOException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
            // might happen, especially under Windows. But I don't wanna make a proper error dialogue for that
        }
    }

    public void destroyCache() throws IOException {
        final File bio = new File(directory, "bio");
        final File nonBio = new File(directory, "not-bio");
        if (bio.exists()) {
            for (File f : bio.listFiles()) {
                Files.deleteIfExists(f.toPath());
            }
        }
        if (nonBio.exists()) {
            for (File f : nonBio.listFiles()) {
                Files.deleteIfExists(f.toPath());
            }
        }
        if (directory.exists()) {
            for (File f : directory.listFiles()) {
                Files.deleteIfExists(f.toPath());
            }
        } else {
            directory.mkdirs();
            bio.mkdirs();
            nonBio.mkdirs();
        }
        try (BufferedWriter bw = Files.newBufferedWriter(new File(directory, "version").toPath(), Charset.forName("UTF-8"))) {
            bw.write(versionNumber.databaseDate);
        }
        compounds.clear();
        compoundsPerFormulaBio.clear();
        compoundsPerFormulaNonBio.clear();
    }

    private List<Compound> internalLoadCompoundsForGivenMolecularFormula(WebAPI webAPI, MolecularFormula formula, boolean bio) throws IOException {

        if (bio) {
            synchronized (compoundsPerFormulaBio) {
                if (compoundsPerFormulaBio.containsKey(formula)) return compoundsPerFormulaBio.get(formula);
            }
        } else {
            synchronized (compoundsPerFormulaNonBio) {
                if (compoundsPerFormulaNonBio.containsKey(formula)) return compoundsPerFormulaNonBio.get(formula);
            }
        }
        final File dir = new File(directory, bio ? "bio" : "not-bio");
        if (!dir.exists()) dir.mkdirs();
        final File mfile = new File(dir, formula.toString() + ".json.gz");
        List<Compound> compounds=null;
        if (mfile.exists()) {
            try (final JsonParser parser = Json.createParser(new GZIPInputStream(new FileInputStream(mfile)))) {
                compounds = new ArrayList<>();
                Compound.parseCompounds(fingerprintVersion, compounds, parser);
            } catch (IOException | JsonException e) {
                logger.error("Error while reading cached formula file for \"" + formula.toString() + "\". Reload file via webservice.", e);
            }
        }
        if (compounds==null) {
            if (webAPI==null) {
                try (final WebAPI webAPI2 = new WebAPI()) {
                    compounds = webAPI2.getCompoundsFor(formula, mfile, fingerprintVersion, bio);
                }
            } else {
                compounds = webAPI.getCompoundsFor(formula, mfile, fingerprintVersion, bio);
            }
        }
        ECFPFingerprinter fingerprinter = null;
        for (Compound c : compounds) {
            c.calculateXlogP();
            if (de.unijena.bioinf.sirius.gui.fingerid.CompoundCandidate.ECFP_ENABLED) {
                if (fingerprinter==null) {
                    fingerprinter = new ECFPFingerprinter();
                }
                // add ECFP fingerprint to compound
                // workaround
                c.fingerprint = recomputeECFP(fingerprinter, c.getMolecule(), c.fingerprint, fingerprintVersion);
            }
            synchronized (this.compounds) {
                this.compounds.put(c.inchi.key2D(), c);
            }
        }
        if (bio) {
            synchronized (compoundsPerFormulaBio) {
                compoundsPerFormulaBio.put(formula, compounds);
            }
        } else {
            synchronized (compoundsPerFormulaNonBio) {
                compoundsPerFormulaNonBio.put(formula, compounds);
            }
        }
        return compounds;
    }


    //////////////////////////////////////////////
    ///////////// WORKAROUND ////////////////////

    public static Fingerprint recomputeECFP(ECFPFingerprinter ecfp, IAtomContainer molecule, Fingerprint defaultCdkFingerprint, MaskedFingerprintVersion version) {
        {
            try {
                IBitFingerprint bits = ecfp.getBitFingerprint(molecule);
                final TShortArrayList fps = new TShortArrayList(defaultCdkFingerprint.toIndizesArray());
                final int offset = ((CdkFingerprintVersion)version.getMaskedFingerprintVersion()).getOffsetFor(CdkFingerprintVersion.USED_FINGERPRINTS.ECFP);

                final int[] indizes = version.allowedIndizes();
                int start = Arrays.binarySearch(indizes, offset);
                if (start<0) {
                    start = -(start+1);
                }
                for (; start < indizes.length; ++start) {
                    if (bits.get(indizes[start]-offset)) {
                        fps.add((short)indizes[start]);
                    }
                }
                return new ArrayFingerprint(version, fps.toArray());
            } catch (CDKException e) {
                throw new RuntimeException(e);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////

    public void setDirectory(File directory) {
        this.directory = directory;
        synchronized (this.compounds){
            this.compounds.clear();
        }
        synchronized (this.compoundsPerFormulaNonBio) {
            this.compoundsPerFormulaNonBio.clear();
        }
        synchronized (this.compoundsPerFormulaBio) {
            this.compoundsPerFormulaBio.clear();
        }
        checkedCache = false;
    }

    protected static List<SiriusResultElement> getTopSiriusCandidates(ExperimentContainer container) {
        final ArrayList<SiriusResultElement> elements = new ArrayList<>();
        if (container==null || !container.isComputed() || container.getResults()==null) return elements;
        final SiriusResultElement top = container.getResults().get(0);
        if (top.getResult().getResolvedTree().numberOfEdges()>0)
            elements.add(top);
        final double threshold = calculateThreshold(top.getScore());
        for (int k=1; k < container.getResults().size(); ++k) {
            SiriusResultElement e = container.getResults().get(k);
            if (e.getScore() < threshold) break;
            if (e.getResult().getResolvedTree().numberOfEdges()>0)
                elements.add(e);
        }
        return elements;
    }

    public static double calculateThreshold(double topScore){
        return Math.max(topScore, 0) -  Math.max(5, topScore * 0.25);
    }

    //compute for a single experiment
    public void compute(ExperimentContainer c, boolean bioDb) {
        final ArrayList<FingerIdTask> tasks = new ArrayList<>();
        for (SiriusResultElement e : getTopSiriusCandidates(c)) {
            if (e.getCharge()>0) {
                tasks.add(new FingerIdTask(bioDb, c, e));
            }
        }
        computeAll(tasks);
    }

    //csi fingerid compute all button in main panel
    public void computeAll(List<ExperimentContainer> compounds) {
        stopRunningTasks();
        final ArrayList<FingerIdTask> tasks = new ArrayList<>();
        for (ExperimentContainer c : compounds) {
            for (SiriusResultElement e : getTopSiriusCandidates(c)) {
                if (e.getCharge()>0) {
                    tasks.add(new FingerIdTask(isEnforceBio(), c, e));
                }
            }
        }
        computeAll(tasks);
    }

    private void stopRunningTasks() {
        formulaQueue.clear();
        blastQueue.clear();
        jobQueue.clear();
    }

    public void stopTask(FingerIdTask task) {
        formulaQueue.remove(task);
        blastQueue.remove(task);
        jobQueue.remove(task);
        task.job.error("Canceled", null);
    }

    // this is really the conputation mehtod everything should use in die end
    public void computeAll(Collection<FingerIdTask> compounds) {
        for (FingerIdTask task : compounds) {
            final ComputingStatus status = task.result.getFingerIdComputeState();
            boolean recompute = (task.result.getFingerIdData()!=null && task.result.getFingerIdData().bio != task.bio);
            if (recompute || status == ComputingStatus.UNCOMPUTED || status == ComputingStatus.FAILED) {
                task.result.setFingerIdComputeState(ComputingStatus.COMPUTING);
                if (task.result.getFingerIdData()!=null && task.result.getFingerIdData().platts!=null) {
                    task.prediction = task.result.getFingerIdData().platts;
                    formulaQueue.add(task);
                    blastQueue.add(task);
                } else {
                    formulaQueue.add(task);
                    jobQueue.add(task);
                }
            }
        }
        synchronized (formulaWorker) {formulaWorker.notifyAll();}
        synchronized (jobWorker) {jobWorker.notifyAll();}
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

    protected void refreshConfidence(final ExperimentContainer exp) {
        if (exp.getResults()==null || exp.getResults().isEmpty()) return;
        SiriusResultElement best = null;
        for (SiriusResultElement elem : exp.getResults()) {
            if (elem!= null && elem.getFingerIdData()!=null) {
                if (best==null) best = elem;
                else if (elem.getFingerIdData().scores.length>0 && elem.getFingerIdData().getTopScore() > best.getFingerIdData().getTopScore()) {
                    best = elem;
                }
            }
        }

        if (best!=null) {
            if (Double.isNaN(best.getFingerIdData().getConfidence())) recomputeConfidence(best);
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
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
            return;
        } finally {
            globalLock.unlock();
        }

        if (bioConfidenceScorePredictor!=null) {
            final FingerIdData data = best.getFingerIdData();
            if (data.compounds.length==0) return;

            // bio or pubchem?
            boolean bio = true;
            for (Compound c : data.compounds) {
                if (c.bitset>0) {
                    bio = false;
                    break;
                }
            }
            final QueryPredictor queryPredictor = bio ? bioConfidenceScorePredictor : pubchemConfidenceScorePredictor;

            CompoundWithAbstractFP<ProbabilityFingerprint> query = data.compounds[0].asQuery(data.platts);
            CompoundWithAbstractFP<Fingerprint>[] candidates = new CompoundWithAbstractFP[data.compounds.length];
            for (int k=0; k < candidates.length; ++k) {
                candidates[k] = data.compounds[k].asCandidate();
            }
            try {
                data.setConfidence(queryPredictor.estimateProbability(query, candidates));
            } catch (PredictionException e) {
                data.setConfidence(Double.NaN);
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
            }
        }

    }

    protected class BackgroundThreadFormulas implements Runnable {

        protected volatile boolean shutdown;

        protected volatile boolean failedWhenLoadingStatistics=false;

        @Override
        public void run() {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                }
            }
            final WebAPI webAPI = new WebAPI();
            // first read statistics
            globalLock.lock();
            try {
                if (performances==null) loadStatistics(webAPI);
                globalCondition.signalAll();
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                this.failedWhenLoadingStatistics = true;
                return;
            } finally {
                globalLock.unlock();
            }
            while ((!shutdown)) {
                try {
                    final FingerIdTask container = formulaQueue.poll();
                    if (container==null) {
                        try {
                            synchronized (this) {
                                // add timeout to prevent deadlocks. Even if something really strange
                                // happens, the thread will resume every 3 seconds
                                this.wait(3000);
                            }
                        } catch (InterruptedException e) {
                            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                        }
                    } else {
                        // download molecular formulas
                        final MolecularFormula formula = container.result.getMolecularFormula();
                        final JobLog.Job job = JobLog.getInstance().submitRunning(container.experiment.getGUIName(), "Download " + formula.toString());
                        try {
                            loadCompoundsForGivenMolecularFormula(webAPI, container.result.getMolecularFormula(), container.bio);
                            synchronized (blastWorker) {blastWorker.notifyAll();}

                            job.done();
                        } catch (Throwable e) {
                            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                            job.error(e.getMessage(), e);
                        }
                    }
                } catch (RuntimeException e) {
                    LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                    Thread.yield();
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
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
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
                        container.job = JobLog.getInstance().submitRunning(container.experiment.getGUIName(), "Predict fingerprint");
                        try {
                            final FingerIdJob job = webAPI.submitJob(SiriusDataConverter.experimentContainerToSiriusExperiment(container.experiment), container.result.getResult().getResolvedTree(), fingerprintVersion);
                            jobs.put(container, job);
                        } catch (IOException e) {
                            jobQueue.add(container);
                            container.job.error(e.getMessage(), e);
                        } catch (URISyntaxException e) {
                            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                            container.job.error(e.getMessage(), e);
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
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
                        LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                    }
                }

                if (nothingToDo) {
                    try {
                        synchronized (this) {wait(3000);}
                    } catch (InterruptedException e) {
                        LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
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
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
            } finally {
                globalLock.unlock();
            }
            while ((!shutdown)) {
                final FingerIdTask container = blastQueue.poll();
                try {
                    if (container==null) {
                        synchronized (this) {this.wait(3000);}
                        continue;
                    } else {
                        boolean canRun=true;
                        if (!container.bio) {
                            synchronized (compoundsPerFormulaNonBio) {
                                if (!compoundsPerFormulaNonBio.containsKey(container.result.getMolecularFormula())) canRun = false;
                            }
                        }
                        synchronized (compoundsPerFormulaBio) {
                            if (!compoundsPerFormulaBio.containsKey(container.result.getMolecularFormula())) canRun = false;
                        }
                        if (!canRun) {
                            final boolean queueIsEmpty = blastQueue.isEmpty();
                            blastQueue.add(container);
                            if (queueIsEmpty) {
                                synchronized (this) {
                                    this.wait(3000);
                                }
                            }
                            continue;
                        }
                    }
                    // blast this compound
                    container.job = JobLog.getInstance().submitRunning(container.experiment.getGUIName(), "Search in structure database");
                    try {
                    final FingerIdData data = blast(container.result, container.prediction, container.bio);
                    final ExperimentContainer experiment = container.experiment;
                    final SiriusResultElement resultElement = container.result;
                    resultElement.setFingerIdData(data);
                    resultElement.setFingerIdComputeState(ComputingStatus.COMPUTED);
                    refreshConfidence(experiment);
                    container.job.done();
                    } catch (RuntimeException e) {
                        container.job.error(e.getMessage(), e);
                    }
                } catch (InterruptedException e) {
                    LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                }


            }
        }
    }

    /**
     * Quick'n dirty hack to keep the database in memory
     */
    protected class InMemoryCacheDatabase extends AbstractChemicalDatabase {

        protected boolean bio;

        public InMemoryCacheDatabase(boolean bio) {
            this.bio = bio;
        }

        @Override
        public List<FormulaCandidate> lookupMolecularFormulas(double v, Deviation deviation, PrecursorIonType precursorIonType) throws DatabaseException {
            return restDatabase.lookupMolecularFormulas(v, deviation, precursorIonType);
        }

        @Override
        public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula molecularFormula) throws DatabaseException {
            return restDatabase.lookupStructuresByFormula(molecularFormula);
        }

        @Override
        public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula molecularFormula, T fingerprintCandidates) throws DatabaseException {
            if (!bio) {
                for (Compound c : compoundsPerFormulaNonBio.get(molecularFormula)) {
                    fingerprintCandidates.add(new FingerprintCandidate(c.inchi, c.fingerprint));
                }
            }
            for (Compound c : compoundsPerFormulaBio.get(molecularFormula)) {
                fingerprintCandidates.add(new FingerprintCandidate(c.inchi, c.fingerprint));
            }


            return fingerprintCandidates;
        }

        @Override
        public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> iterable) throws DatabaseException {
            return restDatabase.lookupFingerprintsByInchis(iterable);
        }

        @Override
        public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> iterable) throws DatabaseException {
            return restDatabase.lookupManyInchisByInchiKeys(iterable);
        }

        @Override
        public List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> iterable) throws DatabaseException {
            return restDatabase.lookupManyFingerprintsByInchis(iterable);
        }

        @Override
        public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> iterable) throws DatabaseException {
            return restDatabase.lookupFingerprintsByInchi(iterable);
        }

        @Override
        public void annotateCompounds(List<? extends CompoundCandidate> list) throws DatabaseException {
            restDatabase.annotateCompounds(list);
        }

//        @Override
        public List<InChI> findInchiByNames(List<String> list) throws DatabaseException {
            return null;
        }

        @Override
        public void close() throws IOException {
            restDatabase.close();
        }
    }
}
