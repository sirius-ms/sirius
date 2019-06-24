package de.unijena.bionf.spectral_alignment;

public class SpectralSimilarity {
    public final double similarity;
    public final int shardPeaks;

    public SpectralSimilarity(double similarity, int shardPeaks) {
        this.similarity = similarity;
        this.shardPeaks = shardPeaks;
    }

    @Override
    public String toString() {
        return "cosine = " + similarity + ", " + shardPeaks + " shared peaks.";}
}
