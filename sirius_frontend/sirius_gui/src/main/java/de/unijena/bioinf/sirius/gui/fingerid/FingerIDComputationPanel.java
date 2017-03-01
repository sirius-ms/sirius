package de.unijena.bioinf.sirius.gui.fingerid;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 23.01.17.
 */

import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FingerIDComputationPanel extends JPanel {
    //todo her can we show more option if we select databases
    public JRadioButton pubchem, biodb;
    protected final static String BIO = "bio database", ALL = "PubChem";
    private Border b = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    public FingerIDComputationPanel(boolean isBio) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        final JPanel inner2 = new JPanel();
        inner2.setLayout(new FlowLayout());
        ButtonGroup database = new ButtonGroup();
        pubchem = new JRadioButton(ALL, !isBio);
        biodb = new JRadioButton(BIO, isBio);

        database.add(pubchem);
        database.add(biodb);
        inner2.add(pubchem);
        inner2.add(biodb);
        setBorder(b);
        add(new JXTitledSeparator("Search in"));
        add(inner2);
    }

    public void setIsBioDB(boolean isBio){
        pubchem.setSelected(!isBio);
        biodb.setSelected(isBio);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        pubchem.setEnabled(enabled);
        biodb.setEnabled(enabled);
    }
}
