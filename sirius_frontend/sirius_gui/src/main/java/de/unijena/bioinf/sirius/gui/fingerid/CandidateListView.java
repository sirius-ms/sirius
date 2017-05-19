package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.sirius.gui.table.ActionListView;

import javax.swing.*;
import java.awt.*;

/**
 * Created by fleisch on 16.05.17.
 */
public class CandidateListView extends ActionListView<CandidateList> {
    JPanel north;

    public CandidateListView(CandidateList source) {
        super(source, new CandidateListToolBarPanel());

    }
}
