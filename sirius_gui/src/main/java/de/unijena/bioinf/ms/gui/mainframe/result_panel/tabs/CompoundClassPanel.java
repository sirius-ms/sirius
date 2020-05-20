package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassDetailView;
import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassList;
import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassTableView;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class CompoundClassPanel extends JPanel implements PanelDescription {
    @Override
    public String getDescription() {
        return "<html>"
                + "Detailed information about the PREDICTED Classyfire compound classes of the selected molecular formula."
//                +"<br>"
//                + "Example structures for a selected molecular property are shown in the bottom panel."
                + "</html>";
    }

    protected Logger logger = LoggerFactory.getLogger(CompoundClassPanel.class);

    final JSplitPane sp;

    public CompoundClassPanel(CompoundClassList table, FormulaList siriusResultElements) {
        super(new BorderLayout());

        final CompoundClassTableView center = new CompoundClassTableView(table);
        final CompoundClassDetailView detail = new CompoundClassDetailView(siriusResultElements);
        sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, detail, center);
        add(sp, BorderLayout.CENTER);
    }

}
