package de.unijena.bioinf.sirius.gui.mainframe.molecular_formular;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 25.01.17.
 */

import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.SiriusTableFormat;
import de.unijena.bioinf.sirius.gui.table.list_stats.ListStats;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Display issues in a tabular form.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultTableFormat extends SiriusTableFormat<SiriusResultElement> {
    private static final int COL_COUNT = 8;

    protected SiriusResultTableFormat(ListStats stats) {
        super(stats);
    }


    @Override
    public int highlightColumnIndex() {
        return COL_COUNT;
    }

    @Override
    protected boolean isBest(SiriusResultElement element) {
        return stats.getMax() <= element.getScore();
    }

    @Override
    public int getColumnCount() {
        return COL_COUNT;
    }

    public String getColumnName(int column) {
        switch(column) {
            case 0:
                return "Rank";
            case 1:
                return "Molecular Formula";
            case 2:
                return "Score";
            case 3:
                return "Isotope Score";
            case 4:
                return "Tree Score";
            case 5:
                return "No. nodes";
            case 6:
                return "Total Explained Intensity";
            case 7:
                return "Median Absolute Mass Deviation";
            default:
                throw new IllegalStateException();
        }
    }

    public Object getColumnValue(SiriusResultElement result, int column) {
        switch (column) {
            case 0:
                return result.getRank();
            case 1:
                return result.getFormulaAndIonText();
            case 2:
                return result.getScore();
            case 3:
                return result.getResult().getIsotopeScore();
            case 4:
                return result.getResult().getTreeScore();
            case 5:
                return result.getResult().getBeautifulTree().getFragments().size();
            case 6:
                int treeIntensity = 0;
                TreeScoring treeScoring = result.getResult().getBeautifulTree().getAnnotationOrNull(TreeScoring.class);
                if(treeScoring != null)
                    return treeScoring.getExplainedIntensity();
                else
                    return "value not found";
            case 7:
                ArrayList<Double> massDiviatons = new ArrayList<Double>();

                TreeScoring treeScoring = result.getResult().getBeautifulTree().getAnnotationOrNull(TreeScoring.class);
                if(treeScoring != null)
                    return treeScoring.getExplainedIntensity();
                else
                    return "value not found";
            case 8:
                return isBest(result);
            default:
                throw new IllegalStateException();
        }
    }


}

