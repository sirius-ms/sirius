package de.unijena.bioinf.ms.gui.settings;

import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

public class ILPSettings extends TwoColumnPanel implements SettingsPanel {
    private Properties props;
    final JComboBox<String> solver;
    final FileChooserPanel gurobi;
    final FileChooserPanel cplex;


    public ILPSettings(Properties properties) {
        super();
        this.props = properties;

        Vector<String> items = new Vector<>(Arrays.asList("gurobi,cplex,glpk", "gurobi,glpk", "glpk,gurobi", "gurobi", "cplex", "glpk"));
        String selected = props.getProperty("de.unijena.bioinf.sirius.treebuilder.solvers");
        if (!items.contains(selected))
            items.add(selected);
        solver = new JComboBox<>(items);
        solver.setSelectedItem(selected);
        solver.setToolTipText("Choose the allowed solvers and in which order they should be checked. Note that glpk is part of Sirius whereas the others not");
        add(new JLabel("Select solver(s):"), solver);

        add(new JXTitledSeparator("Gurobi"));
        gurobi = new FileChooserPanel(props.getProperty("de.unijena.bioinf.sirius.gurobi.home", "no gurobi specified"));
        add(new JLabel("GUROBI_HOME:"), gurobi);

        add(new JXTitledSeparator("CPLEX"));
        cplex = new FileChooserPanel(props.getProperty("de.unijena.bioinf.sirius.cplex.home", "no cplex specified"));
        add(new JLabel("CPLEX_HOME:"), cplex);

    }

    @Override
    public void refreshValues() {

    }

    @Override
    public void saveProperties() {
        props.setProperty("de.unijena.bioinf.sirius.treebuilder.solvers", (String) solver.getSelectedItem());
        props.setProperty("de.unijena.bioinf.sirius.gurobi.home", gurobi.getFilePath());
        props.setProperty("de.unijena.bioinf.sirius.cplex.home", cplex.getFilePath());
    }

    @Override
    public String name() {
        return "ILP-Solvers";
    }
}
