package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassList;
import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassTableView;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
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
    public CompoundClassPanel(CompoundClassList table) {
        super(new BorderLayout());

        final CompoundClassTableView center = new CompoundClassTableView(table);
//        JPanel south;
//        final StructurePreview preview = new StructurePreview(table);

        /*center.addSelectionListener(e -> {
            ListSelectionModel m = (ListSelectionModel)e.getSource();
            final int index = m.getAnchorSelectionIndex();
            if (index>=0) {
                preview.setMolecularProperty(center.getFilteredSource().get(index));
            }
        });*/
//        south = new JPanel(new BorderLayout());
//        south.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
//        south.add(preview,BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
//        add(south, BorderLayout.SOUTH);
    }


}
