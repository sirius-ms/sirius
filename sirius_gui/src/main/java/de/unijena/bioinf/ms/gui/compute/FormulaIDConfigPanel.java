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

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.sirius.Ms1Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Panel to configure SIRIUS Computations
 * Provides CONFIGS for SiriusSubTool
 *
 * @author Marcus Ludwig, Markus Fleischauer
 * @since 12.01.17
 */
public class FormulaIDConfigPanel extends SubToolConfigPanel<SiriusOptions> {
    protected Logger logger = LoggerFactory.getLogger(FormulaIDConfigPanel.class);

    public enum Instrument {
        QTOF("Q-TOF", MsInstrumentation.Instrument.QTOF, "qtof", 10),
        ORBI("Orbitrap", MsInstrumentation.Instrument.ORBI, "orbitrap", 5),
        FTICR("FT-ICR", MsInstrumentation.Instrument.FTICR, "orbitrap", 2);
//        BRUKER("Q-TOF (isotopes)", MsInstrumentation.Instrument.BRUKER_MAXIS, "qtof", 10); // there is now if separate MS/MS isotope setting

        public final String name, profile;
        public final MsInstrumentation instrument;
        public final int ppm;

        Instrument(String name, MsInstrumentation instrument, String profile, int ppm) {
            this.name = name;
            this.profile = profile;
            this.ppm = ppm;
            this.instrument = instrument;
        }

        public String asProfile() {
            return profile;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    protected final JCheckboxListPanel<String> ionizationList;
    protected final JCheckboxListPanel<CustomDataSources.Source> searchDBList;
    protected final JComboBox<Instrument> profileSelector;
    protected final JSpinner ppmSpinner, candidatesSpinner, candidatesPerIonSpinner, treeTimeout, comoundTimeout, mzHeuristic, mzHeuristicOnly;

    enum Strategy {IGNORE, SCORE} //todo remove if Filter is implemented

    protected final JComboBox<Strategy> ms2IsotpeSetting;
//    protected final JComboBox<IsotopeMs2Settings.Strategy> ms2IsotpeSetting;

    //    protected final JCheckBox restrictToOrganics;
    protected ElementsPanel elementPanel;
//    protected JButton elementAutoDetect;


    protected final List<InstanceBean> ecs;


    protected final Dialog owner;

    public FormulaIDConfigPanel(Dialog owner, List<InstanceBean> ecs, boolean ms2) {
        super(SiriusOptions.class);
        this.ecs = ecs;
        this.owner = owner;


        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        final JPanel center = applyDefaultLayout(new JPanel());
        add(center);

        // configure small stuff panel
        final TwoColumnPanel smallParameters = new TwoColumnPanel();
        center.add(new TextHeaderBoxPanel("General", smallParameters));

        profileSelector = makeParameterComboBox("AlgorithmProfile", List.of(Instrument.values()), Instrument::asProfile);
        smallParameters.addNamed("Instrument", profileSelector);

        smallParameters.addNamed("Filter by isotope pattern", makeParameterCheckBox("IsotopeSettings.filter"));

        ms2IsotpeSetting = makeParameterComboBox("IsotopeMs2Settings", Strategy.class);
        ppmSpinner = makeParameterSpinner("MS2MassDeviation.allowedMassDeviation",
                PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation.getPpm(),
                0.25, 50, 0.25, m -> m.getNumber().doubleValue() + "ppm");

        if (ms2) {
            smallParameters.addNamed("MS2 mass accuracy (ppm)", ppmSpinner);
            smallParameters.addNamed("MS/MS isotope scorer", ms2IsotpeSetting);
        }

        candidatesSpinner = makeIntParameterSpinner("NumberOfCandidates", 1, 10000, 1);
        smallParameters.addNamed("Candidates stored", candidatesSpinner);

        candidatesPerIonSpinner = makeIntParameterSpinner("NumberOfCandidatesPerIon", 0, 10000, 1);
        smallParameters.addNamed("Min candidates per Ion stored", candidatesPerIonSpinner);

//        restrictToOrganics = new JCheckBox(); //todo implement parameter?? or as constraint?
//        GuiUtils.assignParameterToolTip(restrictToOrganics, "RestrictToOrganics");
//        parameterBindings.put("RestrictToOrganics", () -> String.valueOf(restrictToOrganics.isSelected()));
//        smallParameters.addNamed("Restrict to organics", restrictToOrganics);

        //sync profile with ppm spinner
        profileSelector.addItemListener(e -> {
            final Instrument i = (Instrument) e.getItem();
            final double recommendedPPM = i.ppm;
            ppmSpinner.setValue(recommendedPPM);
        });

        // configure database to search list
        searchDBList = new JCheckboxListPanel<>(new DBSelectionList(), "Use DB formulas only");
        GuiUtils.assignParameterToolTip(searchDBList.checkBoxList, "FormulaSearchDB");
        center.add(searchDBList);
        parameterBindings.put("FormulaSearchDB", () -> String.join(",", getFormulaSearchDBStrings()));

        //configure ionization panels
        ionizationList = new JCheckboxListPanel<>(new JCheckBoxList<>(), "Possible Ionizations", GuiUtils.formatToolTip("Set possible ionisation for data with unknown ionization. SIRIUS will try to auto-detect adducts that can be derived from this ionizations"));
        ionizationList.checkBoxList.setPrototypeCellValue(new CheckBoxListItem<>("[M + Na]+ ", false));
        center.add(ionizationList);
        parameterBindings.put("AdductSettings.detectable", () -> getDerivedDetectableAdducts().toString());
        parameterBindings.put("AdductSettings.fallback", () -> getDerivedDetectableAdducts().toString());


        // configure Element panel
        makeElementPanel(ecs.size() > 1);
        add(elementPanel);
        parameterBindings.put("FormulaSettings.enforced", () -> {
            return elementPanel.getElementConstraints().toString(); //todo check if this makes scence
        });

        parameterBindings.put("FormulaSettings.detectable", () -> {
            final List<Element> elementsToAutoDetect = elementPanel.individualAutoDetect ? elementPanel.getElementsToAutoDetect() : Collections.emptyList();
            return (elementsToAutoDetect.isEmpty() ? "," :
                    elementsToAutoDetect.stream().map(Element::toString).collect(Collectors.joining(",")));
        }); //todo check if this makes sense


        // ilp timeouts
        final TwoColumnPanel ilpOptions = new TwoColumnPanel();

        treeTimeout = makeIntParameterSpinner("Timeout.secondsPerTree", 0, Integer.MAX_VALUE, 1);
        ilpOptions.addNamed("Tree timeout", treeTimeout);

        comoundTimeout = makeIntParameterSpinner("Timeout.secondsPerInstance", 0, Integer.MAX_VALUE, 1);
        ilpOptions.addNamed("Compound timeout", comoundTimeout);

        mzHeuristic = makeIntParameterSpinner("UseHeuristic.mzToUseHeuristic", 0, 3000, 5);
        ilpOptions.addNamed("Use heuristic above m/z", mzHeuristic);

        mzHeuristicOnly = makeIntParameterSpinner("UseHeuristic.mzToUseHeuristicOnly", 0, 3000, 5);
        ilpOptions.addNamed("Use heuristic only above m/z", mzHeuristicOnly);

        if (ms2)
            center.add(new TextHeaderBoxPanel("ILP", ilpOptions));

        // ionization refresh
        refreshPossibleIonizations(ecs.stream().map(it -> it.getIonization().getIonization().toString()).collect(Collectors.toSet()), true);
    }

    public void refreshPossibleIonizations(Set<String> ionTypes, boolean enabled) {
        java.util.List<String> ionizations = new ArrayList<>();

        if (!ionTypes.isEmpty()) {
            if (ionTypes.contains(PrecursorIonType.unknownPositive().getIonization().getName())) {
                ionizations.addAll(PeriodicTable.getInstance().getPositiveIonizationsAsString());
            }
            if (ionTypes.contains(PrecursorIonType.unknownNegative().getIonization().getName())) {
                ionizations.addAll(PeriodicTable.getInstance().getNegativeIonizationsAsString());
            }
        }
        if (ionizations.isEmpty()) {
            ionizationList.checkBoxList.replaceElements(ionTypes.stream().sorted().collect(Collectors.toList()));
            ionizationList.checkBoxList.checkAll();
            ionizationList.setEnabled(false);
        } else {
            Collections.sort(ionizations);
            ionizationList.checkBoxList.replaceElements(ionizations);
            ionizationList.checkBoxList.checkAll();
            ionizationList.setEnabled(enabled);
        }

        if (ecs.size() == 1 && isEnabled() && !ecs.get(0).getMs2Spectra().isEmpty())
            detectPossibleAdducts(ecs.get(0));
    }

    protected void makeElementPanel(boolean multi) {
        final FormulaSettings formulaSettings = PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSettings.class);
        List<Element> possDetectableElements = new ArrayList<>(ApplicationCore.SIRIUS_PROVIDER.sirius().getMs1Preprocessor().getSetOfPredictableElements());

        final JButton elementAutoDetect;
        if (multi) {
            elementAutoDetect = null;
            elementPanel = new ElementsPanel(owner, 4, possDetectableElements, formulaSettings.getAutoDetectionElements(), formulaSettings.getEnforcedAlphabet());
        } else {
            /////////////Solo Element//////////////////////
            elementPanel = new ElementsPanel(owner, 4, formulaSettings.getEnforcedAlphabet());
            elementAutoDetect = new JButton("Auto detect");
            elementAutoDetect.setToolTipText("Auto detectable element are: "
                    + possDetectableElements.stream().map(Element::toString).collect(Collectors.joining(",")));
            elementAutoDetect.addActionListener(e -> detectElements());
            elementAutoDetect.setEnabled(true);
            elementPanel.lowerPanel.add(elementAutoDetect);
        }

        //enable disable element panel if db is selected
        searchDBList.checkBoxList.addListSelectionListener(e -> {
            final List<CustomDataSources.Source> source = getFormulaSearchDBs();
            elementPanel.enableElementSelection(source == null || source.isEmpty());
            if (elementAutoDetect != null)
                elementAutoDetect.setEnabled(source == null || source.isEmpty());
        });
        elementPanel.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));
    }

    protected void detectElements() {
        String notWorkingMessage = "Element detection requires MS1 spectrum with isotope pattern.";
        InstanceBean ec = ecs.get(0);
        if (!ec.getMs1Spectra().isEmpty() || ec.getMergedMs1Spectrum() != null) {
            Jobs.runInBackgroundAndLoad(owner, "Detecting Elements...", () -> {
                final Ms1Preprocessor pp = ApplicationCore.SIRIUS_PROVIDER.sirius().getMs1Preprocessor();
                ProcessedInput pi = pp.preprocess(new MutableMs2Experiment(ec.getExperiment(), false));

                pi.getAnnotation(FormulaConstraints.class).
                        ifPresentOrElse(c -> {
                                    for (Element element : c.getChemicalAlphabet()) {
                                        if (c.getUpperbound(element) <= 0) {
                                            c.setLowerbound(element, 0);
                                            c.setUpperbound(element, 0);
                                        }
                                    }
                                    elementPanel.setSelectedElements(c);
                                },
                                () -> new ExceptionDialog(owner, notWorkingMessage)
                        );
            }).getResult();
        } else {
            new ExceptionDialog(owner, notWorkingMessage);
        }
    }

    protected void detectPossibleAdducts(InstanceBean ec) {
        String notWorkingMessage = "Adduct detection requires MS1 spectrum.";
        if (!ec.getMs1Spectra().isEmpty() || ec.getMergedMs1Spectrum() != null) {
            Jobs.runInBackgroundAndLoad(owner, "Detecting adducts...", () -> {
                final Ms1Preprocessor pp = ApplicationCore.SIRIUS_PROVIDER.sirius().getMs1Preprocessor();
                ProcessedInput pi = pp.preprocess(new MutableMs2Experiment(ec.getExperiment(), false));

                pi.getAnnotation(PossibleAdducts.class).
                        ifPresentOrElse(pa -> {
                                    //todo do we want to add adducts?
                                    ionizationList.checkBoxList.uncheckAll();
                                    pa.getIonModes().stream().map(IonMode::toString).forEach(ionizationList.checkBoxList::check);
                                },
                                () -> new ExceptionDialog(owner, "Failed to detect Adducts from MS1")
                        );
            }).getResult();
        } else {
            LoggerFactory.getLogger(getClass()).warn(notWorkingMessage);
        }
    }

    public Instrument getInstrument() {
        return (Instrument) profileSelector.getSelectedItem();
    }

    public double getPpm() {
        return ((SpinnerNumberModel) ppmSpinner.getModel()).getNumber().doubleValue();
    }

    public int getNumOfCandidates() {
        return ((SpinnerNumberModel) candidatesSpinner.getModel()).getNumber().intValue();
    }

    public int getNumOfCandidatesPerIon() {
        return ((SpinnerNumberModel) candidatesPerIonSpinner.getModel()).getNumber().intValue();
    }

    public List<CustomDataSources.Source> getFormulaSearchDBs() {
        return searchDBList.checkBoxList.getCheckedItems();
    }

    public List<String> getFormulaSearchDBStrings() {
        return getFormulaSearchDBs().stream().map(CustomDataSources.Source::id).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public PossibleAdducts getDerivedDetectableAdducts() {
        Set<PrecursorIonType> det = new HashSet<>(PropertyManager.DEFAULTS.createInstanceWithDefaults(AdductSettings.class).getDetectable());
        Set<Ionization> keep = ionizationList.checkBoxList.getCheckedItems().stream().map(PrecursorIonType::parsePrecursorIonType).flatMap(Optional::stream).map(PrecursorIonType::getIonization).collect(Collectors.toSet());
        det.removeIf(s -> !keep.contains(s.getIonization()));
        return new PossibleAdducts(det);
    }
}
