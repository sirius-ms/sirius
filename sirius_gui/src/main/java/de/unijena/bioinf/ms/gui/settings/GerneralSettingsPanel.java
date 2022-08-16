/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.settings;

import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.jdesktop.swingx.JXTitledSeparator;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class GerneralSettingsPanel extends TwoColumnPanel implements SettingsPanel {
    private Properties props;
    final JSpinner scalingSpinner;
    final int scaling;

    final FileChooserPanel db;
    final JComboBox<String> solver;
    private boolean restartRequired = false;

    final JCheckBox allowMS1Only;

    public GerneralSettingsPanel(Properties properties) {
        super();
        this.props = properties;
        add(new JXTitledSeparator("Graphical User Interface"));
        scaling = Integer.parseInt(props.getProperty("sun.java2d.uiScale", System.getProperty("sun.java2d.uiScale", "1")));

        SpinnerNumberModel model = new SpinnerNumberModel(scaling, 1, 5, 1);
        scalingSpinner = new JSpinner(model);
        scalingSpinner.setToolTipText(GuiUtils.formatToolTip("Set scaling factor of the Graphical User Interface"));
        addNamed("Scaling Factor", scalingSpinner);

        add(new JXTitledSeparator("Data Import"));
        allowMS1Only = new JCheckBox();
        allowMS1Only.setSelected(Boolean.parseBoolean(props.getProperty("de.unijena.bioinf.sirius.ui.allowMs1Only","true")));
        allowMS1Only.setToolTipText(GuiUtils.formatToolTip("If checked data without MS/MS spectra will be imported. Otherwise they will be skipped during import."));
        addNamed("Import data without MS/MS", allowMS1Only);

        add(new JXTitledSeparator("ILP solver"));
        Vector<String> items = new Vector<>(Arrays.asList("clp,cplex,gurobi,glpk", "cplex,gurobi,clp,glpk", "cplex,clp,glpk", "gurobi,clp,glpk", "clp,glpk", "glpk,clp", "gurobi", "cplex", "clp", "glpk"));
        String selected = props.getProperty("de.unijena.bioinf.sirius.treebuilder.solvers");
        if (!items.contains(selected))
            items.add(selected);
        solver = new JComboBox<>(items);
        solver.setSelectedItem(selected);
        solver.setToolTipText(GuiUtils.formatToolTip("Choose the allowed solvers and in which order they should be checked. Note that glpk is part of Sirius whereas the others not"));
        add(new JLabel("Allowed solvers:"), solver);

        add(new JXTitledSeparator("CSI:FingerID"));
        String p = props.getProperty("de.unijena.bioinf.sirius.fingerID.cache");
        db = new FileChooserPanel(p, JFileChooser.DIRECTORIES_ONLY);
        db.setToolTipText(GuiUtils.formatToolTip("Specify the directory where CSI:FingerID should store the compound candidates."));
        add(new JLabel("Database cache:"), db);
        JButton clearDBCache = new JButton("Clear cache");
        clearDBCache.addActionListener(evt -> {
            Jobs.runInBackgroundAndLoad(MF, "Clearing database cache...", () -> {
                try {
                    SearchableDatabases.getWebDatabaseCacheStorage().clear();
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when clearing DB cache", e);
                    new StacktraceDialog(MF, "Error when clearing DB cache", e);
                }
            });
        });
        addNamed("", clearDBCache);
    }

    @Override
    public boolean restartRequired() {
        return restartRequired;
    }

    @Override
    public void saveProperties() {
        props.setProperty("de.unijena.bioinf.sirius.treebuilder.solvers", (String) solver.getSelectedItem());
        props.setProperty("de.unijena.bioinf.sirius.ui.allowMs1Only", String.valueOf(allowMS1Only.isSelected()));
//        props.setProperty("de.unijena.bioinf.sirius.treebuilder.timeout", treeTimeout.getNumber().toString());
        final Path dir = Paths.get(db.getFilePath());
        if (Files.isDirectory(dir)) {
            props.setProperty("de.unijena.bioinf.sirius.fingerID.cache", dir.toAbsolutePath().toString());
            //todo do we need to invalidate chache somehow
            /*Jobs.runInBackgroundAndLoad(MF, () -> {
                System.out.println("WaRN Check if we have to do something???");
            });*/
        } else {
            LoggerFactory.getLogger(this.getClass()).warn("Specified path is not a directory (" + dir.toString() + "). Directory not Changed!");
        }
        if (scaling != (int) scalingSpinner.getValue()) {
            props.setProperty("sun.java2d.uiScale", String.valueOf((int) scalingSpinner.getValue()));
            restartRequired = true;
        }
    }

    @Override
    public String name() {
        return "General";
    }

}
