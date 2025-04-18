package de.unijena.bioinf.ms.gui.compute;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public class ComputeActionsPanel extends JPanel {
    private final JButton compute;
    private final JButton abort;
    private final JCheckBox recomputeBox;
    private final JButton showCommand;
    private final JButton showJson;

    public ComputeActionsPanel(int numOfInstances) {
        super(new GridLayout(1, 3));

        JPanel lsouthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        recomputeBox = new JCheckBox("Recompute already computed tasks?", false);
        recomputeBox.setToolTipText("If checked, all selected compounds will be computed. Already computed analysis steps will be recomputed.");
        lsouthPanel.add(recomputeBox);

        if (numOfInstances == 1)
            recomputeBox.setSelected(true);

        JPanel csouthPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        showCommand = new JButton("Show Command");


        showJson = new JButton("Show JSON");
        showJson.setToolTipText("Open current parameters in a JSON viewer.");

        csouthPanel.add(showCommand);
        csouthPanel.add(showJson);

        JPanel rsouthPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        compute = new JButton("Compute");
        compute.setVisible(numOfInstances > 0);

        abort = new JButton("Cancel");

        rsouthPanel.add(compute);
        rsouthPanel.add(abort);

        add(lsouthPanel);
        add(csouthPanel);
        add(rsouthPanel);
    }
}