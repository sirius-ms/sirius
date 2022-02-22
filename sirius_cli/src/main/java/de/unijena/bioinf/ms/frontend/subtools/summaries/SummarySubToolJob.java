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

import de.unijena.bioinf.ms.frontend.subtools.PostprocessingJob;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class SummarySubToolJob extends PostprocessingJob<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(SummarySubToolJob.class);
    private final ProjectSpaceManager project;
    private final ParameterConfig config;
    private final SummaryOptions options;

    public SummarySubToolJob(ProjectSpaceManager project, ParameterConfig config, SummaryOptions options) {
        this.project = project;
        this.config = config;
        this.options = options;
    }

    @Override
    protected Boolean compute() throws Exception {
        try {
            //use all experiments in workspace to create summaries

            LOG.info("Writing summary files...");
            project.writeSummaries(options.location,options.compress, ProjectSpaceManager.defaultSummarizer());
            LOG.info("Project-Space summaries successfully written!");

            return true;
        } catch (ExecutionException e) {
            LOG.error("Error when summarizing project. Project summaries may be incomplete!", e);
            return false;
        } finally {
            project.close(); // close project since this is standalone or postprocessor
        }
    }
}
