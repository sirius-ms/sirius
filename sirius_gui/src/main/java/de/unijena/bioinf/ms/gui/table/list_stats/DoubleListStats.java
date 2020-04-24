package de.unijena.bioinf.ms.gui.table.list_stats;

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

    public DoubleListStats update(double[] values) {
        reset();
        if (values != null) {
            for (double score : values) {
                addValue(score);
            }
        }
        return this;
    }

    public DoubleListStats addValue(double score) {
        scoreSum += score;
        minScoreValue = Math.min(minScoreValue, score);
        maxScoreValue = Math.max(maxScoreValue, score);
        return this;
    }

    public DoubleListStats reset() {
        scoreSum = 0d;
        minScoreValue = Double.POSITIVE_INFINITY;
        maxScoreValue = Double.NEGATIVE_INFINITY;
        return this;
    }

    public DoubleListStats setMinScoreValue(double minScoreValue) {
        this.minScoreValue = minScoreValue;
        return this;
    }

    public DoubleListStats setMaxScoreValue(double maxScoreValue) {
        this.maxScoreValue = maxScoreValue;
        return this;
    }

    public DoubleListStats setScoreSum(double scoreSum) {
        this.scoreSum = scoreSum;
        return this;
    }
}
