package de.unijena.bioinf.canopus;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchGenerator implements Runnable {

    protected boolean USE_RING_BUFFER = false;

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
        this.ringBuffer = USE_RING_BUFFER ? new BufferedTrainData(trainingData, capacity, 16000) : null;
    }

    public TrainingBatch poll(int k) {
        if (stop) return null;
        else try {
            if (USE_RING_BUFFER && k % 2 == 1) {
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
