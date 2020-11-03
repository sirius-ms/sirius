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

import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.annotations.RecomputeResults;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ToolChainJob<T> extends ProgressJJob<T> {

    default boolean isRecompute(final @NotNull Instance inst) {
        return inst.getExperiment().getAnnotation(RecomputeResults.class, () -> RecomputeResults.FALSE).value;
    }

    default boolean enableRecompute(final @NotNull Instance inst) {
        return setRecompute(inst, true);
    }

    default boolean disableRecompute(final @NotNull Instance inst) {
        return setRecompute(inst, false);
    }

    default boolean setRecompute(final @NotNull Instance inst, boolean recompute) {
        return inst.getExperiment().setAnnotation(RecomputeResults.class, RecomputeResults.newInstance(recompute));
    }

    boolean isAlreadyComputed(final @NotNull Instance inst);

    default String getToolName() {
        return getClass().getSimpleName();
    }

    void setInvalidator(Consumer<Instance> invalidator);

    void invalidateResults(final @NotNull Instance inst);


    interface Factory<J extends ToolChainJob<?>> {
        J makeJob(JobSubmitter submitter);

        Factory<J> addInvalidator(Consumer<Instance> invalidator);
    }

    abstract class FactoryImpl<J extends ToolChainJob<?>> implements Factory<J> {
        @NotNull
        protected Function<JobSubmitter, J> jobCreator;
        @NotNull
        protected Consumer<Instance> invalidator;

        public FactoryImpl(@NotNull Function<JobSubmitter, J> jobCreator, @Nullable Consumer<Instance> baseInvalidator) {
            this.jobCreator = jobCreator;
            invalidator = baseInvalidator != null ? baseInvalidator : (instance -> {});
        }

        @Override
        public J makeJob(JobSubmitter submitter) {
            J j = jobCreator.apply(submitter);
            j.setInvalidator(invalidator);
            return j;
        }

        @Override
        public Factory<J> addInvalidator(Consumer<Instance> invalidator) {
            this.invalidator = this.invalidator.andThen(invalidator);
            return this;
        }
    }
}
