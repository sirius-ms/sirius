package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ms.gui.fingerid.StructureList;
import de.unijena.bioinf.ms.gui.fingerid.CandidateListStructureView;
import de.unijena.bioinf.ms.gui.fingerid.CandidateListTableView;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;

import javax.swing.*;
import java.awt.*;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateOverviewPanel extends JPanel implements PanelDescription {
    @Override
    public String getDescription() {
        return "<html>"
                + "CSI:FingerID results for all molecular formulas that had been searched."
                +"<br>"
                + "Selected candidate structures are rendered in the bottom panel."
                + "</html>";
    }
    public CandidateOverviewPanel(final StructureList sourceList) {
        super(new BorderLayout());

        final CandidateListTableView north = new CandidateListTableView(sourceList);
        final CandidateListStructureView south = new CandidateListStructureView(north.getFilteredSelectionModel());

        add(north, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

}