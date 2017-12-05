package de.unijena.bioinf.fingerid.fingerprints;

import de.unijena.bioinf.sirius.gui.utils.PanelDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

public class FingerprintPanel extends JPanel implements PanelDescription {
    @Override
    public String getDescription() {
        return "<html>"
                + "Detailed information about the PREDICTED fingerprint of the selected molecular formula."
                +"<br>"
                + "Example structures for a selected molecular property are shown in the bottom panel."
                + "</html>";
    }

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
        south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        south.add(preview,BorderLayout.CENTER);

        add(north, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }


}
