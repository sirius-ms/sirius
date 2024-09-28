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

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.projectspace.IncompatibleFingerprintDataException;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This is a job for Scheduling a workflow synchronization
 * it should just be used to organize the workflow, wait for results and input dependencies
 * NOT for CPU intense task -> use a nested CPU job instead.
 * Note: Scheduler Jobs like this can be blocked without any problems so subjob submission is
 * NOT necessary and NOT recommended.
 */

public abstract class InstanceJob extends ToolChainJobImpl<Instance> implements ToolChainJob<Instance> {
    //todo store only Id and use Cache in projectspace manager instead -> allows for larger InstanceBuffer sizes.
    protected Instance input = null;

    public InstanceJob(JobSubmitter submitter) {
        super(submitter);
    }

    @Override
    public void handleFinishedRequiredJob(JJob required) {
        final Object r = required.result();
        if (r instanceof Instance inst)
            if (input == null || input.equals(r))
                input = inst;
    }


    @Override
    protected Instance compute() throws Exception {
        updateProgress(0);
        checkForInterruption();
        if (checkInput())
            return input;

        final boolean hasResults = isAlreadyComputed(input);
        updateProgress(1);

        checkForInterruption();
        if (!hasResults || input.isRecompute()) {
            if (hasResults) {
                invalidateResults(input);
            }
            updateProgress(2, "Invalidate existing Results and Recompute!");
            progressInfo("Start computation...");
            input.setRecompute(true); // enable recompute so that following tools will recompute if results exist.
            checkForInterruption();
            computeAndAnnotateResult(input);
            checkForInterruption();
            updateProgress(JobProgressEvent.DEFAULT_MAX - 1, "DONE!");
        } else {
            updateProgress(JobProgressEvent.DEFAULT_MAX - 1, "Skipping Job because results already Exist and recompute not requested.");
        }
        return input;
    }

    @Override
    protected void cleanup() {
        super.cleanup();
    }

    @Override
    public String identifier() {
        return super.identifier() + " | " + (input != null ? input.toString() : "<Awaiting Instance>");
    }

    @Override
    public String getProjectName() {
        return (input != null ? input.getProjectSpaceManager().getName() : "<Awaiting Instance>");
    }

    /**
     * Check if the input is valid for computation. May be overwritten by implementations for additional checks.
     *
     * @return false if input data is fine and true if data should be skipped gently. IllegalArgumentException is thrown
     * if the input check needs to cause job failure
     */
    protected boolean checkInput() {
        if (input == null)
            throw new IllegalArgumentException("No Input available! Maybe a previous job could not provide the needed results due to failure.");
        if (needsMs2())
            if (input.getExperiment().getMs2Spectra().isEmpty()) {
                logInfo("Input contains no non empty MS/MS spectrum but MS/MS data is mandatory for this job. Skipping Instance!");
                return true;
            }
        return false;
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

    protected void checkFingerprintCompatibilityOrThrow() throws TimeoutException, InterruptedException, IncompatibleFingerprintDataException {
        if (!checkFingerprintCompatibility())
            throw new IncompatibleFingerprintDataException();
    }

    protected boolean checkFingerprintCompatibility() throws TimeoutException, InterruptedException {
        return input.getProjectSpaceManager().checkAndFixDataFiles(this::checkForInterruption);
    }

    protected boolean needsMs2() {
        return true;
    }
}
