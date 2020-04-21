package de.unijena.bioinf.ms.gui.molecular_formular;


import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;

/**
 * Created by fleisch on 12.05.17.
 */
public class FormulaScoreListStats extends DoubleListStats {
    private double expScoreSum;

    public FormulaScoreListStats(double[] values) {
        super(values);
    }

    public FormulaScoreListStats() {
        super();
    }

    public double getExpScoreSum() {
        return expScoreSum;
    }

    public double getExpMaxScore() {
        return Math.exp(getMax() - getMax());
    }

    public double getExpMinScore() {
        return Math.exp(getMin() - getMax());
    }

    @Override
    public void update(double[] values) {
        super.update(values);
        if (values != null)
            for (double value : values)
                expScoreSum += Math.exp(value - getMax());
    }

    @Override
    public void reset() {
        super.reset();
        expScoreSum = 0;
    }
}
