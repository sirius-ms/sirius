package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.chemdb.DatasourceService;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;

public class FilterPanel extends JPanel {

    protected DatasourceService.Sources[] sources;
    protected JCheckBox[] checkboxes;
    protected FingerIdData active;
    protected Runnable filterChangedEvent;

    public FilterPanel() {

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

        for (int k=0; k < sources.length; ++k) {
            checkboxes[k] = new JCheckBox(sources[k].name);
            add(checkboxes[k]);
            if (sources[k] == DatasourceService.Sources.PUBCHEM) checkboxes[k].setSelected(true);
            final int K = k;
            checkboxes[k].addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (active==null) return;
                    if (checkboxes[K].isSelected()) active.dbSelection.add(sources[K]);
                    else active.dbSelection.remove(sources[K]);
                    filterChangedEvent.run();
                }
            });
        }
    }

    public void whenFilterChanges(Runnable runnable) {
        this.filterChangedEvent = runnable;
    }

    public void setActiveExperiment(FingerIdData data) {
        active = data;
        if (active!=null) setSelection(data.dbSelection);
    }

    protected void setSelection(EnumSet<DatasourceService.Sources> sel) {
            for (int k=0; k < sources.length; ++k) {
            checkboxes[k].setSelected(sel.contains(sources[k]));
        }
        filterChangedEvent.run();
    }

    public boolean toggle() {
        setVisible(!isVisible());
        return isVisible();
    }
}
