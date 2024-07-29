package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

import java.util.Optional;

/**
 * Stores comment strings beside peaks. As the Ms2Experiment object model
 * does not really support peak annotations, we instead store them as simple array
 * assuming peak positions are fixed. Thus, this kind of annotation does only makes
 * sense when looking at raw and unprocessed spectra. In particular, we use this annotation
 * as a simple way to transfer comments when reading and writing ms files.
 * We might decide later to extend the Ms2Spectra class to support comments
 */
public class PeakComment implements Ms2ExperimentAnnotation {

    protected final String[] commentsPerPeakMergedMs1;
    protected final String[][] commentsPerPeakMs1;
    protected final String[][] commentsPerPeakMs2;

    public PeakComment(String[] commentsPerPeakMergedMs1, String[][] commentsPerPeakMs1, String[][] commentsPerPeakMs2) {
        this.commentsPerPeakMergedMs1 = commentsPerPeakMergedMs1;
        this.commentsPerPeakMs1 = commentsPerPeakMs1;
        this.commentsPerPeakMs2 = commentsPerPeakMs2;
    }

    public Optional<String> getMergedMs1CommentFor(int peakIndex) {
        return Optional.ofNullable(commentsPerPeakMergedMs1[peakIndex]);
    }

    public Optional<String> getMs1CommentFor(int spectrumIndex, int peakIndex) {
        return Optional.ofNullable(commentsPerPeakMs1[spectrumIndex][peakIndex]);
    }

    public Optional<String> getMs2CommentFor(int spectrumIndex, int peakIndex) {
        return Optional.ofNullable(commentsPerPeakMs2[spectrumIndex][peakIndex]);
    }

    public String[] getCommentsPerPeakMergedMs1() {
        return commentsPerPeakMergedMs1;
    }

    public String[][] getCommentsPerPeakMs1() {
        return commentsPerPeakMs1;
    }

    public String[][] getCommentsPerPeakMs2() {
        return commentsPerPeakMs2;
    }
}
