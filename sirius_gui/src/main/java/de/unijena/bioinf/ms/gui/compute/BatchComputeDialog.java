

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

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.actions.CheckConnectionAction;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.*;
import de.unijena.bioinf.ms.gui.io.LoadController;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.ExperimentEditPanel;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.worker.WorkerWithCharge;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.sirius.Sirius;
import org.jdesktop.swingx.JXTitledSeparator;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

public class BatchComputeDialog extends JDialog /*implements ActionListener*/ {
    public static final String DONT_ASK_RECOMPUTE_KEY = "de.unijena.bioinf.sirius.computeDialog.recompute.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_Z_COMP = "de.unijena.bioinf.sirius.computeDialog.zodiac.compounds.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_Z_MEM = "de.unijena.bioinf.sirius.computeDialog.zodiac.memory.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_S_MASS = "de.unijena.bioinf.sirius.computeDialog.sirius.highmass.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_OUTDATED_PS = "de.unijena.bioinf.sirius.computeDialog.projectspace.outdated.dontAskAgain";
    public static final String DO_NOT_SHOW_AGAIN_KEY_NO_FP_CHECK = "de.unijena.bioinf.sirius.computeDialog.projectspace.outdated.na.dontAskAgain";




    // main parts
    private ExperimentEditPanel editPanel;
    private final Box mainPanel;
    private final JCheckBox recomputeBox;

    // tool configurations
    private final ActFormulaIDConfigPanel formulaIDConfigPanel; //Sirius configs
    private final ActZodiacConfigPanel zodiacConfigs; //Zodiac configs
    private final ActFingerprintConfigPanel csiPredictConfigs; //CSI:FingerID predict configs
    private final ActFingerblastConfigPanel csiSearchConfigs; //CSI:FingerID search configs
    private final ActCanopusConfigPanel canopusConfigPanel; //Canopus configs

    // compounds on which the configured Run will be executed
    private final List<InstanceBean> compoundsToProcess;

    public BatchComputeDialog(MainFrame owner, List<InstanceBean> compoundsToProcess) {
        super(owner, "Compute", true);

        this.compoundsToProcess = compoundsToProcess;

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
            boolean ms2 = compoundsToProcess.stream().anyMatch(inst -> !inst.getMs2Spectra().isEmpty());

            // make subtool config panels
            formulaIDConfigPanel = new ActFormulaIDConfigPanel(this, compoundsToProcess, ms2);
            addConfigPanel("SIRIUS - Molecular Formula Identification", formulaIDConfigPanel);

            zodiacConfigs = new ActZodiacConfigPanel();
            csiPredictConfigs = new ActFingerprintConfigPanel(formulaIDConfigPanel.content.ionizationList.checkBoxList);
            csiSearchConfigs = new ActFingerblastConfigPanel(formulaIDConfigPanel.content.searchDBList.checkBoxList);
            canopusConfigPanel = new ActCanopusConfigPanel();

            if (compoundsToProcess.size() > 1 && ms2){
                zodiacConfigs.addEnableChangeListener((s, enabled) -> {
                    if (enabled) {
                        if (!PropertyManager.getBoolean(DO_NOT_SHOW_AGAIN_KEY_Z_COMP, false)) {
                            if (new QuestionDialog(MainFrame.MF, "Low number of Compounds",
                                    GuiUtils.formatToolTip("Please note that ZODIAC is meant to improve molecular formula annotations on complete LC-MS/MS datasets. Using a low number of compounds may not result in improvements.", "", "Do you wish to continue anyways?"),
                                    DO_NOT_SHOW_AGAIN_KEY_Z_COMP).isAbort()) {
                                zodiacConfigs.activationButton.setSelected(false);
                                return;
                            }
                        }

                        if ((compoundsToProcess.size() > 2000 && (Runtime.getRuntime().maxMemory() / 1024 / 1024 / 1024) < 8)
                                && !PropertyManager.getBoolean(DO_NOT_SHOW_AGAIN_KEY_Z_MEM, false)) {
                            if (new QuestionDialog(MainFrame.MF, "High Memory Consumption",
                                    GuiUtils.formatToolTip("Your ZODIAC analysis contains `" + compoundsToProcess.size() + "` compounds and may therefore consume more system memory than available.", "", "Do you wish to continue anyways?"),
                                    DO_NOT_SHOW_AGAIN_KEY_Z_MEM).isAbort()) {
                                zodiacConfigs.activationButton.setSelected(false);
                                return;
                            }
                        }
                    }


                });
                addConfigPanel("ZODIAC - Network-based improvement of SIRIUS molecular formula ranking", zodiacConfigs);
            }

            if (ms2) {
                JPanel csi = addConfigPanel("CSI:FingerID - Fingerprint Prediction", csiPredictConfigs);
                addConfigPanel("CSI:FingerID - Structure Database Search", csiSearchConfigs, csi);
                addConfigPanel("CANOPUS - Compound Class Prediction", canopusConfigPanel);
            }

            //Make edit panel for single compound mode if needed
            if (compoundsToProcess.size() == 1)
                initSingleExperimentDialog();
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

            JPanel rsouthPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            JButton compute = new JButton("Compute");
            compute.addActionListener(e -> startComputing());
            JButton abort = new JButton("Cancel");
            abort.addActionListener(e -> dispose());
            JButton showCommand = new JButton("Show Command");
            showCommand.addActionListener(e -> {
                final String commandString = String.join(" ", makeCommand(new ArrayList<>()));
                if (warnNoMethodIsSelected()) return;
                new InfoDialog(owner, "Command", GuiUtils.formatToolTip(commandString), null) {
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
            southPanel.add(rsouthPanel);

            this.add(southPanel, BorderLayout.SOUTH);
        }

        //finalize panel build
        setMaximumSize(GuiUtils.getEffectiveScreenSize(getGraphicsConfiguration()));
        if (getMaximumSize().width < getPreferredSize().width)
            mainSP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        configureActions();
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private JPanel addConfigPanel(String header, JPanel configPanel, JPanel appendHorizontally) {
        JPanel stack = new JPanel();
        stack.setLayout(new BorderLayout());
        JXTitledSeparator title = new JXTitledSeparator(header);
        title.setBorder(BorderFactory.createEmptyBorder(GuiUtils.MEDIUM_GAP, 0, GuiUtils.MEDIUM_GAP, GuiUtils.SMALL_GAP));
        stack.add(title, BorderLayout.NORTH);
        stack.add(configPanel, BorderLayout.CENTER);
        appendHorizontally.add(stack);
        return appendHorizontally;
    }

    private JPanel addConfigPanel(String header, JPanel configPanel) {
        JPanel flowContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, GuiUtils.LARGE_GAP, GuiUtils.SMALL_GAP));
        flowContainer.setBorder(BorderFactory.createEmptyBorder());
        addConfigPanel(header, configPanel, flowContainer);
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

    private void saveEdits(InstanceBean ec) {
        Jobs.runInBackgroundAndLoad(this, "Saving changes...", () ->
                LoadController.completeExisting(ec, editPanel));
    }

    ConnectionMonitor.ConnectionCheck checkResult = null;

    private void startComputing() {
        checkResult = null;
        if (warnNoMethodIsSelected()) return;

        if (editPanel != null && compoundsToProcess.size() == 1)
            saveEdits(compoundsToProcess.get(0));

        if (this.recomputeBox.isSelected()) {
            if (!PropertyManager.getBoolean(DONT_ASK_RECOMPUTE_KEY, false) && this.compoundsToProcess.size() > 1) {
                QuestionDialog questionDialog = new QuestionDialog(this, "Recompute?", "<html><body>Do you really want to recompute already computed experiments? <br> All existing results will be lost!</body></html>", DONT_ASK_RECOMPUTE_KEY);
                this.recomputeBox.setSelected(questionDialog.isSuccess());
            }
        }


        if (csiPredictConfigs.isToolSelected() || csiSearchConfigs.isToolSelected() || canopusConfigPanel.isToolSelected() && !PropertyManager.getBoolean(DO_NOT_SHOW_AGAIN_KEY_OUTDATED_PS, false)) {
            //CHECK Server connection
            if (checkResult == null)
                checkResult = CheckConnectionAction.checkConnectionAndLoad();

            if (checkResult.isConnected()) {
                boolean compCheck = Jobs.runInBackgroundAndLoad(MF, "Checking FP version...", new TinyBackgroundJJob<Boolean>() {
                    @Override
                    protected Boolean compute() throws Exception {
                        return MF.ps().checkAndFixDataFiles(this::checkForInterruption);
                    }

                }).getResult();

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
        // todo hotfix to prevent gui from going crazy
        {
            int index = MF.getCompoundListSelectionModel().getMinSelectionIndex();
            MF.getCompoundListSelectionModel().setSelectionInterval(index, index);
        }


        Jobs.runInBackgroundAndLoad(getOwner(), "Submitting Identification Jobs", new TinyBackgroundJJob<>() {
            @Override
            protected Boolean compute() throws InterruptedException, InvocationTargetException {
                updateProgress(0, 100, 0, "Configuring Computation...");
                checkForInterruption();
                List<InstanceBean> finalComps = compoundsToProcess;
                if (formulaIDConfigPanel.isToolSelected()) {
                    List<InstanceBean> lowMass = finalComps.stream().filter(i -> i.getIonMass() <= 850).collect(Collectors.toList());
                    int highMass = finalComps.size() - lowMass.size();
                    final AtomicBoolean success = new AtomicBoolean(false);
                    if (highMass > 1 && !PropertyManager.getBoolean(DO_NOT_SHOW_AGAIN_KEY_S_MASS, false)) //do not ask for a single compound
                        Jobs.runEDTAndWait(() -> success.set(new QuestionDialog(MainFrame.MF, "High mass Compounds detected!",
                                GuiUtils.formatToolTip("Your analysis contains '" + highMass + "' compounds with a mass higher than 850Da. Fragmentation tree computation may take very long (days) to finish. You might want to exclude compounds with mass >850Da and compute them on individual basis afterwards.", "", "Do you wish to exclude the high mass compounds?"),
                                DO_NOT_SHOW_AGAIN_KEY_S_MASS).isSuccess()));
                    if (success.get())
                        finalComps = lowMass;
                }

                checkForInterruption();
                // CHECK ILP SOLVER
                TreeBuilder builder = new Sirius().getMs2Analyzer().getTreeBuilder();
                if (builder == null) {
                    String noILPSolver = "Could not load a valid TreeBuilder (ILP solvers), tried '" + Arrays.toString(TreeBuilderFactory.getBuilderPriorities()) + "'. Please read the installation instructions.";
                    LoggerFactory.getLogger(BatchComputeDialog.class).error(noILPSolver);
                    new ExceptionDialog(BatchComputeDialog.this, noILPSolver);
                    dispose();
                    return false;
                }
                LoggerFactory.getLogger(this.getClass()).info("Compute trees using " + builder);
                updateProgress(0, 100, 1, "ILP solver check DONE!");
                checkForInterruption();

                //CHECK worker availability
                checkConnection();

                updateProgress(0, 100, 2, "Connection check DONE!");
                checkForInterruption();

                try {
                    final List<String> toolList = new ArrayList<>();
                    final List<String> c = makeCommand(toolList);
                    Jobs.runCommand(c, finalComps, String.join(" > ", toolList));
                } catch (Exception e) {
                    new ExceptionDialog(MF, e.getMessage());
                }

                updateProgress(0, 100, 100, "Computation Configured!");
                return true;
            }
        });
        dispose();
    }

    private List<String> makeCommand(final List<String> toolCommands) {
        // create computation parameters
        toolCommands.clear();
        List<String> configCommand = new ArrayList<>();

        configCommand.add("config");
        if (formulaIDConfigPanel != null && formulaIDConfigPanel.isToolSelected()) {
            toolCommands.add(formulaIDConfigPanel.content.toolCommand());
            configCommand.addAll(formulaIDConfigPanel.asParameterList());
        }

        if (zodiacConfigs != null && zodiacConfigs.isToolSelected()) {
            toolCommands.add(zodiacConfigs.content.toolCommand());
            configCommand.addAll(zodiacConfigs.asParameterList());
        }

        if (csiPredictConfigs != null && csiPredictConfigs.isToolSelected()) {
            toolCommands.add(csiPredictConfigs.content.toolCommand());
            configCommand.removeIf(i -> i.startsWith("--AdductSettings.fallback"));
            configCommand.addAll(csiPredictConfigs.asParameterList());
        }


        if (csiSearchConfigs != null && csiSearchConfigs.isToolSelected()) {
            toolCommands.add(csiSearchConfigs.content.toolCommand());
            configCommand.addAll(csiSearchConfigs.asParameterList());
        }

        if (canopusConfigPanel != null && canopusConfigPanel.isToolSelected()) {
            toolCommands.add(canopusConfigPanel.content.toolCommand());
            configCommand.addAll(canopusConfigPanel.asParameterList());
        }

        final List<String> command = new ArrayList<>();
        configCommand.add("--RecomputeResults");
        configCommand.add(String.valueOf(recomputeBox.isSelected()));

        command.addAll(configCommand);
        command.addAll(toolCommands);
        return command;
    }

    private boolean warnNoMethodIsSelected() {
        if (!isAnySelected(formulaIDConfigPanel, zodiacConfigs, csiPredictConfigs, csiSearchConfigs, canopusConfigPanel)) {
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

    private void checkConnection() {
        if (checkResult == null)
            checkResult = CheckConnectionAction.checkConnectionAndLoad();

        if (checkResult != null) {
            if (checkResult.isConnected()) {
                if ((csiPredictConfigs.isToolSelected() || csiSearchConfigs.isToolSelected()) && checkResult.hasWorkerWarning()) {
                    if (checkResult.workerInfo == null ||
                            (!checkResult.workerInfo.supportsAllPredictorTypes(ConnectionMonitor.neededTypes.stream().filter(WorkerWithCharge::isNegative).collect(Collectors.toSet()))
                                    && compoundsToProcess.stream().anyMatch(it -> it.getIonization().isNegative())) ||
                            (!checkResult.workerInfo.supportsAllPredictorTypes(ConnectionMonitor.neededTypes.stream().filter(WorkerWithCharge::isPositive).collect(Collectors.toSet()))
                                    && compoundsToProcess.stream().anyMatch(it -> it.getIonization().isPositive()))
                    ) new WorkerWarningDialog(MF, checkResult.workerInfo == null);
                }
            } else {
                if (formulaIDConfigPanel.content.getFormulaSearchDBs() != null) {
                    new WarnFormulaSourceDialog(MF);
                    formulaIDConfigPanel.content.searchDBList.checkBoxList.uncheckAll();
                }
            }
        } else {
            if (formulaIDConfigPanel.content.getFormulaSearchDBs() != null) {
                new WarnFormulaSourceDialog(MF);
                formulaIDConfigPanel.content.searchDBList.checkBoxList.uncheckAll();
            }
        }
    }

    public void initSingleExperimentDialog() {
        JPanel north = new JPanel(new BorderLayout());
        north.setBorder(BorderFactory.createEmptyBorder(GuiUtils.SMALL_GAP, 0, GuiUtils.SMALL_GAP, 0));

        InstanceBean ec = compoundsToProcess.get(0);
        editPanel = new ExperimentEditPanel(false);
        editPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Edit Input Data"));
        north.add(editPanel, BorderLayout.CENTER);

        //todo beging ugly hack --> we want to manage this by the edit panel instead and fire edit panel events
        editPanel.formulaTF.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                boolean enable = e.getDocument().getLength() == 0;
                formulaIDConfigPanel.content.searchDBList.setEnabled(enable);
                formulaIDConfigPanel.content.candidatesSpinner.setEnabled(enable);
                formulaIDConfigPanel.content.candidatesPerIonSpinner.setEnabled(enable);
            }
        });

        editPanel.ionizationCB.addActionListener(e ->
                formulaIDConfigPanel.content.refreshPossibleIonizations(
                        Collections.singleton(editPanel.getSelectedIonization().getIonization().getName()),
                        formulaIDConfigPanel.isToolSelected())
        );

        formulaIDConfigPanel.addEnableChangeListener((c, e) -> c.refreshPossibleIonizations(Collections.singleton(editPanel.getSelectedIonization().getIonization().getName()), e));

        csiPredictConfigs.content.adductOptions.checkBoxList.addPropertyChangeListener("refresh", evt -> {
            PrecursorIonType ionType = editPanel.getSelectedIonization();
            if (ionType.hasNeitherAdductNorInsource() && !ionType.isIntrinsicalCharged()) {
                csiPredictConfigs.content.adductOptions.setEnabled(csiPredictConfigs.isToolSelected());
            } else {
                csiPredictConfigs.content.adductOptions.checkBoxList.replaceElements(List.of(ionType.toString()));
                csiPredictConfigs.content.adductOptions.checkBoxList.checkAll();
                csiPredictConfigs.content.adductOptions.setEnabled(false);
            }
        });

        editPanel.setData(ec);
        /////// todo ugly hack end
        add(north, BorderLayout.NORTH);
    }

    private static class WarnFormulaSourceDialog extends WarningDialog {
        private final static String DONT_ASK_KEY = PropertyManager.PROPERTY_BASE + ".sirius.computeDialog.formulaSourceWarning.dontAskAgain";
        public static final String FORMULA_SOURCE_WARNING_MESSAGE =
                "<b>Warning:</b> No connection to webservice available! <br>" +
                        "Online databases cannot be used for formula identification.<br> " +
                        "If online databases are selected, the default option <br>" +
                        "(all molecular formulas) will be used instead.";

        public WarnFormulaSourceDialog(Frame owner) {
            super(owner, "<html>" + FORMULA_SOURCE_WARNING_MESSAGE, DONT_ASK_KEY + "</html>");
        }
    }
}