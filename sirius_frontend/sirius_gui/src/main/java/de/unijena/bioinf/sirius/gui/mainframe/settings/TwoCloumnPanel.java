package de.unijena.bioinf.sirius.gui.mainframe.settings;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 07.10.16.
 */

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class TwoCloumnPanel extends JPanel {
    public  final GridBagConstraints both, left, right;



    public TwoCloumnPanel() {
        super();
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));

        left = new GridBagConstraints();
        left.gridx = 0;
        left.fill = GridBagConstraints.NONE;
        left.anchor = GridBagConstraints.EAST;
        left.weightx = 0;
        left.weighty = 0;
        left.insets = new Insets(0, 0, 0, 10);

        right = new GridBagConstraints();
        right.gridx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.anchor = GridBagConstraints.WEST;
        right.weightx = 1;
        right.weighty = 0;

        both = new GridBagConstraints();
        both.gridx = 0;
        both.gridwidth = 2;
        both.fill = GridBagConstraints.BOTH;
        both.insets = new Insets(0, 0, 5, 0);
        setRow(0);
    }

    protected void setRow(int i) {
        left.gridy = i;
        right.gridy = i;
        both.gridy = i;
    }

    protected void add(JComponent leftComp, JComponent rightComp) {

        if (leftComp != null)
            add(leftComp, left);

        if (rightComp != null)
            add(rightComp, right);

        left.gridy++;
        right.gridy++;
        both.gridy++;
    }

    protected void add(JComponent comp) {
        super.add(comp, both);
        left.gridy++;
        right.gridy++;
        both.gridy++;
    }

    public void addVerticalGlue(){
        double weighty = both.weighty;
        both.weighty = Integer.MAX_VALUE;
        add(Box.createVerticalBox());
        both.weighty = weighty;
    }
}
