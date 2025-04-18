package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.*;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


public class ComputeToolPanel extends JPanel {
    public static final String PRESET_FROZEN_MESSAGE = "Could not load preset.";

    private final GlobalConfigPanel globalConfigPanel; // global configurations
    private final ActSpectraSearchConfigPanel spectraSearchConfigPanel; //Library search configs
    private final ActFormulaIDConfigPanel formulaIDConfigPanel; //Sirius configs
    private final ActZodiacConfigPanel zodiacConfigs; //Zodiac configs
    private final ActFingerprintAndCanopusConfigPanel fingerprintAndCanopusConfigPanel; //Combines CSI:FingerID predict and CANOPUS configs
    private final ActFingerblastConfigPanel csiSearchConfigs; //CSI:FingerID search configs
    private final ActMSNovelistConfigPanel msNovelistConfigs; //MsNovelist configs

    public ComputeToolPanel(SiriusGui gui, List<InstanceBean> compoundsToProcess, boolean hasMs2) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setBorder(BorderFactory.createEmptyBorder());

        // make subtool config panels
        globalConfigPanel = new GlobalConfigPanel(gui, compoundsToProcess, hasMs2);
        addConfigPanel("Global Configuration", globalConfigPanel);

        spectraSearchConfigPanel = new ActSpectraSearchConfigPanel(gui, globalConfigPanel, hasMs2);
        addConfigPanel("Spectral Library Search", spectraSearchConfigPanel);

        formulaIDConfigPanel = new ActFormulaIDConfigPanel(gui, compoundsToProcess, globalConfigPanel, hasMs2);
        JPanel formulaRow = addConfigPanel("SIRIUS - Molecular Formula Identification", formulaIDConfigPanel);
        final boolean formulasAvailable = compoundsToProcess.stream().allMatch(inst -> inst.getComputedTools().isFormulaSearch());

        zodiacConfigs = new ActZodiacConfigPanel(gui, compoundsToProcess.size());
        fingerprintAndCanopusConfigPanel = new ActFingerprintAndCanopusConfigPanel(gui);
        csiSearchConfigs = new ActFingerblastConfigPanel(gui, globalConfigPanel);
        msNovelistConfigs = new ActMSNovelistConfigPanel(gui);

        if (compoundsToProcess.size() != 1 && hasMs2) {
            addConfigPanelToRow("ZODIAC - Network-based molecular formula re-ranking", zodiacConfigs, formulaRow);
            zodiacConfigs.addToolDependency(formulaIDConfigPanel, () -> formulasAvailable);
        }

        if (hasMs2) {
            final boolean compoundClassesAvailable = compoundsToProcess.stream().allMatch(inst -> inst.getComputedTools().isCanopus());

            addConfigPanel("Predict properties: CSI:FingerID - Fingerprint Prediction & CANOPUS - Compound Class Prediction", fingerprintAndCanopusConfigPanel);
            fingerprintAndCanopusConfigPanel.addToolDependency(formulaIDConfigPanel, () -> formulasAvailable);

            JPanel searchRow = addConfigPanel("CSI:FingerID - Structure Database Search", csiSearchConfigs);
            addConfigPanelToRow("MSNovelist - De Novo Structure Generation", msNovelistConfigs, searchRow);
            csiSearchConfigs.addToolDependency(fingerprintAndCanopusConfigPanel, () -> compoundClassesAvailable && !formulaIDConfigPanel.isToolSelected());
            msNovelistConfigs.addToolDependency(fingerprintAndCanopusConfigPanel, () -> compoundClassesAvailable && !formulaIDConfigPanel.isToolSelected());

            // computing formulaId will discard fingerprints, so we need to enable it for structure search
            formulaIDConfigPanel.addToolDependencyListener((c, enabled) -> {
                if (enabled && !fingerprintAndCanopusConfigPanel.isToolSelected() && (csiSearchConfigs.isToolSelected() || msNovelistConfigs.isToolSelected())) {
                    fingerprintAndCanopusConfigPanel.activationButton.doClick(0);
                    fingerprintAndCanopusConfigPanel.showAutoEnableInfoDialog(fingerprintAndCanopusConfigPanel.toolName + " is activated because a downstream tool needs its input, which would be deleted by running " + formulaIDConfigPanel.toolName + ".");
                }
            });
        }
    }

    public void disableControls(boolean freeze){
        if (freeze){
            Stream.of(formulaIDConfigPanel, zodiacConfigs, fingerprintAndCanopusConfigPanel, csiSearchConfigs, msNovelistConfigs)
                    .forEach(panel -> {
                        if (panel.isToolSelected()) {
                            panel.activationButton.doClick(0);
                        }
                        panel.setButtonEnabled(false, PRESET_FROZEN_MESSAGE);
                    });
        }else {
            Stream.of(formulaIDConfigPanel, zodiacConfigs, fingerprintAndCanopusConfigPanel, csiSearchConfigs, msNovelistConfigs)
                    .forEach(panel -> panel.setButtonEnabled(true, PRESET_FROZEN_MESSAGE));
        }
        globalConfigPanel.setEnabled(freeze);
    }

    public JobSubmission makeJobSubmission(JobSubmission preset, boolean recompute) {
        JobSubmission sub = new JobSubmission();
        sub.setConfigMap(new HashMap<>());
        sub.getConfigMap().putAll(preset.getConfigMap());
        sub.getConfigMap().putAll(getAllUIParameterBindings());
        sub.getConfigMap().put("RecomputeResults", Boolean.toString(recompute));

        if (spectraSearchConfigPanel.isToolSelected()) {
            sub.setSpectraSearchParams(new SpectralLibrarySearch().enabled(true));
        }

        if (formulaIDConfigPanel.isToolSelected()) {
            sub.setFormulaIdParams(new Sirius().enabled(true));
        }

        if (zodiacConfigs.isToolSelected()) {
            sub.setZodiacParams(new Zodiac().enabled(true));
        }

        //canopus prediction included. Must now run before structure database search
        if (fingerprintAndCanopusConfigPanel.isToolSelected()) {
            sub.setFingerprintPredictionParams(new FingerprintPrediction().enabled(true));
            sub.setCanopusParams(new Canopus().enabled(true));
        }

        if (csiSearchConfigs.isToolSelected()) {
            sub.setStructureDbSearchParams(new StructureDbSearch().enabled(true));
        }

        if (msNovelistConfigs.isToolSelected()) {
            sub.setMsNovelistParams(new MsNovelist().enabled(true));
        }

        return sub;
    }

    /**
     * @return a map of all parameter bindings from the UI elements
     */
    public Map<String, String> getAllUIParameterBindings() {
        HashMap<String, String> bindings = Stream.of(globalConfigPanel, spectraSearchConfigPanel.content, formulaIDConfigPanel.content, zodiacConfigs.content, fingerprintAndCanopusConfigPanel.content, csiSearchConfigs.content, msNovelistConfigs.content)
                .map(ConfigPanel::asConfigMap)
                .collect(HashMap::new, HashMap::putAll, HashMap::putAll);
        return bindings;
    }

    public void setDisplayAdvancedParameters(boolean displayAdvanced) {
        spectraSearchConfigPanel.content.setDisplayAdvancedParameters(displayAdvanced);
        formulaIDConfigPanel.content.setDisplayAdvancedParameters(displayAdvanced);
        zodiacConfigs.content.setDisplayAdvancedParameters(displayAdvanced);
    }

    private JPanel addConfigPanelToRow(String header, JPanel configPanel, JPanel row) {
        JPanel topAlignedStack = new JPanel() {
            @Override
            public int getBaseline(int width, int height) {
                return 0;
            }

            @Override
            public BaselineResizeBehavior getBaselineResizeBehavior() {
                return BaselineResizeBehavior.CONSTANT_ASCENT;
            }
        };

        topAlignedStack.setLayout(new BorderLayout());
        JXTitledSeparator title = new JXTitledSeparator(header);
        title.setBorder(BorderFactory.createEmptyBorder(GuiUtils.MEDIUM_GAP, 0, GuiUtils.MEDIUM_GAP, GuiUtils.SMALL_GAP));
        topAlignedStack.add(title, BorderLayout.NORTH);
        topAlignedStack.add(configPanel, BorderLayout.CENTER);
        row.add(topAlignedStack);
        return row;
    }

    private JPanel addConfigPanel(String header, JPanel configPanel) {
        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT, GuiUtils.LARGE_GAP, GuiUtils.SMALL_GAP);
        flowLayout.setAlignOnBaseline(true);
        JPanel flowContainer = new JPanel(flowLayout);
        flowContainer.setBorder(BorderFactory.createEmptyBorder());
        addConfigPanelToRow(header, configPanel, flowContainer);
        add(flowContainer);
        return flowContainer;
    }

    public Stream<ActivatableConfigPanel<?>> getToolStream(){
        return Stream.of(spectraSearchConfigPanel, formulaIDConfigPanel, zodiacConfigs, fingerprintAndCanopusConfigPanel, csiSearchConfigs, msNovelistConfigs);
    }

    public boolean warnNoMethodIsSelected() {
        if (globalConfigPanel.isEnabled() && getToolStream().noneMatch(configPanel -> configPanel != null && configPanel.isToolSelected())){
            new WarningDialog(SwingUtilities.getWindowAncestor(this), "Please select at least one method.");
            return true;
        }else {
            return false;
        }
    }

    public boolean warnNoAdductSelected() {
        if (formulaIDConfigPanel.isToolSelected() && globalConfigPanel.getSelectedAdducts().isEmpty()) {
            new WarningDialog(SwingUtilities.getWindowAncestor(this), "Please select at least one adduct.");
            return true;
        } else {
            return false;
        }
    }

    public void destroy() {
        formulaIDConfigPanel.destroy(); //Sirius configs
        zodiacConfigs.destroy(); //Zodiac configs
        fingerprintAndCanopusConfigPanel.destroy(); //Combines CSI:FingerID predict and CANOPUS configs
        csiSearchConfigs.destroy(); //CSI:FingerID search configs
        msNovelistConfigs.destroy(); //MsNovelist configs
    }

    public void applyValuesFromPreset(@NotNull JobSubmission preset, @NotNull Map<String, String> configMap) {
        globalConfigPanel.applyValuesFromPreset(configMap);

        spectraSearchConfigPanel.applyValuesFromPreset(preset.getSpectraSearchParams() != null && Boolean.TRUE.equals(preset.getSpectraSearchParams().isEnabled()), configMap);

        formulaIDConfigPanel.applyValuesFromPreset(preset.getFormulaIdParams() != null && Boolean.TRUE.equals(preset.getFormulaIdParams().isEnabled()), configMap);
        zodiacConfigs.applyValuesFromPreset(preset.getZodiacParams() != null && Boolean.TRUE.equals(preset.getZodiacParams().isEnabled()), configMap);

        boolean fpEnabled = preset.getFingerprintPredictionParams() != null && Boolean.TRUE.equals(preset.getFingerprintPredictionParams().isEnabled());
        boolean canopusEnabled = preset.getCanopusParams() != null && Boolean.TRUE.equals(preset.getCanopusParams().isEnabled());
        if (fpEnabled != canopusEnabled) {
            throw new UnsupportedOperationException("Fingerprint and Canopus are not enabled/disabled simultaneously.");
        }
        fingerprintAndCanopusConfigPanel.applyValuesFromPreset(fpEnabled, configMap);
        csiSearchConfigs.applyValuesFromPreset(preset.getStructureDbSearchParams() != null && Boolean.TRUE.equals(preset.getStructureDbSearchParams().isEnabled()), configMap);
        msNovelistConfigs.applyValuesFromPreset(preset.getMsNovelistParams() != null && Boolean.TRUE.equals(preset.getMsNovelistParams().isEnabled()), configMap);
    }

}
