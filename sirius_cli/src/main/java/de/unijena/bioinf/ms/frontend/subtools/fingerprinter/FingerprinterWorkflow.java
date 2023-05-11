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

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FingerprinterWorkflow implements Workflow {

    private final Path outputFile;
    private final RootOptions rootOptions;

    private final Path versionFile;
    private final int charge;

    public FingerprinterWorkflow(RootOptions<?, ?, ?, ?> rootOptions, Path outputFile, int charge,Path versionFile) {
        this.outputFile = outputFile;
        this.rootOptions = rootOptions;
        this.charge=charge;
        this.versionFile=versionFile;

    }

    @Override
    public void run(){
        List<Path> in = rootOptions.getInput().getAllFiles();
        if (in.isEmpty())
            throw new IllegalArgumentException("No input file given!");
        Path inputFile = in.iterator().next();
        @NotNull JobManager jobmanager = SiriusJobs.getGlobalJobManager();


        System.out.println("Happily Computing fingerprints from: " + inputFile.toString() + " to " + outputFile.toString());
        // get WEB API
        WebAPI<?> api = ApplicationCore.WEB_API;


        try {
            //get FingerIdDate File based on charge
            FingerIdData fdata = charge > 0 ? api.getFingerIdData(PredictorType.CSI_FINGERID_POSITIVE) : api.getFingerIdData(PredictorType.CSI_FINGERID_NEGATIVE);
            MaskedFingerprintVersion mask = fdata.getFingerprintVersion();
            CdkFingerprintVersion cdkVersion = api.getCDKChemDBFingerprintVersion();


            ArrayList<String> inchiList = readInput(inputFile);
            List<BasicJJob> jobs = new ArrayList<>();

            for (String smiles : inchiList) {
                BasicJJob fpt_job = new BasicJJob() {
                    @Override
                    protected SmilesFpt compute(){

                        FixedFingerprinter printer = new FixedFingerprinter(cdkVersion);
                        return new SmilesFpt(smiles, mask.mask(printer.computeFingerprintFromSMILES(smiles).asDeterministic()));
                    }


                };
                jobs.add(fpt_job);

            }

            jobs = jobmanager.submitJobsInBatchesByThreads(jobs, 1);

            ArrayList<SmilesFpt> outList = new ArrayList<>();
            for (BasicJJob job : jobs) {
                outList.add((SmilesFpt) job.takeResult());
            }


            writeOutput(outputFile, outList);
            if (versionFile!=null){
                try(BufferedWriter bw = Files.newBufferedWriter(versionFile)){
                    FingerIdData.write(bw, fdata);
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }




    }

    public ArrayList<String> readInput(Path in){
        ArrayList<String> smiles =  new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(in.toFile()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line+" "+line.length());
                if (line.length()>0)smiles.add(line);
            }
            return smiles;
        }catch (Exception e){
            e.printStackTrace();
        }
    return null;
    }

    public void writeOutput(Path outputFile, ArrayList<SmilesFpt> outList) throws IOException {
        BufferedWriter bw =  Files.newBufferedWriter(outputFile);

            for (SmilesFpt obj : outList) {
                bw.write(obj.smiles + "\t" + obj.fpt.toOneZeroString() + "\n");
            }
            bw.close();


    }


}

class SmilesFpt{
    public String smiles;
    public Fingerprint fpt;
    protected SmilesFpt(String smiles, Fingerprint fpt){
        this.fpt=fpt;
        this.smiles=smiles;

    }


}
