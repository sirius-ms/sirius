package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.*;
import net.miginfocom.swing.MigLayout;
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
        this(gui, compoundsToProcess, hasMs2, null);
    }
    public ComputeToolPanel(SiriusGui gui, List<InstanceBean> compoundsToProcess, boolean hasMs2, Integer preferredWidth) {
        super(new MigLayout("insets 0", "[left]20[left]","[top][top][top][top][top][top][top][top][top][top]"));
        setBorder(BorderFactory.createEmptyBorder(GuiUtils.MEDIUM_GAP, GuiUtils.MEDIUM_GAP, 0, GuiUtils.MEDIUM_GAP));

        // make subtool config panels
        globalConfigPanel = new GlobalConfigPanel(gui, compoundsToProcess, hasMs2);
        spectraSearchConfigPanel = new ActSpectraSearchConfigPanel(gui, globalConfigPanel, hasMs2);
        formulaIDConfigPanel = new ActFormulaIDConfigPanel(gui, compoundsToProcess, globalConfigPanel, hasMs2);
        zodiacConfigs = new ActZodiacConfigPanel(gui, compoundsToProcess.size());
        fingerprintAndCanopusConfigPanel = new ActFingerprintAndCanopusConfigPanel(gui);
        csiSearchConfigs = new ActFingerblastConfigPanel(gui, globalConfigPanel);
        msNovelistConfigs = new ActMSNovelistConfigPanel(gui);

        JXTitledSeparator sep = new JXTitledSeparator("Global Configuration");
        //just to prevent resizing of toplevel separators
        if (preferredWidth != null)
            sep.setPreferredSize(new Dimension(preferredWidth - 2*GuiUtils.MEDIUM_GAP - 15 , sep.getPreferredSize().height));

        add(sep, "cell 0 0, spanx 2, growx, aligny top, wrap");
        add(globalConfigPanel, "cell 0 1, spanx 2, aligny top, wrap");

        add(new JXTitledSeparator("SIRIUS - Molecular Formula Identification"), "cell 0 4, growx, aligny top, wrap");
        add(formulaIDConfigPanel, "cell 0 5, aligny top,  wrap");

        if (hasMs2) {
            final boolean formulasAvailable = compoundsToProcess.stream().allMatch(inst -> inst.getComputedTools().isFormulaSearch());
            final boolean compoundClassesAvailable = compoundsToProcess.stream().allMatch(inst -> inst.getComputedTools().isCanopus());

            add(new JXTitledSeparator("Spectral Library Search"), "cell 0 2, growx, spanx 2, aligny top, wrap");
            add(spectraSearchConfigPanel, "cell 0 3, spanx 2, aligny top, wrap");

            if (compoundsToProcess.size() != 1 && compoundsToProcess.stream().filter(InstanceBean::hasMsMs).limit(10).count() == 10) {
                add(new JXTitledSeparator("ZODIAC - Network-based molecular formula re-ranking"), "cell 1 4, growx, aligny top, wrap");
                add(zodiacConfigs, "cell 1 5, aligny top, wrap");
            }

            add(new JXTitledSeparator("Predict properties: CSI:FingerID - Fingerprint Prediction & CANOPUS - Compound Class Prediction"), "cell 0 6, growx, spanx 2, aligny top, wrap");
            add(fingerprintAndCanopusConfigPanel, "cell 0 7, spanx 2, aligny top, wrap");

            add(new JXTitledSeparator("CSI:FingerID - Structure Database Search"), "cell 0 8, growx, aligny top, wrap");
            add(csiSearchConfigs, "cell 0 9, aligny top, wrap");

            add(new JXTitledSeparator("MSNovelist - De Novo Structure Generation"), "cell 1 8, growx, aligny top, wrap");
            add(msNovelistConfigs, "cell 1 9, aligny top, wrap");


            fingerprintAndCanopusConfigPanel.addToolDependency(formulaIDConfigPanel, () -> formulasAvailable);
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
