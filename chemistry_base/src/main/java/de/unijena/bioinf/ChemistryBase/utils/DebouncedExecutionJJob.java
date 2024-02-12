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


package de.unijena.bioinf.ChemistryBase.utils;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import org.jetbrains.annotations.NotNull;

/**
 * Debouncing of functino execution based on JJobs background computations
 */
public class DebouncedExecutionJJob extends TinyBackgroundJJob<Boolean> {
    final long debounceMillis;
    final ExFunctions.Runnable debouncedFunction;
    long lastExecution = 0;

    public DebouncedExecutionJJob(long debounceMillis, @NotNull ExFunctions.Runnable debouncedFunction) {
        this.debounceMillis = debounceMillis;
        this.debouncedFunction = debouncedFunction;
    }


    @Override
    protected Boolean compute() throws Exception {
        while (true) {
            checkForInterruption();
            long wait = debounceMillis - (System.currentTimeMillis() - lastExecution);
            if (wait <= 0) {
                debouncedFunction.run();
                lastExecution = System.currentTimeMillis();
            } else {
                synchronized (this){
                    this.wait(wait);
                }
            }
        }
    }


    public static DebouncedExecutionJJob start(@NotNull Runnable debouncedFunction) {
        return start((ExFunctions.Runnable) debouncedFunction::run);
    }

    public static DebouncedExecutionJJob start(long debounceMillis, @NotNull Runnable debouncedFunction) {
        return start(debounceMillis, (ExFunctions.Runnable) debouncedFunction::run);
    }

    public static DebouncedExecutionJJob start(@NotNull ExFunctions.Runnable debouncedFunction) {
        return start(500, debouncedFunction);
    }

    public static DebouncedExecutionJJob start(long debounceMillis, @NotNull ExFunctions.Runnable debouncedFunction) {
        return (DebouncedExecutionJJob) SiriusJobs.runInBackground(new DebouncedExecutionJJob(debounceMillis, debouncedFunction));
    }
}
