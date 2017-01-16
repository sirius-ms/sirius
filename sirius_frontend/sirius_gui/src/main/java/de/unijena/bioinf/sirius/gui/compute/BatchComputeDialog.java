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

package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree.TreeBuilderFactory;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.ElementPredictor;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.sirius.gui.dialogs.NoConnectionDialog;
import de.unijena.bioinf.sirius.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.utils.Icons;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.SeparatorUI;
import javax.swing.plaf.SplitPaneUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

public class BatchComputeDialog extends JDialog implements ActionListener {

    private static String SEARCH_PUBCHEM = "Search PubChem structure database with CSI:FingerId";
    private static String SEARCH_BIODB = "Search bio database with CSI:FingerId";

    private JButton compute, abort, recompute;


    private ElementsPanel elementPanel;
    private SearchProfilePanel searchProfilePanel;
    private JCheckBox runCSIFingerId;
    private MainFrame owner;

    private Sirius sirius;

    private boolean success;
    private HashMap<String, Ionization> stringToIonMap;
    private HashMap<Ionization, String> ionToStringMap;

    public BatchComputeDialog(MainFrame owner) {
        super(owner, "compute", true);
        this.owner = owner;
        this.success = false;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.setLayout(new BorderLayout());

        Box mainPanel = Box.createVerticalBox();

        this.add(mainPanel, BorderLayout.CENTER);

        /////////////////////////////////////////////
        this.sirius = new Sirius();
        ElementPredictor elementPredictor = sirius.getElementPrediction();
        List<Element> detectableElements = new ArrayList<>();
        for (Element element : elementPredictor.getChemicalAlphabet().getElements()) {
            if (elementPredictor.isPredictable(element)) detectableElements.add(element);
        }
        elementPanel = new ElementsPanel(this, 3, detectableElements);
        mainPanel.add(elementPanel);

        /////////////////////////////////////////////

        boolean enableFallback = hasCompoundWithUnknownIonization();
        searchProfilePanel = new SearchProfilePanel(this, enableFallback);
        mainPanel.add(searchProfilePanel);
        searchProfilePanel.formulaCombobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                FormulaSource source = searchProfilePanel.getFormulaSource();
                enableElementSelection(source==FormulaSource.ALL_POSSIBLE);
                runCSIFingerId.setText(source == FormulaSource.BIODB ? SEARCH_BIODB : SEARCH_PUBCHEM);
            }
        });



        JPanel stack = new JPanel();
        stack.setLayout(new BorderLayout());
        stack.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "CSI:FingerId search"));

        {
            JPanel otherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            final JLabel label = new JLabel(Icons.FINGER_64, SwingConstants.LEFT);
//            runCSIFingerId = new JCheckBox(getSelectedFormulaSource()==FormulaSource.BIODB ? SEARCH_BIODB : SEARCH_PUBCHEM);
            //changed
            runCSIFingerId = new JCheckBox(searchProfilePanel.getFormulaSource()==FormulaSource.BIODB ? SEARCH_BIODB : SEARCH_PUBCHEM);
            otherPanel.add(label);
            otherPanel.add(runCSIFingerId);
            stack.add(otherPanel, BorderLayout.CENTER);
        }
        mainPanel.add(stack);

        ///


        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel,BoxLayout.LINE_AXIS));

        JPanel lsouthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        recompute = new JButton("Recompute all");
        recompute.addActionListener(this);
        recompute.setToolTipText("Recompute all experiments. Even already computed ones.");
        lsouthPanel.add(recompute);

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


        this.pack();
        this.setResizable(false);
        setLocationRelativeTo(getParent());
        this.setVisible(true);

    }

    public void enableElementSelection(boolean enabled) {
        elementPanel.enableElementSelection(enabled);
    }


    private boolean hasCompoundWithUnknownIonization() {
        Enumeration<ExperimentContainer> compounds = owner.getCompounds();
        while (compounds.hasMoreElements()) {
            final ExperimentContainer ec = compounds.nextElement();
            if (ec.isUncomputed()) {
                if (ec.getIonization() == null || ec.getIonization().isIonizationUnknown()) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == abort) {
            this.dispose();
        }  else if (e.getSource() == this.compute) {
            startComputing();
        } else if (e.getSource() == recompute) {
            final String dontAskProperty = "de.unijena.bioinf.sirius.dontAsk.recompute";
            Properties properties = ApplicationCore.getUserCopyOfUserProperties();

            ReturnValue value;
            if (Boolean.parseBoolean(properties.getProperty(dontAskProperty))==true){
                value = ReturnValue.Success;
            } else {
                QuestionDialog questionDialog = new QuestionDialog(this, "Do you want to recompute all experiments?", dontAskProperty);
                value = questionDialog.getReturnValue();
            }

            if (value==ReturnValue.Success){
                final Enumeration<ExperimentContainer> compounds = owner.getCompounds();
                while (compounds.hasMoreElements()) {
                    final ExperimentContainer ec = compounds.nextElement();
                    ec.setComputeState(ComputingStatus.UNCOMPUTED);
                }
                startComputing();
            }
        }
    }

    private void abortComputing() {
        this.dispose();
    }

    private void startComputing() {
        String val = searchProfilePanel.getInstrument();
        String instrument = "";
        if (val.equals("Q-TOF")) {
            instrument = "qtof";
        } else if (val.equals("Orbitrap")) {
            instrument = "orbitrap";
        } else if (val.equals("FT-ICR")) {
            instrument = "fticr";
        } else {
            throw new RuntimeException("no valid instrument");
        }

        FormulaSource formulaSource = searchProfilePanel.getFormulaSource();

        if (formulaSource!=FormulaSource.ALL_POSSIBLE){
            //Test connection, if needed
            if (!WebAPI.getRESTDb(BioFilter.ALL).testConnection()){
                new NoConnectionDialog(this);
                dispose();
                return;
            }
        }

        FormulaConstraints constraints = elementPanel.getElementConstraints();
        List<Element> elementsToAutoDetect = elementPanel.getElementsToAutoDetect();

        final double ppm = searchProfilePanel.getPpm();

        final int candidates = searchProfilePanel.getNumberOfCandidates();

        // CHECK ILP SOLVER

        TreeBuilder builder = new Sirius().getMs2Analyzer().getTreeBuilder();

        if (builder == null) {
            String noILPSolver = "Could not load a valid TreeBuilder (ILP solvers) " + Arrays.toString(TreeBuilderFactory.getBuilderPriorities()) + ". Please read the installation instructions.";
            LoggerFactory.getLogger(this.getClass()).error(noILPSolver);
            new ErrorReportDialog(this, noILPSolver);
            dispose();
            return;
        }
        LoggerFactory.getLogger(this.getClass()).info("Compute trees using " + builder.getDescription());

        // treatment of unknown ionization
        final boolean treatAsHydrogen;
        treatAsHydrogen = (searchProfilePanel.getIonization().equals("treat as protonation"));

        //entspricht setup() Methode
        final BackgroundComputation bgc = owner.getBackgroundComputation();
        final Enumeration<ExperimentContainer> compounds = owner.getCompounds();
        final ArrayList<BackgroundComputation.Task> tasks = new ArrayList<>();
        final ArrayList<ExperimentContainer> compoundList = new ArrayList<>();
        while (compounds.hasMoreElements()) {
            final ExperimentContainer ec = compounds.nextElement();
            if (ec.isUncomputed()) {

                if (treatAsHydrogen && ec.getIonization().isIonizationUnknown()) {
                    if (ec.getIonization() == null || ec.getIonization().getCharge() > 0) {
                        ec.setIonization(PrecursorIonType.getPrecursorIonType("[M+H]+"));
                    } else {
                        ec.setIonization(PrecursorIonType.getPrecursorIonType("[M-H]-"));
                    }
                }

                FormulaConstraints individualConstraints = new FormulaConstraints(constraints);
                if (!elementsToAutoDetect.isEmpty() && !ec.getMs1Spectra().isEmpty()){
                    MutableMs2Experiment exp = SiriusDataConverter.experimentContainerToSiriusExperiment(ec, SiriusDataConverter.enumOrNameToIontype(searchProfilePanel.getIonization()), ec.getFocusedMass());
                    FormulaConstraints autoConstraints = sirius.predictElementsFromMs1(exp);
                    if (autoConstraints!=null){
                        ElementPredictor predictor = sirius.getElementPrediction();
                        for (Element element : elementsToAutoDetect) {
                            if (predictor.isPredictable(element)){
                                individualConstraints.setUpperbound(element, autoConstraints.getUpperbound(element));
                            }
                        }
                    }
                }

                final BackgroundComputation.Task task = new BackgroundComputation.Task(instrument, ec, individualConstraints, ppm, candidates, formulaSource, runCSIFingerId.isSelected());
                tasks.add(task);
                compoundList.add(ec);
            }
        }
        bgc.addAll(tasks);
        for (ExperimentContainer ec : compoundList) {
            owner.refreshCompound(ec);
        }
        owner.computationStarted();
        dispose();
    }

    public boolean isSuccessful() {
        return this.success;
    }


}