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
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class DataSetJob extends ToolChainJobImpl<Iterable<Instance>> implements ToolChainJob<Iterable<Instance>> {
    private List<JJob<?>> failedJobs = new ArrayList<>();
    private List<Instance> failedInstances = new ArrayList<>();
    private List<Instance> inputInstances = new ArrayList<>();

    protected long maxProgress = 100;

    private final Predicate<Instance> inputValidator;

    public DataSetJob(@NotNull Predicate<Instance> inputValidator, @NotNull JobSubmitter submitter) {
        this(inputValidator, submitter, ReqJobFailBehaviour.WARN);
    }

    public DataSetJob(@NotNull Predicate<Instance> inputValidator, @NotNull JobSubmitter submitter, @NotNull ReqJobFailBehaviour failBehaviour) {
        super(submitter, failBehaviour);
        this.inputValidator = inputValidator;
    }

    @Override
    protected Iterable<Instance> compute() throws Exception {
        checkInputs();
        maxProgress = inputInstances.size() * 101L + 1;
        updateProgress(0L, maxProgress, Math.round(.25 * inputInstances.size()), "Invalidate existing Results and Recompute!");

        //todo maybe make decidable if any or all match
        final boolean hasResults = inputInstances.stream().anyMatch(this::isAlreadyComputed);
        final boolean recompute = inputInstances.stream().anyMatch(this::isRecompute);

        updateProgress(Math.round(.5 * inputInstances.size()), "Invalidate existing Results and Recompute!");


        if (!hasResults || recompute) {
            if (hasResults) {
                inputInstances.forEach(this::invalidateResults);
            }
            updateProgress(Math.round(.9 * inputInstances.size()), "Invalidate existing Results and Recompute!");

            progressInfo( "Start computation...");
            inputInstances.forEach(this::enableRecompute); // enable recompute so that following tools will recompute if results exist.
            updateProgress(inputInstances.size());
            computeAndAnnotateResult(inputInstances);
            updateProgress(maxProgress - 1, "DONE!");
        } else {
            updateProgress(maxProgress - 1, "Skipping Job because results already Exist and recompute not requested.");
        }

        return inputInstances;
    }

    protected void checkInputs() {
        {
            final Map<Boolean, List<Instance>> splitted = inputInstances.stream().collect(Collectors.partitioningBy(inputValidator));
            inputInstances = splitted.get(true);
            failedInstances = splitted.get(false);
        }

        if (inputInstances == null || inputInstances.isEmpty())
            throw new IllegalArgumentException("No Input found, All dependent SubToolJobs are failed.");
        if (!failedJobs.isEmpty())
            logWarn("There are " + failedJobs.size() + " failed input providing InstanceJobs!"
                    + " Skipping Failed InstanceJobs: " + System.lineSeparator() + "\t"
                    + failedJobs.stream().map(JJob::identifier).collect(Collectors.joining(System.lineSeparator() + "\t"))
            );
        if (!failedInstances.isEmpty())
            logWarn("There are " + failedInstances.size() + " invalid input Instances!"
                    + " Skipping Invalid Input Instances: " + System.lineSeparator() + "\t"
                    + failedInstances.stream().map(Instance::toString).collect(Collectors.joining(System.lineSeparator() + "\t"))
            );
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        failedJobs = null;
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        if (required instanceof InstanceJob) {
            final Object r = required.result();
            if (r == null)
                failedJobs.add(required);
            else
                inputInstances.add((Instance) r);
        }
    }


    protected abstract void computeAndAnnotateResult(final @NotNull List<Instance> expRes) throws Exception;

    public List<JJob<?>> getFailedJobs() {
        return failedJobs;
    }

    public boolean hasFailedInstances() {
        return failedJobs != null && !failedJobs.isEmpty();
    }


    public static class Factory<T extends DataSetJob> extends ToolChainJob.FactoryImpl<T> {
        public Factory(@NotNull Function<JobSubmitter, T> jobCreator, @NotNull Consumer<Instance> baseInvalidator) {
            super(jobCreator, baseInvalidator);
        }

        public T createToolJob(Iterable<JJob<Instance>> dataSet) {
            return createToolJob(dataSet, SiriusJobs.getGlobalJobManager());
        }

        public T createToolJob(Iterable<JJob<Instance>> dataSet, @NotNull JobSubmitter jobSubmitter) {
            final T job = makeJob(jobSubmitter);
            dataSet.forEach(job::addRequiredJob);
            return job;
        }
    }


}
