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
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree.TreeBuilderFactory;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.ElementPredictor;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.actions.CheckConnectionAction;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.db.SearchableDatabase;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIDComputationPanel;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.utils.ToolbarToggleButton;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

public class BatchComputeDialog extends JDialog implements ActionListener {

    private JButton compute;
    private JButton abort;

    private JCheckBox recompute;

    private ElementsPanel elementPanel;
    private JButton elementAutoDetect = null;
    private JComboBox<Peak> box = null;

    private SearchProfilePanel searchProfilePanel;
    private ToolbarToggleButton runCSIFingerId;
    private FingerIDComputationPanel csiOptions;
    private MainFrame owner;
    List<ExperimentContainer> compoundsToProcess;

    private Sirius sirius;
    private boolean success;

    public BatchComputeDialog(MainFrame owner, List<ExperimentContainer> compoundsToProcess) {
        super(owner, "compute", true);
        this.owner = owner;
        this.compoundsToProcess = compoundsToProcess;
        this.success = false;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.setLayout(new BorderLayout());

        Box mainPanel = Box.createVerticalBox();
        this.add(mainPanel, BorderLayout.CENTER);
        //mainpanel done


        this.sirius = new Sirius();
        ElementPredictor elementPredictor = sirius.getElementPrediction();
        List<Element> detectableElements = new ArrayList<>();
        for (Element element : elementPredictor.getChemicalAlphabet().getElements()) {
            if (elementPredictor.isPredictable(element)) detectableElements.add(element);
        }


        if (compoundsToProcess.size() > 1) {
            ///////////////////Multi Element//////////////////////
            elementPanel = new ElementsPanel(this, 4, detectableElements);
            mainPanel.add(elementPanel);
            boolean enableFallback = hasCompoundWithUnknownIonization();
            searchProfilePanel = new SearchProfilePanel(this, enableFallback);
            /////////////////////////////////////////////
        } else {
            initSingleExperiment(mainPanel, detectableElements);
            searchProfilePanel = new SearchProfilePanel(this, compoundsToProcess.get(0).getIonization());
        }


        mainPanel.add(searchProfilePanel);
        searchProfilePanel.formulaCombobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                SearchableDatabase source = searchProfilePanel.getFormulaSource();
                enableElementSelection(source == null);
                if (!csiOptions.isEnabled()) csiOptions.setDb(source);
            }
        });


        JPanel stack = new JPanel();
        stack.setLayout(new BorderLayout());
        stack.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "CSI:FingerId search"));


        JPanel otherPanel = new JPanel();
        otherPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        csiOptions = new FingerIDComputationPanel(owner.getCsiFingerId().getAvailableDatabases());
        if (!csiOptions.isEnabled()) csiOptions.setDb(searchProfilePanel.getFormulaSource());
        csiOptions.setMaximumSize(csiOptions.getPreferredSize());

        if (MainFrame.MF.getCsiFingerId().isEnabled() && ((CheckConnectionAction) SiriusActions.CHECK_CONNECTION.getInstance()).isActive.get()) {
            runCSIFingerId = new ToolbarToggleButton(Icons.FINGER_32, "Enable/Disable CSI:FingerID search");
            runCSIFingerId.setEnabled(true);
        } else {
            runCSIFingerId = new ToolbarToggleButton(Icons.FINGER_32, "Can't connect to CSI:FingerID server!");
            runCSIFingerId.setEnabled(false);
        }

        runCSIFingerId.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                csiOptions.setEnabled(runCSIFingerId.isSelected());
            }
        });


        otherPanel.add(runCSIFingerId);
//        otherPanel.add(Box.createHorizontalGlue());
        otherPanel.add(csiOptions);
//        otherPanel.add(Box.createHorizontalGlue());
        runCSIFingerId.setSelected(false);
        csiOptions.setEnabled(false);


        stack.add(otherPanel, BorderLayout.CENTER);
        mainPanel.add(stack);


        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.LINE_AXIS));

        JPanel lsouthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        recompute = new JCheckBox("Recompute already computed compounds?", false);
        recompute.setToolTipText("If checked, all selected compounds will be computed. Already computed ones we be recomputed.");
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


        this.pack();
        this.setResizable(false);
        setLocationRelativeTo(getParent());
        this.setVisible(true);

    }

    public void enableElementSelection(boolean enabled) {
        elementPanel.enableElementSelection(enabled);
        if (elementAutoDetect != null)
            elementAutoDetect.setEnabled(enabled);
    }


    private boolean hasCompoundWithUnknownIonization() {
        Iterator<ExperimentContainer> compounds = this.compoundsToProcess.iterator();
        while (compounds.hasNext()) {
            final ExperimentContainer ec = compounds.next();
            if (ec.getIonization() == null || ec.getIonization().isIonizationUnknown()) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == abort) {
            this.dispose();
        } else if (e.getSource() == this.compute) {
            startComputing();
        } else if (e.getSource() == elementAutoDetect) {
            String notWorkingMessage = "Element detection requires MS1 spectrum with isotope pattern.";
            ExperimentContainer ec = compoundsToProcess.get(0);
            if (!ec.getMs1Spectra().isEmpty() || ec.getCorrelatedSpectrum() != null) {
                MutableMs2Experiment exp = SiriusDataConverter.experimentContainerToSiriusExperiment(ec, SiriusDataConverter.enumOrNameToIontype(searchProfilePanel.getIonization()), getSelectedIonMass());
                ElementPredictor predictor = sirius.getElementPrediction();
                final FormulaConstraints c = sirius.predictElementsFromMs1(exp);
                if (c != null) {
                    for (Element element : c.getChemicalAlphabet()) {
                        if (!predictor.isPredictable(element)) {
                            c.setLowerbound(element, 0);
                            c.setUpperbound(element, 0);
                        }
                    }
                    elementPanel.setSelectedElements(c);
                } else {
                    new ExceptionDialog(this, notWorkingMessage);
                }
            } else {
                new ExceptionDialog(this, notWorkingMessage);
            }
        }
    }

    private Double getSelectedIonMass() {
        Object selected = box.getSelectedItem();
        double pm = 0;
        if (selected instanceof Peak) {
            Peak cp = (Peak) selected;
            pm = cp.getMass();
        } else if (selected != null && !selected.toString().isEmpty()) {
            pm = Double.parseDouble(selected.toString());
        } else return null;
        return pm;
    }

    private void abortComputing() {
        this.dispose();
    }

    private void startComputing() {
        if (recompute.isSelected()) {
            final String dontAskProperty = "de.unijena.bioinf.sirius.dontAsk.recompute";
            Properties properties = ApplicationCore.SIRIUS_PROPERTIES_FILE.getCopyOfPersistentProperties();

            ReturnValue value;
            if (Boolean.parseBoolean(properties.getProperty(dontAskProperty, "false")) || this.compoundsToProcess.size() == 1) {
                value = ReturnValue.Success;
            } else {
                QuestionDialog questionDialog = new QuestionDialog(this, "<html><body>Do you really want to recompute already computed experiments? <br> All existing results will be lost!</body></html>", dontAskProperty);
                value = questionDialog.getReturnValue();
            }

            //reset status of uncomputed values
            if (value == ReturnValue.Success) {
                final Iterator<ExperimentContainer> compounds = this.compoundsToProcess.iterator();
                while (compounds.hasNext()) {
                    final ExperimentContainer ec = compounds.next();
                    ec.setComputeState(ComputingStatus.UNCOMPUTED);
                }
            }
        }


        String instrument = searchProfilePanel.getInstrument().profile;

        SearchableDatabase searchableDatabase = searchProfilePanel.getFormulaSource();

        FormulaConstraints constraints = elementPanel.getElementConstraints();
        List<Element> elementsToAutoDetect = Collections.EMPTY_LIST;
        if (elementPanel.individualAutoDetect)
            elementsToAutoDetect = elementPanel.getElementsToAutoDetect();


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
        final boolean treatAsHydrogen = (searchProfilePanel.getIonization().equals("treat as protonation"));

        //entspricht setup() Methode
        final BackgroundComputation bgc = owner.getBackgroundComputation();
        final Iterator<ExperimentContainer> compounds = this.compoundsToProcess.iterator();
        final ArrayList<BackgroundComputation.Task> tasks = new ArrayList<>();
        while (compounds.hasNext()) {
            final ExperimentContainer ec = compounds.next();
            if (ec.isUncomputed()) {

                if (this.compoundsToProcess.size() == 1) {
                    //if one experiment is selected, force ionization
                    ec.setIonization(SiriusDataConverter.enumOrNameToIontype(searchProfilePanel.getIonization()));
                }
                if (treatAsHydrogen && ec.getIonization().isIonizationUnknown()) {
                    if (ec.getIonization() == null || ec.getIonization().getCharge() > 0) {
                        ec.setIonization(PrecursorIonType.getPrecursorIonType("[M+H]+"));
                    } else {
                        ec.setIonization(PrecursorIonType.getPrecursorIonType("[M-H]-"));
                    }
                }

                FormulaConstraints individualConstraints = new FormulaConstraints(constraints);
                if (!elementsToAutoDetect.isEmpty() && !ec.getMs1Spectra().isEmpty()) {
                    MutableMs2Experiment exp = SiriusDataConverter.experimentContainerToSiriusExperiment(ec, SiriusDataConverter.enumOrNameToIontype(searchProfilePanel.getIonization()), ec.getFocusedMass());
                    FormulaConstraints autoConstraints = sirius.predictElementsFromMs1(exp);
                    if (autoConstraints != null) {
                        ElementPredictor predictor = sirius.getElementPrediction();
                        for (Element element : elementsToAutoDetect) {
                            if (predictor.isPredictable(element)) {
                                individualConstraints.setUpperbound(element, autoConstraints.getUpperbound(element));
                            }
                        }
                    }
                }
                final BackgroundComputation.Task task = new BackgroundComputation.Task(instrument, ec, individualConstraints, ppm, candidates, searchableDatabase, searchProfilePanel.hasIsotopesEnabled(), runCSIFingerId.isSelected(), csiOptions.getDb(), searchProfilePanel.restrictToOrganics());
                tasks.add(task);
            }
        }
        bgc.addAll(tasks);
        dispose();
    }

    public boolean isSuccessful() {
        return this.success;
    }

    public void initSingleExperiment(Box mainPanel, List<Element> detectableElements) {
        final ExperimentContainer ec = compoundsToProcess.get(0);
        JPanel focMassPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        Vector<Peak> masses = new Vector<>();
        double maxInt = -1;
        Object maxObj = null;
        List<CompactSpectrum> ms1Spectra = new ArrayList<>(ec.getMs1Spectra());
        if (ec.getCorrelatedSpectrum() != null)
            ms1Spectra.add(ec.getCorrelatedSpectrum());
        final SimpleMutableSpectrum massBuffer = new SimpleMutableSpectrum();

        // falls MS1 verfügbar biete MS1 Peaks an, ansonsten nehme MS2 und normalisiere global
        Peak bestDataIon = null;
        final Deviation dev = new Deviation(20);
        final double focusedMass = ec.getDataFocusedMass();
        if (!ms1Spectra.isEmpty()) {
            for (CompactSpectrum cms1 : ms1Spectra) {
                if (focusedMass > 0) {
                    final int i = Spectrums.mostIntensivePeakWithin(cms1, focusedMass, dev);
                    if (i >= 0) {
                        massBuffer.addPeak(cms1.getMzAt(i), cms1.getRelativeIntensity(i));
                        bestDataIon = cms1.getPeakAt(i);
                    }
                }
                // for each isotope pattern add starting peak with at least 2% intensity
                for (int i = 0; i < cms1.size(); ++i) {
                    int j = Spectrums.mostIntensivePeakWithin(cms1, cms1.getMzAt(i) - 1.0033, dev);
                    if (j < 0 || cms1.getIntensityAt(j) < 0.02) {
                        massBuffer.addPeak(cms1.getMzAt(i), cms1.getRelativeIntensity(i));
                    }
                }
            }

        } else {
            // take the highest peak with at least 5% intensity that is not preceeded by
            // possible isotope peaks

            // I hate marvins data structures -_-
            final SimpleMutableSpectrum mergedSpec = new SimpleMutableSpectrum(Spectrums.mergeSpectra(new Deviation(20), true, true, SiriusDataConverter.myxoMs2ToSiriusMs2(ec.getMs2Spectra(), 0d)));
            Spectrums.normalize(mergedSpec, Normalization.Max(1d));
            Spectrums.applyBaseline(mergedSpec, 0.05);
            final SimpleSpectrum spec = new SimpleSpectrum(mergedSpec);
            // search parent peak
            int largestPeak = spec.size() - 1;
            for (; largestPeak > 0; --largestPeak) {
                final int isotopePeak = Spectrums.mostIntensivePeakWithin(spec, spec.getMzAt(largestPeak) - 1d, new Deviation(10, 0.2));
                if (isotopePeak < 0 || spec.getIntensityAt(isotopePeak) < spec.getIntensityAt(largestPeak)) break;
            }
            double expectedParentMass = 0d;
            if (largestPeak > 0) {
                expectedParentMass = spec.getMzAt(largestPeak);
            }


            for (CompactSpectrum sp : ec.getMs2Spectra()) {
                for (int i = 0; i < sp.getSize(); i++) {
                    if (sp.getPeakAt(i).getIntensity() > maxInt) {
                        maxInt = sp.getPeakAt(i).getIntensity();
                    }
                    massBuffer.addPeak(sp.getMzAt(i), sp.getRelativeIntensity(i));
                    if ((focusedMass > 0 && dev.inErrorWindow(sp.getPeakAt(i).getMass(), focusedMass)) || (expectedParentMass > 0 && dev.inErrorWindow(sp.getPeakAt(i).getMass(), expectedParentMass))) {
                        if (bestDataIon == null || sp.getPeakAt(i).getIntensity() > bestDataIon.getIntensity())
                            bestDataIon = sp.getPeakAt(i);
                    }
                }
            }
        }
        Spectrums.mergePeaksWithinSpectrum(massBuffer, dev, false);
        Peak defaultIon = null;
        for (Peak p : massBuffer) {
            masses.add(p);
            if (bestDataIon != null && dev.inErrorWindow(p.getMass(), bestDataIon.getMass())) {
                defaultIon = p;
            }
            if (bestDataIon == null && (defaultIon == null || p.getMass() > defaultIon.getMass())) {
                defaultIon = p;
            }
        }

        box = new JComboBox<>(masses);
        if (defaultIon != null)
            box.setSelectedItem(defaultIon);

        box.setEditable(true);
        MyListCellRenderer renderer = new MyListCellRenderer(masses);
        box.setRenderer(renderer);
        box.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Double value = getSelectedIonMass();
                if (value != null) ec.setSelectedFocusedMass(value);
            }
        });

        AutoCompleteDecorator.decorate(box, new ObjectToStringConverter() {
            @Override
            public String getPreferredStringForItem(Object item) {
                if (item instanceof Peak) {
                    Peak peak = (Peak) item;
                    return String.valueOf(peak.getMass());
                } else {
                    return (String) item;
                }

            }
        });
        focMassPanel.add(box);
        focMassPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Parent mass"));



		/*
         * Was abgefragt werden muss:
		 *
		 * foc. mass
		 * Ionisierung
		 * seltene Elemente abseits von CHNOPS Br, B, Cl, Se, F, I
		 *
		 */

        JButton autoDetectFM = new JButton("Most intensive peak");
        autoDetectFM.addActionListener(this);
        if (masses.isEmpty()) autoDetectFM.setEnabled(false);
        JButton expFM = new JButton("File value");
        expFM.addActionListener(this);
        if (defaultIon != null) {
            box.setSelectedItem(defaultIon);
        } else if (bestDataIon == null) {
            expFM.setEnabled(false);
            if (masses.isEmpty()) {
                box.setSelectedItem("");
            } else {
                box.setSelectedItem(maxObj);
            }
        } else if (bestDataIon != null) {
            box.setSelectedItem(bestDataIon);
        } else {
            box.setSelectedItem(String.valueOf(focusedMass));
        }

        focMassPanel.add(autoDetectFM);
        focMassPanel.add(expFM);
        mainPanel.add(focMassPanel, BorderLayout.NORTH);


        /////////////Solo Element//////////////////////
        elementPanel = new ElementsPanel(this, 4);
        mainPanel.add(elementPanel);

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
    }

}