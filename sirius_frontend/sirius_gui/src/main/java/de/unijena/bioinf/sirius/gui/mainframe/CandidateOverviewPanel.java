package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.sirius.gui.fingerid.CandidateList;
import de.unijena.bioinf.sirius.gui.fingerid.CandidateListStructureView;
import de.unijena.bioinf.sirius.gui.fingerid.CandidateListTableView;
import de.unijena.bioinf.sirius.gui.fingerid.CandidateListDetailViewPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateOverviewPanel extends JPanel {
    public CandidateOverviewPanel(final CandidateList sourceList) {
        super(new BorderLayout());

        final CandidateListTableView north = new CandidateListTableView(sourceList);
        final CandidateListStructureView south = new CandidateListStructureView(north.getFilteredSelectionModel());

        add(north, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

}