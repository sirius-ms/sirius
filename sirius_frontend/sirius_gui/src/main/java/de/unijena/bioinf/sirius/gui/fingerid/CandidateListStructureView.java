package de.unijena.bioinf.sirius.gui.fingerid;

import ca.odell.glazedlists.*;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.table.ActionListView;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by fleisch on 24.05.17.
 */
public class CandidateListStructureView extends JPanel {

    public CandidateListStructureView(final DefaultEventSelectionModel<CompoundCandidate> selections) {
        setLayout(new BorderLayout());
        final ObservableElementList<CompoundCandidate> list = new ObservableElementList<CompoundCandidate>(new BasicEventList<CompoundCandidate>(), GlazedLists.beanConnector(CompoundCandidate.class));

        DefaultEventListModel<CompoundCandidate> model = new DefaultEventListModel<>(new SortedList<>(list));
        final JList<CompoundCandidate> resultListView =  new JList<>(model);


        resultListView.setCellRenderer(new CandidateStructureCellRenderer());
        resultListView.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        resultListView.setPrototypeCellValue(CompoundCandidate.PROTOTYPE);
        resultListView.setVisibleRowCount(1);

        final JScrollPane listJSP = new JScrollPane(resultListView, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(listJSP, BorderLayout.SOUTH);

        selections.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                System.out.println("select");
                list.clear();
                if (!selections.isSelectionEmpty()) {
                    list.addAll(selections.getSelected());
                    revalidate(); //todo I really do not no why i have to call that. horizontal list seemms to be buggy
                }
            }
        });
    }
}
