/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.utils;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.rest.client.AbstractClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class NetUtils {
    public static final Logger LOG = LoggerFactory.getLogger(NetUtils.class);

    public static void tryAndWaitAsJJob(NetRunnable tryToDo) {
        tryAndWaitAsJJob(() -> {
            tryToDo.run();
            return true;
        });
    }

    public static <R> R tryAndWaitAsJJob(NetSupplier<R> tryToDo) {
        return SiriusJobs.runInBackground(new TinyBackgroundJJob<R>() {
            @Override
            protected R compute() throws InterruptedException, TimeoutException {
                return tryAndWait(tryToDo, this::checkForInterruption);
            }
        }).takeResult();
    }

    public static void tryAndWait(NetRunnable tryToDo, InterruptionCheck interrupted) throws TimeoutException, InterruptedException {
        tryAndWait(tryToDo, interrupted, Long.MAX_VALUE);
    }

    public static void tryAndWait(NetRunnable tryToDo, InterruptionCheck interrupted, long timeout) throws InterruptedException, TimeoutException {
        tryAndWait(() -> {
            tryToDo.run();
            return true;
        }, interrupted, timeout);
    }

    public static <R> R tryAndWait(NetSupplier<R> tryToDo, InterruptionCheck interrupted) throws TimeoutException, InterruptedException {
        return tryAndWait(tryToDo, interrupted, Long.MAX_VALUE);
    }

    public static <R> R tryAndWait(NetSupplier<R> tryToDo, InterruptionCheck interrupted, long timeout) throws InterruptedException, TimeoutException {
        long waitTime = INIT_WAIT_TIME;
        while (timeout > 0) {
            try {
                interrupted.check();
                return tryToDo.get();
            } catch (IOException retry) {
                waitTime = (long) Math.min(waitTime * WAIT_TIME_MULTIPLIER, MAX_WAIT_TIME);
                timeout -= waitTime;

                if (AbstractClient.DEBUG) {
                    LOG.warn("Error when try to connect to Server. Try again in " + waitTime / 1000d + "s", retry);
                } else {
                    LOG.warn("Error when try to connect to Server. Try again in " + waitTime / 1000d + "s \n Cause: " + retry.getMessage());
                    LOG.debug("Error when try to connect to Server. Try again in " + waitTime / 1000d + "s", retry);
                }

                sleep(interrupted, waitTime);
            }
        }
        throw new TimeoutException("Stop trying because of Timeout!");
    }

    public static final int INIT_WAIT_TIME = 2000;
    public static final int MAX_WAIT_TIME = 120000;
    public static final float WAIT_TIME_MULTIPLIER = 2;
    public static final int TICK = 1000; //1 sek. without interruption check

    public static void sleep(@NotNull final InterruptionCheck interrupted, long waitTime) throws InterruptedException {
        for (long i = waitTime; i > 0; i -= TICK) {
            interrupted.check();
            Thread.sleep(Math.min(i, TICK));
        }
    }

    @FunctionalInterface
    public interface NetSupplier<R> {
        R get() throws InterruptedException, TimeoutException, IOException;
    }

    @FunctionalInterface
    public interface NetRunnable {
        void run() throws InterruptedException, TimeoutException, IOException;
    }

    @FunctionalInterface
    public interface InterruptionCheck {
        void check() throws InterruptedException;
    }

}
