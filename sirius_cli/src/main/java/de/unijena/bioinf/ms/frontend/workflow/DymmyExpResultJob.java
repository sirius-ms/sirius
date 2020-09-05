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

package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

@FunctionalInterface
public interface DymmyExpResultJob extends JJob<Instance> {


    @Override
    default void addPropertyChangeListener(PropertyChangeListener listener) {
        /*ignored*/
    }

    @Override
    default void removePropertyChangeListener(PropertyChangeListener listener) {
        /*ignored*/
    }

    @Override
    default void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        /*ignored*/
    }

    @Override
    default JobType getType() {
        return JobType.SCHEDULER;
    }

    @Override
    default JJob<Instance> asType(JobType jobType) {
        return this;
    }

    @Override
    default JobPriority getPriority() {
        return JobPriority.MEDIUM;
    }

    @Override
    default void setPriority(JobPriority jobPriority) {
        /*ignored*/
    }

    @Override
    default JobState getState() {
        return JobState.DONE;
    }

    @Override
    default <T> T setState(JobState state, Function<JJob<Instance>, T> doOnChange) {
        /*ignored*/
        return null;
    }

    @Override
    default void cancel(boolean mayInterruptIfRunning) {
        /*ignored*/
    }

    @Override
    default Instance awaitResult() throws ExecutionException {
        return result();
    }


    @Override
    default int compareTo(@NotNull JJob o) {
        return Integer.MAX_VALUE;
    }

    @Override
    default Instance call() throws Exception {
        return result();
    }
}
