package de.unijena.bioinf.sirius.gui.mainframe.results;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 26.01.17.
 */

import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.mainframe.results.results_table.SiriusResultTablePanel;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ResultsOverviewPanel extends JSplitPane {
    private JList<ExperimentContainer> compoundList = new JList<>();
    public ResultsOverviewPanel(MainFrame owner) {
        super(JSplitPane.HORIZONTAL_SPLIT, true);
        compoundList = owner.compoundList;

        JPanel north = new SiriusResultTablePanel(compoundList);
        JPanel south = createSouthPanel();

    }

    private JPanel createSouthPanel() {
        JSplitPane s =  new JSplitPane(JSplitPane.VERTICAL_SPLIT,true);
        JPanel left =  new SpectraVisualizationPanel(compoundList.getSelectedValue());
        return null;
    }


}
