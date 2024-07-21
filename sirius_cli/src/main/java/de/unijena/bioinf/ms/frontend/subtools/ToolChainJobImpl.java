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

package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.jjobs.BasicDependentMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class ToolChainJobImpl<R> extends BasicDependentMasterJJob<R> implements ToolChainJob<R>, JobSubmitter {
    private final JobSubmitter submitter;
    private Consumer<Instance> invalidator;

    public ToolChainJobImpl(@NotNull JobSubmitter submitter) {
        super(JobType.SCHEDULER);
        this.submitter = submitter;
    }

    public ToolChainJobImpl(@NotNull JobSubmitter submitter, @NotNull ReqJobFailBehaviour failBehaviour) {
        super(JobType.SCHEDULER, failBehaviour);
        this.submitter = submitter;
    }

    @Override
    public void setInvalidator(Consumer<Instance> invalidator) {
        this.invalidator = invalidator;
    }

    @Override
    public void invalidateResults(@NotNull Instance inst) {
        if (invalidator != null)
            invalidator.accept(inst);
    }


    //todo this is a workaround to prevent thread interrupts on subtool jobs that perform database operations since they
    // are likely causing that the db channel is closed. instead we perform controlled shutdown of the subtool
    // maybe allow this to be configured in MasterJJob itself.
    @Override
    public void cancel(boolean mayInterruptIfRunning) {
        this.stateLock.lock();

        try {
            if (this.future != null) {
                this.logDebug("Try to Cancel Running Job. Sending interruption commands to the current job (and to all subjobs).");
                this.forEachSubJobSynchronized((subJob) -> subJob.cancel(mayInterruptIfRunning));
                if (mayInterruptIfRunning)
                    logDebug("Prevent thread interruption, to protect DB channel!");
                if (this.future.cancel(false) && this.getState().ordinal() <= JobState.RUNNING.ordinal())
                    this.setState(JobState.CANCELED);
            } else {
                try {
                    this.setState(JobState.CANCELED);
                    this.logDebug("Canceled Waiting Job by interrupting the waiting thread.");
                } finally {
                    if (!this.isClean.getAndSet(true)) {
                        try {
                            this.cleanup();
                        } catch (Exception var14) {
                            Exception e = var14;
                            this.logError("Unexpected Error during job Cleanup!", e);
                        }
                    }

                }
            }
            this.waiter.countDown();
        } finally {
            this.stateLock.unlock();
        }
    }

    @Override
    public <Job extends JJob<Result>, Result> Job submitJob(Job job) {
        job.delegateLog(this);
        return submitter.submitJob(job);
    }

    @Override
    public <Job extends JJob<Result>, Result> Job submitSubJob(Job subjob) {
        subjob.delegateLog(this);
        return super.submitSubJob(subjob);
    }
}
