package de.unijena.bioinf.ms.gui.canopus;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;
import de.unijena.bioinf.ms.frontend.io.projectspace.FormulaResultBean;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.PanelDescription;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class CanopusPanel extends JPanel implements ActionListener, ActiveElementChangedListener<FormulaResultBean, InstanceBean>, PanelDescription {

//    protected ClassyfireTreePanel treePanel;

    public CanopusPanel() {
        super(new BorderLayout());
//        this.treePanel = new ClassyfireTreePanel(ApplicationCore.CANOPUS);
//        final JScrollPane scroll = new JScrollPane(treePanel);
//        treePanel.setScrollPane(scroll);
//        add(scroll, BorderLayout.CENTER);

    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
//        treePanel.updateTree(experiment, sre);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
}
