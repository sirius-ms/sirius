

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

package de.unijena.bioinf.ms.gui.compute;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.CheckConnectionAction;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.dialogs.*;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionChecks;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.MessageBanner;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import de.unijena.bioinf.ms.gui.utils.toggleswitch.toggle.JToggleSwitch;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.*;
import lombok.extern.slf4j.Slf4j;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isConnected;
import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isWarningOnly;

@Slf4j
public class BatchComputeDialog extends JDialog {
    public static final String DONT_ASK_RECOMPUTE_KEY = "de.unijena.bioinf.sirius.computeDialog.recompute.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_S_MASS = "de.unijena.bioinf.sirius.computeDialog.sirius.highmass.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_OUTDATED_PS = "de.unijena.bioinf.sirius.computeDialog.projectspace.outdated.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_NO_FP_CHECK = "de.unijena.bioinf.sirius.computeDialog.projectspace.outdated.na.dontAskAgain";
    public static final String DO_NOT_SHOW_PRESET_HIDDEN_PARAMETERS = "de.unijena.bioinf.sirius.computeDialog.preset.hiddenParameters.dontAskAgain";

    // should be the same as returned by the server
    public static final String DEFAULT_PRESET_NAME = "Default";
    public static final String MS1_PRESET_NAME = "MS1";

    public static final String PRESET_FROZEN_MESSAGE = "Could not load preset.";

    // main parts
    private final Box centerPanel;
    private JCheckBox recomputeBox;
    private JButton showCommand;

    // tool configurations
    private ActFormulaIDConfigPanel formulaIDConfigPanel; //Sirius configs
    private ActZodiacConfigPanel zodiacConfigs; //Zodiac configs
    private ActFingerprintAndCanopusConfigPanel fingerprintAndCanopusConfigPanel; //Combines CSI:FingerID predict and CANOPUS configs
    private ActFingerblastConfigPanel csiSearchConfigs; //CSI:FingerID search configs
    private ActMSNovelistConfigPanel msNovelistConfigs; //MsNovelist configs

    // compounds on which the configured Run will be executed
    private final List<InstanceBean> compoundsToProcess;
    private boolean ms2;

    protected boolean isAdvancedView = false;

    private final SiriusGui gui;
    private final JPanel main;

    private PropertyChangeListener connectionListener;

    private JComboBox<String> presetDropdown;
    private Map<String, StoredJobSubmission> allPresets;
    private JobSubmission preset;
    private boolean presetFrozen;
    private MessageBanner presetInfoBanner;
    private MessageBanner presetWarningBanner;
    private MessageBanner connectionMessage;
    private ItemListener presetChangeListener;

    public BatchComputeDialog(SiriusGui gui, List<InstanceBean> compoundsToProcess) {
        super(gui.getMainFrame(), compoundsToProcess.isEmpty() ? "Edit Presets" : "Compute", true);
        gui.getConnectionMonitor().checkConnectionInBackground();
        this.gui = gui;
        this.compoundsToProcess = compoundsToProcess;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        main = new JPanel(new BorderLayout());
        LoadablePanel loadableWrapper = new LoadablePanel(main, "Initializing...");
        loadableWrapper.setLoading(true, true);

        add(loadableWrapper, BorderLayout.CENTER);

        centerPanel = Box.createVerticalBox();
        centerPanel.setBorder(BorderFactory.createEmptyBorder());
        final JScrollPane mainSP = new JScrollPane(centerPanel);
        mainSP.setBorder(BorderFactory.createEtchedBorder());
        mainSP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mainSP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainSP.getVerticalScrollBar().setUnitIncrement(16);
        main.add(mainSP, BorderLayout.CENTER);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(makeBanners(), BorderLayout.NORTH);

        JPanel topLine = new JPanel();
        topLine.setLayout(new BoxLayout(topLine, BoxLayout.LINE_AXIS));

        topLine.add(makePresetPanel());
        topLine.add(makeAdvancedModeToggle());

        northPanel.add(topLine, BorderLayout.CENTER);
        add(northPanel, BorderLayout.NORTH);
        main.add(northPanel, BorderLayout.NORTH);

        loadableWrapper.runInBackgroundAndLoad(() -> {
            ms2 = compoundsToProcess.stream().anyMatch(InstanceBean::hasMsMs) || compoundsToProcess.isEmpty();  // Empty compounds if the dialog is opened to edit presets, ms2 UI should be active
            {
                // make subtool config panels
                formulaIDConfigPanel = new ActFormulaIDConfigPanel(gui, this, compoundsToProcess, ms2, isAdvancedView);
                addConfigPanel("SIRIUS - Molecular Formula Identification", formulaIDConfigPanel);
                final boolean formulasAvailable = compoundsToProcess.stream().allMatch(inst -> inst.getComputedTools().isFormulaSearch());

                zodiacConfigs = new ActZodiacConfigPanel(gui, isAdvancedView, compoundsToProcess.size());
                fingerprintAndCanopusConfigPanel = new ActFingerprintAndCanopusConfigPanel(gui);
                csiSearchConfigs = new ActFingerblastConfigPanel(gui, formulaIDConfigPanel.content);
                msNovelistConfigs = new ActMSNovelistConfigPanel(gui);

                if (!isSingleCompound() && ms2) {
                    addConfigPanel("ZODIAC - Network-based improvement of SIRIUS molecular formula ranking", zodiacConfigs);
                    zodiacConfigs.addToolDependency(formulaIDConfigPanel, () -> formulasAvailable);
                }

                if (ms2) {
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
            // make south panel with Recompute/Compute/Abort
            {
                JPanel southPanel = new JPanel(new GridLayout(1, 3));

                JPanel lsouthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                recomputeBox = new JCheckBox("Recompute already computed tasks?", false);
                recomputeBox.setToolTipText("If checked, all selected compounds will be computed. Already computed analysis steps will be recomputed.");
                lsouthPanel.add(recomputeBox);

                if (isSingleCompound()) recomputeBox.setSelected(true);

                JPanel csouthPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

                showCommand = new JButton("Show Command");
                showCommand.addActionListener(e -> {
                    JobSubmission js = stripDefaultValues(makeJobSubmission());
                    List<String> command = gui.applySiriusClient((c, pid) -> c.jobs().getCommand(js));
                    final String commandString = String.join(" ", command);
                    if (warnNoMethodIsSelected()) return;
                    if (warnNoAdductSelected()) return;
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

                JButton showJson = new JButton("Show JSON");
                showJson.setToolTipText("Open current parameters in a JSON viewer.");
                showJson.addActionListener(e -> viewJobJsonDialog());

                csouthPanel.add(showCommand);
                csouthPanel.add(showJson);

                JPanel rsouthPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton compute = new JButton("Compute");
                if (compoundsToProcess.isEmpty()) {
                    compute.setVisible(false);
                }
                compute.addActionListener(e -> startComputing());
                JButton abort = new JButton("Cancel");
                abort.addActionListener(e -> dispose());

                rsouthPanel.add(compute);
                rsouthPanel.add(abort);

                southPanel.add(lsouthPanel);
                southPanel.add(csouthPanel);
                southPanel.add(rsouthPanel);

                main.add(southPanel, BorderLayout.SOUTH);
            }


            configureActions();

            checkResult = gui.getConnectionMonitor().getCurrentCheckResult();
            if (ConnectionChecks.isInternet(checkResult) && !ConnectionChecks.isLoggedIn(checkResult)) {
                SiriusActions.SIGN_IN.getInstance(gui, true).actionPerformed(null);
                checkResult = gui.getConnectionMonitor().checkConnection();
            }

            reloadPresets();
            activateDefaultPreset();
            updateConnectionBanner(checkResult);

            connectionListener = evt -> {
                if (evt instanceof ConnectionMonitor.ConnectionEvent stateEvent)
                    Jobs.runEDTLater(() -> updateConnectionBanner(stateEvent.getConnectionCheck()));
            };
            gui.getConnectionMonitor().addConnectionListener(connectionListener);
        });

        setPreferredSize(new Dimension(1150, 1024));
        //finalize panel build
        setMaximumSize(GuiUtils.getEffectiveScreenSize(getGraphicsConfiguration()));
        if (getMaximumSize().width < getPreferredSize().width)
            mainSP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private void activateDefaultPreset() {
        activatePreset(ms2 ? DEFAULT_PRESET_NAME : MS1_PRESET_NAME);
    }

    private boolean isSingleCompound() {
        return compoundsToProcess.size() == 1;
    }

    @Override
    public void dispose() {
        try {
            super.dispose();
            if (connectionListener != null)
                gui.getConnectionMonitor().removePropertyChangeListener(connectionListener);
        } finally {
            formulaIDConfigPanel.destroy(); //Sirius configs
            zodiacConfigs.destroy(); //Zodiac configs
            fingerprintAndCanopusConfigPanel.destroy(); //Combines CSI:FingerID predict and CANOPUS configs
            csiSearchConfigs.destroy(); //CSI:FingerID search configs
            msNovelistConfigs.destroy(); //MsNovelist configs
        }
    }

    private MainFrame mf() {
        return gui.getMainFrame();
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
        centerPanel.add(flowContainer);
        return flowContainer;
    }

    private void configureActions() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
        KeyStroke escKey = KeyStroke.getKeyStroke("ESCAPE");
        String enterAction = "compute";
        String escAction = "abort";
        inputMap.put(enterKey, enterAction);
        inputMap.put(escKey, escAction);
        getRootPane().getActionMap().put(enterAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startComputing();
            }
        });
        getRootPane().getActionMap().put(escAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abortComputing();
            }
        });
    }

    private void abortComputing() {
        this.dispose();
    }


    ConnectionCheck checkResult = null;

    private void startComputing() {
        checkResult = null;
        if (warnNoMethodIsSelected()) return;
        if (warnNoAdductSelected()) return;

        if (this.recomputeBox.isSelected() && !isSingleCompound()) {
            QuestionDialog questionDialog = new QuestionDialog(this, "Recompute?", "<html><body>Do you really want to recompute already computed experiments? <br> All existing results will be lost!</body></html>", DONT_ASK_RECOMPUTE_KEY, ReturnValue.Success);
            this.recomputeBox.setSelected(questionDialog.isSuccess());
        }


        if (!PropertyManager.getBoolean(DO_NOT_SHOW_AGAIN_KEY_OUTDATED_PS, false)) {
            //CHECK Server connection
            if (checkResult == null)
                checkResult = CheckConnectionAction.checkConnectionAndLoad(gui);

            if (isConnected(checkResult)) {
                boolean compCheck = Jobs.runInBackgroundAndLoad(mf(), "Checking FP version...", () ->
                        gui.getProjectManager().getProjectInfo().isCompatible()).getResult();

                if (!compCheck) {
                    new WarningDialog(this, "Outdated Fingerprint version!", "<html><body>You are working on an project with an outdated Fingerprint version!<br> " +
                            "CSI:FingerID and CANOPUS are not available; all jobs will be skipped.<br>" +
                            "Project conversion can be selected during import or via the commandline.<br><br>" +
                            "</body></html>", DO_NOT_SHOW_AGAIN_KEY_OUTDATED_PS);
                }
            } else {
                new WarningDialog(this, "No web service connection!",
                        "<html><body>Could not perform Fingerprint compatibility check <br> " +
                                "due to missing web service connection (see Webservice panel for details). <br> " +
                                "CSI:FingerID and CANOPUS are not available; all corresponding jobs will fail and be skipped." +
                                "</body></html>", DO_NOT_SHOW_AGAIN_KEY_NO_FP_CHECK);


            }
        }

        Jobs.runInBackgroundAndLoad(getOwner(), "Submitting Identification Jobs", new TinyBackgroundJJob<>() {
            @Override
            protected Boolean compute() throws InterruptedException, InvocationTargetException {
                updateProgress(0, 100, 0, "Configuring Computation...");
                //prevent many instance updates caused by multi selection
                Jobs.runEDTLater(() -> gui.getMainFrame().getCompoundList().getCompoundListSelectionModel().clearSelection());

                checkForInterruption();
                List<InstanceBean> finalComps = compoundsToProcess;

                if (formulaIDConfigPanel.isToolSelected()) {
                    List<InstanceBean> lowMass = finalComps.stream().filter(i -> i.getIonMass() <= 850).collect(Collectors.toList());
                    int highMass = finalComps.size() - lowMass.size();
                    final AtomicBoolean success = new AtomicBoolean(false);
                    if (highMass > 1) //do not ask for a single compound
                        Jobs.runEDTAndWait(() -> success.set(new QuestionDialog(mf(), "High mass Compounds detected!",
                                GuiUtils.formatToolTip("Your analysis contains '" + highMass + "' compounds with a mass higher than 850Da. Fragmentation tree computation may take very long (days) to finish. You might want to exclude compounds with mass >850Da and compute them on individual basis afterwards.", "", "Do you wish to exclude the high mass compounds?"),
                                DO_NOT_SHOW_AGAIN_KEY_S_MASS).isSuccess()));
                    if (success.get())
                        finalComps = lowMass;
                    checkForInterruption();

                    // CHECK ILP SOLVER
                    //check for IPL solver only if it is actually needed during analysis
                    double minMass = finalComps.stream().mapToDouble(InstanceBean::getIonMass).min().orElse(0);
                    if (((SpinnerNumberModel) formulaIDConfigPanel.getContent().mzHeuristicOnly.getModel()).getNumber().doubleValue() > minMass) {
                        updateProgress(0, 100, 0, "Checking ILP solvers...");
                        Info info = gui.getSiriusClient().infos().getInfo(false, false);
                        if (info.getAvailableILPSolvers().isEmpty()) {
                            String noILPSolver = "Could not load a valid TreeBuilder (ILP solvers), tried '" +
                                    Arrays.toString(TreeBuilderFactory.getBuilderPriorities()) +
                                    "'. You can switch to heuristic tree computation only to compute results without the need of an ILP Solver.";
                            log.error(noILPSolver);
                            new ExceptionDialog(BatchComputeDialog.this, noILPSolver);
                            dispose();
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
                    JobSubmission jobSubmission = makeJobSubmission();
                    if (finalComps != null && !finalComps.isEmpty())
                        jobSubmission.setAlignedFeatureIds(finalComps.stream()
                                .map(InstanceBean::getFeatureId).toList());
                    gui.applySiriusClient((c, pid) -> c.jobs().startJob(pid, jobSubmission, List.of(JobOptField.COMMAND)));
                } catch (Exception e) {
                    log.error("Error when starting Computation.", e);
                    new ExceptionDialog(mf(), "Error when starting Computation: " + e.getMessage());
                }

                updateProgress(0, 100, 100, "Computation Configured!");
                return true;
            }
        });
        dispose();
    }

    /**
     * Removes parameters with values equal to the default from the configMap of the passed JobSubmission
     */
    private JobSubmission stripDefaultValues(JobSubmission js) {
        JobSubmission defaultPreset = allPresets.get(DEFAULT_PRESET_NAME).getJobSubmission();
        Set<String> nonDefaultParameters = js.getConfigMap().entrySet().stream()
                .filter(e -> !e.getValue().equals(defaultPreset.getConfigMap().get(e.getKey())))
                .filter(e -> !(e.getKey().equals("AdductSettings.detectable")
                        && adductsEqual(e.getValue(), defaultPreset.getConfigMap().get(e.getKey()))))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        js.getConfigMap().keySet().retainAll(nonDefaultParameters);
        return js;
    }

    private JobSubmission makeJobSubmission() {
        if (presetFrozen) {
            return preset;
        }

        JobSubmission sub = new JobSubmission();
        sub.setConfigMap(new HashMap<>());
        sub.getConfigMap().putAll(preset.getConfigMap());
        sub.getConfigMap().putAll(getAllUIParameterBindings());

        if (formulaIDConfigPanel.isToolSelected()) {
            if (checkResult == null || isConnected(checkResult) || isWarningOnly(checkResult))
                sub.spectraSearchParams(new SpectralLibrarySearch().enabled(true));
            else
                log.warn("Do not perform spectral matching due to missing server connection.");
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
    private Map<String, String> getAllUIParameterBindings() {
        HashMap<String, String> bindings = Stream.of(formulaIDConfigPanel, zodiacConfigs, fingerprintAndCanopusConfigPanel, csiSearchConfigs, msNovelistConfigs)
                .map(ActivatableConfigPanel::asConfigMap)
                .collect(HashMap::new, HashMap::putAll, HashMap::putAll);
        bindings.put("RecomputeResults", Boolean.toString(recomputeBox.isSelected()));
        return bindings;
    }

    private boolean warnNoMethodIsSelected() {
        if (isAnySelected(formulaIDConfigPanel, zodiacConfigs, fingerprintAndCanopusConfigPanel, csiSearchConfigs, msNovelistConfigs) || presetFrozen) {
            return false;
        } else {
            new WarningDialog(this, "Please select at least one method.");
            return true;
        }
    }

    private boolean isAnySelected(ActivatableConfigPanel<?>... configPanels) {
        for (ActivatableConfigPanel<?> configPanel : configPanels) {
            if (configPanel != null && configPanel.isToolSelected()) return true;
        }
        return false;
    }

    private boolean warnNoAdductSelected() {
        if (formulaIDConfigPanel != null && formulaIDConfigPanel.isToolSelected() && !isAnyAdductSelected(formulaIDConfigPanel)) {
            new WarningDialog(this, "Please select at least one adduct.");
            return true;
        } else {
            return false;
        }
    }

    private boolean isAnyAdductSelected(ActFormulaIDConfigPanel configPanel) {
        return !configPanel.getContent().getSelectedAdducts().isEmpty();
    }

    private void updateConnectionBanner(ConnectionCheck checkResult) {
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

    private void showPresetInfoBanner(String message) {
        presetInfoBanner.setText(message + ". You can start a computation with this preset, but cannot edit the parameters.");
        presetInfoBanner.setVisible(true);
    }

    private void showPresetWarningBanner(String message) {
        presetWarningBanner.setText(message + ". Computation with this preset might not work as expected.");
        presetWarningBanner.setVisible(true);
    }

    private void hidePresetBanners() {
        presetInfoBanner.setVisible(false);
        presetWarningBanner.setVisible(false);
    }

    private JPanel makeAdvancedModeToggle() {
        JToggleSwitch toggle = new JToggleSwitch();
        toggle.setToolTipText("Show/Hide advanced parameters.");
        toggle.setPreferredSize(new Dimension(40, 28));
        toggle.addEventToggleSelected(selected -> {
            isAdvancedView = !isAdvancedView;

            formulaIDConfigPanel.content.setDisplayAdvancedParameters(isAdvancedView);
            zodiacConfigs.content.setDisplayAdvancedParameters(isAdvancedView);
        });

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.add(toggle);
        panel.add(new JLabel("Advanced"));

        return panel;
    }

    private JPanel makePresetPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Preset"));

        presetDropdown = new JComboBox<>();

        panel.add(presetDropdown);

        JButton savePreset = new JButton("Save");
        savePreset.setEnabled(false);
        if (isSingleCompound()) {
            savePreset.setToolTipText("Cannot save presets in single compound mode.");
        } else {
            savePreset.setToolTipText("Update current preset with selected parameters.");
        }


        JButton saveAsPreset = new JButton("Save as");
        if (isSingleCompound()) {
            saveAsPreset.setToolTipText("Cannot save presets in single compound mode.");
            saveAsPreset.setEnabled(false);
        } else {
            saveAsPreset.setToolTipText("Save current selection as a new preset.");
        }

        JButton exportPreset = new JButton("Export");
        exportPreset.setToolTipText("Export the selected preset as JSON\n(NOT the current selection).");

        JButton importPreset = new JButton("Import");
        importPreset.setToolTipText("Import a preset JSON file.");

        JButton removePreset = new JButton("Remove");
        removePreset.setEnabled(false);

        panel.add(savePreset);
        panel.add(saveAsPreset);
        panel.add(removePreset);
        panel.add(Box.createRigidArea(new Dimension(GuiUtils.MEDIUM_GAP, 0)));
        panel.add(exportPreset);
        panel.add(importPreset);

        presetChangeListener = event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                String presetName = (String)event.getItem();
                activatePreset(presetName);

                boolean editable = allPresets.get(presetName).isEditable();
                savePreset.setEnabled(editable && !presetFrozen && !isSingleCompound());
                removePreset.setEnabled(editable);
            }
        };

        savePreset.addActionListener(e -> {
            String presetName = (String) presetDropdown.getSelectedItem();
            JobSubmission currentConfig = makeJobSubmission();
            gui.applySiriusClient((c, pid) -> c.jobs().saveJobConfig(presetName, currentConfig, true, false));
            reloadPresets();
            activatePreset(presetName);
        });

        saveAsPreset.addActionListener(e -> {
            StoredJobSubmission newJobSubmission = savePresetAs(makeJobSubmission(), presetDropdown.getSelectedItem() + "_copy");
            if (newJobSubmission != null) {
                reloadPresets();
                activatePreset(newJobSubmission.getName());
            }
        });

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
                            activatePreset(newJobSubmission.getName());
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
            activateDefaultPreset();
        });

        return panel;
    }

    private static String stripExtension(String name) {
        int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return name;
        }
        return name.substring(0, lastDotIndex);
    }

    private StoredJobSubmission savePresetAs(JobSubmission js, String suggestedName) {
        String newPresetName = (String)JOptionPane.showInputDialog(
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
            Jobs.runEDTLater(() -> new StacktraceDialog(this, gui.getSiriusClient().unwrapErrorMessage(ex), ex));
            return null;
        }
    }

    /**
     * Removes all current presets from the preset dropdown and loads them again, preserving selection if possible.
     * If the previously selected preset was removed, some other preset should be activated after calling this method, otherwise the UI will be in an inconsistent state
     */
    private void reloadPresets() {
        String oldSelection = (String) presetDropdown.getSelectedItem();
        presetDropdown.removeItemListener(presetChangeListener);  // the first item added to the combobox gets selected, and we don't want to activate it immediately
        presetDropdown.removeAllItems();
        allPresets = new HashMap<>();
        List<StoredJobSubmission> configsFromServer = gui.applySiriusClient((c, pid) -> c.jobs().getJobConfigs());
        for (StoredJobSubmission c : configsFromServer) {
            allPresets.put(c.getName(), c);
            presetDropdown.addItem(c.getName());
        }
        if (oldSelection != null && allPresets.containsKey(oldSelection)) {
            presetDropdown.setSelectedItem(oldSelection);
        }
        presetDropdown.addItemListener(presetChangeListener);
    }

    private void activatePreset(String presetName) {
        if (!Objects.equals(presetDropdown.getSelectedItem(), presetName)) {
            presetDropdown.getModel().setSelectedItem(presetName);
            return;
        }
        presetUnfreeze();
        try {
            preset = allPresets.get(presetName).getJobSubmission();
            JobSubmission defaultPreset = allPresets.get(DEFAULT_PRESET_NAME).getJobSubmission();
            if (allPresets.get(presetName).isEditable()) {
                // If custom DBs change, preset will have an outdated list that causes a warning,
                // they will be all selected anyway, so we can ignore it
                Set<String> ignoredHiddenParameters = Set.of("SpectralSearchDB");

                Set<String> uiParameters = getAllUIParameterBindings().keySet();
                List<String> hiddenParameters = preset.getConfigMap().entrySet().stream()
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
                    Jobs.runEDTLater(() -> new InfoDialog(this,
                            GuiUtils.formatToolTip(hiddenParameters),
                            DO_NOT_SHOW_PRESET_HIDDEN_PARAMETERS));
                    showPresetInfoBanner("Preset specifies parameters that are not visible in the compute dialog.");
                    presetFreeze();
                    return;
                }
            }

            // Fall back to default parameters in the GUI if they are missing in the selected preset
            final Map<String, String> configMap = defaultPreset.getConfigMap() != null ? new HashMap<>(defaultPreset.getConfigMap()) : new HashMap<>();
            if (preset.getConfigMap() != null)
                configMap.putAll(preset.getConfigMap());

            csiSearchConfigs.getContent().getStructureSearchStrategy().removeDivergingDatabaseListener();

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

            recomputeBox.setSelected(isSingleCompound() || Boolean.parseBoolean(configMap.get("RecomputeResults")));
        } catch (UnsupportedOperationException e) {
            Jobs.runEDTLater(() -> new InfoDialog(this,
                    "Preset is not compatible with the compute dialog:<br>" + e.getMessage() + "<br><br>You can start a computation with this preset, but cannot edit the parameters."
            ));
            showPresetInfoBanner(e.getMessage());
            presetFreeze();
        } catch (Exception e) {
            Jobs.runEDTLater(() -> new WarningDialog(this,
                    "Error loading preset",
                    "The preset cannot be loaded:<br>" + e.getMessage() + "<br><br>Computation with this preset might not work as expected..",
                    null
            ));
            showPresetWarningBanner(e.getMessage());
            presetFreeze();
        }
    }

    private boolean adductsEqual(String adducts1, String adducts2) {
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

    private void presetFreeze() {
        presetFrozen = true;
        Stream.of(formulaIDConfigPanel, zodiacConfigs, fingerprintAndCanopusConfigPanel, csiSearchConfigs, msNovelistConfigs)
                .forEach(panel -> {
                    if (panel.isToolSelected()) {
                        panel.activationButton.doClick(0);
                    }
                    panel.setButtonEnabled(false, PRESET_FROZEN_MESSAGE);
                });

        recomputeBox.setEnabled(false);
        showCommand.setEnabled(false);
    }

    private void presetUnfreeze() {
        presetFrozen = false;
        hidePresetBanners();
        Stream.of(formulaIDConfigPanel, zodiacConfigs, fingerprintAndCanopusConfigPanel, csiSearchConfigs, msNovelistConfigs)
                .forEach(panel -> panel.setButtonEnabled(true, PRESET_FROZEN_MESSAGE));

        recomputeBox.setEnabled(true);
        showCommand.setEnabled(true);
    }

    private void viewJobJsonDialog() {
        try {
            String json = toJson(makeJobSubmission());

            JTextArea textArea = new JTextArea(json);
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            textArea.setBackground(Colors.BACKGROUND);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 600));

            JOptionPane.showMessageDialog(this, scrollPane, "Computation parameters", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), null, JOptionPane.ERROR_MESSAGE);
        }
    }

    private String toJson(Object obj) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Pretty print
        return objectMapper.writeValueAsString(obj);
    }
}