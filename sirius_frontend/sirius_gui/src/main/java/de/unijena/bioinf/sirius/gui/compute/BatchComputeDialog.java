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

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree.TreeBuilderFactory;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.sirius.gui.dialogs.NoConnectionDialog;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.utils.Icons;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

public class BatchComputeDialog extends JDialog implements ActionListener {

    private static String SEARCH_PUBCHEM = "Search PubChem structure database with CSI:FingerId";
    private static String SEARCH_BIODB = "Search bio database with CSI:FingerId";

    private JButton compute, abort;


    private ElementsPanel elementPanel;
    private JButton elementAutoDetect;
    private String autoDetectTextEnabled = "Auto detection enabled";
    private String autoDetectTextDisabled= "Auto detection disabled";
    private SearchProfilePanel searchProfilePanel;
    private JCheckBox runCSIFingerId;
    private MainFrame owner;

    private boolean success;
    private HashMap<String, Ionization> stringToIonMap;
    private HashMap<Ionization, String> ionToStringMap;
    private final JSpinner candidatesSpinner = null;

    public BatchComputeDialog(MainFrame owner) {
        super(owner, "compute", true);
        this.owner = owner;
        this.success = false;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.setLayout(new BorderLayout());

        Box mainPanel = Box.createVerticalBox();

        this.add(mainPanel, BorderLayout.CENTER);


        /////////////////////////////////////////////
        elementPanel = new ElementsPanel(this);
        mainPanel.add(elementPanel);

//        elementPanel.add(Box.createHorizontalGlue());

        elementAutoDetect = new JButton(autoDetectTextEnabled);

        FontMetrics metrics = elementAutoDetect.getFontMetrics(elementAutoDetect.getFont());
        Dimension preferred = elementAutoDetect.getPreferredSize();
        int height = preferred.height;
        int currentWidth = preferred.width;
        int textWidth = metrics.stringWidth(autoDetectTextEnabled);
        int margin = currentWidth-textWidth;
        int newWidth = Math.max(textWidth, metrics.stringWidth(autoDetectTextDisabled));
        elementAutoDetect.setPreferredSize(new Dimension(newWidth+margin, height));


        elementAutoDetect.addActionListener(this);
        elementAutoDetect.setEnabled(true);
        elementPanel.add(elementAutoDetect);

        useElementAutodetect(true);

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

    public void enableElementSelection(boolean enabled) {
        elementPanel.enableElementSelection(enabled);
        elementAutoDetect.setEnabled(enabled);
    }

    public void useElementAutodetect(boolean enable){
        elementPanel.enableElementSelection(!enable);
        if (enable){
            elementAutoDetect.setText(autoDetectTextEnabled);
        } else {
            elementAutoDetect.setText(autoDetectTextDisabled);
        }

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
        }else if (e.getSource() == elementAutoDetect) {
            useElementAutodetect(!elementAutoDetect.getText().equals(autoDetectTextEnabled));
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

        final FormulaConstraints constraints = elementPanel.getElementConstraints();

        final double ppm = searchProfilePanel.getPpm();

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

                final BackgroundComputation.Task task = new BackgroundComputation.Task(instrument, ec, constraints, ppm, candidates, formulaSource, runCSIFingerId.isSelected());
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