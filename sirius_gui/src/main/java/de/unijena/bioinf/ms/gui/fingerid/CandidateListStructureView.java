package de.unijena.bioinf.ms.gui.fingerid;

import ca.odell.glazedlists.*;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

/**
 * Created by fleisch on 24.05.17.
 */
public class CandidateListStructureView extends JPanel {

    public CandidateListStructureView(final DefaultEventSelectionModel<FingerprintCandidateBean> selections) {
        setLayout(new BorderLayout());
        final ObservableElementList<FingerprintCandidateBean> list = new ObservableElementList<FingerprintCandidateBean>(new BasicEventList<FingerprintCandidateBean>(), GlazedLists.beanConnector(FingerprintCandidateBean.class));

        DefaultEventListModel<FingerprintCandidateBean> model = new DefaultEventListModel<>(new SortedList<>(list));
        final JList<FingerprintCandidateBean> resultListView =  new JList<>(model);


        resultListView.setCellRenderer(new CandidateStructureCellRenderer());
        resultListView.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        resultListView.setPrototypeCellValue(FingerprintCandidateBean.PROTOTYPE);
        resultListView.setVisibleRowCount(1);

        final JScrollPane listJSP = new JScrollPane(resultListView, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(listJSP, BorderLayout.SOUTH);

        selections.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                list.clear();
                if (!selections.isSelectionEmpty()) {
                    list.addAll(selections.getSelected());
                    revalidate(); //Fleisch really does not no why he has to call that. Horizontal list seemms to be buggy
                }
            }
        });
    }
}
