package de.unijena.bioinf.ms.gui.mainframe.instance_panel;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
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
    final FilterList<InstanceBean> compoundList;
    final DefaultEventSelectionModel<InstanceBean> compountListSelectionModel;

    private final List<ExperimentListChangeListener> listeners = new LinkedList<>();

    public CompoundList(@NotNull final GuiProjectSpaceManager ps) {
        searchField = new SearchTextField();

        compoundList = new FilterList<>(new ObservableElementList<>(ps.INSTANCE_LIST, GlazedLists.beanConnector(InstanceBean.class)),
                new TextComponentMatcherEditor<>(searchField.textField, (baseList, element) -> {
                    baseList.add(element.getGUIName());
                    baseList.add(element.getIonization().toString());
                    baseList.add(String.valueOf(element.getIonMass()));
                }, true));


        compountListSelectionModel = new DefaultEventSelectionModel<>(compoundList);

        compountListSelectionModel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()){
                compountListSelectionModel.getDeselected().forEach(InstanceBean::unregisterProjectSpaceListeners);
                compountListSelectionModel.getSelected().forEach(InstanceBean::registerProjectSpaceListeners);
                notifyListenerSelectionChange();
            }
        });
        compoundList.addListEventListener(this::notifyListenerDataChange);
    }

    public void orderById() {
        compoundList.sort(Comparator.comparing(InstanceBean::getGUIName));
    }

    public void orderByMass() {
        compoundList.sort((o1, o2) -> {
            double mz1 = o1.getIonMass();
            if (mz1 <= 0 || Double.isNaN(mz1)) mz1 = Double.POSITIVE_INFINITY;
            double mz2 = o2.getIonMass();
            if (mz2 <= 0 || Double.isNaN(mz2)) mz2 = Double.POSITIVE_INFINITY;
            return Double.compare(mz1, mz2);
        });
    }

    private void notifyListenerDataChange(ListEvent<InstanceBean> event) {
        for (ExperimentListChangeListener l : listeners) {
            event.reset();//this is hell important to reset the iterator
            l.listChanged(event,compountListSelectionModel);
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
