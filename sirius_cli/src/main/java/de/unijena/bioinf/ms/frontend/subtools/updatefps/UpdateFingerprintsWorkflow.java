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

package de.unijena.bioinf.ms.frontend.subtools.updatefps;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.frontend.subtools.fingerblast.FingerblastOptions;
import de.unijena.bioinf.ms.frontend.subtools.fingerprint.FingerprintOptions;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class UpdateFingerprintsWorkflow extends BasicMasterJJob<Boolean> implements Workflow {

    private final PreprocessingJob<?> preprocessingJob;

    public UpdateFingerprintsWorkflow(PreprocessingJob<?> preprocessingJob) {
        super(JobType.SCHEDULER);
        this.preprocessingJob = preprocessingJob;
    }

    @Override
    protected Boolean compute() throws Exception {
        Iterable<? extends Instance> instances = SiriusJobs.getGlobalJobManager().submitJob(preprocessingJob).awaitResult();
        ProjectSpaceManager projectSpace = instances.iterator().next().getProjectSpaceManager(); // todo Hacky: implement real multi project solution?!

        List<Consumer<Instance>> invalidators = new ArrayList<>();
        invalidators.add(new FingerprintOptions(null).getInvalidator());
        invalidators.add(new FingerblastOptions(null).getInvalidator());
        invalidators.add(new CanopusOptions(null).getInvalidator());
        final AtomicInteger progress = new AtomicInteger(0);
        final int max = projectSpace.size() * invalidators.size() + 3;
        updateProgress(0, max, progress.getAndIncrement(), "Starting Update...");
        // remove fingerprint related results
        projectSpace.forEach(i -> invalidators.forEach(inv -> {
            updateProgress(0, max, progress.getAndIncrement(), "Deleting results for '" + i.getName() + "'...");
            inv.accept(i);
        }));
        //remove Fingerprint data
        updateProgress(0, max, progress.getAndIncrement(), "delete CSI:FinerID and CANOPUS fingerprint Data");
        projectSpace.deleteFingerprintData();
        updateProgress(0, max, progress.get(), "DONE!");
        return true;
    }

    @Override
    public void run() {
        try {
            SiriusJobs.getGlobalJobManager().submitJob(this).awaitResult();
        } catch (ExecutionException e) {
            LoggerFactory.getLogger(this.getClass()).error("Error when updating Fingerprint Definition", e);
        }
    }

    @Override
    public void cancel() {
        cancel(false);
    }
}
