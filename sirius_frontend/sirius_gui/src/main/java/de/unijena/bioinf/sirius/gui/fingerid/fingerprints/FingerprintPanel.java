package de.unijena.bioinf.sirius.gui.fingerid.fingerprints;

import de.unijena.bioinf.sirius.gui.fingerid.CandidateListDetailViewPanel;

import javax.swing.*;
import java.awt.*;

public class FingerprintPanel extends JPanel {

    public FingerprintPanel(FingerprintTable table) {
        super(new BorderLayout());

        final FingerprintTableView north = new FingerprintTableView(table);
        final CandidateListDetailViewPanel south = null;

        JSplitPane major = new JSplitPane(JSplitPane.VERTICAL_SPLIT, north, south);
        major.setDividerLocation(250);
        add(major, BorderLayout.CENTER);
    }

}
