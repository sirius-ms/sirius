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

package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.ms_viewer.SpectraViewContainer;
import de.unijena.bioinf.ms.gui.tree_viewer.TreeConfig;
import de.unijena.bioinf.ms.gui.tree_viewer.TreeViewerBridge;
import de.unijena.bioinf.ms.gui.tree_viewer.TreeViewerSettings;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.sirius.ms.sdk.model.MsData;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Set;

/**
 * Combined panel that integrates both SpectraVisualizationPanel and TreeVisualizationPanel
 * with a unified toolbar.
 */
public class SpectraTreeVisualizationPanel extends JPanel implements ItemListener, Loadable {

    // Unified toolbar
    protected final JToolBar combinedToolBar;
    
    // Content panel (will contain both spectra and tree panels)
    protected final FormulaTreePanel contentPanel;
    
    // Frame for any popup dialogs
    JFrame popupOwner;
    
    // SpectraVisualizationPanel elements
    protected JComboBox<String> modesBox;
    protected JComboBox<String> ceBox;
    protected JComboBox<String> normBox;
    protected JComboBox<String> intBox;
    protected JButton saveSpectraButton;
    protected Set<String> possibleModes;
    protected String preferredMode;
    protected MsData msData;
    
    // TreeVisualizationPanel elements
    protected JComboBox<String> presetBox;
    protected JSlider scaleSlider;
    protected JButton saveTreeBtn;
    protected JButton advancedSettingsBtn;
    protected JButton resetBtn;
    protected TreeViewerSettings settings;
    @Getter
    protected TreeConfig localConfig;
    
    // Constants from SpectraVisualizationPanel
    public static final String MS1_DISPLAY = "MS1", MS1_MIRROR_DISPLAY = "MS1 mirror-plot", 
            MS2_DISPLAY = "MS2", MS2_MIRROR_DISPLAY = "MS2 mirror-plot",
            MS2_MERGED_DISPLAY = "merged";
    
    public final static String SQRT = "square root";
    public final static String KEEPINT = "no";
    public final static String[] intensityTransformModes = new String[]{
            KEEPINT, SQRT
    };

    public final static String MAXNORM = "l∞ (max)";
    public final static String SUMNORM = "l1 (sum)";
    public final static String L2 = "l2";
    public final static String[] normalizationModes = new String[]{
            MAXNORM, L2, SUMNORM
    };
    
    /**
     * Constructs a new combined visualization panel
     */
    public SpectraTreeVisualizationPanel(FormulaList formulaList, SiriusGui siriusGui) {
        this(formulaList, MS1_DISPLAY, siriusGui);
    }

    
    /**
     * Constructs a new combined visualization panel with preferred mode and MS2 mirror option
     * @param preferredMode the preferred spectra display mode
     */
    public SpectraTreeVisualizationPanel(FormulaList formulaList, String preferredMode, SiriusGui siriusGui) {
        this(formulaList,preferredMode, Set.of(MS1_DISPLAY, MS1_MIRROR_DISPLAY, MS2_DISPLAY), siriusGui);
    }
    
    /**
     * Main constructor
     * @param preferredMode the preferred spectra display mode
     * @param possibleModes the set of possible display modes
     */
    public SpectraTreeVisualizationPanel(FormulaList formulaList, String preferredMode, Set<String> possibleModes, SiriusGui siriusGui) {
        this.setLayout(new BorderLayout());
        this.possibleModes = possibleModes;
        this.preferredMode = preferredMode;
        this.popupOwner = (JFrame) SwingUtilities.getWindowAncestor(this);
        
        // Initialize tree config
        localConfig = new TreeConfig();
        localConfig.setFromString(
            "presets", PropertyManager.getProperty(
                PropertyManager.PROPERTY_BASE + ".tree_viewer.presets"));
                
        // Create combined toolbar with BoxLayout to allow left and right alignment
        combinedToolBar = new JToolBar();
        combinedToolBar.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        combinedToolBar.setFloatable(false);
        combinedToolBar.setPreferredSize(new Dimension(combinedToolBar.getPreferredSize().width, 32));
        combinedToolBar.setLayout(new BoxLayout(combinedToolBar, BoxLayout.X_AXIS));
        
        // Initialize and add spectra visualization toolbar components (left side)
        initSpectraToolbarComponents();
        
        // Add a glue component to push remaining components to the right
        combinedToolBar.add(Box.createHorizontalGlue());
        
        // Initialize and add tree visualization toolbar components (right side)
        initTreeToolbarComponents();
        
        // Add the toolbar to the north position
        add(combinedToolBar, BorderLayout.NORTH);
        
        // Create content panel that will hold both visualizations
        contentPanel = new FormulaTreePanel(formulaList, siriusGui);
        
        add(contentPanel, BorderLayout.CENTER);

        // Initially disable toolbar
        setToolbarEnabled(true);
        
        setVisible(true);
    }
    
    /**
     * Initialize spectra visualization toolbar components
     */
    private void initSpectraToolbarComponents() {
        // Mode label and combo box
        JLabel modeLabel = new JLabel("Mode");
        modeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        modesBox = new JComboBox<>();
        possibleModes.forEach(modesBox::addItem);
        modesBox.setSelectedIndex(0);
        modesBox.addItemListener(this);

        ceBox = new JComboBox<>();
        ceBox.addItemListener(this);
        
        combinedToolBar.add(modeLabel);
        combinedToolBar.add(modesBox);
        combinedToolBar.add(ceBox);
        
        // Normalization and intensity transform
        intBox = new JComboBox<>(intensityTransformModes);
        normBox = new JComboBox<>(normalizationModes);
        
        combinedToolBar.add(new JLabel("Norm: "));
        combinedToolBar.add(normBox);
        combinedToolBar.add(new JLabel("Intensity transform: "));
        combinedToolBar.add(intBox);
        
        // Set default values based on preferred mode
        if (preferredMode.equals(MS2_MIRROR_DISPLAY)) {
            intBox.setSelectedItem(SQRT);
            normBox.setSelectedItem(L2);
        } else if (preferredMode.equals(MS1_MIRROR_DISPLAY)) {
            intBox.setSelectedItem(KEEPINT);
            normBox.setSelectedItem(MAXNORM);
        } else {
            intBox.setSelectedItem(KEEPINT);
            normBox.setSelectedItem(MAXNORM);
        }
        
        intBox.addItemListener(this);
        normBox.addItemListener(this);
        
        // Save spectra button
        combinedToolBar.addSeparator(new Dimension(10, 10));
        saveSpectraButton = Buttons.getExportButton24("Export spectra");
        saveSpectraButton.addActionListener(evt -> saveSpectra());
        saveSpectraButton.setToolTipText("Export the current spectra view to various formats");
        combinedToolBar.add(saveSpectraButton);
        
        combinedToolBar.addSeparator(new Dimension(10, 10));
    }
    
    /**
     * Initialize tree visualization toolbar components
     */
    private void initTreeToolbarComponents() {
        // Preset label and combo box
        JLabel presetLabel = new JLabel("Preset");
        presetLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        presetBox = new JComboBox<>((String[]) localConfig.get("presets"));
        presetBox.addActionListener(evt -> applyPreset((String) presetBox.getSelectedItem()));
        presetBox.setSelectedItem(
            PropertyManager.getProperty(PropertyManager.PROPERTY_BASE
                                       + ".tree_viewer.preset"));
                                       
        combinedToolBar.add(presetLabel);
        combinedToolBar.add(presetBox);
        
        // Save tree button
        combinedToolBar.addSeparator(new Dimension(10, 10));
        saveTreeBtn = Buttons.getExportButton24("Export tree");
        saveTreeBtn.addActionListener(evt -> saveTree());
        saveTreeBtn.setToolTipText("Export current tree view to various formats");
        combinedToolBar.add(saveTreeBtn);
        
        // Scale slider
        combinedToolBar.addSeparator(new Dimension(10, 10));
        scaleSlider = new JSlider(JSlider.HORIZONTAL,
                TreeViewerBridge.TREE_SCALE_MIN,
                TreeViewerBridge.TREE_SCALE_MAX,
                TreeViewerBridge.TREE_SCALE_INIT);
        scaleSlider.addChangeListener(evt -> scaleTree(scaleSlider.getValue()));
        scaleSlider.setToolTipText("Increase/Decrease the space between nodes");
        
        JLabel scaleSliderLabel = new JLabel("Scale");
        scaleSliderLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        scaleSliderLabel.setToolTipText("Increase/Decrease the space between nodes");
        
        combinedToolBar.add(scaleSliderLabel);
        combinedToolBar.add(scaleSlider);
        
        // Advanced settings button
        combinedToolBar.addSeparator(new Dimension(10, 10));
        advancedSettingsBtn = new JButton("Customize");
        advancedSettingsBtn.addActionListener(evt -> toggleTreeSettings());
        advancedSettingsBtn.setToolTipText("Customize various settings for the visualization");
        combinedToolBar.add(advancedSettingsBtn);
        
        // Reset button
        combinedToolBar.addSeparator(new Dimension(10, 10));
        resetBtn = new JButton("Reset");
        resetBtn.addActionListener(evt -> resetTreeView());
        resetBtn.setToolTipText("Revert any changes made to the visualization and the tree itself");
        combinedToolBar.add(resetBtn);
    }
    
    /**
     * Enables or disables all toolbar components
     * @param enabled whether components should be enabled
     */
    protected void setToolbarEnabled(boolean enabled) {
        for (Component comp : combinedToolBar.getComponents()) {
            comp.setEnabled(enabled);
        }
    }
    
    /**
     * Called when an item state changes in one of our combo boxes
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            if (e.getSource() == modesBox || e.getSource() == ceBox || e.getSource() == normBox || e.getSource() == intBox) {
                if (e.getSource() == modesBox){
                    final String sel = (String) modesBox.getSelectedItem();
                    ceBox.setVisible(sel != null && sel.startsWith(MS2_DISPLAY));
                    preferredMode = sel;
                }
                contentPanel.updateSpectrumSettings(getCEIndex(), (String)modesBox.getSelectedItem(), getNormalizationMode(), getIntensityMode());
            }
        }
    }

    
    /**
     * Apply a preset to the tree viewer
     * @param preset the preset name to apply
     */
    public void applyPreset(String preset) {
        if (localConfig == null)
            localConfig = new TreeConfig();
            
        String propertyPrefix = PropertyManager.PROPERTY_BASE + ".tree_viewer.";
        String presetPropertyPrefix = propertyPrefix + preset + ".";

        for (String setting : TreeConfig.SETTINGS)
            localConfig.setFromString(
                    setting, PropertyManager.getProperty(
                            // preferably use preset value
                            presetPropertyPrefix + setting,
                            propertyPrefix + setting, null));

        updateColorPalette();
        updateConfig();
        
        if (settings != null)
            settings.updateConfig();
    }
    
    /**
     * Update the color palette for the tree
     */
    protected void updateColorPalette() {
        localConfig.set("colorScheme2",
                TreeViewerBridge.get2ColorPaletteByNameOrDefault((String)localConfig.get("colorScheme2")));
        localConfig.set("colorScheme3",
                TreeViewerBridge.get3ColorPaletteByNameOrDefault((String)localConfig.get("colorScheme3")));
    }
    
    /**
     * Update the tree configuration
     */
    public void updateConfig() {
        // Nothing to change for now
    }
    
    /**
     * Reset the tree view
     */
    public void resetTreeView() {
        // This would reset the tree view
        // Implementation details would be added here
        LoggerFactory.getLogger(getClass()).info("Tree view reset requested");
    }
    
    /**
     * Scale the tree based on slider value
     * @param value the slider value
     */
    private void scaleTree(int value) {
        // This would scale the tree
        // Implementation details would be added here
        LoggerFactory.getLogger(getClass()).info("Tree scaling to: {}", value);
    }
    
    /**
     * Toggle tree settings dialog
     */
    private void toggleTreeSettings() {
        if (settings != null) {
            Jobs.runEDTLater(settings::toggleShow);
        } else {
            LoggerFactory.getLogger(getClass()).info("Tree settings requested but not initialized");
        }
    }
    
    /**
     * Save the spectra visualization
     */
    private void saveSpectra() {
        // This would save the spectra visualization
        // Implementation details would be added here
        LoggerFactory.getLogger(getClass()).info("Save spectra requested");
    }
    
    /**
     * Save the tree visualization
     */
    private void saveTree() {
        // This would save the tree visualization
        // Implementation details would be added here  
        LoggerFactory.getLogger(getClass()).info("Save tree requested");
    }
    
    /**
     * Get the normalization mode from the selection
     */
    private Normalization getNormalizationMode() {
        switch ((String)normBox.getSelectedItem()) {
            case SUMNORM: return Normalization.Sum(1d);
            case L2: return Normalization.L2();
            case MAXNORM: return Normalization.Max;
        }
        return Normalization.Max;
    }
    
    /**
     * Get the intensity transform mode from the selection
     */
    private SpectraViewContainer.IntensityTransform getIntensityMode() {
        switch ((String)intBox.getSelectedItem()) {
            case KEEPINT: return SpectraViewContainer.IntensityTransform.No;
            case SQRT: return SpectraViewContainer.IntensityTransform.Sqrt;
        }
        return SpectraViewContainer.IntensityTransform.No;
    }
    
    /**
     * Get the collision energy index
     */
    private int getCEIndex() {
        return ceBox.getSelectedItem() == null || ceBox.getSelectedItem().equals(MS2_MERGED_DISPLAY) ? -1 : ceBox.getSelectedIndex();
    }

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
//        return center.setLoading(loading, absolute);
        return false;
    }
}