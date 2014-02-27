package de.unijena.bioinf.FTAnalysis;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class JobScheduler {

    private ExecutorService service;
    private Lock lock;
    private ReentrantLock progressLock;
    private AtomicInteger progress;
    private int numberOfTasks;
    private ProgressObserver progressObserver;

    public JobScheduler() {
        this.lock = new ReentrantLock();
        this.progressLock = new ReentrantLock();
    }

    public Lock getLock() {
        return lock;
    }

    public ProgressObserver getProgressObserver() {
        return progressObserver;
    }

    public void setProgressObserver(ProgressObserver progressObserver) {
        if (numberOfTasks > 0) throw new RuntimeException("Cannot change observer while working on submitted tasks");
        this.progressObserver = progressObserver;
    }

    public <T> void submit(List<T> input, final Job<T> job) {
        lock.lock();
        if (numberOfTasks > 0) throw new RuntimeException("Cannot work on new tasks while working on old ones");
        this.progress = new AtomicInteger(0);
        this.numberOfTasks = input.size();
        this.service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        lock.unlock();
        final int updateSteps = (int)Math.ceil(numberOfTasks/100f);
        for (final T data : input) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    job.call(data);
                    final int newValue = progress.incrementAndGet();
                    if (progressObserver != null && newValue % updateSteps == 0) {
                        if (progressLock.tryLock()) {
                            try {
                                progressObserver.newProgress(((float)newValue)/numberOfTasks);
                            } finally {
                                progressLock.unlock();
                            }
                        }
                    }
                }
            });
        }
        service.shutdown();
        try {
            service.awaitTermination(1000, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        numberOfTasks = 0;
    }

    public float getProgress() {
        return progress.floatValue()/numberOfTasks;
    }


    public interface Job<T> {
        public void call(T input);
    }
    public interface ProgressObserver {
        public void newProgress(float progress);
    }
}
