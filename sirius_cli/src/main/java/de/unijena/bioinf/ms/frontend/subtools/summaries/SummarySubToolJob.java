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
import de.unijena.bioinf.ms.frontend.subtools.PostprocessingJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummarySubToolJob extends PostprocessingJob<Boolean> implements Workflow {
    private static final Logger LOG = LoggerFactory.getLogger(SummarySubToolJob.class);
    private final ProjectSpaceManager project;
    private final ParameterConfig config;
    private final SummaryOptions options;

    public SummarySubToolJob(ProjectSpaceManager project, ParameterConfig config, SummaryOptions options) {
        this.project = project;
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
        try {
            //use all experiments in workspace to create summaries
            LOG.info("Writing summary files...");
            StopWatch w = new StopWatch(); w.start();
            SiriusProjectSpace.SummarizerJob job = project.projectSpace().makeSummarizerJob(options.location, options.compress, ProjectSpaceManager.defaultSummarizer());
            job.addJobProgressListener(this::updateProgress);
            SiriusJobs.getGlobalJobManager().submitJob(job).awaitResult();
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
