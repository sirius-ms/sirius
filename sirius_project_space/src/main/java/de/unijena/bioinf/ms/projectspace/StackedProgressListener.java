package de.unijena.bioinf.ms.projectspace;

import org.jetbrains.annotations.NotNull;

public class StackedProgressListener implements ProgressListener {
    private final ProgressListener progress;

    private final int max;
    private int globalCurrent = 0;
    private int localCurrent = 0;

    public StackedProgressListener(int globalMaxProgress, @NotNull ProgressListener progress) {
        this.max = globalMaxProgress;
        this.progress = progress;
    }

    @Override
    public void doOnProgress(int currentProgress, int maxProgress, @NotNull String message) {
        if (currentProgress > 0)
            globalCurrent += (currentProgress - localCurrent);
        localCurrent = currentProgress;
        progress.doOnProgress(globalCurrent, max, message);
    }

}
