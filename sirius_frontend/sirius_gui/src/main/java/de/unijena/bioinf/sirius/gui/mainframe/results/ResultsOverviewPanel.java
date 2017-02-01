package de.unijena.bioinf.sirius.gui.mainframe.results;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 26.01.17.
 */

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
//todo this is beta state
public class ResultsOverviewPanel extends JPanel {
    public ResultsOverviewPanel(final JPanel north, final JPanel left, final int lIndex, final JPanel right, final int rIndex) {
        super(new BorderLayout());


        JSplitPane east = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        east.setDividerLocation(.5d);
        east.setResizeWeight(.5d);
        JSplitPane major = new JSplitPane(JSplitPane.VERTICAL_SPLIT, north, east);
        major.setDividerLocation(250);
        add(major, BorderLayout.CENTER);
    }


}
