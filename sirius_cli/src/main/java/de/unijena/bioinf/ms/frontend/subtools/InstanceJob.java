/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This is a job for Scheduling an workflow synchronization
 * it should just be used to organize the workflow, wait for results and input dependencies
 * NOT for CPU intense task -> use a nested CPU job instead.
 * Note: Scheduler Jobs like this can be blocked without any problems so subjob submission is
 * NOT necessary and NOT recommended.
 */

public abstract class InstanceJob extends ToolChainJobImpl<Instance> implements ToolChainJob<Instance> {
    protected Instance input = null;

    public InstanceJob(JobSubmitter submitter) {
        super(submitter);
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        final Object r = required.result();
        if (r instanceof Instance)
            if (input == null || input.equals(r))
                input = (Instance) r;
    }


    @Override
    protected Instance compute() throws Exception {
        checkInput();
        final boolean hasResults = isAlreadyComputed(input);
        if (!hasResults || isRecompute(input)) {
            if (hasResults){
                updateProgress(0, 100, 2, "Invalidate existing Results and Recompute!");
                invalidateResults(input);
            }
            updateProgress(0, 100, 99, "Start computation...");
            setRecompute(input,true); // enable recompute so that following tools will recompute if results exist.
            computeAndAnnotateResult(input);
            updateProgress(0, 100, 99, "DONE!");
        } else {
            updateProgress(0, 100, 99, "Skipping Job because results already Exist and recompute not requested.");
        }

        return input;
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        final Class<? extends DataAnnotation>[] ca = compoundComponentsToClear();
        if (input != null) {
            if (ca != null && ca.length > 0) input.clearCompoundCache(ca);
            final Class<? extends DataAnnotation>[] ra = formulaResultComponentsToClear();
            if (ra != null && ra.length > 0) input.clearFormulaResultsCache(ra);
        }
    }

    @Override
    public String identifier() {
        return super.identifier() + " | Instance: " + (input != null ? input.toString() : "Awaiting Instance!");
    }

    protected void checkInput() {
        if (input == null)
            throw new IllegalArgumentException("No Input available! Maybe a previous job could not provide the needed results due to failure.");
    }

    protected Class<? extends DataAnnotation>[] compoundComponentsToClear() {
        return null;
    }

    protected Class<? extends DataAnnotation>[] formulaResultComponentsToClear() {
        return null;
    }

    protected abstract void computeAndAnnotateResult(final @NotNull Instance expRes) throws Exception;

    public static class Factory<T extends InstanceJob> extends ToolChainJob.FactoryImpl<T> {

        public Factory(@NotNull Function<JobSubmitter, T> jobCreator, @Nullable Consumer<Instance> baseInvalidator) {
            super(jobCreator, baseInvalidator);
        }

        public T createToolJob(@NotNull JJob<Instance> inputProvidingJob) {
            return createToolJob(inputProvidingJob, SiriusJobs.getGlobalJobManager());
        }

        public T createToolJob(@NotNull JJob<Instance> inputProvidingJob, @NotNull JobSubmitter submitter) {
            final T job = makeJob(submitter);
            job.addRequiredJob(inputProvidingJob);
            return job;
        }

    }
}
