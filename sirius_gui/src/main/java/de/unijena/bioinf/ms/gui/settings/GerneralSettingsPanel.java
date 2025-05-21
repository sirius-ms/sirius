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
import de.unijena.bioinf.ms.gui.compute.BatchComputeDialog;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.properties.MolecularStructuresDisplayColors;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourUtils;
import org.jdesktop.swingx.JXTitledSeparator;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import static de.unijena.bioinf.ms.gui.properties.GuiProperties.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class GerneralSettingsPanel extends TwoColumnPanel implements SettingsPanel {
    public static final String DO_NOT_SHOW_AGAIN_ACTIVATE_LIBRARY_TAB = "de.unijena.bioinf.sirius.settings.activateSpectralLibraryTab.dontAskAgain";

    private Properties props;
    final JSpinner scalingSpinner;
    final int scaling;
    private boolean enableAllSoftwareTours = false;

    final String theme;

    final JComboBox<String> themeBox;

    final FileChooserPanel db;
    final JCheckBox showSpectraMatchPanel;
    final JComboBox<String> solver;
    final JComboBox<ConfidenceDisplayMode> confidenceDisplayMode;
    final JComboBox<MolecularStructuresDisplayColors> molecularStructuresDisplayColors;
    private boolean restartRequired = false;
    private SiriusGui gui;

    public GerneralSettingsPanel(Properties properties, SiriusGui gui) {
        super();
        this.props = properties;
        this.gui = gui;
        add(new JXTitledSeparator("Graphical User Interface"));

        theme = props.getProperty("de.unijena.bioinf.sirius.ui.theme", "Light");
        String[] themes = new String[]{"Light", "Dark"};
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
        // confidenceDisplayMode shows the settings used at program start.
        confidenceDisplayMode = GuiUtils.makeParameterComboBoxFromDescriptiveValues(ConfidenceDisplayMode.values());

        try {
            confidenceDisplayMode.setSelectedItem(ConfidenceDisplayMode.valueOf(props.getProperty(CONFIDENCE_DISPLAY_MODE_KEY, "APPROXIMATE")));
        } catch (IllegalArgumentException e) {
            confidenceDisplayMode.setSelectedItem(ConfidenceDisplayMode.APPROXIMATE);
        }
        JLabel confLabel = new JLabel("Confidence score display mode");
        confLabel.setToolTipText("The default confidence score display mode used after every new program start.");
        add(confLabel,confidenceDisplayMode);

        //molecularStructuresDisplayColors uses the current settings and also stores it persistently
        molecularStructuresDisplayColors = GuiUtils.makeParameterComboBoxFromDescriptiveValues(MolecularStructuresDisplayColors.values());
        molecularStructuresDisplayColors.setSelectedItem(gui.getProperties().getMolecularStructureDisplayColors());
        add(new JLabel("Molecular structures display color"), molecularStructuresDisplayColors);

        showSpectraMatchPanel = new JCheckBox();
        showSpectraMatchPanel.setToolTipText("Show a result tab with all spectral library matches for the selected features.");
        showSpectraMatchPanel.setSelected(gui.getProperties().isShowSpectraMatchPanel());
        addNamed("Show \"Library Matches\" tab", showSpectraMatchPanel);
        showSpectraMatchPanel.addActionListener(evt -> {
            if (showSpectraMatchPanel.isSelected()) {
                new WarningDialog(gui.getMainFrame(),
                        "Activate spectral library results tab",
                        GuiUtils.formatToolTip(
                                "SIRIUS automatically searches in your spectral libraries as part of the molecular formula annotation step. " +
                                "Library hits can be viewed via the \"Structures\" tab after performing structure database search. This integrated view allows you to seamlessly compare structure database and spectral library hits.",
                                "By activating the \"Library Matches\" tab, you can also view the spectral library hits independently of the molecular structure list from the \"Structures\" tab.", "",
                                "NOTE: In SIRIUS, each spectral library is also a molecular structure database. ANY hit in this library can also be found via CSI:FingerID structure database search. " +
                                        "Since structure database results depend on the selected molecular formula, SIRIUS ensures that molecular structures with a formula corresponding to a good spectral library hit are considered - even if this molecular formula receives a low score. " +
                                        "In this way, molecular structures of well-matching reference spectra are automatically included in the structure database search.", "",
                                        "To ensure that the database search is performed on all your spectral libraries and CSI:FingerID does not miss a candidate, you still need to select these libraries (databases) in the database search step."),
                        DO_NOT_SHOW_AGAIN_ACTIVATE_LIBRARY_TAB);
            }
        });

        //software tour
        JButton enableTour = new JButton("Enable all tours");
        addNamed("Software tours", enableTour);
        enableTour.addActionListener(evt -> {
            enableAllSoftwareTours = true;
            //make it persistent even when cancel is clicked
            SoftwareTourUtils.enableAllTours(gui.getProperties());
        });


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

        add(new JXTitledSeparator("Presets"));
        JButton editPresets = new JButton("Edit Presets");
        addNamed("", editPresets);
        editPresets.addActionListener(evt -> {
            new BatchComputeDialog(gui, List.of());
        });

        add(new JXTitledSeparator("REST API"));
        JButton openSwaggerInBrowser = new JButton("Open API in browser");
        openSwaggerInBrowser.setToolTipText("Open URL of the REST API in the browser.");
        addNamed("", openSwaggerInBrowser);
        openSwaggerInBrowser.addActionListener(evt -> {
            try {
                GuiUtils.openURLInSystemBrowser(URI.create(gui.getSiriusClient().getApiClient().getBasePath()), gui);
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

        props.setProperty(SHOW_SPECTRA_MATCH_PANEL_KEY, String.valueOf(showSpectraMatchPanel.isSelected()));
        gui.getProperties().setShowSpectraMatchPanel(showSpectraMatchPanel.isSelected());

        props.setProperty("de.unijena.bioinf.sirius.treebuilder.solvers", (String) solver.getSelectedItem());

        props.setProperty(CONFIDENCE_DISPLAY_MODE_KEY, ((ConfidenceDisplayMode) confidenceDisplayMode.getSelectedItem()).name());
        gui.getProperties().setConfidenceDisplayMode((ConfidenceDisplayMode) confidenceDisplayMode.getSelectedItem());

        props.setProperty(MOLECULAR_STRUCTURES_DISPLAY_COLORS_KEY,((MolecularStructuresDisplayColors) molecularStructuresDisplayColors.getSelectedItem()).name());
        gui.getProperties().setMolecularStructureDisplayColors((MolecularStructuresDisplayColors) molecularStructuresDisplayColors.getSelectedItem());

        final Path dir = Paths.get(db.getFilePath());
        if (Files.isDirectory(dir)) {
            props.setProperty("de.unijena.bioinf.sirius.fingerID.cache", dir.toAbsolutePath().toString());
        } else {
            LoggerFactory.getLogger(this.getClass()).warn("Specified path is not a directory ({}). Directory not Changed!", dir);
        }
        if (scaling != (int) scalingSpinner.getValue()) {
            props.setProperty("sun.java2d.uiScale", String.valueOf((int) scalingSpinner.getValue()));
            restartRequired = true;
        }

        if (enableAllSoftwareTours) {
            //still required to make sure that properties are not overwritten again
            SoftwareTourUtils.enableAllTours(gui.getProperties(), props);
        }
    }

    @Override
    public String name() {
        return "General";
    }

}
