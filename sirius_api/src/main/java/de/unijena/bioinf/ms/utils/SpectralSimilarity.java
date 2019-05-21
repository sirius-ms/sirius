package de.unijena.bioinf.ms.utils;

public class SpectralSimilarity {
    final double similarity;
    final int shardPeaks;

    public SpectralSimilarity(double similarity, int shardPeaks) {
        this.similarity = similarity;
        this.shardPeaks = shardPeaks;
    }
}
