package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import ca.odell.glazedlists.swing.DefaultEventListModel;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaTableCompactView extends FormulaTableView {
    public FormulaTableCompactView(FormulaTable source) {
        super(source);

        final JList<SiriusResultElement> resultListView;
        resultListView = new JList<>(new DefaultEventListModel<>(source.resultList));
        resultListView.setCellRenderer(source.cellRenderer);
        resultListView.setSelectionModel(source.selectionModel);
        resultListView.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        resultListView.setVisibleRowCount(1);
        resultListView.setMinimumSize(new Dimension(0, 45));
        resultListView.setPreferredSize(new Dimension(0, 45));

        JScrollPane listJSP = new JScrollPane(resultListView, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        setLayout(new BorderLayout());
        add(listJSP,BorderLayout.NORTH);
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(1, 5, 0, 0), "Molecular formulas"));


        resultListView.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click detected
                    int index = resultListView.locationToIndex(e.getPoint());
                    resultListView.setSelectedIndex(index);
                    SiriusActions.COMPUTE_CSI_LOCAL.getInstance().actionPerformed(new ActionEvent(resultListView, 112, SiriusActions.COMPUTE_CSI_LOCAL.name()));
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
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

    }
}
