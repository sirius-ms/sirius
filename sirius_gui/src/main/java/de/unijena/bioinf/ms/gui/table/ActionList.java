package de.unijena.bioinf.ms.gui.table;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.frontend.core.SiriusPCS;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by fleisch on 15.05.17.
 */
public abstract class ActionList<E extends SiriusPCS, D> implements ActiveElements<E, D> {
    public enum DataSelectionStrategy {ALL, FIRST_SELECTED, ALL_SELECTED}

    private final List<ActiveElementChangedListener> listeners = new LinkedList<>();

    protected ObservableElementList<E> elementList;
    protected DefaultEventSelectionModel<E> selectionModel;

    protected D data = null;
    public final DataSelectionStrategy selectionType;

    public ActionList(Class<E> cls) {
        this(cls, DataSelectionStrategy.FIRST_SELECTED);
    }

    public ActionList(Class<E> cls, DataSelectionStrategy strategy) {
        selectionType = strategy;
        elementList = new ObservableElementList<E>(new BasicEventList<E>(), GlazedLists.beanConnector(cls));
        selectionModel = new DefaultEventSelectionModel<>(elementList);
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);


        selectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!selectionModel.getValueIsAdjusting()) {
                    if (selectionModel.isSelectionEmpty() || elementList == null || elementList.isEmpty())
                        notifyListeners(data, null, elementList, selectionModel);
                    else
                        notifyListeners(data, elementList.get(selectionModel.getMinSelectionIndex()), elementList, selectionModel);
                }
            }
        });

        elementList.addListEventListener(new ListEventListener<E>() {
            @Override
            public void listChanged(ListEvent<E> listChanges) {
                if (!selectionModel.getValueIsAdjusting()) {
                    if (!selectionModel.isSelectionEmpty() && elementList != null && !elementList.isEmpty()) {
                        while (listChanges.next()) {
                            if (selectionModel.getMinSelectionIndex() == listChanges.getIndex()) {
                                notifyListeners(data, elementList.get(selectionModel.getMinSelectionIndex()), elementList, selectionModel);
                                return;
                            }
                        }
                    }
                }
            }
        });
    }

    public ObservableElementList<E> getElementList() {
        return elementList;
    }

    public DefaultEventSelectionModel<E> getResultListSelectionModel() {
        return selectionModel;
    }

    public void addActiveResultChangedListener(ActiveElementChangedListener listener) {
        listeners.add(listener);
    }

    public void removeActiveResultChangedListener(ActiveElementChangedListener listener) {
        listeners.remove(listener);
    }

    protected void notifyListeners(D data, E element, List<E> sre, ListSelectionModel selections) {
        for (ActiveElementChangedListener listener : listeners) {
            listener.resultsChanged(data, element, sre, selections);
        }
    }
}
