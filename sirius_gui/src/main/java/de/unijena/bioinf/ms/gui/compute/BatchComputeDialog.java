

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
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.SpectraSearchOptions;
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
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.*;
import lombok.extern.slf4j.Slf4j;
import org.jdesktop.swingx.JXTitledSeparator;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.awt.event.ItemEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isConnected;
import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isWarningOnly;

@Slf4j
public class BatchComputeDialog extends JDialog {
    public static final String DONT_ASK_RECOMPUTE_KEY = "de.unijena.bioinf.sirius.computeDialog.recompute.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_Z_COMP = "de.unijena.bioinf.sirius.computeDialog.zodiac.compounds.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_Z_MEM = "de.unijena.bioinf.sirius.computeDialog.zodiac.memory.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_S_MASS = "de.unijena.bioinf.sirius.computeDialog.sirius.highmass.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_OUTDATED_PS = "de.unijena.bioinf.sirius.computeDialog.projectspace.outdated.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_NO_FP_CHECK = "de.unijena.bioinf.sirius.computeDialog.projectspace.outdated.na.dontAskAgain";
    public static final String DO_NOT_SHOW_PRESET_HIDDEN_PARAMETERS = "de.unijena.bioinf.sirius.computeDialog.preset.hiddenParameters.dontAskAgain";

    public static final String DEFAULT_PRESET_DISPLAY_NAME = "default";
    public static final String PRESET_FROZEN_MESSAGE = "Could not load preset.";

    // main parts
    private Box centerPanel;
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

    protected JButton toggleAdvancedMode;
    protected boolean isAdvancedView = false;

    private final SiriusGui gui;
    private final JPanel main;
    private final LoadablePanel loadableWrapper;

    private PropertyChangeListener connectionListener;

    private JComboBox<String> presetDropdown;
    private JobSubmission preset;
    private boolean presetFrozen;

    private MessageBanner presetMessage;
    private MessageBanner connectionMessage;

    public BatchComputeDialog(SiriusGui gui, List<InstanceBean> compoundsToProcess) {
        super(gui.getMainFrame(), "Compute", true);
        gui.getConnectionMonitor().checkConnectionInBackground();
        this.gui = gui;
        this.compoundsToProcess = compoundsToProcess;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        main = new JPanel(new BorderLayout());
        loadableWrapper = new LoadablePanel(main, "Initializing...");
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
        northPanel.add(makePresetPanel(), BorderLayout.CENTER);
        add(northPanel, BorderLayout.NORTH);
        main.add(northPanel, BorderLayout.NORTH);

        loadableWrapper.runInBackgroundAndLoad(() -> {
            final boolean ms2 = compoundsToProcess.stream().anyMatch(inst -> Utils.notNullOrEmpty(inst.getMsData().getMs2Spectra()));
            {
                // make subtool config panels
                formulaIDConfigPanel = new ActFormulaIDConfigPanel(gui, this, compoundsToProcess, ms2, isAdvancedView);
                addConfigPanel("SIRIUS - Molecular Formula Identification", formulaIDConfigPanel);
                final boolean formulasAvailable = compoundsToProcess.stream().allMatch(inst -> inst.getComputedTools().isFormulaSearch());

                zodiacConfigs = new ActZodiacConfigPanel(gui, isAdvancedView);
                fingerprintAndCanopusConfigPanel = new ActFingerprintAndCanopusConfigPanel(gui);
                csiSearchConfigs = new ActFingerblastConfigPanel(gui, formulaIDConfigPanel.content);
                msNovelistConfigs = new ActMSNovelistConfigPanel(gui);

                if (!isSingleCompound() && ms2) {
                    zodiacConfigs.addEnableChangeListener((s, enabled) -> {
                        if (enabled) {
                            if (new QuestionDialog(mf(), "Low number of Compounds",
                                    GuiUtils.formatToolTip("Please note that ZODIAC is meant to improve molecular formula annotations on complete LC-MS/MS datasets. Using a low number of compounds may not result in improvements.", "", "Do you wish to continue anyways?"),
                                    DO_NOT_SHOW_AGAIN_KEY_Z_COMP, ReturnValue.Success).isCancel()) {
                                zodiacConfigs.activationButton.setSelected(false);
                                return;
                            }


                            if ((compoundsToProcess.size() > 2000 && (Runtime.getRuntime().maxMemory() / 1024 / 1024 / 1024) < 8)) {
                                if (new QuestionDialog(mf(), "High Memory Consumption",
                                        GuiUtils.formatToolTip("Your ZODIAC analysis contains `" + compoundsToProcess.size() + "` compounds and may therefore consume more system memory than available.", "", "Do you wish to continue anyways?"),
                                        DO_NOT_SHOW_AGAIN_KEY_Z_MEM, ReturnValue.Success).isCancel()) {
                                    zodiacConfigs.activationButton.setSelected(false);
                                }
                            }
                        }


                    });
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
                    formulaIDConfigPanel.addEnableChangeListener((c, enabled) -> {
                        if (enabled && !fingerprintAndCanopusConfigPanel.isToolSelected() && (csiSearchConfigs.isToolSelected() || msNovelistConfigs.isToolSelected())) {
                            fingerprintAndCanopusConfigPanel.activationButton.doClick(0);
                            fingerprintAndCanopusConfigPanel.showAutoEnableInfoDialog(fingerprintAndCanopusConfigPanel.toolName + " is activated because a downstream tool needs its input, which would be deleted by running " + formulaIDConfigPanel.toolName + ".");
                        }
                    });
                }
            }
            // make south panel with Recompute/Compute/Abort
            {
                JPanel southPanel = new JPanel();
                southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.LINE_AXIS));

                JPanel lsouthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
                recomputeBox = new JCheckBox("Recompute already computed tasks?", false);
                recomputeBox.setToolTipText("If checked, all selected compounds will be computed. Already computed analysis steps will be recomputed.");
                lsouthPanel.add(recomputeBox);

                if (isSingleCompound()) recomputeBox.setSelected(true);

                JPanel csouthPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
                final String SHOW_ADVANCED = "Show advanced settings";
                final String HIDE_ADVANCED = "Hide advanced settings";
                toggleAdvancedMode = new JButton(SHOW_ADVANCED);
                isAdvancedView = false;
                toggleAdvancedMode.addActionListener(e -> {
                    isAdvancedView = !isAdvancedView;
                    if (isAdvancedView) {
                        toggleAdvancedMode.setText(HIDE_ADVANCED);
                    } else {
                        toggleAdvancedMode.setText(SHOW_ADVANCED);
                    }

                    formulaIDConfigPanel.content.setDisplayAdvancedParameters(isAdvancedView);
                    zodiacConfigs.content.setDisplayAdvancedParameters(isAdvancedView);
                });
                csouthPanel.add(toggleAdvancedMode);

                JPanel rsouthPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
                JButton compute = new JButton("Compute");
                compute.addActionListener(e -> startComputing());
                JButton abort = new JButton("Cancel");
                abort.addActionListener(e -> dispose());
                showCommand = new JButton("Show Command");
                showCommand.addActionListener(e -> {
                    final String commandString = String.join(" ", makeCommand(new ArrayList<>()));
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

                rsouthPanel.add(showCommand);
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

            activatePreset(DEFAULT_PRESET_DISPLAY_NAME);
            updateConnectionBanner(checkResult);

            connectionListener = evt -> {
                if (evt instanceof ConnectionMonitor.ConnectionStateEvent stateEvent)
                    Jobs.runEDTLater(() -> updateConnectionBanner(stateEvent.getConnectionCheck()));
            };
            gui.getConnectionMonitor().addConnectionStateListener(connectionListener);
        });

        setPreferredSize(new Dimension(1125, 1000));
        //finalize panel build
        setMaximumSize(GuiUtils.getEffectiveScreenSize(getGraphicsConfiguration()));
        if (getMaximumSize().width < getPreferredSize().width)
            mainSP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
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

    //todo nightsky: add endpoint to create command without submission to to NIghtsky API (dry run or try submission?) or create from submisstion in GUI.
    @Deprecated
    private List<String> makeCommand(final List<String> toolCommands) {
        // create computation parameters
        toolCommands.clear();
        List<String> configCommand = new ArrayList<>();

        configCommand.add("config");
        if (formulaIDConfigPanel != null && formulaIDConfigPanel.isToolSelected()) {
            toolCommands.add(SpectraSearchOptions.class.getAnnotation(CommandLine.Command.class).name());
            toolCommands.add(formulaIDConfigPanel.content.toolCommand());
            configCommand.addAll(formulaIDConfigPanel.asParameterList());
        }

        if (zodiacConfigs != null && zodiacConfigs.isToolSelected()) {
            toolCommands.add(zodiacConfigs.content.toolCommand());
            configCommand.addAll(zodiacConfigs.asParameterList());
        }

        //canopus prediction included. Must now run before structure database search
        if (fingerprintAndCanopusConfigPanel != null && fingerprintAndCanopusConfigPanel.isToolSelected()) {
            toolCommands.addAll(fingerprintAndCanopusConfigPanel.content.toolCommands());
            configCommand.addAll(fingerprintAndCanopusConfigPanel.asParameterList());
        }

        if (csiSearchConfigs != null && csiSearchConfigs.isToolSelected()) {
            toolCommands.add(csiSearchConfigs.content.toolCommand());
            configCommand.addAll(csiSearchConfigs.asParameterList());
        }

        if (msNovelistConfigs != null && msNovelistConfigs.isToolSelected()) {
            toolCommands.add(msNovelistConfigs.content.toolCommand());
            configCommand.addAll(msNovelistConfigs.asParameterList());
        }

        List<String> command = new ArrayList<>();
        configCommand.add("--RecomputeResults=" + recomputeBox.isSelected());

        command.addAll(configCommand);
        command.addAll(toolCommands);
        command = command.stream().map(s -> s.replaceAll("\\s+", "")).collect(Collectors.toList());
        return command;
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
        presetMessage = new MessageBanner("Preset sets hidden parameters: You can start a computation with this preset, but cannot edit the parameters.", MessageBanner.BannerType.INFO);
        presetMessage.setVisible(false);

        connectionMessage = new MessageBanner();
        connectionMessage.setVisible(false);

        JPanel bannerPanel = new JPanel(new BorderLayout());
        bannerPanel.add(connectionMessage, BorderLayout.NORTH);
        bannerPanel.add(presetMessage, BorderLayout.SOUTH);
        return bannerPanel;
    }

    private JPanel makePresetPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Preset"));

        presetDropdown = new JComboBox<>();
        reloadPresets();

        panel.add(presetDropdown);

        JButton savePreset = new JButton("Save");
        savePreset.setEnabled(false);
        if (isSingleCompound()) {
            savePreset.setToolTipText("Cannot save presets in single compound mode");
        } else {
            savePreset.setToolTipText("Update current preset with selected parameters");
        }


        JButton saveAsPreset = new JButton("Save as");
        if (isSingleCompound()) {
            saveAsPreset.setToolTipText("Cannot save presets in single compound mode");
            saveAsPreset.setEnabled(false);
        } else {
            saveAsPreset.setToolTipText("Save current selection as a new preset");
        }

        JButton viewPreset = new JButton("View");
        viewPreset.addActionListener(e -> viewPresetDialog());

        JButton removePreset = new JButton("Remove");
        removePreset.setEnabled(false);

        panel.add(savePreset);
        panel.add(saveAsPreset);
        panel.add(viewPreset);
        panel.add(removePreset);

        presetDropdown.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                String presetName = (String)event.getItem();
                activatePreset(presetName);

                boolean defaultSelected = presetName.equals(DEFAULT_PRESET_DISPLAY_NAME);
                savePreset.setEnabled(!defaultSelected && !presetFrozen && !isSingleCompound());
                removePreset.setEnabled(!defaultSelected);
            }
        });

        savePreset.addActionListener(e -> {
            String presetName = (String) presetDropdown.getSelectedItem();
            JobSubmission currentConfig = makeJobSubmission();
            gui.applySiriusClient((c, pid) -> c.jobs().saveJobConfig(presetName, currentConfig, true));
            activatePreset(presetName);
        });

        saveAsPreset.addActionListener(e -> {

            String newPresetName = (String)JOptionPane.showInputDialog(
                    this,
                    "New preset name",
                    null,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    presetDropdown.getSelectedItem() + "_copy");

            if (newPresetName != null && !newPresetName.isBlank()) {
                JobSubmission currentConfig = makeJobSubmission();
                try {
                    String createdPresetName = gui.applySiriusClient((c, pid) -> c.jobs().saveJobConfig(newPresetName, currentConfig, false));
                    reloadPresets();
                    presetDropdown.setSelectedItem(createdPresetName);
                } catch (Exception ex) {
                    Jobs.runEDTLater(() -> new StacktraceDialog(this, ex.getMessage(), ex));
                }
            }
        });

        removePreset.addActionListener(e -> {
            String presetName = (String) presetDropdown.getSelectedItem();
            gui.acceptSiriusClient((c, pid) -> c.jobs().deleteJobConfig(presetName));
            reloadPresets();
        });

        return panel;
    }

    /**
     * Removes all current presets from the preset dropdown and loads them again
     */
    private void reloadPresets() {
        presetDropdown.removeAllItems();
        List<String> presetNames = new ArrayList<>();
        presetNames.add(DEFAULT_PRESET_DISPLAY_NAME);
        presetNames.addAll(gui.applySiriusClient((c, pid) -> c.jobs().getJobConfigNames()));
        presetNames.forEach(presetDropdown::addItem);
    }

    private void activatePreset(String presetName) {
        presetUnfreeze();
        try {
            JobSubmission defaultPreset = gui.applySiriusClient((c, pid) -> c.jobs().getDefaultJobConfig(true, true));
            boolean defaultSelected = presetName.equals(DEFAULT_PRESET_DISPLAY_NAME);
            if (defaultSelected) {
                preset = defaultPreset;
            } else {
                preset = gui.applySiriusClient((c, pid) -> c.jobs().getJobConfig(presetName, true, true));
                Set<String> uiParameters = getAllUIParameterBindings().keySet();
                List<String> hiddenParameters = preset.getConfigMap().entrySet().stream()
                        .filter(e -> !uiParameters.contains(e.getKey()))
                        .filter(e -> !e.getValue().equals(defaultPreset.getConfigMap().get(e.getKey())))
                        .filter(e -> !(e.getKey().equals("AdductSettings.detectable")
                                && adductsEqual(e.getValue(), defaultPreset.getConfigMap().get(e.getKey()))))
                        .map(e -> e.getKey() + " = " + e.getValue() + "\n")
                        .collect(Collectors.toCollection(ArrayList::new));
                if (!hiddenParameters.isEmpty()) {
                    hiddenParameters.addFirst("Preset sets hidden parameters:\n");
                    hiddenParameters.add("\nYou can start a computation with this preset, but cannot edit the parameters.");
                    Jobs.runEDTLater(() -> new InfoDialog(this, "Preset sets hidden parameters", GuiUtils.formatToolTip(hiddenParameters),
                            DO_NOT_SHOW_PRESET_HIDDEN_PARAMETERS));
                    presetFreeze();
                    return;
                }
            }

            Map<String, String> configMap = preset.getConfigMap();

            formulaIDConfigPanel.applyValuesFromPreset(preset.getFormulaIdParams() != null && Boolean.TRUE.equals(preset.getFormulaIdParams().isEnabled()), configMap, defaultSelected);
            zodiacConfigs.applyValuesFromPreset(preset.getZodiacParams() != null && Boolean.TRUE.equals(preset.getZodiacParams().isEnabled()), configMap);

            boolean fpEnabled = preset.getFingerprintPredictionParams() != null && preset.getFingerprintPredictionParams().isEnabled();
            boolean canopusEnabled = preset.getCanopusParams() != null && preset.getCanopusParams().isEnabled();
            if (fpEnabled != canopusEnabled) {
                throw new UnsupportedOperationException("Fingerprint and Canopus are not enabled/disabled simultaneously");
            }
            fingerprintAndCanopusConfigPanel.applyValuesFromPreset(fpEnabled, configMap);
            csiSearchConfigs.applyValuesFromPreset(preset.getStructureDbSearchParams() != null && Boolean.TRUE.equals(preset.getStructureDbSearchParams().isEnabled()), configMap);
            msNovelistConfigs.applyValuesFromPreset(preset.getMsNovelistParams() != null && Boolean.TRUE.equals(preset.getMsNovelistParams().isEnabled()), configMap);

            recomputeBox.setSelected(Boolean.parseBoolean(configMap.get("RecomputeResults")));
        } catch (Exception e) {
            Jobs.runEDTLater(() -> new WarningDialog(this,
                    "Error When loading preset",
                    "The preset cannot be loaded:\n" + e.getMessage() + "\n\nYou can start a computation with this preset, but cannot edit the parameters.",
                    null
            ));
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
        presetMessage.setVisible(true);
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
        presetMessage.setVisible(false);
        Stream.of(formulaIDConfigPanel, zodiacConfigs, fingerprintAndCanopusConfigPanel, csiSearchConfigs, msNovelistConfigs)
                .forEach(panel -> panel.setButtonEnabled(true, PRESET_FROZEN_MESSAGE));

        recomputeBox.setEnabled(true);
        showCommand.setEnabled(true);
    }

    private void viewPresetDialog() {
        try {
            String json = toJson(preset);
            String presetName = (String) presetDropdown.getSelectedItem();

            JTextArea textArea = new JTextArea(json);
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            textArea.setBackground(Colors.BACKGROUND);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 600));

            JOptionPane.showMessageDialog(this, scrollPane, "Preset source: " + presetName, JOptionPane.PLAIN_MESSAGE);
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