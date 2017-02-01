package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import ca.odell.glazedlists.swing.DefaultEventListModel;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.mainframe.results.ResultTreeListTextCellRenderer;
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
        resultListView.setPrototypeCellValue(ResultTreeListTextCellRenderer.PROTOTYPE);
        resultListView.setMinimumSize(new Dimension(0, 45));

        setLayout(new BorderLayout());


        JScrollPane listJSP = new JScrollPane(resultListView,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(listJSP, BorderLayout.NORTH);






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

        //decorate this guy
        KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
        resultListView.getInputMap().put(enterKey, SiriusActions.COMPUTE_CSI_LOCAL.name());
        resultListView.getActionMap().put(SiriusActions.COMPUTE_CSI_LOCAL.name(),SiriusActions.COMPUTE_CSI_LOCAL.getInstance());
    }
}
