package de.unijena.bioinf.ms.gui.mainframe.molecular_formular;


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

    @Override
    public void update(double[] values) {
        reset();
        if (values != null) {
            for (double score : values) {
                expScoreSum += Math.exp(score);
                scoreSum += score;

                minScoreValue = Math.min(minScoreValue, score);
                maxScoreValue = Math.max(maxScoreValue, score);
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        expScoreSum = 0;
    }
}
