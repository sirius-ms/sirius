package de.unijena.bioinf.sirius.gui.canopus;

import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.sirius.gui.utils.PanelDescription;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class CanopusPanel extends JPanel implements ActionListener, ActiveElementChangedListener<SiriusResultElement,ExperimentContainer>, PanelDescription {

    protected ClassyfireTreePanel treePanel;

    public CanopusPanel() {
        super(new BorderLayout());
        this.treePanel = new ClassyfireTreePanel();
        final JScrollPane scroll = new JScrollPane(treePanel);
        treePanel.setScrollPane(scroll);
        add(scroll, BorderLayout.CENTER);

    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public void resultsChanged(ExperimentContainer experiment, SiriusResultElement sre, List<SiriusResultElement> resultElements, ListSelectionModel selections) {
        treePanel.updateTree(experiment, sre);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
}
