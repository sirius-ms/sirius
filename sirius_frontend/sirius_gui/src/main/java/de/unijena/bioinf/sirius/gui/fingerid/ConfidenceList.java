package de.unijena.bioinf.sirius.gui.fingerid;

import ca.odell.glazedlists.*;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.Matchers;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.sirius.gui.compute.CompoundModel;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.*;
import java.util.List;

public class ConfidenceList extends JList<ExperimentContainer> {
    public final static ConfidenceComparator COMPARATOR =  new ConfidenceComparator();
    protected SortedList<ExperimentContainer> results;

    public ConfidenceList(EventList<ExperimentContainer> source) {
        super();

        results = new SortedList<>(new FilterList<>(
                        new ObservableElementList<>(source,
                            GlazedLists.beanConnector(ExperimentContainer.class)), new ConfidenceMatcher()));
        results.setComparator(COMPARATOR);
        results.setMode(SortedList.STRICT_SORT_ORDER);
        setModel(new DefaultEventListModel<>(results));
        setCellRenderer(new IdentificationCellRenderer());
    }

    protected static class ConfidenceMatcher implements Matcher<ExperimentContainer>{
        @Override
        public boolean matches(ExperimentContainer ec) {
            return ec.getBestHit()!=null && ec.getBestHit().getFingerIdData()!=null && ec.getBestHit().fingerIdComputeState == ComputingStatus.COMPUTED && !Double.isNaN(ec.getBestHit().getFingerIdData().getConfidence());
        }
    }

    protected static class ConfidenceComparator implements Comparator<ExperimentContainer>{
        @Override
        public int compare(ExperimentContainer o1, ExperimentContainer o2) {
            return Double.compare(o1.getBestHit().getFingerIdData().getConfidence(),o2.getBestHit().getFingerIdData().getConfidence());
        }
    }
}
