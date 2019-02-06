package de.unijena.bioinf.ms.gui.canopus;

import de.unijena.bioinf.ms.gui.sirius.ExperimentResultBean;
import de.unijena.bioinf.ms.gui.sirius.SiriusResultElement;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.PanelDescription;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class CanopusPanel extends JPanel implements ActionListener, ActiveElementChangedListener<SiriusResultElement, ExperimentResultBean>, PanelDescription {

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
    public void resultsChanged(ExperimentResultBean experiment, SiriusResultElement sre, List<SiriusResultElement> resultElements, ListSelectionModel selections) {
        treePanel.updateTree(experiment, sre);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
}
