package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.chemdb.custom.CustomDataSourceService;
import de.unijena.bioinf.ms.frontend.io.projectspace.FormulaResultBean;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.WrapLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DBFilterPanel extends JPanel implements ActiveElementChangedListener<FingerprintCandidateBean, Set<FormulaResultBean>>, CustomDataSourceService.DataSourceChangeListener {
    private final List<FilterChangeListener> listeners = new LinkedList<>();

    protected long bitSet;
    protected List<JCheckBox> checkboxes;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    public DBFilterPanel(CandidateList sourceList) {
        setLayout(new WrapLayout(FlowLayout.LEFT, 5, 1));
        this.checkboxes = new ArrayList<>(CustomDataSourceService.size());
        for (CustomDataSourceService.Source source : CustomDataSourceService.sources()) {
            checkboxes.add(new JCheckBox(source.name()));
        }
        addBoxes();
        CustomDataSourceService.addListener(this);
        sourceList.addActiveResultChangedListener(this);
    }

    public void addFilterChangeListener(FilterChangeListener listener) {
        listeners.add(listener);
    }

    public void fireFilterChangeEvent() {
        for (FilterChangeListener listener : listeners) {
            listener.fireFilterChanged(bitSet);
        }
    }

    protected void addBoxes() {
        Collections.sort(checkboxes, new Comparator<JCheckBox>() {
            @Override
            public int compare(JCheckBox o1, JCheckBox o2) {
                return o1.getText().toUpperCase().compareTo(o2.getText().toUpperCase());
            }
        });

        this.bitSet = 0L;
        for (final JCheckBox box : checkboxes) {
            if (box.isSelected())
                this.bitSet |= CustomDataSourceService.getSourceFromName(box.getText()).flag();
            add(box);
            box.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (!isRefreshing.get()) {
                        if (box.isSelected())
                            bitSet |= CustomDataSourceService.getSourceFromName(box.getText()).flag();
                        else
                            bitSet &= ~CustomDataSourceService.getSourceFromName(box.getText()).flag();
                        fireFilterChangeEvent();
                    }
                }
            });
        }
    }

    protected void reset() {
        isRefreshing.set(true);
        bitSet = 0;
        try {
            for (JCheckBox checkbox : checkboxes) {
                checkbox.setSelected(false);
            }
        } finally {
            fireFilterChangeEvent();
            isRefreshing.set(false);
        }
    }

    public boolean toggle() {
        setVisible(!isVisible());
        return isVisible();
    }

    @Override
    public void resultsChanged(Set<FormulaResultBean> datas, FingerprintCandidateBean sre, List<FingerprintCandidateBean> resultElements, ListSelectionModel selections) {
        reset();
    }

    @Override
    public void fireDataSourceChanged(Collection<String> changes) {
        HashSet<String> changed = new HashSet<>(changes);
        isRefreshing.set(true);
        boolean c = false;
        Iterator<JCheckBox> it = checkboxes.iterator();

        while (it.hasNext()) {
            JCheckBox checkbox = it.next();
            if (changed.remove(checkbox.getText())) {
                it.remove();
                c = true;
            }
        }

        for (String name : changed) {
            checkboxes.add(new JCheckBox(name));
            c = true;
        }

        if (c) {
            removeAll();
            addBoxes();
            revalidate();
            repaint();
            fireFilterChangeEvent();
        }


        isRefreshing.set(false);
    }

    public interface FilterChangeListener extends EventListener {
        void fireFilterChanged(long filterSet);
    }
}
