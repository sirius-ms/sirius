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

    public void update(double[] values) {
        reset();
        if (values != null) {
            for (double score : values) {
                addValue(score);
            }
        }
    }

    public void addValue(double score) {
        scoreSum += score;
        minScoreValue = Math.min(minScoreValue, score);
        maxScoreValue = Math.max(maxScoreValue, score);
    }

    public void reset() {
        scoreSum = 0d;
        minScoreValue = Double.POSITIVE_INFINITY;
        maxScoreValue = Double.NEGATIVE_INFINITY;
    }

    public void setMinScoreValue(double minScoreValue) {
        this.minScoreValue = minScoreValue;
    }

    public void setMaxScoreValue(double maxScoreValue) {
        this.maxScoreValue = maxScoreValue;
    }

    public void setScoreSum(double scoreSum) {
        this.scoreSum = scoreSum;
    }
}
