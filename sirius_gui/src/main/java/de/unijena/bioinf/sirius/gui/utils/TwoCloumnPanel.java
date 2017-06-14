package de.unijena.bioinf.sirius.gui.utils;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 07.10.16.
 */

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class TwoCloumnPanel extends JPanel {
    public  final GridBagConstraints both, left, right;



    public TwoCloumnPanel() {
        this(GridBagConstraints.EAST,GridBagConstraints.WEST);
    }
    public TwoCloumnPanel(final int leftAnchor, final int rightAnchor) {
        super();
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));

        left = new GridBagConstraints();
        left.gridx = 0;
        left.fill = GridBagConstraints.NONE;
        left.anchor = leftAnchor;
        left.weightx = 0;
        left.weighty = 0;
        left.insets = new Insets(0, 0, 0, 5);

        right = new GridBagConstraints();
        right.gridx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.anchor = rightAnchor;
        right.weightx = 1;
        right.weighty = 0;

        both = new GridBagConstraints();
        both.gridx = 0;
        both.gridwidth = 2;
        both.fill = GridBagConstraints.HORIZONTAL;
        right.weightx = 1;
        right.weighty = 0;
        both.insets = new Insets(0, 0, 5, 0);
        setRow(0);
    }

    public TwoCloumnPanel(JComponent left, JComponent right) {
        this();
        add(left,right);
    }

    public void setRow(int i) {
        left.gridy = i;
        right.gridy = i;
        both.gridy = i;
    }

    public void resetRow() {
        int i = this.getComponentCount();
        left.gridy = i;
        right.gridy = i;
        both.gridy = i;
    }



    public void add(JComponent leftComp, JComponent rightComp) {

        add(leftComp,rightComp,0,false);
    }

    public void add(JComponent leftComp, JComponent rightComp, int gap, boolean verticalResize) {
        if (verticalResize) {
            left.fill = GridBagConstraints.VERTICAL;
            left.weighty = 1;
            right.fill = GridBagConstraints.BOTH;
            right.weighty = 1;
        }

        left.insets.top += gap;
        right.insets.top += gap;

        if (leftComp != null)
            add(leftComp, left);

        if (rightComp != null)
            add(rightComp, right);


        left.fill = GridBagConstraints.NONE;
        right.fill = GridBagConstraints.HORIZONTAL;

        left.weighty = 0;
        right.weighty = 0;

        left.insets.top -= gap;
        right.insets.top -= gap;

        left.gridy++;
        right.gridy++;
        both.gridy++;
    }

    public void add(JComponent comp){
        add(comp,0,false);
    }

    public void add(JComponent comp, int gap, boolean verticalResize) {
        if (verticalResize){
            both.fill = GridBagConstraints.BOTH;
            both.weighty = 1;
        }
        both.insets.top += gap;

        super.add(comp, both);

        both.fill = GridBagConstraints.HORIZONTAL;
        both.insets.top -= gap;
        both.weighty = 0;

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
