package de.unijena.bioinf.lcms.align;

public interface AlignmentAlgorithm {
    public interface CallbackForAlign {
        public void alignWith(AlignWithRecalibration rec, MoI[] left, MoI[] right, int leftIndex, int rightIndex);
    }
    public interface CallbackForLeftOver {
        public void leftOver(AlignWithRecalibration rec, MoI[] right, int rightIndex);
    }

    /**
     * Align the given list of MoIs left with right. Not each MoI has to be aligned, but each
     * MoI can only be part of at most one alignment. Call the align callback
     * for each aligned MoI pair. MoIs are quaranteed to be sorted by mass
     * @param stats has infos about expected mz and ret deviations
     * @param left mz ordered array of MoIs to align
     * @param right mz ordered array of MoIs to align
     * @param align callback to define which MoIs to align
     */
    public void align(AlignmentStatistics stats, AlignmentScorer scorer, AlignWithRecalibration rec, MoI[] left,
                      MoI[] right, CallbackForAlign align, CallbackForLeftOver leftOver);
}
