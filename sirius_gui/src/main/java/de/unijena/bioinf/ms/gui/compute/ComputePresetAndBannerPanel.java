package de.unijena.bioinf.ms.gui.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.net.ConnectionChecks;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.MessageBanner;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import de.unijena.bioinf.ms.gui.utils.toggleswitch.toggle.JToggleSwitch;
import de.unijena.bioinf.ms.gui.utils.toggleswitch.toggle.ToggleListener;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.sirius.ms.sdk.model.ConnectionCheck;
import io.sirius.ms.sdk.model.JobSubmission;
import io.sirius.ms.sdk.model.StoredJobSubmission;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComputePresetAndBannerPanel extends JPanel {
    // should be the same as returned by the server
    public static final String DEFAULT_PRESET_NAME = "Default";
    public static final String MS1_PRESET_NAME = "MS1";

    private final SiriusGui gui;
    private final boolean singleCompoundMode;
    private final boolean ms2Mode;


    // view
    private JToggleSwitch toggle;
    @Getter private JButton savePreset;
    @Getter private JButton saveAsPreset;
    private JButton exportPreset;
    private JButton importPreset;
    @Getter private JButton removePreset;
    @Getter private JComboBox<String> presetDropdown;
    private MessageBanner presetInfoBanner;
    private MessageBanner presetWarningBanner;
    private MessageBanner connectionMessage;

    // Model
    private Map<String, StoredJobSubmission> allPresets;


    public ComputePresetAndBannerPanel(SiriusGui gui, boolean singleCompoundMode, boolean ms2Mode) {
        super(new BorderLayout());
        this.gui = gui;
        this.singleCompoundMode = singleCompoundMode;
        this.ms2Mode = ms2Mode;
        setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.SMALL_GAP, 0, GuiUtils.SMALL_GAP));

        add(makeBanners(), BorderLayout.NORTH);

        Box topLine = Box.createHorizontalBox();
        topLine.add(makePresetPanel());
        topLine.add(makeAdvancedModeToggle());

        add(topLine, BorderLayout.CENTER);

        initControls();
    }

    private JPanel makeBanners() {
        presetInfoBanner = new MessageBanner("", MessageBanner.BannerType.INFO);
        presetInfoBanner.setVisible(false);

        presetWarningBanner = new MessageBanner("", MessageBanner.BannerType.WARNING);
        presetWarningBanner.setVisible(false);

        connectionMessage = new MessageBanner();
        connectionMessage.setVisible(false);

        JPanel bannerPanel = new JPanel(new BorderLayout());
        bannerPanel.add(connectionMessage, BorderLayout.NORTH);
        bannerPanel.add(presetInfoBanner, BorderLayout.CENTER);
        bannerPanel.add(presetWarningBanner, BorderLayout.SOUTH);
        return bannerPanel;
    }

    private JPanel makeAdvancedModeToggle() {
        toggle = new JToggleSwitch();
        toggle.setToolTipText("Show/Hide advanced parameters.");
        toggle.setPreferredSize(new Dimension(40, 28));


        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(toggle);
        panel.add(new JLabel("Advanced"));

        return panel;
    }

    private JPanel makePresetPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Preset"));

        presetDropdown = new JComboBox<>();
        presetDropdown.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.BatchCompute_PresetDropDown);

        panel.add(presetDropdown);

        savePreset = new JButton("Save");
        savePreset.setEnabled(false);
        if (singleCompoundMode) {
            savePreset.setToolTipText("Cannot save presets in single compound mode.");
        } else {
            savePreset.setToolTipText("Update current preset with selected parameters.");
        }


        saveAsPreset = new JButton("Save as");
        if (singleCompoundMode) {
            saveAsPreset.setToolTipText("Cannot save presets in single compound mode.");
            saveAsPreset.setEnabled(false);
        } else {
            saveAsPreset.setToolTipText("Save current selection as a new preset.");
        }

        exportPreset = new JButton("Export");
        exportPreset.setToolTipText("Export the selected preset as JSON\n(NOT the current selection).");

        importPreset = new JButton("Import");
        importPreset.setToolTipText("Import a preset JSON file.");

        removePreset = new JButton("Remove");
        removePreset.setEnabled(false);

        panel.add(savePreset);
        panel.add(saveAsPreset);
        panel.add(removePreset);
        panel.add(Box.createRigidArea(new Dimension(GuiUtils.MEDIUM_GAP, 0)));
        panel.add(exportPreset);
        panel.add(importPreset);

        return panel;
    }

    private void initControls() {
        exportPreset.addActionListener(e -> {
            String fileName = presetDropdown.getSelectedItem() + ".json";
            File file = new File(PropertyManager.getProperty(SiriusProperties.DEFAULT_SAVE_DIR_PATH), fileName);
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(file);
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                if (fileToSave.exists()) {
                    if (JOptionPane.showOptionDialog(this,
                            "File " + fileName + " already exists. Overwrite?",
                            null,
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            new Object[]{"Overwrite", "Cancel"},
                            null) != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                try {
                    JobSubmission preset = getSelectedPreset().getJobSubmission();
                    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fileToSave, preset);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), null, JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        importPreset.addActionListener(e -> {
            JFileChooser presetFileChooser = new JFileChooser(PropertyManager.getProperty(SiriusProperties.DEFAULT_SAVE_DIR_PATH));
            presetFileChooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
            if (presetFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File presetFile = presetFileChooser.getSelectedFile();
                String presetName = stripExtension(presetFile.getName());
                try {
                    JobSubmission importedPreset = new ObjectMapper().readValue(presetFile, JobSubmission.class);
                    StoredJobSubmission newJobSubmission = savePresetAs(importedPreset, presetName);

                    if (newJobSubmission != null) {
                        reloadPresets();
                        if (JOptionPane.showConfirmDialog(this,
                                "Switch to the new preset?",
                                null,
                                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                            selectPreset(newJobSubmission.getName(), true);
                        }
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), null, JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        removePreset.addActionListener(e -> {
            String presetName = (String) presetDropdown.getSelectedItem();
            gui.acceptSiriusClient((c, pid) -> c.jobs().deleteJobConfig(presetName));
            reloadPresets();
            selectDefaultPreset(true);
        });
    }

    private static String stripExtension(String name) {
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return name;
        }
        return name.substring(0, lastDotIndex);
    }


    public boolean isAdvancedView(){
        return toggle.isSelected();
    }

    public void addAdvancedViewListener(@NotNull ToggleListener event){
        toggle.addEventToggleSelected(event);
    }

    public void updateConnectionBanner(ConnectionCheck checkResult) {
        if (connectionMessage != null)
            connectionMessage.setVisible(false);

        if (ConnectionChecks.isInternet(checkResult) && !ConnectionChecks.isLoggedIn(checkResult)) {
            connectionMessage.update("Not logged in! Most of the tools will not be available without being logged in. Please log in!",
                    MessageBanner.BannerType.WARNING, true);
        } else if (!ConnectionChecks.isInternet(checkResult)) {
            connectionMessage.update("No Connection! There is an issue with the server connection. Please check 'Webservice' for details.",
                    MessageBanner.BannerType.ERROR, true);
        }

    }

    public void showPresetInfoBanner(String message) {
        presetInfoBanner.setText(message + ". You can start a computation with this preset, but cannot edit the parameters.");
        presetInfoBanner.setVisible(true);
    }

    public void showPresetWarningBanner(String message) {
        presetWarningBanner.setText(message + ". Computation with this preset might not work as expected.");
        presetWarningBanner.setVisible(true);
    }

    public void hidePresetBanners() {
        presetInfoBanner.setVisible(false);
        presetWarningBanner.setVisible(false);
    }

    public StoredJobSubmission savePresetAs(JobSubmission js, String suggestedName) {
        String newPresetName = (String) JOptionPane.showInputDialog(
                this,
                "New preset name",
                null,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                suggestedName);

        if (newPresetName == null || newPresetName.isBlank()) {
            return null;
        }

        boolean overwrite = false;
        if (allPresets.containsKey(newPresetName)) {
            if (allPresets.get(newPresetName).isEditable()) {
                if (JOptionPane.showOptionDialog(this,
                        "Preset " + newPresetName + " already exists. Overwrite?",
                        null,
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object[]{"Overwrite", "Cancel"},
                        null) != JOptionPane.YES_OPTION) {
                    return null;
                } else {
                    overwrite = true;
                }
            } else {
                JOptionPane.showMessageDialog(this, "Preset " + newPresetName + " already exists, and is not editable.", null, JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        final boolean finalOverwrite = overwrite;  // lambda requires final variable

        try {
            return gui.applySiriusClient((c, pid) -> c.jobs().saveJobConfig(newPresetName, js, finalOverwrite, false));
        } catch (Exception ex) {
            Jobs.runEDTLater(() -> new StacktraceDialog(SwingUtilities.getWindowAncestor(this), gui.getSiriusClient().unwrapErrorMessage(ex), ex));
            return null;
        }
    }


    /**
     * Removes all current presets from the preset dropdown and loads them again, preserving selection if possible.
     * If the previously selected preset was removed, some other preset should be activated after calling this method, otherwise the UI will be in an inconsistent state
     */
    public void reloadPresets() {
        String oldSelection = (String) presetDropdown.getSelectedItem();
        ItemListener[] listeners = presetDropdown.getItemListeners();
        for (ItemListener listener : listeners)
            presetDropdown.removeItemListener(listener);

        try {
            presetDropdown.removeAllItems();
            presetDropdown.setSelectedItem(null);

            allPresets = new HashMap<>();
            List<StoredJobSubmission> configsFromServer = gui.applySiriusClient((c, pid) -> c.jobs().getJobConfigs());
            for (StoredJobSubmission c : configsFromServer) {
                allPresets.put(c.getName(), c);
                presetDropdown.addItem(c.getName());
            }
            if (oldSelection != null && allPresets.containsKey(oldSelection)) {
                presetDropdown.setSelectedItem(oldSelection);
            }
        } finally {
            for (ItemListener listener : listeners)
                presetDropdown.addItemListener(listener);
        }
    }

    public void selectDefaultPreset(){
        selectDefaultPreset(false);
    }

    public void selectDefaultPreset(boolean enforceChangeEvent) {
        selectPreset(ms2Mode ? DEFAULT_PRESET_NAME : MS1_PRESET_NAME, enforceChangeEvent);
    }

    public void selectPreset(String presetName) {
        selectPreset(presetName, false);
    }
    public void selectPreset(String presetName, boolean enforceChangeEvent) {
        if (enforceChangeEvent)
            presetDropdown.setSelectedItem(null);
        presetDropdown.setSelectedItem(presetName);
    }

    public StoredJobSubmission getSelectedPreset() {
        return getPreset((String) presetDropdown.getSelectedItem());
    }

    public StoredJobSubmission getDefaultPreset() {
        return getPreset(ms2Mode ? DEFAULT_PRESET_NAME : MS1_PRESET_NAME);
    }

    public StoredJobSubmission getPreset(String presetName) {
        return allPresets.get(presetName);
    }

}
