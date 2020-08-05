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

package de.unijena.bioinf.canopus;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchGenerator implements Runnable {

    protected final ArrayBlockingQueue<TrainingBatch> batches;
    protected final TrainingData trainingData;
    protected final AtomicInteger iterationNum;

    protected final BufferedTrainData ringBuffer;

    protected final ExecutorService service;
    protected volatile boolean stop;

    public BatchGenerator(TrainingData trainingData, int capacity) {
        this.trainingData = trainingData;
        this.batches = new ArrayBlockingQueue<TrainingBatch>(capacity);
        this.stop = false;
        this.iterationNum = new AtomicInteger(0);
        this.service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.ringBuffer = new BufferedTrainData(trainingData, capacity, 16000);
    }

    public TrainingBatch poll(int k) {
        if (stop) return null;
        else try {
            if (k % 2 == 1) {
                if (!ringBuffer.done.isEmpty()) {
                    synchronized (ringBuffer) {
                        BufferedTrainData.Buffer b = ringBuffer.getDone();
                        if (b!=null) {
                            return b.use();
                        }
                    }
                }
            }
            return batches.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void stop() {
        this.stop = true;
        // we have to clear the queue, such that the stop operation is notified
        batches.clear();
        service.shutdown();
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                batches.put(trainingData.generateBatch(iterationNum.incrementAndGet(), ringBuffer, service));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
