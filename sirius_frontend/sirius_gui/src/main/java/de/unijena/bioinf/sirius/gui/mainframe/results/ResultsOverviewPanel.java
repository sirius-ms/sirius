package de.unijena.bioinf.sirius.gui.mainframe.results;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 26.01.17.
 */

import de.unijena.bioinf.sirius.gui.mainframe.results.results_table.SiriusResultTablePanel;

import javax.swing.*;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ResultsOverviewPanel extends JSplitPane {
    public ResultsOverviewPanel(final ResultPanel owner, final JComponent left, final int lIndex, final JComponent right, final int rIndex) {
        super(JSplitPane.VERTICAL_SPLIT, true);

        JPanel north = new SiriusResultTablePanel(MF.getCompountListPanel());

        JSplitPane south = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);

        left.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                owner.setSelectedIndex(lIndex);
            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        right.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                owner.setSelectedIndex(rIndex);
            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        south.add(left);
        south.add(right);

        add(north);
        add(south);


    }


}
