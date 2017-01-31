package de.unijena.bioinf.sirius.gui.mainframe.results;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 26.01.17.
 */

import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.FormulaTableDetailView;
import de.unijena.bioinf.sirius.gui.mainframe.results.results_table.SiriusResultTablePanel;

import javax.swing.*;
import java.awt.*;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ResultsOverviewPanel extends JPanel {
    public ResultsOverviewPanel(final JPanel north, final JPanel left, final int lIndex, final JPanel right, final int rIndex) {
        super(new BorderLayout());


        JSplitPane east =  new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,left,right);
        east.setDividerLocation(.5d);
        east.setResizeWeight(.5d);
        JSplitPane major =  new JSplitPane(JSplitPane.VERTICAL_SPLIT,north,east);
        major.setDividerLocation(250);
        add(major,BorderLayout.CENTER);




// setResizeWeight(.5d);
//        JPanel north = new SiriusResultTablePanel(MF.getCompountListPanel());
//        north.setMinimumSize(new Dimension(400,north.getMinimumSize().height));


//        JPanel south = new JPanel(new BorderLayout());

//        left.setPreferredSize();
//        right.setPreferredSize(new Dimension(150,500));

        /*left.addMouseListener(new MouseListener() {
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
*/
//        south.add(left, BorderLayout.WEST);
//        south.add(right, BorderLayout.EAST);
//
//        add(north, BorderLayout.NORTH);
//        add(south, BorderLayout.SOUTH);


    }


}
