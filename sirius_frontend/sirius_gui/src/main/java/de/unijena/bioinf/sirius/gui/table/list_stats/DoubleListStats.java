package de.unijena.bioinf.sirius.gui.table.list_stats;

/**
 * Created by fleisch on 12.05.17.
 */
public class DoubleListStats implements ListStats {
    protected double scoreSum;
    protected double minScoreValue;
    protected double maxScoreValue;

    public DoubleListStats(double[] values) {
        update(values);
    }

    public DoubleListStats() {
        reset();
    }

    @Override
    public double getMax() {
        return maxScoreValue;
    }

    @Override
    public double getMin() {
        return minScoreValue;
    }

    @Override
    public double getSum() {
        return scoreSum;
    }

    public void update(double[] values) {
        reset();
        if (values != null) {
            for (double score : values) {
                scoreSum += score;

                minScoreValue = Math.min(minScoreValue, score);
                maxScoreValue = Math.max(maxScoreValue, score);
            }
        }
    }

    public void reset() {
        scoreSum = 0;
        minScoreValue = Double.MAX_VALUE;
        maxScoreValue = Double.MIN_VALUE;
    }
}
