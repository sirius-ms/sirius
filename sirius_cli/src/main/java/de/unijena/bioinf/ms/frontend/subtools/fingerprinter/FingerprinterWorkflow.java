/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.fingerprinter;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.webapi.WebAPI;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
public class FingerprinterWorkflow implements Workflow {
    private final Path outputFile;
    private final RootOptions<?> rootOptions;
    private final Path versionFile;
    private final int charge;
    private final BlockingSet<BasicJJob<Void>> jobs;
    private FingerIdData fdata;
    private MaskedFingerprintVersion mask;
    private CdkFingerprintVersion cdkVersion;
    private final Map<String, Exception> failedComputations = new ConcurrentHashMap<>();

    public FingerprinterWorkflow(RootOptions<?> rootOptions, Path outputFile, int charge, Path versionFile, int bufferSize) {
        this.outputFile = outputFile;
        this.rootOptions = rootOptions;
        this.charge = charge;
        this.versionFile = versionFile;
        int bufferSizeInit = bufferSize == 0 ? (5 * SiriusJobs.getCPUThreads()) : bufferSize;
        this.jobs = new BlockingSet<>(bufferSizeInit);
    }

    @Override
    public void run() {

        List<Path> in = rootOptions.getInput().getAllFiles();
        if (in.isEmpty())
            throw new IllegalArgumentException("No input file given!");

        Path inputFile = in.iterator().next();

        loadFingerprintVersionData();

        try (BufferedWriter bw = Files.newBufferedWriter(outputFile)) {

            // Creating producer job that reads smiles, build worker jobs adds them to a fixed blocking set
            BasicJJob<Void> producer = buildProducer(inputFile, bw);
            SiriusJobs.getGlobalJobManager().submitJob(producer);
            producer.awaitResult();
            LoggerFactory.getLogger(getClass()).info("DONE!");

        } catch (IOException | ExecutionException e) {
            LoggerFactory.getLogger(getClass()).error("Unexpected error during fingerprint computation", e);
        }

        if (!failedComputations.isEmpty()) {
            LoggerFactory.getLogger(getClass()).info("Following smiles could not be computed:");
            for (String smiles : failedComputations.keySet()) {
                LoggerFactory.getLogger(getClass()).info(smiles + ": " +  failedComputations.get(smiles).toString());
            }
        }

        if (versionFile != null) {
            LoggerFactory.getLogger(getClass()).info("Writing fingerprint definition file to '" + versionFile.toString() + "'...");
            try (BufferedWriter bw1 = Files.newBufferedWriter(versionFile)) {
                FingerIdData.write(bw1, fdata);
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).error(String.valueOf(e));
            }
        }
    }

    public void loadFingerprintVersionData() {
        // get WEB API
        WebAPI<?> api = ApplicationCore.WEB_API;
        try {
            // get FingerIdDate File based on charge
            fdata = charge > 0 ? api.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE) : api.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE);
            mask = fdata.getFingerprintVersion();
            cdkVersion = api.getCDKChemDBFingerprintVersion();
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Unexpected error during api access", e);
        }
    }

    public BasicJJob<Void> buildProducer(Path inputFile, BufferedWriter bw) {
        return new BasicJJob<>() {
            @Override
            protected Void compute() {
                try (BufferedReader br = Files.newBufferedReader(inputFile)) {
                    String smiles;
                    while ((smiles = br.readLine()) != null) {
                        if (smiles.length() > 0) {
                            BasicJJob<Void> worker = buildWorker(smiles, bw);
                            jobs.add(worker);
                            SiriusJobs.getGlobalJobManager().submitJob(worker);
                        }
                    }
                    jobs.waitForEmpty();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        };
    }

    public BasicJJob<Void> buildWorker(String smiles, BufferedWriter bw) {
        return new BasicJJob<>() {
            @Override
            protected Void compute() {
                FixedFingerprinter printer = new FixedFingerprinter(cdkVersion, ApplicationCore.IFP_CACHE());
                try {
                    // computing fingerprint
                    SmilesFpt smilesFpt = new SmilesFpt(smiles, mask.mask(printer.computeFingerprintFromSMILES(smiles).toIndizesArray()));
                    bw.write(smilesFpt.smiles + "\t" + smilesFpt.fpt.toCommaSeparatedString() + System.lineSeparator());
                } catch (IOException | RuntimeException e) {
                    // if an error occurs, skip this smiles
                    failedComputations.put(smiles, e);
                }
                jobs.remove(this);
                return null;
            }

        };
    }
}

class SmilesFpt {
    public String smiles;
    public Fingerprint fpt;

    protected SmilesFpt(String smiles, Fingerprint fpt) {
        this.fpt = fpt;
        this.smiles = smiles;
    }
}

class BlockingSet<E> {
    private final Set<E> set = new HashSet<>();
    private final int bufferSize;

    protected BlockingSet(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    protected synchronized void add(E element) throws InterruptedException {
        while (set.size() >= bufferSize) {
            wait();
        }
        set.add(element);
    }

    protected synchronized boolean remove(E element) {
        boolean success = set.remove(element);
        if (success)
            notify();
        return success;
    }

    protected synchronized int size() {
        return set.size();
    }

    protected synchronized void waitForEmpty() throws InterruptedException {
        while (!set.isEmpty()) {
            wait();
        }
    }
}
