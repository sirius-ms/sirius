/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.Run;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.io.projectspace.GPSMFactory;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceFactory;
import de.unijena.bioinf.ms.frontend.subtools.RootOptionsCLI;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.gui.actions.CheckConnectionAction;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.*;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.ExperimentEditPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.Ms1Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.Sirius;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

public class BatchComputeDialog extends JDialog implements ActionListener {
    public static final String DONT_ASK_RECOMPUTE_KEY = "de.unijena.bioinf.sirius.computeDialog.recompute.dontAskAgain";

    private JButton compute;
    private JButton abort;

    private JCheckBox recompute;

    private JButton elementAutoDetect = null;

    private ElementsPanel elementPanel;
    private ExperimentEditPanel editPanel;
    private SearchProfilePanel searchProfilePanel; //Sirius configs
    private FingerIDComputationPanel csiOptions; //FingerIS configs

    private MainFrame owner;
    List<InstanceBean> compoundsToProcess;

    private Sirius sirius;
    private boolean success;

    public BatchComputeDialog(MainFrame owner, List<InstanceBean> compoundsToProcess) {
        super(owner, "compute", true);
        this.owner = owner;
        this.compoundsToProcess = compoundsToProcess;
        this.success = false;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());


        Box mainPanel = Box.createVerticalBox();
        add(mainPanel, BorderLayout.CENTER);
        //mainpanel done


        // set list of detectable elements
        this.sirius = ApplicationCore.SIRIUS_PROVIDER.sirius();
        List<Element> detectableElements = new ArrayList<>(sirius.getMs1Preprocessor().getSetOfPredictableElements());
        detectableElements.sort(Comparator.naturalOrder());


        searchProfilePanel = new SearchProfilePanel(this, compoundsToProcess);
        mainPanel.add(searchProfilePanel);
        searchProfilePanel.formulaCombobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                SearchableDatabase source = searchProfilePanel.getFormulaSource();
                enableElementSelection(source == null);
                if (!csiOptions.isEnabled()) csiOptions.dbSelectionOptions.setDb(source);
            }
        });

        csiOptions = new FingerIDComputationPanel(SearchableDatabases.getAvailableDatabases(), searchProfilePanel.ionizationPanel.checkBoxList, true, true);
        if (!csiOptions.isEnabled()) csiOptions.dbSelectionOptions.setDb(searchProfilePanel.getFormulaSource());
        csiOptions.setMaximumSize(csiOptions.getPreferredSize());

        if (compoundsToProcess.size() > 1) {
            ///////////////////Multi Element//////////////////////
            elementPanel = new ElementsPanel(this, 4, detectableElements);
            add(elementPanel, BorderLayout.NORTH);
            /////////////////////////////////////////////
        } else {
            initSingleExperimentDialog(detectableElements);
        }


        JPanel stack = new JPanel();
        stack.setLayout(new BorderLayout());
        stack.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "CSI:FingerID - Structure Elucidation"));

        stack.add(csiOptions, BorderLayout.CENTER);
        mainPanel.add(stack);


        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.LINE_AXIS));

        JPanel lsouthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        recompute = new JCheckBox("Recompute already computed tasks?", false);
        recompute.setToolTipText("If checked, all selected compounds will be computed. Already computed analysis steps will be recomputed.");
        lsouthPanel.add(recompute);

        //checkConnectionToUrl by default when just one experiment is selected
        if (compoundsToProcess.size() == 1) recompute.setSelected(true);

        JPanel rsouthPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        compute = new JButton("Compute");
        compute.addActionListener(this);
        abort = new JButton("Abort");
        abort.addActionListener(this);
        rsouthPanel.add(compute);
        rsouthPanel.add(abort);

        southPanel.add(lsouthPanel);
        southPanel.add(rsouthPanel);

        this.add(southPanel, BorderLayout.SOUTH);

        {
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


        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());
        setVisible(true);

    }

    public void enableElementSelection(boolean enabled) {
        elementPanel.enableElementSelection(enabled);
        if (elementAutoDetect != null)
            elementAutoDetect.setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == abort) {
            this.dispose();
        } else if (e.getSource() == this.compute) {
            if (editPanel != null && compoundsToProcess.size() == 1)
                saveEdits(compoundsToProcess.get(0));
            startComputing();
        } else if (e.getSource() == elementAutoDetect) {
            String notWorkingMessage = "Element detection requires MS1 spectrum with isotope pattern.";
            InstanceBean ec = compoundsToProcess.get(0);
            if (!ec.getMs1Spectra().isEmpty() || ec.getMergedMs1Spectrum() != null) {
                final Ms1Preprocessor pp = sirius.getMs1Preprocessor();
                ProcessedInput pi = pp.preprocess(ec.getExperiment());

                pi.getAnnotation(FormulaConstraints.class).
                        ifPresentOrElse(c -> {
                                    final Set<Element> pe = pp.getSetOfPredictableElements();
                                    for (Element element : c.getChemicalAlphabet()) {
                                        if (!pe.contains(element)) {
                                            c.setLowerbound(element, 0);
                                            c.setUpperbound(element, 0);
                                        }
                                    }
                                    elementPanel.setSelectedElements(c);
                                },
                                () -> new ExceptionDialog(this, notWorkingMessage)
                        );

            } else {
                new ExceptionDialog(this, notWorkingMessage);
            }
        }
    }

    private void abortComputing() {
        this.dispose();
    }

    private void saveEdits(InstanceBean ec) {
        Jobs.runInBackgroundAndLoad(this, "Saving changes...", () -> {
            final InstanceBean.Setter expSetter = ec.set();
            if (editPanel.validateFormula()) {
                final MolecularFormula nuFormula = editPanel.getMolecularFormula();
                expSetter.setMolecularFormula(nuFormula);
            }

            final double ionMass = editPanel.getSelectedIonMass();
            if (ionMass > 0)
                expSetter.setIonMass(ionMass);
            expSetter.setIonization(editPanel.getSelectedIonization());
            expSetter.setName(editPanel.getExperiementName());
            expSetter.apply(); //applies changes to experiment an writes it to projectspace
        });
    }

    private void startComputing() {
        if (recompute.isSelected()) {
            boolean isSuccsess = true;
            if (!PropertyManager.getBoolean(DONT_ASK_RECOMPUTE_KEY, false) && this.compoundsToProcess.size() > 1) {
                QuestionDialog questionDialog = new QuestionDialog(this, "<html><body>Do you really want to recompute already computed experiments? <br> All existing results will be lost!</body></html>", DONT_ASK_RECOMPUTE_KEY);
                isSuccsess = questionDialog.isSuccess();
            }

            //reset status of already computed values to uncomputed if needed
            /*if (isSuccsess) {
                final Iterator<InstanceBean> compounds = this.compoundsToProcess.iterator();
                while (compounds.hasNext()) {
                    final InstanceBean ec = compounds.next();
                    ec.setSiriusComputeState(ComputingStatus.UNCOMPUTED);
                    ec.setBestHit(null);
                    ec.getMs2Experiment().clearAllAnnotations();
                }
            }*/
            //todo implement compute state handling
        }

        //CHECK worker availability
        checkConnection();

        //collect job parameter from view
        final SearchProfilePanel.Instruments instrument = searchProfilePanel.getInstrument();
        final SearchableDatabase searchableDatabase = searchProfilePanel.getFormulaSource();
        final FormulaConstraints constraints = elementPanel.getElementConstraints();
        final List<Element> elementsToAutoDetect = elementPanel.individualAutoDetect ? elementPanel.getElementsToAutoDetect() : Collections.EMPTY_LIST;
        final double ppm = searchProfilePanel.getPpm();
        final int candidates = searchProfilePanel.getNumberOfCandidates();
        ////////////////////////////////////////////////////////////////

        // CHECK ILP SOLVER
        TreeBuilder builder = new Sirius().getMs2Analyzer().getTreeBuilder();
        if (builder == null) {
            String noILPSolver = "Could not load a valid TreeBuilder (ILP solvers) " + Arrays.toString(TreeBuilderFactory.getBuilderPriorities()) + ". Please read the installation instructions.";
            LoggerFactory.getLogger(this.getClass()).error(noILPSolver);
            new ErrorReportDialog(this, noILPSolver);
            dispose();
            return;
        }
        LoggerFactory.getLogger(this.getClass()).info("Compute trees using " + builder);


        Jobs.runInBackgroundAndLoad(owner, "Submitting Identification Jobs", new TinyBackgroundJJob() {
            @Override
            protected Object compute() throws InterruptedException {
                //entspricht setup() Methode
                final Iterator<InstanceBean> compounds = compoundsToProcess.iterator();
//                final int max = compoundsToProcess.size();
//                int progress = 0;

//                while (compounds.hasNext()) {
//                    final InstanceBean ec = compounds.next();
//                    checkForInterruption();
//                    if (ec.isUncomputed() || ec.getResults().isEmpty()) {
                updateProgress(0, 1, 0, "Configuring Computation...");
                        checkForInterruption();

                try {
                    final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
                    WorkflowBuilder<RootOptionsCLI> wfBuilder = new WorkflowBuilder<>(new RootOptionsCLI(configOptionLoader, new GPSMFactory()), configOptionLoader);
                    Run computation = new Run(wfBuilder);

                    // create computation parameters
                    List<String> configs = new ArrayList<>();
                    configs.add("config");
//                    List<String> tools = new ArrayList<>();

                    configs.add("--AlgorithmProfile=" + instrument.profile);
                    configs.add("--MS2MassDeviation.allowedMassDeviation=" + ppm + "ppm");

                    configs.add("--FormulaSearchDB=" + searchableDatabase.name());
                    configs.add("--StructureSearchDB=" + csiOptions.dbSelectionOptions.getDb().name());


                    FormulaConstraints c = constraints;
                    if (searchProfilePanel.restrictToOrganics())
                        c = constraints.intersection(new FormulaConstraints("C,H,N,O,P,S,B,Br,Cl,F,I"));
                    configs.add("--FormulaSettings.fallback=" + c.toString());
                    configs.add("--FormulaSettings.detectable=" + elementsToAutoDetect.stream().map(Element::toString).collect(Collectors.joining(",")));

                    configs.add("--AdductSettings.enforced=" + csiOptions.getPossibleAdducts().toString());

                    configs.add("--NumberOfCandidates=" + candidates);
//                            configs.add("--NumberOfCandidatesPerIon=" + candidatesPerIon);

//                            configs.add("--NumberOfStructureCandidates=" + structureCandidates);

                    configs.add("--RecomputeResults=" + recompute.isSelected());


                    configs.add("sirius");

//                            if (zodiac)
//                                configs.add("zodiac");

                    if (csiOptions.isCSISelected())
                        configs.add("fingerid");

//                            if (zodiac)
//                            configs.add("canopus");


                    computation.parseArgs(configs.toArray(String[]::new));
                    Jobs.runInBackground(computation::compute);//todo make som nice head job that does some organizing stuff
                } catch (IOException e) {
                    e.printStackTrace();
                }


                        /*//prepare input data for identication
                        PrepareSiriusIdentificationInputJob prepareJob = new PrepareSiriusIdentificationInputJob(
                                ec,
                                instrument,
                                ppm,
                                searchProfilePanel.restrictToOrganics(),
                                searchableDatabase,
                                new FormulaConstraints(constraints),
                                Collections.unmodifiableList(elementsToAutoDetect),
                                searchProfilePanel.getPossibleIonModes(),
                                csiOptions.getPossibleAdducts()
                        );
                        Jobs.submit(prepareJob);*/

                      /*  SiriusIdentificationGuiJob identificationJob = null;
                        if (!ec.isComputed()) {
                            identificationJob = new SiriusIdentificationGuiJob(instrument.profile, candidates, ec);
                            identificationJob.addRequiredJob(prepareJob);
                            Jobs.submit(identificationJob);
                        }*/

                        /*if (csiOptions.isCSISelected() && ec.getBestHit() == null) {
                            FingerIDSearchGuiJob fingeridJob = new FingerIDSearchGuiJob(csiOptions.dbSelectionOptions.getDb(), ec);
                            fingeridJob.addRequiredJob(identificationJob);
                            fingeridJob.addRequiredJob(prepareJob);
                            Jobs.submit(fingeridJob);
                        }*/
//                    }

                updateProgress(0, 1, 1, "DONE!");
//                }
                return true;
            }
        });
        dispose();
    }

    private void checkConnection() {
        final @Nullable ConnectionMonitor.ConnetionCheck cc = CheckConnectionAction.checkConnectionAndLoad();

        if (cc != null) {
            if (cc.isConnected()) {
                if (csiOptions.isCSISelected() && cc.hasWorkerWarning()) {
                    new WorkerWarningDialog(MF, cc.workerInfo == null);
                }
            } else {
                if (searchProfilePanel.getFormulaSource() != null) {
                    new WarnFormulaSourceDialog(MF);
                    searchProfilePanel.formulaCombobox.setSelectedIndex(0);
                }
            }
        } else {
            if (searchProfilePanel.getFormulaSource() != null) {
                new WarnFormulaSourceDialog(MF);
                searchProfilePanel.formulaCombobox.setSelectedIndex(0);
            }
        }
    }

    public boolean isSuccessful() {
        return this.success;
    }

    public void initSingleExperimentDialog(List<Element> detectableElements) {
        JPanel north = new JPanel(new BorderLayout());

        InstanceBean ec = compoundsToProcess.get(0);
        editPanel = new ExperimentEditPanel();
        editPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Edit Input Data"));
        north.add(editPanel, BorderLayout.NORTH);

        //todo beging ugly hack --> we want to manage this by the edit panel instead and fire eit panel events
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
                searchProfilePanel.formulaCombobox.setEnabled(enable);
                searchProfilePanel.candidatesSpinner.setEnabled(enable);
            }
        });

        editPanel.ionizationCB.addActionListener(e -> {
            PrecursorIonType ionType = editPanel.getSelectedIonization();
            searchProfilePanel.refreshPossibleIonizations(Collections.singleton(ionType.getIonization().getName()));
            pack();
        });


        csiOptions.adductOptions.checkBoxList.addPropertyChangeListener("refresh", evt -> {
            PrecursorIonType ionType = editPanel.getSelectedIonization();
            if (!ionType.getAdduct().isEmpty()) {
                csiOptions.adductOptions.checkBoxList.uncheckAll();
                csiOptions.adductOptions.checkBoxList.check(ionType.toString());
                csiOptions.adductOptions.setEnabled(false);
            } else {
                csiOptions.adductOptions.setEnabled(csiOptions.isCSISelected());
            }
        });

//        searchProfilePanel.refreshPossibleIonizations(Collections.singleton(editPanel.getSelectedIonization().getIonization().getName()));
        editPanel.setData(ec);
        ///////ugly hack end

        /////////////Solo Element//////////////////////
        elementPanel = new ElementsPanel(this, 4);
        north.add(elementPanel, BorderLayout.SOUTH);

        StringBuilder builder = new StringBuilder();
        builder.append("Auto detectable element are: ");
        for (int i = 0; i < detectableElements.size(); i++) {
            if (i != 0) builder.append(", ");
            builder.append(detectableElements.get(i).getSymbol());
        }
        elementAutoDetect = new JButton("Auto detect");
        elementAutoDetect.setToolTipText(builder.toString());
        elementAutoDetect.addActionListener(this);
        elementAutoDetect.setEnabled(true);
        elementPanel.lowerPanel.add(elementAutoDetect);
        /////////////////////////////////////////////

        add(north, BorderLayout.NORTH);
    }

    private class WarnFormulaSourceDialog extends WarningDialog {
        private final static String DONT_ASK_KEY = PropertyManager.PROPERTY_BASE + ".sirius.computeDialog.formulaSourceWarning.dontAskAgain";
        public static final String FORMULA_SOURCE_WARNING_MESSAGE =
                "<b>Warning:</b> No connection to webservice available! <br>" +
                        "Online databases cannot be used for formula identification.<br> " +
                        "If online databases are selected, the default option <br>" +
                        "(all molecular formulas) will be used instead.";

        public WarnFormulaSourceDialog(Frame owner) {
            super(owner, "<html>" + FORMULA_SOURCE_WARNING_MESSAGE, DONT_ASK_KEY + "</html>" );
        }
    }
}