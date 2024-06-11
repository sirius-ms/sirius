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

import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.jdesktop.swingx.JXTitledSeparator;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class GerneralSettingsPanel extends TwoColumnPanel implements SettingsPanel {
    private Properties props;
    final JSpinner scalingSpinner;
    final int scaling;

    final String theme;

    final JComboBox<String> themeBox;

    final FileChooserPanel db;
    final JComboBox<String> solver, confidenceDisplayMode;
    private boolean restartRequired = false;

    public GerneralSettingsPanel(Properties properties, SiriusGui gui) {
        super();
        this.props = properties;
        add(new JXTitledSeparator("Graphical User Interface"));

        theme = props.getProperty("de.unijena.bioinf.sirius.ui.theme", "Light");
        String[] themes = new String[]{"Light", "Dark", "Classic"};
        themeBox = new JComboBox<>(themes);
        themeBox.setSelectedItem(theme);
        themeBox.setToolTipText("Set theme of the Graphical User Interface");
        addNamed("UI Theme", themeBox);

        scaling = Integer.parseInt(props.getProperty("sun.java2d.uiScale", System.getProperty("sun.java2d.uiScale", "1")));

        SpinnerNumberModel model = new SpinnerNumberModel(scaling, 1, 5, 1);
        scalingSpinner = new JSpinner(model);
        scalingSpinner.setToolTipText(GuiUtils.formatToolTip("Set scaling factor of the Graphical User Interface"));
        addNamed("Scaling Factor", scalingSpinner);

        add(new JXTitledSeparator("Display settings"));
        Vector<String> modes =  new Vector<>(Arrays.asList("approximate (default)","exact"));
        String selectedMode = props.getProperty("de.unijena.bioinf.sirius.ui.confidenceDisplayMode");
        confidenceDisplayMode = new JComboBox<>(modes);
        confidenceDisplayMode.setSelectedItem(selectedMode);
        confidenceDisplayMode.setToolTipText(GuiUtils.formatToolTip("Select the confidence score display mode. \"exact\" will show confidences for the exact top hit structure to be correct. \"approximate\" will show confidences for the top hit or a sufficiently similar structure to be correct. Structure candidates that are within the similarity threshold are marked in green"));
        add(new JLabel("Confidence score display mode"),confidenceDisplayMode);

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
        final JFrame mf = (JFrame) SwingUtilities.getWindowAncestor(this);
        clearDBCache.addActionListener(evt -> {
            Jobs.runInBackgroundAndLoad(mf, "Clearing database cache...", () -> {
                try {
                    CustomDataSources.getWebDatabaseCacheStorage().clear();
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when clearing DB cache", e);
                    new StacktraceDialog(mf, "Error when clearing DB cache", e);
                }
            });
        });
        addNamed("", clearDBCache);

        add(new JXTitledSeparator("REST API"));
        JButton openSwaggerInBrowser = new JButton("Open API in browser");
        openSwaggerInBrowser.setToolTipText("Open URL of the REST API in the browser.");
        addNamed("", openSwaggerInBrowser);
        openSwaggerInBrowser.addActionListener(evt -> {
            try {
                Desktop.getDesktop().browse(URI.create(gui.getSiriusClient().getApiClient().getBasePath()));
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).error("Cannot open API in browser.", e);
            }
        });

    }

    @Override
    public boolean restartRequired() {
        return restartRequired;
    }

    @Override
    public void saveProperties() {
        String selectedTheme = (String) themeBox.getSelectedItem();
        if (!theme.equals(selectedTheme)) {
            props.setProperty("de.unijena.bioinf.sirius.ui.theme", selectedTheme);
            restartRequired = true;
        }
        props.setProperty("de.unijena.bioinf.sirius.treebuilder.solvers", (String) solver.getSelectedItem());
        props.setProperty("de.unijena.bioinf.sirius.ui.confidenceDisplayMode",String.valueOf(confidenceDisplayMode.getSelectedItem()));
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
