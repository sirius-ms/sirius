package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.*;
import java.util.List;

public class DBFilterPanel extends JPanel implements ActiveElementChangedListener<CompoundCandidate, Set<FingerIdData>> {
    private final List<FilterChangeListener> listeners = new LinkedList<>();

    protected DatasourceService.Sources[] sources;
    protected JCheckBox[] checkboxes;

    private boolean isRefreshing = false;
    private EnumSet<DatasourceService.Sources> dbs = EnumSet.of(DatasourceService.Sources.PUBCHEM);

    public DBFilterPanel(CandidateList sourceList) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

        final DatasourceService.Sources[] names = DatasourceService.Sources.values();
        Arrays.sort(names, new Comparator<DatasourceService.Sources>() {
            @Override
            public int compare(DatasourceService.Sources o1, DatasourceService.Sources o2) {
                return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
            }
        });

        this.sources = names;
        this.checkboxes = new JCheckBox[names.length];

        for (int k = 0; k < sources.length; ++k) {
            final JCheckBox box = new JCheckBox(sources[k].name);
            final DatasourceService.Sources source = sources[k];
            checkboxes[k] = box;
            add(box);
            if (source == DatasourceService.Sources.PUBCHEM) box.setSelected(true);

            box.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (!isRefreshing) {
                        if (box.isSelected())
                            dbs.add(source);
                        else
                            dbs.remove(source);
                        fireFilterChangeEvent();
                    }
                }
            });
        }

        sourceList.addActiveResultChangedListener(this);
    }

    public void addFilterChangeListener(FilterChangeListener listener) {
        listeners.add(listener);
    }

    public void fireFilterChangeEvent() {
        for (FilterChangeListener listener : listeners) {
            listener.fireFilterChanged(dbs);
        }
    }

    protected void refreshSelection(EnumSet<DatasourceService.Sources> sel) {
        isRefreshing = true;
        dbs = sel;
        try {
            for (int k = 0; k < sources.length; ++k) {
                checkboxes[k].setSelected(dbs.contains(sources[k]));
            }

        } finally {
            fireFilterChangeEvent();
            isRefreshing = false;
        }
    }

    public boolean toggle() {
        setVisible(!isVisible());
        return isVisible();
    }

    @Override
    public void resultsChanged(Set<FingerIdData> datas, CompoundCandidate sre, List<CompoundCandidate> resultElements, ListSelectionModel selections) {
        refreshSelection(EnumSet.of(DatasourceService.Sources.PUBCHEM));
    }

    public interface FilterChangeListener extends EventListener {
        void fireFilterChanged(EnumSet<DatasourceService.Sources> filterSet);
    }
}
