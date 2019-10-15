package de.unijena.bioinf.ms.gui.mainframe.experiments;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.ms.frontend.io.projectspace.GuiProjectSpace;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;
import de.unijena.bioinf.ms.gui.utils.SearchTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class CompoundList {

    final SearchTextField searchField;
    final FilterList<InstanceBean> compoundList;
    final DefaultEventSelectionModel<InstanceBean> compountListSelectionModel;

    private final List<ExperimentListChangeListener> listeners = new LinkedList<>();

    public CompoundList(@NotNull final GuiProjectSpace ps) {
        searchField = new SearchTextField();


        compoundList = new FilterList<>(new ObservableElementList<>(ps.COMPOUNT_LIST, GlazedLists.beanConnector(InstanceBean.class)),
                new TextComponentMatcherEditor<>(searchField.textField, new TextFilterator<InstanceBean>() {
                    @Override
                    public void getFilterStrings(List<String> baseList, InstanceBean element) {
                        baseList.add(element.getGUIName());
                        baseList.add(element.getIonization().toString());
                        baseList.add(String.valueOf(element.getIonMass()));
                    }
                }, true));


        compountListSelectionModel = new DefaultEventSelectionModel<>(compoundList);

        compountListSelectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    notifyListenerSelectionChange();
                }
            }
        });
        compoundList.addListEventListener(new ListEventListener<InstanceBean>() {
            @Override
            public void listChanged(final ListEvent<InstanceBean> listChanges) {
                notifyListenerDataChange(listChanges);
            }
        });
    }

    public void orderById() {
        Collections.sort(compoundList, new Comparator<InstanceBean>() {
            @Override
            public int compare(InstanceBean o1, InstanceBean o2) {
                return o1.getGUIName().compareTo(o2.getGUIName());
            }
        });
    }

    public void orderByMass() {
        Collections.sort(compoundList, new Comparator<InstanceBean>() {
            @Override
            public int compare(InstanceBean o1, InstanceBean o2) {
                double mz1 = o1.getIonMass();
                if (mz1 <= 0 || Double.isNaN(mz1)) mz1 = Double.POSITIVE_INFINITY;
                double mz2 = o2.getIonMass();
                if (mz2 <= 0 || Double.isNaN(mz2)) mz2 = Double.POSITIVE_INFINITY;
                return Double.compare(mz1, mz2);
            }
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
