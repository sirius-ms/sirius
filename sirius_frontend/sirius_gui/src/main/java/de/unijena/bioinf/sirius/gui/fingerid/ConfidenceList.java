package de.unijena.bioinf.sirius.gui.fingerid;

import ca.odell.glazedlists.EventList;
import de.unijena.bioinf.sirius.gui.compute.CompoundModel;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ConfidenceList extends JPanel implements ListSelectionListener {

    protected JList<ExperimentContainer> results;
    protected MainFrame owner;

    public ConfidenceList(MainFrame owner) {
        this.results = new JList<>(new ListModel(owner.getCompoundsList()));
        this.owner = owner;
        results.setCellRenderer(new IdentificationCellRenderer());

        results.addListSelectionListener(this);
        setLayout(new BorderLayout());

        add(results, BorderLayout.CENTER);
    }

    public void refreshList() {
        ((ListModel)results.getModel()).refreshInnerList();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        final ExperimentContainer container = results.getSelectedValue();
        owner.selectExperimentContainer(container, container.getBestHit());
    }

    protected class ListModel extends AbstractListModel<ExperimentContainer> {

        EventList<ExperimentContainer> outerList;
        List<ExperimentContainer> innerList;

        public ListModel(EventList<ExperimentContainer> outerList) {
            this.outerList = outerList;
            this.innerList = new ArrayList<>(outerList.size());
            refreshInnerList();
        }

        protected void refreshInnerList() {
            innerList.clear();
            Iterator<ExperimentContainer> en = outerList.iterator();
            while (en.hasNext()) {
                ExperimentContainer ec = en.next();
                if (ec.getBestHit()!=null && ec.getBestHit().getFingerIdData()!=null && !Double.isNaN(ec.getBestHit().getFingerIdData().confidence)) {
                    innerList.add(ec);
                }
            }
            Collections.sort(innerList, new Comparator<ExperimentContainer>() {
                @Override
                public int compare(ExperimentContainer o1, ExperimentContainer o2) {
                    return Double.compare(o2.getBestHit().getFingerIdData().confidence, o1.getBestHit().getFingerIdData().confidence);
                }
            });
            fireContentsChanged(this, 0, innerList.size());
        }

        @Override
        public int getSize() {
            return innerList.size();
        }

        @Override
        public ExperimentContainer getElementAt(int index) {
            return innerList.get(index);
        }
    }
}
