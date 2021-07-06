package de.unijena.bioinf.ms.gui.lcms_viewer;

import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ms.gui.configs.Colors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LCMSToolbar extends JToolBar {

    private LCMSViewerPanel panel;
    private JComboBox<Entry> box;

    public LCMSToolbar(LCMSViewerPanel lcmsViewerPanel) {
        this.panel = lcmsViewerPanel;
        box = new JComboBox<>();
        JLabel label = new JLabel();
        box.setRenderer(new ListCellRenderer<Entry>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends Entry> list, Entry value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value==null) return label;
                label.setText(value.toString());
                Color forc, backc;
                if (isSelected) {
                    backc = Colors.LIST_SELECTED_BACKGROUND;
                    forc = Colors.LIST_ACTIVATED_FOREGROUND;
                } else {
                    if (index % 2 == 0) backc = Colors.LIST_EVEN_BACKGROUND;
                    else backc= Colors.LIST_UNEVEN_BACKGROUND;
                    forc = Colors.LIST_ACTIVATED_FOREGROUND;
                }
                label.setBackground(backc);
                label.setForeground(forc);
                return label;
            }
        });
        box.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                final Entry selectedItem = (Entry)box.getSelectedItem();
                if (selectedItem!=null) panel.setActiveIndex(selectedItem.id);
            }
        });
        add(box);
    }

    public void reloadContent(LCMSPeakInformation peakInformation) {
        List<Entry> entries = new ArrayList<>();
        for (int i=0, n=peakInformation.length(); i < n; ++i) {
            entries.add(new Entry(peakInformation.getNameFor(i),
                    peakInformation.getTracesFor(i).get().getIonTrace().getMonoisotopicPeak().getApexIntensity(),i));
        }
        float mx=0f;
        for (Entry e : entries) mx = Math.max(mx,e.intensity);
        for (Entry e : entries) e.intensity /= mx;
        box.removeAllItems();
        for (Entry e : entries) {
            box.addItem(e);
        }
    }

    protected static class Entry {
        private String name;
        private float intensity;
        private int id;

        public Entry(String name, float intensity, int id) {
            this.name = name;
            this.intensity = intensity;
            this.id = id;
        }

        @Override
        public String toString() {
            if (intensity > 0.1) {
                return String.format(Locale.US, "<html>%s <b>%d %%</b></html>", name, Math.round(100*intensity));
            } else {
                return String.format(Locale.US, "%s %d %%", name, Math.round(100*intensity));
            }
        }
    }
}
