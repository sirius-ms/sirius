/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.fingerprinter;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FingerprinterWorkflow implements Workflow {

    private final Path outputFile;
    private final RootOptions<?, ?, ?, ?> rootOptions;

    private final Path versionFile;
    private final int charge;

    public FingerprinterWorkflow(RootOptions<?, ?, ?, ?> rootOptions, Path outputFile, int charge, Path versionFile) {
        this.outputFile = outputFile;
        this.rootOptions = rootOptions;
        this.charge = charge;
        this.versionFile = versionFile;

    }

    @Override
    public void run() {
        //todo maybe fixed size buffer for memory optimization
        List<Path> in = rootOptions.getInput().getAllFiles();
        if (in.isEmpty())
            throw new IllegalArgumentException("No input file given!");

        Path inputFile = in.iterator().next();
        // get WEB API
        WebAPI<?> api = ApplicationCore.WEB_API;

        try {
            //get FingerIdDate File based on charge
            FingerIdData fdata = charge > 0 ? api.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE) : api.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE);
            MaskedFingerprintVersion mask = fdata.getFingerprintVersion();
            CdkFingerprintVersion cdkVersion = api.getCDKChemDBFingerprintVersion();

            LoggerFactory.getLogger(getClass()).info("Reading input from '" + inputFile.toString() + "'...");
            List<String> smilesList = readInput(inputFile);

            List<BasicJJob<SmilesFpt>> jobs = new ArrayList<>();

            LoggerFactory.getLogger(getClass()).info("Creating fingerprint jobs for '" +smilesList.size() + "' input structures.");
            for (String smiles : smilesList) {
                BasicJJob<SmilesFpt> fpt_job = new BasicJJob<>() {
                    @Override
                    protected SmilesFpt compute() {
                        FixedFingerprinter printer = new FixedFingerprinter(cdkVersion);
                        return new SmilesFpt(smiles, mask.mask(printer.computeFingerprintFromSMILES(smiles).toIndizesArray()));
                    }
                };
                jobs.add(fpt_job);
            }

            LoggerFactory.getLogger(getClass()).info("Computing fingerprints...");
            jobs = SiriusJobs.getGlobalJobManager().submitJobsInBatchesByThreads(jobs, SiriusJobs.getCPUThreads());
            //collect jobs skipping failed ones (null)
            List<SmilesFpt> outList = jobs.stream().map(JJob::getResult).filter(Objects::nonNull).toList();

            LoggerFactory.getLogger(getClass()).info("Writing fingerprints to '" + outputFile.toString() + "'...");
            writeOutput(outputFile, outList);
            if (versionFile != null) {
                LoggerFactory.getLogger(getClass()).info("Writing fingerprint definition file to '" + versionFile.toString() + "'...");
                try (BufferedWriter bw = Files.newBufferedWriter(versionFile)) {
                    FingerIdData.write(bw, fdata);
                }
            }
            LoggerFactory.getLogger(getClass()).info("DONE!");
        } catch (IOException e) {
           LoggerFactory.getLogger(getClass()).error("Unexpected error during fingerprint computation", e);
        }
    }

    public List<String> readInput(Path in) throws IOException {
        List<String> smiles = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(in)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 0) smiles.add(line);
            }
            return smiles;
        }
    }

    public void writeOutput(Path outputFile, List<SmilesFpt> outList) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(outputFile)) {
            for (SmilesFpt obj : outList) {
                bw.write(obj.smiles + "\t" + obj.fpt.toCommaSeparatedString() + System.lineSeparator());
            }
        }
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
