package de.unijena.bioinf.sirius.gui.utils.jCheckboxList;

import de.unijena.bioinf.sirius.gui.utils.RelativeLayout;
import de.unijena.bioinf.sirius.gui.utils.TextHeaderBoxPanel;

import javax.swing.*;
import java.awt.*;

public class JCheckboxListPanel<E> extends TextHeaderBoxPanel {
    public final JCheckBoxList<E> checkBoxList;
    private final JButton all = new JButton("all");
    private final JButton none = new JButton("none");

    public JCheckboxListPanel(JCheckBoxList<E> sourceList, String headline, String tooltip) {
        this(sourceList, headline);
        setToolTipText(tooltip);
    }

    public JCheckboxListPanel(JCheckBoxList<E> sourceList, String headline) {
        super(headline);
        checkBoxList = sourceList;
        checkBoxList.setVisibleRowCount(6);

        JScrollPane sp = new JScrollPane(checkBoxList);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(sp, BorderLayout.CENTER);

        all.addActionListener(e -> checkBoxList.checkAll());
        none.addActionListener(e -> checkBoxList.uncheckAll());
        RelativeLayout l = new RelativeLayout(RelativeLayout.X_AXIS, 1);
        l.setAlignment(RelativeLayout.CENTER);
        JPanel buttons = new JPanel();
        buttons.add(all);
        buttons.add(none);
        add(buttons, BorderLayout.SOUTH);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        all.setEnabled(enabled);
        none.setEnabled(enabled);
        checkBoxList.setEnabled(enabled);
    }
}
