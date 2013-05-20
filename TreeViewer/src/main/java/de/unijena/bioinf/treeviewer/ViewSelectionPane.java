package de.unijena.bioinf.treeviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class ViewSelectionPane extends JPanel {

    private final Profile profile;

    public ViewSelectionPane(Profile profileP, final Runnable refreshExec) {
        this.profile = profileP;
        setLayout(new BorderLayout());
        if (profile != null) {
            final String[] names = new String[profile.getHeats().size()+1];
            names[0] = "none";
            int k=1;
            for (Heat h : profile.getHeats()) {
                names[k++] = h.getName();
            }
            final JComboBox<String> heats = new JComboBox<String>(names);
            heats.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final int index = heats.getSelectedIndex();
                    final Heat heat = (index == 0) ? null : profile.getHeats().get(index-1);
                    if (heat != profile.getActiveHeat()) {
                        profile.setActiveHeat(heat);
                        new Thread(refreshExec).start();
                    }
                }
            });
            final int i = (profile.getActiveHeat() == null ? 0 : profile.getHeats().indexOf(profile.getActiveHeat())+1);
            heats.setSelectedIndex(i);
            add(heats, BorderLayout.NORTH);
            final JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            final JPanel panel2 = new JPanel();
            panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
            panel.add(panel2);
            for (Property p : profile.getProperties()) {
                final JCheckBox check = new JCheckBox();
                panel2.add(new Checkbox(p, refreshExec), BorderLayout.WEST);
            }
            add(new JScrollPane(panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),BorderLayout.CENTER);
        }
    }

    private static class Checkbox extends JCheckBox implements ItemListener {

        private Property property;
        private Runnable refreshExec;

        private Checkbox(Property prop, Runnable refreshExec) {
            super(prop.getName());
            this.property = prop;
            this.refreshExec = refreshExec;
            this.setSelected(true);
            addItemListener(this);
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            property.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            new Thread(refreshExec).start();
        }
    }



}
