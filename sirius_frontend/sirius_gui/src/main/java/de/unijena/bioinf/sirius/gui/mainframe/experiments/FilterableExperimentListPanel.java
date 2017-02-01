package de.unijena.bioinf.sirius.gui.mainframe.experiments;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 01.02.17.
 */

import de.unijena.bioinf.sirius.gui.settings.TwoCloumnPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FilterableExperimentListPanel extends TwoCloumnPanel {
    public FilterableExperimentListPanel(ExperimentListView view) {
        add(new JLabel(" Filter:"), view.sourceList.searchField);
        add(view, 0, true);
        setBorder(new EmptyBorder(0, 0, 0, 0));
    }
}
