package de.unijena.bioinf.ms.gui.mainframe.experiments;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 26.01.17.
 */

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.ms.io.projectspace.GuiProjectSpace;
import de.unijena.bioinf.ms.gui.sirius.ExperimentResultBean;
import de.unijena.bioinf.ms.gui.utils.SearchTextField;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ExperimentList {

    final SearchTextField searchField;
    final FilterList<ExperimentResultBean> compoundList;
    final DefaultEventSelectionModel<ExperimentResultBean> compountListSelectionModel;

    private final List<ExperimentListChangeListener> listeners = new LinkedList<>();

    public ExperimentList() {
        searchField = new SearchTextField();


        compoundList = new FilterList<>(new ObservableElementList<>(GuiProjectSpace.PS.COMPOUNT_LIST, GlazedLists.beanConnector(ExperimentResultBean.class)),
                new TextComponentMatcherEditor<>(searchField.textField, new TextFilterator<ExperimentResultBean>() {
                    @Override
                    public void getFilterStrings(List<String> baseList, ExperimentResultBean element) {
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
        compoundList.addListEventListener(new ListEventListener<ExperimentResultBean>() {
            @Override
            public void listChanged(final ListEvent<ExperimentResultBean> listChanges) {
                notifyListenerDataChange(listChanges);
            }
        });
    }

    public void orderById() {
        Collections.sort(compoundList, new Comparator<ExperimentResultBean>() {
            @Override
            public int compare(ExperimentResultBean o1, ExperimentResultBean o2) {
                return o1.getGUIName().compareTo(o2.getGUIName());
            }
        });
    }

    public void orderByMass() {
        Collections.sort(compoundList, new Comparator<ExperimentResultBean>() {
            @Override
            public int compare(ExperimentResultBean o1, ExperimentResultBean o2) {
                double mz1 = o1.getIonMass();
                if (mz1 <= 0 || Double.isNaN(mz1)) mz1 = Double.POSITIVE_INFINITY;
                double mz2 = o2.getIonMass();
                if (mz2 <= 0 || Double.isNaN(mz2)) mz2 = Double.POSITIVE_INFINITY;
                return Double.compare(mz1, mz2);
            }
        });
    }

    private void notifyListenerDataChange(ListEvent<ExperimentResultBean> event) {
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

    public DefaultEventSelectionModel<ExperimentResultBean> getCompoundListSelectionModel() {
        return compountListSelectionModel;
    }

    public FilterList<ExperimentResultBean> getCompoundList() {
        return compoundList;
    }


}
