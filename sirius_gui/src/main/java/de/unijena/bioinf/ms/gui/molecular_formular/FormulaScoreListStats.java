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
    public FormulaScoreListStats update(double[] values) {
        super.update(values);
        if (values != null)
            for (double value : values)
                expScoreSum += Math.exp(value - getMax());
        return this;
    }

    @Override
    public FormulaScoreListStats reset() {
        super.reset();
        expScoreSum = 0;
        return this;
    }
}
