package de.unijena.bioinf.ms.gui.mainframe.instance_panel;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.ms.gui.utils.SearchTextField;
import de.unijena.bioinf.projectspace.GuiProjectSpaceManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * This is the main List of the SIRIUS UI.
 * It shows the main Instances (former Compounds or Experiments)
 * It is usually a singleton and backed by the INSTANCE_LIST of the  {@link GuiProjectSpaceManager}
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class CompoundList {

    final SearchTextField searchField;
    final SortedList<InstanceBean> sortedScource;
    final FilterList<InstanceBean> compoundList;
    final DefaultEventSelectionModel<InstanceBean> compountListSelectionModel;

    private final List<ExperimentListChangeListener> listeners = new LinkedList<>();

    public CompoundList(@NotNull final GuiProjectSpaceManager ps) {
        searchField = new SearchTextField();

        sortedScource = new SortedList<>(new ObservableElementList<>(ps.INSTANCE_LIST, GlazedLists.beanConnector(InstanceBean.class)), Comparator.comparing(b -> b.getID().getCompoundIndex()));
        compoundList = new FilterList<>(sortedScource,
                new TextComponentMatcherEditor<>(searchField.textField, (baseList, element) -> {
                    baseList.add(element.getGUIName());
                    baseList.add(element.getIonization().toString());
                    baseList.add(String.valueOf(element.getIonMass()));
                }, true));


        compountListSelectionModel = new DefaultEventSelectionModel<>(compoundList);

        compountListSelectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                compountListSelectionModel.getDeselected().forEach(InstanceBean::unregisterProjectSpaceListeners);
                compountListSelectionModel.getSelected().forEach(InstanceBean::registerProjectSpaceListeners);
                notifyListenerSelectionChange();
            }
        });
        compoundList.addListEventListener(this::notifyListenerDataChange);
    }

    public void orderBy(@NotNull final Comparator<InstanceBean> comp) {
        sortedScource.setComparator(comp);
    }

    private void notifyListenerDataChange(ListEvent<InstanceBean> event) {
        for (ExperimentListChangeListener l : listeners) {
            event.reset();//this is hell important to reset the iterator
            l.listChanged(event, compountListSelectionModel);
        }
    }

    private void notifyListenerSelectionChange() {
        for (ExperimentListChangeListener l : listeners) {
            l.listSelectionChanged(compountListSelectionModel);
        }
    }

    //API methods
    public void addChangeListener(ExperimentListChangeListener l) {
        listeners.add(l);
    }

    public void removeChangeListener(ExperimentListChangeListener l) {
        listeners.remove(l);
    }

    public DefaultEventSelectionModel<InstanceBean> getCompoundListSelectionModel() {
        return compountListSelectionModel;
    }

    public FilterList<InstanceBean> getCompoundList() {
        return compoundList;
    }
}
