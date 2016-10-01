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
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree.TreeBuilderFactory;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.cli.CLI;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

public class BatchComputeDialog extends JDialog implements ActionListener {

    private JButton compute, abort;

    private JCheckBox bromine, borone, selenium, chlorine, iodine, fluorine;
    private JTextField elementTF;
    private JButton elementButton;
    private JCheckBox elementAutoDetect;
    private MainFrame owner;

    private TreeSet<String> additionalElements;

    private Vector<String> ionizations, instruments;
    private JComboBox<String> ionizationCB, instrumentCB, formulaCombobox;
    private JSpinner ppmSpinner;
    private SpinnerNumberModel snm;

    private boolean success;
    private HashMap<String, Ionization> stringToIonMap;
    private HashMap<Ionization, String> ionToStringMap;
    private final JSpinner candidatesSpinner;

    public BatchComputeDialog(MainFrame owner) {
        super(owner, "compute", true);
        this.owner = owner;
        this.success = false;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        additionalElements = new TreeSet<>();

        this.setLayout(new BorderLayout());

        Box mainPanel = Box.createVerticalBox();

        this.add(mainPanel, BorderLayout.CENTER);


        /////////////////////////////////////////////
//		Box elementPanel = Box.createVerticalBox();
        JPanel elementPanel = new JPanel(new BorderLayout());
        elementPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "elements beside CHNOPS"));
        mainPanel.add(elementPanel);

        bromine = new JCheckBox("bromine");
        borone = new JCheckBox("borone");
        selenium = new JCheckBox("selenium");
        chlorine = new JCheckBox("chlorine");
        iodine = new JCheckBox("iodine");
        fluorine = new JCheckBox("fluorine");

        JPanel elements = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        elements.add(bromine);
        elements.add(borone);
        elements.add(chlorine);
        elements.add(fluorine);
        elements.add(iodine);
        elements.add(selenium);

        elementAutoDetect = new JCheckBox("Auto detect");
        elementAutoDetect.setEnabled(false);
        elementAutoDetect.addActionListener(this);
        elementTF = new JTextField(10);
        elementTF.setEditable(false);
        elementButton = new JButton("More elements");
        elementButton.addActionListener(this);

        elements.add(elementAutoDetect);
        elementPanel.add(elements, BorderLayout.NORTH);

        JPanel elements2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        elements2.add(elementTF);
        elements2.add(elementButton);
        elementPanel.add(elements2, BorderLayout.SOUTH);


//		elementPanel.add(Box.createVerticalGlue());

        /////////////////////////////////////////////
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "other"));

        JPanel otherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        instruments = new Vector<>();
        instruments.add("Q-TOF");
        instruments.add("Orbitrap");
        instruments.add("FT-ICR");
        instrumentCB = new JComboBox<>(instruments);
        otherPanel.add(new JLabel("  instrument"));
        otherPanel.add(instrumentCB);

        this.snm = new SpinnerNumberModel(10, 0.25, 20, 0.25);
        this.ppmSpinner = new JSpinner(this.snm);
        this.ppmSpinner.setMinimumSize(new Dimension(70, 26));
        this.ppmSpinner.setPreferredSize(new Dimension(70, 26));
        otherPanel.add(new JLabel("  ppm"));
        otherPanel.add(this.ppmSpinner);

        final SpinnerNumberModel candidatesNumberModel = new SpinnerNumberModel(10, 1, 1000, 1);
        candidatesSpinner = new JSpinner(candidatesNumberModel);
        candidatesSpinner.setMinimumSize(new Dimension(70, 26));
        candidatesSpinner.setPreferredSize(new Dimension(70, 26));
        otherPanel.add(new JLabel(" candidates"));
        otherPanel.add(candidatesSpinner);

        instrumentCB.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                final String name = (String) e.getItem();
                final double recommendedPPM;

                if (name.equals("Q-TOF")) recommendedPPM = 10;
                else if (name.equals("Orbitrap")) recommendedPPM = 5;
                else if (name.equals("FT-ICR")) recommendedPPM = 2;
                else recommendedPPM = 10;

                ppmSpinner.setValue(new Double(recommendedPPM)); // TODO: test
            }
        });
        stack.add(otherPanel);

        //
        otherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        ionizations = new Vector<>();
        ionizations.add("treat as protonation");
        ionizations.add("try common adduct types");
        ionizationCB = new JComboBox<>(ionizations);
        ionizationCB.setSelectedIndex(0);
        ionizationCB.setEnabled(hasCompoundWithUnknownIonization());
        otherPanel.add(new JLabel("  fallback for unknown adduct types"));
        otherPanel.add(ionizationCB);
        stack.add(otherPanel);

        //////////
        {
            JLabel label = new JLabel("Consider ");
            final Vector<String> values = new Vector<>();
            values.add("all possible molecular formulas");
            values.add("PubChem formulas");
            values.add("formulas from biological databases");
            formulaCombobox = new JComboBox<>(values);
            otherPanel.add(label);
            otherPanel.add(formulaCombobox);
            formulaCombobox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    enableElementSelection(formulaCombobox.getSelectedIndex() == 0);
                }
            });
        }


        mainPanel.add(stack);
        ///

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        this.add(southPanel, BorderLayout.SOUTH);
        compute = new JButton("Compute");
        compute.addActionListener(this);
        abort = new JButton("Abort");
        abort.addActionListener(this);
        southPanel.add(compute);
        southPanel.add(abort);

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

    private boolean hasCompoundWithUnknownIonization() {
        Enumeration<ExperimentContainer> compounds = owner.getCompounds();
        while (compounds.hasMoreElements()) {
            final ExperimentContainer ec = compounds.nextElement();
            if (ec.isUncomputed()) {
                if (ec.getIonization() == null || ec.getIonization().isUnknown()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void enableElementSelection(boolean enabled) {
        if (enabled) {
            for (JCheckBox b : Arrays.asList(borone, bromine, chlorine, fluorine, iodine, selenium)) {
                b.setEnabled(true);
            }
            elementButton.setEnabled(true);
            elementAutoDetect.setEnabled(false);
            elementTF.setEnabled(true);
        } else {
            for (JCheckBox b : Arrays.asList(borone, bromine, chlorine, fluorine, iodine, selenium)) {
                b.setEnabled(false);
            }
            elementButton.setEnabled(false);
            elementAutoDetect.setEnabled(false);
            elementTF.setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == abort) {
            this.dispose();
        } else if (e.getSource() == this.elementButton) {
            HashSet<String> eles = new HashSet<>();
            if (borone.isSelected()) eles.add("B");
            if (bromine.isSelected()) eles.add("Br");
            if (chlorine.isSelected()) eles.add("Cl");
            if (fluorine.isSelected()) eles.add("F");
            if (iodine.isSelected()) eles.add("I");
            if (selenium.isSelected()) eles.add("Se");
            eles.addAll(additionalElements);
            AdditionalElementDialog diag = new AdditionalElementDialog(this, eles);
            if (diag.successful()) {
                additionalElements = new TreeSet<>(diag.getSelectedElements());
                if (additionalElements.contains("B")) {
                    borone.setSelected(true);
                    additionalElements.remove("B");
                } else {
                    borone.setSelected(false);
                }
                if (additionalElements.contains("Br")) {
                    bromine.setSelected(true);
                    additionalElements.remove("Br");
                } else {
                    bromine.setSelected(false);
                }
                if (additionalElements.contains("Cl")) {
                    chlorine.setSelected(true);
                    additionalElements.remove("Cl");
                } else {
                    chlorine.setSelected(false);
                }
                if (additionalElements.contains("F")) {
                    fluorine.setSelected(true);
                    additionalElements.remove("F");
                } else {
                    fluorine.setSelected(false);
                }
                if (additionalElements.contains("I")) {
                    iodine.setSelected(true);
                    additionalElements.remove("I");
                } else {
                    iodine.setSelected(false);
                }
                if (additionalElements.contains("Se")) {
                    selenium.setSelected(true);
                    additionalElements.remove("Se");
                } else {
                    selenium.setSelected(false);
                }

                StringBuilder newText = new StringBuilder();
                Iterator<String> it = additionalElements.iterator();
                while (it.hasNext()) {
                    newText.append(it.next());
                    if (it.hasNext()) newText.append(",");
                }
                elementTF.setText(newText.toString());


            }
        } else if (e.getSource() == this.compute) {
            startComputing();
        }
    }

    private void abortComputing() {
        this.dispose();
    }

    private void startComputing() {
        String val = (String) instrumentCB.getSelectedItem();
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

        FormulaSource formulaSource;
        if (formulaCombobox.getSelectedIndex() == 0) formulaSource = FormulaSource.ALL_POSSIBLE;
        else if (formulaCombobox.getSelectedIndex() == 1) formulaSource = FormulaSource.PUBCHEM;
        else formulaSource = FormulaSource.BIODB;

        FormulaConstraints constraints;
        {
            HashSet<String> eles = new HashSet<>();
            if (borone.isSelected()) eles.add("B");
            if (bromine.isSelected()) eles.add("Br");
            if (chlorine.isSelected()) eles.add("Cl");
            if (fluorine.isSelected()) eles.add("F");
            if (iodine.isSelected()) eles.add("I");
            if (selenium.isSelected()) eles.add("Se");
            eles.addAll(additionalElements);
            Element[] elems = new Element[eles.size()];
            int k = 0;
            final PeriodicTable tb = PeriodicTable.getInstance();
            for (String s : eles) {
                final Element elem = tb.getByName(s);
                if (elem != null)
                    elems[k++] = elem;
            }
            if (k < elems.length) elems = Arrays.copyOf(elems, k);
            constraints = new FormulaConstraints().getExtendedConstraints(elems);
        }

        final double ppm = snm.getNumber().doubleValue();

        final int candidates = ((Number) candidatesSpinner.getModel().getValue()).intValue();

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
        treatAsHydrogen = (((String) ionizationCB.getSelectedItem()).equals("treat as protonation"));

        //entspricht setup() Methode
        final BackgroundComputation bgc = owner.getBackgroundComputation();
        final Enumeration<ExperimentContainer> compounds = owner.getCompounds();
        final ArrayList<BackgroundComputation.Task> tasks = new ArrayList<>();
        final ArrayList<ExperimentContainer> compoundList = new ArrayList<>();
        while (compounds.hasMoreElements()) {
            final ExperimentContainer ec = compounds.nextElement();
            if (ec.isUncomputed()) {

                if (treatAsHydrogen && ec.getIonization().isUnknown()) {
                    if (ec.getIonization() == null || ec.getIonization().toRealIonization().getCharge() > 0) {
                        ec.setIonization(SiriusDataConverter.siriusIonizationToEnum(PrecursorIonType.getPrecursorIonType("[M+H]+")));
                    } else {
                        ec.setIonization(SiriusDataConverter.siriusIonizationToEnum(PrecursorIonType.getPrecursorIonType("[M-H]-")));
                    }
                }

                final BackgroundComputation.Task task = new BackgroundComputation.Task(instrument, ec, constraints, ppm, candidates, formulaSource);
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