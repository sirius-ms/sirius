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

package de.unijena.bioinf.ms.frontend.subtools.summaries;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.ms.frontend.subtools.PostprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.*;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class SiriusProjectSpaceSummarySubToolJob extends PostprocessingJob<Boolean> {
    //todo reimplement project independent!
    private static final Logger LOG = LoggerFactory.getLogger(SiriusProjectSpaceSummarySubToolJob.class);
    private final SummaryOptions options;

    private @Nullable PreprocessingJob<?> preprocessingJob;
    private Iterable<? extends Instance> instances;

    public SiriusProjectSpaceSummarySubToolJob(@Nullable PreprocessingJob<?> preprocessingJob, SummaryOptions options) {
        this.preprocessingJob = preprocessingJob;
        this.options = options;
    }

    public SiriusProjectSpaceSummarySubToolJob(SummaryOptions options) {
        this(null, options);
    }

    @Override
    public void setInput(Iterable<? extends Instance> instances, ParameterConfig config) {
        this.instances = instances;
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
        if (instances == null)
            instances = SiriusJobs.getGlobalJobManager().submitJob(preprocessingJob).awaitResult();

        if (!instances.iterator().hasNext())
            return null;

        SiriusProjectSpaceManager project = null;
        try {
            if (instances instanceof SiriusProjectSpaceManager) {
                project = (SiriusProjectSpaceManager) instances;
            } else {
                Instance inst = instances.iterator().next();
                if (inst instanceof SiriusProjectSpaceInstance)
                    project = (SiriusProjectSpaceManager) inst.getProjectSpaceManager();
                else
                    throw new IllegalArgumentException("This summary job only supports the SIRIUS projectSpace!");
            }


            //use all experiments in workspace to create summaries
            LOG.info("Writing summary files...");
            StopWatch w = new StopWatch();
            w.start();
            final JobProgressEventListener listener = this::updateProgress;


            List<CompoundContainerId> ids = null;
            if (!instances.equals(project)) {
                List<CompoundContainerId> idsTMP = new ArrayList<>(project.size());
                instances.forEach(i -> idsTMP.add(((SiriusProjectSpaceInstance)i).getCompoundContainerId()));
                ids = idsTMP;
            }

            SiriusProjectSpace.SummarizerJob job = project.getProjectSpaceImpl()
                    .makeSummarizerJob(options.location, options.format == SummaryOptions.Format.ZIP, ids, SiriusProjectSpaceManager
                            .defaultSummarizer(
                                    options.isTopHitSummary(),
                                    options.isTopHitWithAdductsSummary(),
                                    options.isFullSummary()
                            ));
            job.addJobProgressListener(listener);
            SiriusJobs.getGlobalJobManager().submitJob(job).awaitResult();
            job.removePropertyChangeListener(listener);

            //Note: fingerprint export now is no part of summary subtool anymore, since supported by API and not supported/implement by NoSQL project space

            w.stop();
            LOG.info("Project-Space summaries successfully written in: " + w);

            return true;
        } finally {
            if (!standalone && project != null)
                project.close(); // close project if this is a postprocessor
        }
    }

    @Override
    public void cancel() {
        cancel(true);
    }

    @Override
    protected void cleanup() {
        instances = null;
        preprocessingJob = null;
        super.cleanup();
    }
}
