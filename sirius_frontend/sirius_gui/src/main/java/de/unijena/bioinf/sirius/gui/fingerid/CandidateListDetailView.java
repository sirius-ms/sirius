package de.unijena.bioinf.sirius.gui.fingerid;

import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateListDetailView extends ActionListView<CandidateList> implements ActiveElementChangedListener<SiriusResultElement, ExperimentContainer> {

    private final ActionTable<CompoundCandidate> table;
    private final JTextField searchField = new JTextField();

    public CandidateListDetailView(final CandidateList list) {
        super(list);
        final SortedList<CompoundCandidate> sorted = new SortedList<CompoundCandidate>(source.getElementList());
        final DefaultEventSelectionModel<CompoundCandidate> model = new DefaultEventSelectionModel<>(sorted);

        final CandidateTableFormat tf = new CandidateTableFormat();
        this.table = new ActionTable<>(sorted,
                tf,
                new StringMatcherEditor(tf, searchField));
        table.setSelectionModel(model);

        table.setDefaultRenderer(Object.class, new SiriusResultTableCellRenderer());

//        table.getColumnModel().getColumn(2).setCellRenderer(new BarTableCellRenderer(true, true, source.scoreStats));
//        table.getColumnModel().getColumn(3).setCellRenderer(new BarTableCellRenderer(false, false, source.isotopeScoreStats));
//        table.getColumnModel().getColumn(4).setCellRenderer(new BarTableCellRenderer(false, false, source.treeScoreStats));

        /*table.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click detected
                    int index = table.rowAtPoint(e.getPoint());
                    table.setRowSelectionInterval(index, index);
                    SiriusActions.COMPUTE_CSI_LOCAL.getInstance().actionPerformed(new ActionEvent(table, 112, SiriusActions.COMPUTE_CSI_LOCAL.name()));
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
        });*/

        // todo decoration

        this.add(
                new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER
        );

        this.add(createNorth(), BorderLayout.NORTH);


    }

    ///////////////// Internal //////////////////////////
    protected JPanel createNorth() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JPanel sp = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        sp.add(new JLabel("Filter"));
        sp.add(searchField);
        top.add(sp);
        return top;
    }

    @Override
    public void resultsChanged(ExperimentContainer experiment, SiriusResultElement sre, List<SiriusResultElement> resultElements, ListSelectionModel selections) {

    }
}
