package de.unijena.bioinf.ms.gui.mainframe.molecular_formular;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 25.01.17.
 */

import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.ms.frontend.io.projectspace.FormulaResultBean;
import de.unijena.bioinf.ms.gui.table.SiriusTableFormat;
import de.unijena.bioinf.ms.gui.table.list_stats.ListStats;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;

/**
 * Display issues in a tabular form.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultTableFormat extends SiriusTableFormat<FormulaResultBean> {
    private static final int COL_COUNT = 9;

    protected SiriusResultTableFormat(ListStats stats) {
        super(stats);
    }


    @Override
    public int highlightColumnIndex() {
        return COL_COUNT;
    }

    @Override
    protected boolean isBest(FormulaResultBean element) {
        return stats.getMax() <= element.getScoreValue(SiriusScore.class);
    }

    @Override
    public int getColumnCount() {
        return COL_COUNT;
    }

    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Rank";
            case 1:
                return "Molecular Formula";
            case 2:
                return "Adduct";
            case 3:
                return "Zodiac Score";
            case 4:
                return "Sirius Score";
            case 5:
                return "Isotope Score";
            case 6:
                return "Tree Score";
            case 7:
                return "Explained Peaks";
            case 8:
                return "Total Explained Intensity";
            case 9:
                return "Median Absolute Mass Deviation in ppm";
            case 10:
                return "Best";
            default:
                throw new IllegalStateException();
        }
    }

    public Object getColumnValue(FormulaResultBean result, int column) {
        switch (column) {
            case 0:
                return result.getRank();
            case 1:
                return result.getMolecularFormula().toString();
            case 2:
                return result.getPrecursorIonType().toString();
            case 3:
                return result.getScoreValue(ZodiacScore.class);
            case 4:
                return result.getScoreValue(SiriusScore.class);
            case 5:
                return result.getScoreValue(IsotopeScore.class);
            case 6:
                return result.getScoreValue(TreeScore.class);
            case 7:
                final double expPeaks = result.getNumOfExplainedPeaks();
                if (Double.isNaN(expPeaks))
                    return "Value not found";
                else
                    return expPeaks;
            case 8:
                final double intensity = result.getExplainedIntensityRatio();
                if (Double.isNaN(intensity))
                    return "Value not found";
                else
                    return intensity;
            case 9:
                TreeNode visibleTreeRoot = result.getTreeVisualization();
                if (visibleTreeRoot != null && visibleTreeRoot.getMedianMassDeviation() != null)
                    return visibleTreeRoot.getMedianMassDeviation();
                else
                    return "Value not found";
            case 10:
                return isBest(result);
            default:
                throw new IllegalStateException();
        }
    }
}

