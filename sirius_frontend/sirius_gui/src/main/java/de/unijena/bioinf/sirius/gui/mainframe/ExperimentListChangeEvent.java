package de.unijena.bioinf.sirius.gui.mainframe;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 26.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import javax.swing.*;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ExperimentListChangeEvent {
    public final static int DELETE = ListEvent.DELETE;
    public final static int UPDATE = ListEvent.UPDATE;
    public final static int INSERT = ListEvent.INSERT;
    public final static int SELECTION = 3;
    public final static int DELETE_SELECTED = ListEvent.DELETE + SELECTION;
    public final static int UPDATE_SELECTED = ListEvent.UPDATE + SELECTION;
    public final static int INSERT_SELECTED = ListEvent.INSERT + SELECTION;

    protected final JList<ExperimentContainer> sourceList;
    public final ListEvent<ExperimentContainer> changes;
    public final TIntSet types;

    public ExperimentListChangeEvent(JList<ExperimentContainer> sourceList) {
        this.sourceList = sourceList;
        types = new TIntHashSet();
        types.add(SELECTION);
        changes = null;
    }

    public ExperimentListChangeEvent(JList<ExperimentContainer> sourceList, ListEvent<ExperimentContainer> changes) {
        this.sourceList = sourceList;
        types = new TIntHashSet();
        types.add(DELETE);
        types.add(UPDATE);
        types.add(INSERT);
        if (sourceList.getMaxSelectionIndex() > sourceList.getModel().getSize())
            if (sourceList.getMinSelectionIndex() > sourceList.getModel().getSize())
                sourceList.clearSelection();//todo this is not save rework on this topic
            else
                sourceList.setSelectedIndex(sourceList.getMinSelectionIndex());

        this.changes = changes;
    }

    public List<ExperimentContainer> getSelected() {
        return sourceList.getSelectedValuesList();
    }

    public int[] getSelectedIndeces() {
        return sourceList.getSelectedIndices();
    }

    public int getFirstSelectionIndex() {
        return sourceList.getSelectedIndex();
    }

    public ExperimentContainer getFirstSelectionValue() {
        return sourceList.getSelectedValue();
    }

    public boolean isSelected(int i) {
        return sourceList.isSelectedIndex(i);
    }

}
