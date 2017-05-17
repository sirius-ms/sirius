package de.unijena.bioinf.sirius.gui.fingerid;

import ca.odell.glazedlists.swing.DefaultEventListModel;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.ActionListView;

import javax.swing.*;
import java.awt.*;

/**
 * Created by fleisch on 16.05.17.
 */
public class CandidateListView extends ActionListView<CandidateList> {

    public CandidateListView(CandidateList source) {
        super(source);

        /*final JList<SiriusResultElement> resultListView;
        resultListView = new JList<>(new DefaultEventListModel<>(source.getElementList()));
        resultListView.setCellRenderer();
        resultListView.setSelectionModel(source.getResultListSelectionModel());
//        resultListView.setLayoutOrientation(JList.HORIZONTAL_WRAP);
//        resultListView.setVisibleRowCount(1);
        resultListView.setPrototypeCellValue();
//        resultListView.setMinimumSize(new Dimension(0, 45));

        setLayout(new BorderLayout());


        JScrollPane listJSP = new JScrollPane(resultListView,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(listJSP, BorderLayout.CENTER);*/
    }
}
