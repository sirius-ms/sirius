package de.unijena.bioinf.sirius.gui.fingerid.fingerprints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

public class FingerprintPanel extends JPanel {
    protected Logger logger = LoggerFactory.getLogger(FingerprintPanel.class);
    public FingerprintPanel(FingerprintTable table) {
        super(new BorderLayout());

        final FingerprintTableView north = new FingerprintTableView(table);
        JPanel south;
        final StructurePreview preview = new StructurePreview(table.visualizations);

        north.addSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                ListSelectionModel m = (ListSelectionModel)e.getSource();
                final int index = m.getAnchorSelectionIndex();
                if (index>=0) {
                    preview.setMolecularProperty(north.getFilteredSource().get(index));
                }
            }
        });
        south = preview;

        JSplitPane major = new JSplitPane(JSplitPane.VERTICAL_SPLIT, north, south);
        major.setResizeWeight(1d);
        add(major, BorderLayout.CENTER);
    }

}
