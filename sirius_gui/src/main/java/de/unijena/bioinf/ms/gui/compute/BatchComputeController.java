package de.unijena.bioinf.ms.gui.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.CheckConnectionAction;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.dialogs.InfoDialog;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.compute.ComputePresetAndBannerPanel.DEFAULT_PRESET_NAME;
import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isConnected;

@Slf4j
public class BatchComputeController {
    public static final String DONT_ASK_RECOMPUTE_KEY = "de.unijena.bioinf.sirius.computeDialog.recompute.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_S_MASS = "de.unijena.bioinf.sirius.computeDialog.sirius.highmass.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_OUTDATED_PS = "de.unijena.bioinf.sirius.computeDialog.projectspace.outdated.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_NO_FP_CHECK = "de.unijena.bioinf.sirius.computeDialog.projectspace.outdated.na.dontAskAgain";
    public static final String DO_NOT_SHOW_PRESET_HIDDEN_PARAMETERS = "de.unijena.bioinf.sirius.computeDialog.preset.hiddenParameters.dontAskAgain";

    private final SiriusGui gui;

    private final Window owner;

    private final ComputeToolPanel toolsPanel;
    private final ComputePresetAndBannerPanel presetPanel;
    private final ComputeActionsPanel computeActionsPanel;
    private final List<InstanceBean> instanceToProcess;

    private boolean presetFrozen = false;

    public BatchComputeController(SiriusGui gui, Window owner, ComputeToolPanel toolsPanel, ComputePresetAndBannerPanel presetPanel, ComputeActionsPanel computeActionsPanel, List<InstanceBean> instanceToProcess) {
        this.gui = gui;
        this.owner = owner;
        this.toolsPanel = toolsPanel;
        this.presetPanel = presetPanel;
        this.computeActionsPanel = computeActionsPanel;
        this.instanceToProcess = instanceToProcess;

        presetPanel.getPresetDropdown().addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED)
                activatePreset((String)event.getItem());
        });

        presetPanel.getSavePreset().addActionListener(e -> {
            JobSubmission currentConfig = makeJobSubmission();

            String presetName = (String) presetPanel.getPresetDropdown().getSelectedItem();
            StoredJobSubmission newJobSubmission = gui.applySiriusClient((c, pid) -> c.jobs().saveJobConfig(presetName, currentConfig, true, false));

            presetPanel.reloadPresets();
            if (newJobSubmission != null)
                presetPanel.selectPreset(presetName, true);
        });

        presetPanel.getSaveAsPreset().addActionListener(e -> {
            JobSubmission currentConfig = makeJobSubmission();

            StoredJobSubmission newJobSubmission = presetPanel.savePresetAs(currentConfig, presetPanel.getPresetDropdown().getSelectedItem() + "_copy");

            presetPanel.reloadPresets();
            if (newJobSubmission != null)
                presetPanel.selectPreset(newJobSubmission.getName(), true);
        });

        // add tools panel controls
        presetPanel.addAdvancedViewListener(toolsPanel::setDisplayAdvancedParameters);
        toolsPanel.setDisplayAdvancedParameters(presetPanel.isAdvancedView());
        // auto enable library search for default preset if custom db is selected.
        // auto disable for all presets if all custom dbs have been unselected
        // todo remove/change if remote spec libs are available.
        toolsPanel.getGlobalConfigPanel().getSearchDBList().checkBoxList.addCheckBoxListener(e -> {
            @SuppressWarnings("unchecked")
            SearchableDatabase item = (SearchableDatabase) ((CheckBoxListItem<Object>) e.getItem()).getValue();
            if (item.isCustomDb()){
                ActSpectraSearchConfigPanel spectralPanel = toolsPanel.getSpectraSearchConfigPanel();
                // we only
                if (DEFAULT_PRESET_NAME.equals(presetPanel.getSelectedPreset().getName()) && e.getStateChange() == ItemEvent.SELECTED && !spectralPanel.activationButton.isSelected() ) {
                    spectralPanel.activationButton.setSelected(true);
                    spectralPanel.setComponentsEnabled(spectralPanel.activationButton.isSelected());
                } else if (e.getStateChange() == ItemEvent.DESELECTED && spectralPanel.activationButton.isSelected()) {
                    if ( toolsPanel.getGlobalConfigPanel().getSearchDBList().checkBoxList.getCheckedItems().stream().noneMatch(SearchableDatabase::isCustomDb)) {
                        spectralPanel.activationButton.setSelected(false);
                        spectralPanel.setComponentsEnabled(spectralPanel.activationButton.isSelected());
                    }
                }
            }
        });

        // add action panel controls
        computeActionsPanel.getShowCommand().addActionListener(e -> {
            JobSubmission js = stripDefaultValues(makeJobSubmission(), presetPanel.getDefaultPreset().getJobSubmission());
            List<String> command = gui.applySiriusClient((c, pid) -> c.jobs().getCommand(js));
            final String commandString = String.join(" ", command);
            if (toolsPanel.warnNoMethodIsSelected()) return;
            if (toolsPanel.warnNoAdductSelected()) return;
            new InfoDialog(gui.getMainFrame(), "Command", GuiUtils.formatToolTip(commandString), null) {
                @Override
                protected void decorateButtonPanel(JPanel boxedButtonPanel) {
                    JButton copyCommand = new JButton("Copy Command");
                    copyCommand.setToolTipText("Copy command to clipboard.");
                    copyCommand.addActionListener(evt -> {
                        StringSelection stringSelection = new StringSelection(commandString);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(stringSelection, null);
                    });
                    Arrays.stream(boxedButtonPanel.getComponents()).forEach(boxedButtonPanel::remove);
                    boxedButtonPanel.add(copyCommand);
                    super.decorateButtonPanel(boxedButtonPanel);
                }
            };
        });

        computeActionsPanel.getShowJson().addActionListener(e -> viewJobJsonDialog(owner, makeJobSubmission()));
        computeActionsPanel.getCompute().addActionListener(e -> startComputing());
        computeActionsPanel.getAbort().addActionListener(e -> owner.dispose());
    }

    private boolean isSingleCompound() {
        return instanceToProcess.size() == 1;
    }

    private JobSubmission makeJobSubmission() {
        return presetFrozen
                ? presetPanel.getSelectedPreset().getJobSubmission()
                : toolsPanel.makeJobSubmission(presetPanel.getSelectedPreset().getJobSubmission(), computeActionsPanel.getRecomputeBox().isSelected());
    }

    private void presetFreeze() {
        presetFrozen = true;
        toolsPanel.disableControls(true);

        computeActionsPanel.getRecomputeBox().setEnabled(false);
        computeActionsPanel.getShowCommand().setEnabled(false);
    }

    private void presetUnfreeze() {
        presetFrozen = false;
        presetPanel.hidePresetBanners();
        toolsPanel.disableControls(false);

        computeActionsPanel.getRecomputeBox().setEnabled(true);
        computeActionsPanel.getShowCommand().setEnabled(true);
    }


    private void activatePreset(String presetName) {
        presetUnfreeze();

        StoredJobSubmission selectedStoredPreset = presetPanel.getPreset(presetName);
        JobSubmission selectedPreset = selectedStoredPreset.getJobSubmission();
        JobSubmission defaultPreset = presetPanel.getDefaultPreset().getJobSubmission();
        try {
            if (selectedStoredPreset.isEditable()) {
                // If custom DBs change, preset will have an outdated list that causes a warning,
                // they will be all selected anyway, so we can ignore it
                Set<String> ignoredHiddenParameters = Set.of();

                Set<String> uiParameters = new HashSet<>(toolsPanel.getAllUIParameterBindings().keySet());
                uiParameters.add("RecomputeResults");

                List<String> hiddenParameters = selectedPreset.getConfigMap().entrySet().stream()
                        .filter(e -> !uiParameters.contains(e.getKey()))
                        .filter(e -> !ignoredHiddenParameters.contains(e.getKey()))
                        .filter(e -> !e.getValue().equals(defaultPreset.getConfigMap().get(e.getKey())))
                        .filter(e -> !(e.getKey().equals("AdductSettings.detectable")
                                && adductsEqual(e.getValue(), defaultPreset.getConfigMap().get(e.getKey()))))
                        .map(e -> e.getKey() + " = " + e.getValue() + "\n")
                        .collect(Collectors.toCollection(ArrayList::new));
                if (!hiddenParameters.isEmpty()) {
                    hiddenParameters.addFirst("Preset specifies parameters that are not visible in the compute dialog:\n");
                    hiddenParameters.add("\nYou can start a computation with this preset, but cannot edit the parameters.");
                    Jobs.runEDTLater(() -> new InfoDialog(owner,
                            GuiUtils.formatToolTip(hiddenParameters),
                            DO_NOT_SHOW_PRESET_HIDDEN_PARAMETERS));
                    presetPanel.showPresetInfoBanner("Preset specifies parameters that are not visible in the compute dialog.");
                    presetFreeze();
                    return;
                }
            }

            // Fall back to default parameters in the GUI if they are missing in the selected preset
            final Map<String, String> configMap = defaultPreset.getConfigMap() != null ? new HashMap<>(defaultPreset.getConfigMap()) : new HashMap<>();
            if (selectedPreset.getConfigMap() != null)
                configMap.putAll(selectedPreset.getConfigMap());

           toolsPanel.applyValuesFromPreset(selectedPreset, configMap);
           computeActionsPanel.getRecomputeBox().setSelected(isSingleCompound() || Boolean.parseBoolean(configMap.get("RecomputeResults")));
        } catch (UnsupportedOperationException e) {
            Jobs.runEDTLater(() -> new InfoDialog(owner,
                    "Preset is not compatible with the compute dialog:<br>" + e.getMessage() + "<br><br>You can start a computation with this preset, but cannot edit the parameters."
            ));
            presetPanel.showPresetInfoBanner(e.getMessage());
            presetFreeze();
        } catch (Exception e) {
            Jobs.runEDTLater(() -> new WarningDialog(owner,
                    "Error loading preset",
                    "The preset cannot be loaded:<br>" + e.getMessage() + "<br><br>Computation with this preset might not work as expected.",
                    null
            ));
            presetPanel.showPresetWarningBanner(e.getMessage());
            presetFreeze();
        } finally {
            presetPanel.getSavePreset().setEnabled(selectedStoredPreset.isEditable() && !presetFrozen && !isSingleCompound());
            presetPanel.getRemovePreset().setEnabled(selectedStoredPreset.isEditable());
        }
    }




    public void startComputing() {
        Window rootOwner = owner.getOwner() != null ? owner.getOwner() : gui.getMainFrame();

        if (toolsPanel.warnNoMethodIsSelected()) return;
        if (toolsPanel.warnNoAdductSelected()) return;


        JobSubmission jobSubmission = makeJobSubmission();
        if (Boolean.TRUE.equals(jobSubmission.isRecompute()) && !isSingleCompound()) {
            QuestionDialog questionDialog = new QuestionDialog(owner, "Recompute?", "<html><body>Do you really want to recompute already computed experiments? <br> All existing results will be lost!</body></html>", DONT_ASK_RECOMPUTE_KEY, ReturnValue.Success);
            jobSubmission.setRecompute(questionDialog.isSuccess());
        }


        if (!PropertyManager.getBoolean(DO_NOT_SHOW_AGAIN_KEY_OUTDATED_PS, false)) {
            //CHECK Server connection
            ConnectionCheck checkResult = CheckConnectionAction.checkConnectionAndLoad(gui);

            if (isConnected(checkResult)) {
                boolean compCheck = Jobs.runInBackgroundAndLoad(rootOwner, "Checking FP version...", () ->
                        gui.getProjectManager().getProjectInfo().isCompatible()).getResult();

                if (!compCheck) {
                    new WarningDialog(owner, "Outdated Fingerprint version!", "<html><body>You are working on an project with an outdated Fingerprint version!<br> " +
                            "CSI:FingerID and CANOPUS are not available; all jobs will be skipped.<br>" +
                            "Project conversion can be selected during import or via the commandline.<br><br>" +
                            "</body></html>", DO_NOT_SHOW_AGAIN_KEY_OUTDATED_PS);
                }
            } else {
                new WarningDialog(owner, "No web service connection!",
                        "<html><body>Could not perform Fingerprint compatibility check <br> " +
                                "due to missing web service connection (see Webservice panel for details). <br> " +
                                "CSI:FingerID and CANOPUS are not available; all corresponding jobs will fail and be skipped." +
                                "</body></html>", DO_NOT_SHOW_AGAIN_KEY_NO_FP_CHECK);


            }
        }

        Jobs.runInBackgroundAndLoad(rootOwner, "Submitting Identification Jobs", new TinyBackgroundJJob<>() {
            @Override
            protected Boolean compute() throws InterruptedException, InvocationTargetException {
                updateProgress(0, 100, 0, "Configuring Computation...");
                //prevent many instance updates caused by multi selection
                Jobs.runEDTLater(() -> gui.getMainFrame().getCompoundList().getCompoundListSelectionModel().clearSelection());

                checkForInterruption();

                List<InstanceBean> finalComps = instanceToProcess;

                Sirius formulaIdParams = jobSubmission.getFormulaIdParams();
                if (formulaIdParams != null && formulaIdParams.isEnabled()) {
                    List<InstanceBean> lowMass = finalComps.stream().filter(i -> i.getIonMass() <= 850).collect(Collectors.toList());
                    int highMass = finalComps.size() - lowMass.size();
                    final AtomicBoolean success = new AtomicBoolean(false);
                    if (highMass > 1) //do not ask for a single compound
                        Jobs.runEDTAndWait(() -> success.set(new QuestionDialog(rootOwner, "High mass Compounds detected!",
                                GuiUtils.formatToolTip("Your analysis contains '" + highMass + "' compounds with a mass higher than 850Da. Fragmentation tree computation may take very long (days) to finish. You might want to exclude compounds with mass >850Da and compute them on individual basis afterwards.", "", "Do you wish to exclude the high mass compounds?"),
                                DO_NOT_SHOW_AGAIN_KEY_S_MASS).isSuccess()));
                    if (success.get())
                        finalComps = lowMass;
                    checkForInterruption();

                    // CHECK ILP SOLVER
                    //check for IPL solver only if it is actually needed during analysis
                    double minMass = finalComps.stream().mapToDouble(InstanceBean::getIonMass).min().orElse(0);

                    if (Optional.of(formulaIdParams).map(Sirius::getUseHeuristic).map(UseHeuristic::getUseOnlyHeuristicAboveMz).map(val -> val > minMass).orElse(true)) {
                        updateProgress(0, 100, 0, "Checking ILP solvers...");
                        Info info = gui.getSiriusClient().infos().getInfo(false, false);
                        if (info.getAvailableILPSolvers().isEmpty()) {
                            String noILPSolver = "Could not load a valid TreeBuilder (ILP solvers), tried '" +
                                    Arrays.toString(TreeBuilderFactory.getBuilderPriorities()) +
                                    "'. You can switch to heuristic tree computation only to compute results without the need of an ILP Solver.";
                            log.error(noILPSolver);
                            new ExceptionDialog(owner, noILPSolver);
                            return false;
                        } else {
                            log.info("Compute trees using {}", info.getAvailableILPSolvers().getFirst());
                        }
                        updateProgress(0, 100, 1, "ILP solver check DONE!");
                    }
                }

                updateProgress(0, 100, 2, "Connection check DONE!");
                checkForInterruption();

                try {
                    // set Input data to jobsubmission
                    if (finalComps != null && !finalComps.isEmpty())
                        jobSubmission.setAlignedFeatureIds(finalComps.stream()
                                .map(InstanceBean::getFeatureId).toList());
                    gui.applySiriusClient((c, pid) -> c.jobs().startJob(pid, jobSubmission, List.of(JobOptField.COMMAND)));
                } catch (Exception e) {
                    log.error("Error when starting Computation.", e);
                    new ExceptionDialog(rootOwner, "Error when starting Computation: " + e.getMessage());
                }

                updateProgress(0, 100, 100, "Computation Configured!");
                return true;
            }
        });
        owner.dispose();
    }

    // todo static helpers
    /**
     * Removes parameters with values equal to the default from the configMap of the passed JobSubmission
     */
    private static JobSubmission stripDefaultValues(@NotNull JobSubmission presetToStrip, @NotNull JobSubmission defaultPreset) {
        Set<String> nonDefaultParameters = presetToStrip.getConfigMap().entrySet().stream()
                .filter(e -> !e.getValue().equals(defaultPreset.getConfigMap().get(e.getKey())))
                .filter(e -> !(e.getKey().equals("AdductSettings.detectable")
                        && adductsEqual(e.getValue(), defaultPreset.getConfigMap().get(e.getKey()))))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        presetToStrip.getConfigMap().keySet().retainAll(nonDefaultParameters);
        return presetToStrip;
    }

    private static boolean adductsEqual(String adducts1, String adducts2) {
        try {
            PrecursorIonType[] a1 = ParameterConfig.convertToCollection(PrecursorIonType.class, adducts1);
            PrecursorIonType[] a2 = ParameterConfig.convertToCollection(PrecursorIonType.class, adducts2);
            Arrays.sort(a1);
            Arrays.sort(a2);
            return Arrays.equals(a1, a2);
        } catch (Exception e) {
            return false;
        }
    }

    //todo extract to dialog window
    private static void viewJobJsonDialog(Window owner, JobSubmission jobSubmission) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Pretty print
            String json = objectMapper.writeValueAsString(jobSubmission);

            JTextArea textArea = new JTextArea(json);
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            textArea.setBackground(Colors.BACKGROUND);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 600));

            JOptionPane.showMessageDialog(owner, scrollPane, "Computation parameters", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(owner, e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
        }
    }
}
