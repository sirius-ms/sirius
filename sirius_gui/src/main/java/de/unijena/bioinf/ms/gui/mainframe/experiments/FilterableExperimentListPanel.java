package de.unijena.bioinf.ms.gui.mainframe.experiments;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 01.02.17.
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FilterableExperimentListPanel extends JPanel {
    public FilterableExperimentListPanel(ExperimentListView view) {
        super(new BorderLayout());
        add(view.sourceList.searchField,BorderLayout.NORTH);
        add(view,BorderLayout.CENTER);
        setBorder(new EmptyBorder(0, 0, 0, 0));
    }
}
