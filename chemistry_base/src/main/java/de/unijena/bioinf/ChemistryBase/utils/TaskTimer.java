package de.unijena.bioinf.ChemistryBase.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A utility class for measuring execution times of tasks and subtasks.
 */
public class TaskTimer {
    private final String name;
    private final long startTimeMillis;
    private long endTimeMillis;
    private final List<TaskTiming> taskTimings = new ArrayList<>();
    private TaskTiming currentTask = null;
    
    /**
     * Represents timing information for a single task.
     */
    public static class TaskTiming {
        private final String name;
        private final long startTimeMillis;
        private long endTimeMillis;
        
        private TaskTiming(String name, long startTimeMillis) {
            this.name = name;
            this.startTimeMillis = startTimeMillis;
        }
        
        /**
         * Gets the task name.
         */
        public String getName() {
            return name;
        }
        
        /**
         * Gets the task duration in milliseconds.
         */
        public long getDurationMillis() {
            return endTimeMillis - startTimeMillis;
        }
        
        /**
         * Format the task timing as a readable string.
         */
        @Override
        public String toString() {
            return formatDuration(getDurationMillis());
        }
    }
    
    /**
     * Creates a new started TaskTimer.
     */
    public static TaskTimer createStarted(String name) {
        return new TaskTimer(name);
    }
    
    /**
     * Creates a new started TaskTimer.
     */
    private TaskTimer(String name) {
        this.name = name;
        this.startTimeMillis = System.currentTimeMillis();
    }
    
    /**
     * Starts timing a new task.
     */
    public void startTask(String taskName) {
        if (currentTask != null) {
            endTask();
        }
        currentTask = new TaskTiming(taskName, System.currentTimeMillis());
    }
    
    /**
     * Ends the current task and adds it to the completed tasks list.
     */
    public TaskTiming endTask() {
        TaskTiming tmp = null;
        if (currentTask != null) {
            currentTask.endTimeMillis = System.currentTimeMillis();
            taskTimings.add(currentTask);
            tmp = currentTask;
            currentTask = null;
        }
        return tmp;
    }
    
    /**
     * Stops the timer.
     */
    public void stop() {
        if (currentTask != null) {
            endTask();
        }
        if (endTimeMillis == 0) {
            endTimeMillis = System.currentTimeMillis();
        }
    }
    
    /**
     * Gets the total duration in milliseconds.
     */
    public long getTotalDurationMillis() {
        if (endTimeMillis == 0) {
            return System.currentTimeMillis() - startTimeMillis;
        }
        return endTimeMillis - startTimeMillis;
    }
    
    /**
     * Gets the list of completed task timings.
     */
    public List<TaskTiming> getTaskTimings() {
        return new ArrayList<>(taskTimings);
    }
    
    /**
     * Format the duration as a readable string.
     */
    public static String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");
        if (millis > 0) {
            sb.append(" ").append(millis).append("ms");
        }
        
        return sb.toString();
    }
    
    /**
     * Returns a human-readable summary of all tasks.
     */
    @Override
    public String toString() {
        return formatDuration(getTotalDurationMillis());
    }
    
    /**
     * Returns the timing for a specific task by name.
     */
    public String getTaskTimeString(String taskName) {
        for (TaskTiming timing : taskTimings) {
            if (timing.getName().equals(taskName)) {
                return timing.toString();
            }
        }
        return "Task not found: " + taskName;
    }
}