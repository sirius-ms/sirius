/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.ms.frontend.workfow;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.ToolChainJob;
import de.unijena.bioinf.ms.frontend.workflow.InstanceBufferFactory;
import de.unijena.bioinf.ms.frontend.workflow.SimpleInstanceBuffer;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public class GuiInstanceBufferFactory implements InstanceBufferFactory<SimpleInstanceBuffer> {
    /**
     * Here we can Control which jobs should be visible in the Job Panel
     * @param bufferSize
     * @param instances
     * @param tasks
     * @param dependJob
     * @return
     */


    @Override
    public SimpleInstanceBuffer create(int bufferSize, @NotNull Iterator<? extends Instance> instances, @NotNull List<InstanceJob.Factory<?>> tasks, @Nullable DataSetJob.Factory<?> dependJob, @NotNull JobProgressMerger progressSupport) {
        return new SimpleInstanceBuffer(bufferSize, instances, tasks, dependJob, progressSupport, new JobSubmitter() {
            @Override
            public <Job extends JJob<Result>, Result> Job submitJob(Job j) { //todo what do we want to show here?
                if (j instanceof ToolChainJob) {
                    final String jobType = ((ToolChainJob<?>) j).getToolName();
                    Jobs.submit((ProgressJJob<?>)j, j::identifier, () -> jobType);
                    return j;
                } else {
                    return Jobs.MANAGER().submitJob(j);
                }
            }
        });
    }
}
