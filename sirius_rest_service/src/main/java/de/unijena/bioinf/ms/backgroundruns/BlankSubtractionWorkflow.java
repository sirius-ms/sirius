/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.backgroundruns;

import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.jjobs.ProgressSupport;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinitionImport;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static de.unijena.bioinf.ms.gui.blank_subtraction.BlankSubtraction.*;

public class BlankSubtractionWorkflow implements Workflow, ProgressSupport {

    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);

    private final NoSQLProjectImpl project;
    private final NoSQLProjectSpaceManager psm;

    private final List<String> sampleRunIds;
    private final List<String> blankRunIds;
    private final List<String> controlRunIds;

    public BlankSubtractionWorkflow(Project<? extends ProjectSpaceManager> project, List<String> sampleRunIds, List<String> blankRunIds, List<String> controlRunIds) {
        if (project instanceof NoSQLProjectImpl noSQLProject) {
            this.project = noSQLProject;
            this.psm = noSQLProject.getProjectSpaceManager();
        } else {
            throw new IllegalArgumentException("Project space type not supported!");
        }

        this.sampleRunIds = sampleRunIds;
        this.blankRunIds = blankRunIds;
        this.controlRunIds = controlRunIds;
    }

    @Override
    public void updateProgress(long min, long max, long progress, @Nullable String shortInfo) {
        progressSupport.updateConnectedProgress(min, max, progress, shortInfo);
    }

    @Override
    public void addJobProgressListener(JobProgressEventListener listener) {
        progressSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removeJobProgressListener(JobProgressEventListener listener) {
        progressSupport.removeProgress(listener);
    }

    @Override
    public JobProgressEvent currentProgress() {
        return progressSupport.currentConnectedProgress();
    }

    @Override
    public JobProgressEvent currentCombinedProgress() {
        return progressSupport.currentCombinedProgress();
    }

    @Override
    public void run() {

        try {
            project.findTagByName(TAG_NAME);
        } catch (ResponseStatusException e) {
            project.createTag(TagDefinitionImport.builder()
                    .tagName(TAG_NAME)
                    .tagType(TAG_TYPE)
                    .description(TAG_DESC)
                    .valueType(ValueType.TEXT)
                    .possibleValues(POSSIBLE_VALUES)
                    .build(), false);
        }

        Tag tag = Tag.builder()
                .tagName(TAG_NAME)
                .value(SAMPLE)
                .build();

        for (String runId : sampleRunIds)
            project.addTagsToObject(Run.class, runId, List.of(tag));


        tag.setValue(BLANK);
        for (String runId : blankRunIds)
            project.addTagsToObject(Run.class, runId, List.of(tag));

        tag.setValue(CTRL);
        for (String runId : controlRunIds)
            project.addTagsToObject(Run.class, runId, List.of(tag));

        try {
            project.findTagGroup(SAMPLE_GRP_NAME);
        } catch (ResponseStatusException e) {
            project.addTagGroup(SAMPLE_GRP_NAME, SAMPLE_GRP_QUERY, TAG_TYPE);
        }
        try {
            project.findTagGroup(BLANK_GRP_NAME);
        } catch (ResponseStatusException e) {
            project.addTagGroup(BLANK_GRP_NAME, BLANK_GRP_QUERY, TAG_TYPE);
        }
        try {
            project.findTagGroup(CTRL_GRP_NAME);
        } catch (ResponseStatusException e) {
            project.addTagGroup(CTRL_GRP_NAME, CTRL_GRP_QUERY, TAG_TYPE);
        }

        FoldChangeWorkflow blankWorkflow = new FoldChangeWorkflow(psm, SAMPLE_GRP_NAME, BLANK_GRP_NAME, AggregationType.AVG, QuantMeasure.APEX_INTENSITY, AlignedFeature.class);
        FoldChangeWorkflow ctrlWorkflow = new FoldChangeWorkflow(psm, SAMPLE_GRP_NAME, CTRL_GRP_NAME, AggregationType.AVG, QuantMeasure.APEX_INTENSITY, AlignedFeature.class);

        blankWorkflow.addJobProgressListener(progressSupport);
        ctrlWorkflow.addJobProgressListener(progressSupport);

        blankWorkflow.run();
        ctrlWorkflow.run();

    }

}
