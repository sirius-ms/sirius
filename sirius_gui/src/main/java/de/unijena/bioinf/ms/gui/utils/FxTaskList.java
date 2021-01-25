package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;

/**
 * Stores a list of tasks. Each task remove itself from the list as soon as it is finished
 * or canceled. Insertion of tasks and canceling is thread-safe.
 */
public class FxTaskList {
    private final ConcurrentLinkedQueue<Task> taskList;

    public FxTaskList() {
        this.taskList = new ConcurrentLinkedQueue<>();
    }

    public void runJFXLater(Runnable runnable) {
        Jobs.runJFXLater(enqueue(runnable));
    }

    public Runnable enqueue(final Runnable task) {
        return new Task(this, task);
    }

    public void cancelAll() {
        this.taskList.forEach(task->task.cancel(true));
    }

    private static class Task extends FutureTask<Void> {
        private FxTaskList queue;

        private Task(FxTaskList queue, Runnable runnable) {
            super(runnable, null);
            this.queue = queue;
            queue.taskList.add(this);
        }

        @Override
        protected void done() {
            super.done();
            queue.taskList.remove(this);
        }
    }
}
