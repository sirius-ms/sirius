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

package de.unijena.bioinf.ms.frontend.subtools.summaries;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.ZipCompressionMethod;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.PostprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.export.tables.ExportPredictionsOptions;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.summaries.SummaryLocations;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SummarySubToolJob extends PostprocessingJob<Boolean> implements Workflow {
    private static final Logger LOG = LoggerFactory.getLogger(SummarySubToolJob.class);
    private final SummaryOptions options;
    private final ParameterConfig config;
    private final RootOptions<?,?,?,?> rootOptions;

    public SummarySubToolJob(RootOptions<?,?,?,?> rootOptions, ParameterConfig config, SummaryOptions options) {
        this.rootOptions = rootOptions;
        this.config = config;
        this.options = options;
    }

    private boolean standalone = false;

    public boolean isStandalone() {
        return standalone;
    }

    public void setStandalone(boolean standalone) {
        this.standalone = standalone;
    }

    @Override
    protected Boolean compute() throws Exception {
        ProjectSpaceManager project = rootOptions.getProjectSpace();
        try {
            //use all experiments in workspace to create summaries
            LOG.info("Writing summary files...");
            StopWatch w = new StopWatch(); w.start();
            final JobProgressEventListener listener = this::updateProgress;

            //todo ugly hack to prevent double import of compounds during cli, find real solution
            Iterable<? extends Instance> compounds = rootOptions instanceof CLIRootOptions ?  project
                    : SiriusJobs.getGlobalJobManager().submitJob(rootOptions.makeDefaultPreprocessingJob()).awaitResult();


            List<CompoundContainerId> ids = null;
            if (compounds != null && !compounds.equals(project)) {
                List<CompoundContainerId> idsTMP = new ArrayList<>(project.size());
                compounds.forEach(i -> idsTMP.add(i.getID()));
                ids = idsTMP;
            }

            SiriusProjectSpace.SummarizerJob job = project.projectSpace().makeSummarizerJob(options.location, options.compress, ids, ProjectSpaceManager.defaultSummarizer());
            job.addJobProgressListener(listener);
            SiriusJobs.getGlobalJobManager().submitJob(job).awaitResult();
            job.removePropertyChangeListener(listener);

            if (options.isAnyPredictionOptionSet()) { // this includes options.predictionsOptions null check
                Path root = options.compress
                        ? FileUtils.asZipFSPath(options.location, false, true, ZipCompressionMethod.DEFLATED)
                        : options.location;
                try {
                    LOG.info("Writing positive ion mode predictions table...");
                    ExportPredictionsOptions.ExportPredictionJJob posJob = new ExportPredictionsOptions.ExportPredictionJJob(
                            options.predictionsOptions, 1, compounds,
                            () -> Files.newBufferedWriter(root.resolve(SummaryLocations.PREDICTIONS)));
                    posJob.addJobProgressListener(listener);
                    SiriusJobs.getGlobalJobManager().submitJob(posJob).awaitResult();
                    posJob.removePropertyChangeListener(listener);

                    LOG.info("Writing negative ion mode predictions table...");
                    ExportPredictionsOptions.ExportPredictionJJob negJob = new ExportPredictionsOptions.ExportPredictionJJob(
                            options.predictionsOptions, -1, compounds,
                            () -> Files.newBufferedWriter(root.resolve(SummaryLocations.PREDICTIONS_NEG)));
                    negJob.addJobProgressListener(listener);
                    SiriusJobs.getGlobalJobManager().submitJob(negJob).awaitResult();
                    negJob.removePropertyChangeListener(listener);
                }finally {
                    //close and write zip file
                    if (!root.getFileSystem().equals(FileSystems.getDefault()))
                        root.getFileSystem().close();
                }
            }

            w.stop();
            LOG.info("Project-Space summaries successfully written in: " + w);

            return true;
        } finally {
            if (!standalone)
                project.close(); // close project if this is a postprocessor
        }
    }

    @Override
    public void run() {
        setStandalone(true);
        SiriusJobs.getGlobalJobManager().submitJob(this).takeResult();
    }

    @Override
    public void cancel() {
        cancel(true);
    }
}
