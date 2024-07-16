

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

import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.SpectraSearchOptions;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.CheckConnectionAction;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.dialogs.InfoDialog;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.nightsky.sdk.model.*;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jdesktop.swingx.JXTitledSeparator;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isConnected;
import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isWarningOnly;


public class BatchComputeDialog extends JDialog {
    public static final String DONT_ASK_RECOMPUTE_KEY = "de.unijena.bioinf.sirius.computeDialog.recompute.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_Z_COMP = "de.unijena.bioinf.sirius.computeDialog.zodiac.compounds.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_Z_MEM = "de.unijena.bioinf.sirius.computeDialog.zodiac.memory.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_S_MASS = "de.unijena.bioinf.sirius.computeDialog.sirius.highmass.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_OUTDATED_PS = "de.unijena.bioinf.sirius.computeDialog.projectspace.outdated.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_NO_FP_CHECK = "de.unijena.bioinf.sirius.computeDialog.projectspace.outdated.na.dontAskAgain";


    // main parts
    private Box mainPanel;
    private JCheckBox recomputeBox;

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

    public BatchComputeDialog(SiriusGui gui, List<InstanceBean> compoundsToProcess) {
        super(gui.getMainFrame(), "Compute", true);
        gui.getConnectionMonitor().checkConnectionInBackground();
        this.gui = gui;
        this.compoundsToProcess = compoundsToProcess;
        final boolean ms2 = compoundsToProcess.stream().anyMatch(inst -> !inst.getMsData().getMs2Spectra().isEmpty());
        ActFormulaIDConfigPanel tmp = new ActFormulaIDConfigPanel(gui, this, compoundsToProcess, ms2, isAdvancedView); //needs to be created outside the loading job because it also starts background job that might cause a deadlock otherwise
        Jobs.runInBackgroundAndLoad(this, "Initializing Compute Dialog...", () -> {
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout());

            mainPanel = Box.createVerticalBox();
            mainPanel.setBorder(BorderFactory.createEmptyBorder());
            final JScrollPane mainSP = new JScrollPane(mainPanel);
            mainSP.setBorder(BorderFactory.createEtchedBorder());
            mainSP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            mainSP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            mainSP.getVerticalScrollBar().setUnitIncrement(16);
            add(mainSP, BorderLayout.CENTER);

            {
                // make subtool config panels
                formulaIDConfigPanel = tmp;
                addConfigPanel("SIRIUS - Molecular Formula Identification", formulaIDConfigPanel);

                zodiacConfigs = new ActZodiacConfigPanel(gui, isAdvancedView);
                fingerprintAndCanopusConfigPanel = new ActFingerprintAndCanopusConfigPanel(gui);
                csiSearchConfigs = new ActFingerblastConfigPanel(gui, formulaIDConfigPanel.content);
                msNovelistConfigs = new ActMSNovelistConfigPanel(gui);

                if (compoundsToProcess.size() > 1 && ms2) {
                    zodiacConfigs.addEnableChangeListener((s, enabled) -> {
                        if (enabled) {
                            if (new QuestionDialog(mf(), "Low number of Compounds",
                                    GuiUtils.formatToolTip("Please note that ZODIAC is meant to improve molecular formula annotations on complete LC-MS/MS datasets. Using a low number of compounds may not result in improvements.", "", "Do you wish to continue anyways?"),
                                    DO_NOT_SHOW_AGAIN_KEY_Z_COMP).isCancel()) {
                                zodiacConfigs.activationButton.setSelected(false);
                                return;
                            }


                            if ((compoundsToProcess.size() > 2000 && (Runtime.getRuntime().maxMemory() / 1024 / 1024 / 1024) < 8)) {
                                if (new QuestionDialog(mf(), "High Memory Consumption",
                                        GuiUtils.formatToolTip("Your ZODIAC analysis contains `" + compoundsToProcess.size() + "` compounds and may therefore consume more system memory than available.", "", "Do you wish to continue anyways?"),
                                        DO_NOT_SHOW_AGAIN_KEY_Z_MEM).isCancel()) {
                                    zodiacConfigs.activationButton.setSelected(false);
                                }
                            }
                        }


                    });
                    addConfigPanel("ZODIAC - Network-based improvement of SIRIUS molecular formula ranking", zodiacConfigs);
                }

                if (ms2) {
                    addConfigPanel("Predict properties: CSI:FingerID - Fingerprint Prediction & CANOPUS - Compound Class Prediction", fingerprintAndCanopusConfigPanel);
                    JPanel searchRow = addConfigPanel("CSI:FingerID - Structure Database Search", csiSearchConfigs);
                    addConfigPanelToRow("MSNovelist - De Novo Structure Generation", msNovelistConfigs, searchRow);
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

                //checkConnectionToUrl by default when just one experiment is selected
                if (compoundsToProcess.size() == 1) recomputeBox.setSelected(true);

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
                JButton showCommand = new JButton("Show Command");
                showCommand.addActionListener(e -> {
                    final String commandString = String.join(" ", makeCommand(new ArrayList<>()));
                    if (warnNoMethodIsSelected()) return;
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

                this.add(southPanel, BorderLayout.SOUTH);
            }

            //finalize panel build
            setMaximumSize(GuiUtils.getEffectiveScreenSize(getGraphicsConfiguration()));
            if (getMaximumSize().width < getPreferredSize().width)
                mainSP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            configureActions();

            checkResult = gui.getConnectionMonitor().getCurrentCheckResult();
        });

        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    @Override
    public void dispose() {
        try {
            super.dispose();
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
        mainPanel.add(flowContainer);
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

        if (this.recomputeBox.isSelected()) {
            if (this.compoundsToProcess.size() > 1) {
                QuestionDialog questionDialog = new QuestionDialog(this, "Recompute?", "<html><body>Do you really want to recompute already computed experiments? <br> All existing results will be lost!</body></html>", DONT_ASK_RECOMPUTE_KEY);
                this.recomputeBox.setSelected(questionDialog.isSuccess());
            }
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
                    if (((Double) formulaIDConfigPanel.getContent().mzHeuristicOnly.getValue()) > minMass) {
                        updateProgress(0, 100, 0, "Checking ILP solvers...");
                        Info info = gui.getSiriusClient().infos().getInfo(false, false);
                        if (info.getAvailableILPSolvers().isEmpty()) {
                            String noILPSolver = "Could not load a valid TreeBuilder (ILP solvers), tried '" +
                                    Arrays.toString(TreeBuilderFactory.getBuilderPriorities()) +
                                    "'. You can switch to heuristic tree computation only to compute results without the need of an ILP Solver.";
                            LoggerFactory.getLogger(BatchComputeDialog.class).error(noILPSolver);
                            new ExceptionDialog(BatchComputeDialog.this, noILPSolver);
                            dispose();
                            return false;
                        } else {
                            LoggerFactory.getLogger(this.getClass()).info("Compute trees using " + info.getAvailableILPSolvers().getFirst());
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
                    Job job = gui.applySiriusClient((c, pid) -> c.jobs().startJob(pid, jobSubmission, List.of(JobOptField.COMMAND)));
                } catch (Exception e) {
                    LoggerFactory.getLogger(getClass()).error("Error when starting Computation.", e);
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
            configCommand.add(SpectraSearchOptions.class.getAnnotation(CommandLine.Command.class).name());
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
        // create computation parameters
        JobSubmission sub = new JobSubmission();
        sub.setConfigMap(new HashMap<>());

        if (formulaIDConfigPanel != null && formulaIDConfigPanel.isToolSelected()) {
            if (checkResult == null || isConnected(checkResult) || isWarningOnly(checkResult))
                sub.spectraSearchParams(new SpectralLibrarySearch().enabled(true));
            else
                LoggerFactory.getLogger(getClass()).warn("Do not perform spectral matching due to missing server connection.");
            sub.setFormulaIdParams(new Sirius().enabled(true));
            sub.getConfigMap().putAll(formulaIDConfigPanel.asConfigMap());
        }

        if (zodiacConfigs != null && zodiacConfigs.isToolSelected()) {
            sub.setZodiacParams(new Zodiac().enabled(true));
            sub.getConfigMap().putAll(zodiacConfigs.asConfigMap());
        }

        //canopus prediction included. Must now run before structure database search
        if (fingerprintAndCanopusConfigPanel != null && fingerprintAndCanopusConfigPanel.isToolSelected()) {
            sub.setFingerprintPredictionParams(new FingerprintPrediction().enabled(true));
            sub.setCanopusParams(new Canopus().enabled(true));
            sub.getConfigMap().putAll(fingerprintAndCanopusConfigPanel.asConfigMap());
        }

        if (csiSearchConfigs != null && csiSearchConfigs.isToolSelected()) {
            sub.setStructureDbSearchParams(new StructureDbSearch().enabled(true));
            sub.getConfigMap().putAll(csiSearchConfigs.asConfigMap());
        }

        if (msNovelistConfigs != null && msNovelistConfigs.isToolSelected()) {
            sub.setMsNovelistParams(new MsNovelist().enabled(true));
            sub.getConfigMap().putAll(msNovelistConfigs.asConfigMap());
        }

        sub.setRecompute(recomputeBox.isSelected());
        return sub;
    }

    private boolean warnNoMethodIsSelected() {
        if (!isAnySelected(formulaIDConfigPanel, zodiacConfigs, fingerprintAndCanopusConfigPanel, csiSearchConfigs, msNovelistConfigs)) {
            new WarningDialog(this, "Please select at least one method.");
            return true;
        } else {
            return false;
        }
    }

    private boolean isAnySelected(ActivatableConfigPanel... configPanels) {
        for (ActivatableConfigPanel configPanel : configPanels) {
            if (configPanel != null && configPanel.isToolSelected()) return true;
        }
        return false;
    }

    //todo reenable in the future?
//    private void checkConnection(ConnectionCheck checkResult) {
//       if (checkResult != null) {
//            if (isConnected(checkResult)) {
//                if ((fingerprintAndCanopusConfigPanel.isToolSelected() || csiSearchConfigs.isToolSelected() || msNovelistConfigs.isToolSelected()) && isWorkerWarning(checkResult)) {
//                    if (checkResult.getWorkerInfo() == null ||
//                            (!checkResult.isSupportsNegPredictorTypes()
//                                    && compoundsToProcess.stream().anyMatch(it -> it.getIonType().isNegative())) ||
//
//                            (!checkResult.isSupportsPosPredictorTypes()
//                                    && compoundsToProcess.stream().anyMatch(it -> it.getIonType().isPositive()))
//                    ) new WorkerWarningDialog(gui, checkResult.getWorkerInfo() == null);
//                }
//            } else {
//                if (formulaIDConfigPanel.content.getFormulaSearchDBs() != null) {
//                    new WarnFormulaSourceDialog(mf());
//                    formulaIDConfigPanel.content.getSearchDBList().checkBoxList.uncheckAll();
//                }
//            }
//        } else {
//            if (formulaIDConfigPanel.content.getFormulaSearchDBs() != null) {
//                new WarnFormulaSourceDialog(mf());
//                formulaIDConfigPanel.content.getSearchDBList().checkBoxList.uncheckAll();
//            }
//        }
//    }
//
//    private static class WarnFormulaSourceDialog extends WarningDialog {
//        private final static String DONT_ASK_KEY = PropertyManager.PROPERTY_BASE + ".sirius.computeDialog.formulaSourceWarning.dontAskAgain";
//        public static final String FORMULA_SOURCE_WARNING_MESSAGE =
//                "<b>Warning:</b> No connection to webservice available! <br>" +
//                        "Online databases cannot be used for formula identification.<br> " +
//                        "If online databases are selected, the default option <br>" +
//                        "(all molecular formulas) will be used instead. Spectral library matching will also not be performed.";
//
//        public WarnFormulaSourceDialog(Frame owner) {
//            super(owner, FORMULA_SOURCE_WARNING_MESSAGE, DONT_ASK_KEY);
//        }
//    }
}