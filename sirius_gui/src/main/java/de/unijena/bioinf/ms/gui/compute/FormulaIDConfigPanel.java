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

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.RelativeLayout;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Panel to configure SIRIUS Computations
 * Provides CONFIGS for SiriusSubTool
 *
 * @author Marcus Ludwig, Markus Fleischauer
 * @since 12.01.17
 */
public class
FormulaIDConfigPanel extends SubToolConfigPanelAdvancedParams<SiriusOptions> {
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

    protected JCheckboxListPanel<String> adductList;
    protected JToggleButton enforceAdducts;
    protected JComboBox<Instrument> profileSelector;
    protected JSpinner ppmSpinner, candidatesSpinner, candidatesPerIonSpinner, treeTimeout, comoundTimeout, mzHeuristic, mzHeuristicOnly;

    public enum Strategy {IGNORE, SCORE} //todo remove if Filter is implemented

    protected JComboBox<Strategy> ms2IsotpeSetting;

    protected FormulaSearchStrategy formulaSearchStrategy;


    protected final List<InstanceBean> ecs;


    protected final Dialog owner;

    protected boolean hasMs2;


    public FormulaIDConfigPanel(Dialog owner, List<InstanceBean> ecs, boolean ms2, boolean displayAdvancedParameters) {
        super(SiriusOptions.class, displayAdvancedParameters);
        this.ecs = ecs;
        this.owner = owner;
        this.hasMs2 = ms2;

        createPanel();
    }

    private void createPanel() {

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        final JPanel center = applyDefaultLayout(new JPanel());
        add(center);
        add(Box.createRigidArea(new Dimension(0, GuiUtils.LARGE_GAP)));

        // configure small stuff panel
        {
            final TwoColumnPanel smallParameters = new TwoColumnPanel();
            center.add(new TextHeaderBoxPanel("General", smallParameters));

            profileSelector = makeParameterComboBox("AlgorithmProfile", List.of(Instrument.values()), Instrument::asProfile);
            smallParameters.addNamed("Instrument", profileSelector);

            addAdvancedParameter(smallParameters, "Filter by isotope pattern", makeParameterCheckBox("IsotopeSettings.filter"));

            ms2IsotpeSetting = makeParameterComboBox("IsotopeMs2Settings", Strategy.class);
            ppmSpinner = makeParameterSpinner("MS2MassDeviation.allowedMassDeviation",
                    PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation.getPpm(),
                    0.25, 50, 0.25, m -> m.getNumber().doubleValue() + "ppm");

            if (hasMs2) {
                smallParameters.addNamed("MS2 mass accuracy (ppm)", ppmSpinner);
                addAdvancedParameter(smallParameters, "MS/MS isotope scorer", ms2IsotpeSetting);
            }

            candidatesSpinner = makeIntParameterSpinner("NumberOfCandidates", 1, 10000, 1);
            addAdvancedParameter(smallParameters, "Candidates stored", candidatesSpinner);

            candidatesPerIonSpinner = makeIntParameterSpinner("NumberOfCandidatesPerIon", 0, 10000, 1);
            addAdvancedParameter(smallParameters, "Min candidates per ionization stored", candidatesPerIonSpinner);

            smallParameters.addNamed("Fix formula for detected lipid", makeParameterCheckBox("EnforceElGordoFormula")); //El Gordo detects lipids and by default fixes the formula


            //sync profile with ppm spinner
            profileSelector.addItemListener(e -> {
                final Instrument i = (Instrument) e.getItem();
                final double recommendedPPM = i.ppm;
                ppmSpinner.setValue(recommendedPPM);
            });
        }

        //configure adduct panel
        adductList = new JCheckboxListPanel<>(new JCheckBoxList<>(), isBatchDialog() ? "Fallback Adducts" : "Possible Adducts",
                GuiUtils.formatToolTip("Set expected adduct for data with unknown adduct."));
        adductList.checkBoxList.setPrototypeCellValue(new CheckBoxListItem<>("[M + Na]+ ", false));
        center.add(adductList);
        parameterBindings.put("AdductSettings.fallback", () -> getSelectedAdducts().toString());

        enforceAdducts =  new JToggleButton("enforce", false);
        enforceAdducts.setToolTipText(GuiUtils.formatToolTip("Enforce the selected adducts instead of using them only as fallback only."));
        if (isBatchDialog()) {
            adductList.buttons.add(enforceAdducts);
            parameterBindings.put("AdductSettings.enforced", () -> enforceAdducts.isSelected() ? getSelectedAdducts().toString() : PossibleAdducts.empty().toString());
        } else {
            //alway enforce adducts for single feature.
            parameterBindings.put("AdductSettings.enforced", () -> getSelectedAdducts().toString());
            parameterBindings.put("AdductSettings.detectable", () -> "");
        }

        formulaSearchStrategy = new FormulaSearchStrategy(owner, ecs, hasMs2, isBatchDialog(), parameterBindings);
        add(formulaSearchStrategy);

        // ilp timeouts
        if (hasMs2) {
            final TwoColumnPanel ilpOptions = new TwoColumnPanel();

            treeTimeout = makeIntParameterSpinner("Timeout.secondsPerTree", 0, Integer.MAX_VALUE, 1);
            ilpOptions.addNamed("Tree timeout", treeTimeout);

            comoundTimeout = makeIntParameterSpinner("Timeout.secondsPerInstance", 0, Integer.MAX_VALUE, 1);
            ilpOptions.addNamed("Compound timeout", comoundTimeout);

            mzHeuristic = makeIntParameterSpinner("UseHeuristic.mzToUseHeuristic", 0, 3000, 5);
            ilpOptions.addNamed("Use heuristic above m/z", mzHeuristic);

            mzHeuristicOnly = makeIntParameterSpinner("UseHeuristic.mzToUseHeuristicOnly", 0, 3000, 5);
            ilpOptions.addNamed("Use heuristic only above m/z", mzHeuristicOnly);

            final JPanel technicalParameters = new JPanel();
            RelativeLayout rl = new RelativeLayout(RelativeLayout.Y_AXIS, GuiUtils.MEDIUM_GAP);
            rl.setAlignment(RelativeLayout.LEADING);
            rl.setBorderGap(0);
            technicalParameters.setLayout(rl);
            technicalParameters.add(Box.createRigidArea(new Dimension(0, GuiUtils.LARGE_GAP)));
            technicalParameters.add(new TextHeaderBoxPanel("Fragmentation tree computation", ilpOptions));
            technicalParameters.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));
            add(technicalParameters);
            addAdvancedComponent(technicalParameters);
        }

        // add ionization's of selected compounds to default
        refreshPossibleAdducts(ecs.stream().map(it -> it.getIonType().toString()).collect(Collectors.toSet()), true);
    }

    protected boolean isBatchDialog() {
        return ecs.size() > 1; //should never be 0
    }

    private void addAdvancedParameter(TwoColumnPanel panel, String name, Component control) {
        JLabel label = new JLabel(name);
        panel.add(label, control);

        addAdvancedComponent(label);
        addAdvancedComponent(control);
    }

    public void refreshPossibleAdducts(Set<String> precursorIonTypes, boolean enabled) {
        Set<String> adducts = new HashSet<>();
        Set<String> adductsEnabled = new HashSet<>();

        if (!precursorIonTypes.isEmpty()) {
            AdductSettings settings = PropertyManager.DEFAULTS.createInstanceWithDefaults(AdductSettings.class);
            if (precursorIonTypes.contains(PrecursorIonType.unknownPositive().toString())) {
                adducts.addAll(PeriodicTable.getInstance().getPositiveAdductsAsString());
                adductsEnabled.addAll(
                        Stream.concat(settings.getFallback().stream().filter(PrecursorIonType::isPositive),
                                settings.getEnforced().stream().filter(PrecursorIonType::isPositive))
                        .map(PrecursorIonType::toString)
                        .collect(Collectors.toSet()));
            }

            if (precursorIonTypes.contains(PrecursorIonType.unknownNegative().toString())) {
                adducts.addAll(PeriodicTable.getInstance().getNegativeAdductsAsString());
                adductsEnabled.addAll(
                        Stream.concat(settings.getFallback().stream().filter(PrecursorIonType::isNegative),
                                        settings.getEnforced().stream().filter(PrecursorIonType::isNegative))
                        .map(PrecursorIonType::toString)
                        .collect(Collectors.toSet()));
            }
            adducts.addAll(adductsEnabled);
        }

        if (adducts.isEmpty()) {
            adductList.checkBoxList.replaceElements(precursorIonTypes.stream().sorted().collect(Collectors.toList()));
            adductList.checkBoxList.checkAll();
            adductList.setEnabled(false);
        } else {
            adductList.checkBoxList.replaceElements(adducts.stream().sorted().toList());
            adductList.checkBoxList.uncheckAll();
            if (!isBatchDialog() && !ecs.get(0).getMs2Spectra().isEmpty()) {
                detectPossibleAdducts(ecs.get(0));
            } else {
                adductsEnabled.forEach(adductList.checkBoxList::check);
            }
            adductList.setEnabled(enabled);
        }

    }

    protected void detectPossibleAdducts(InstanceBean ec) {
        //todo is this the same detection as happening in batch mode?
        String notWorkingMessage = "Adduct detection requires MS1 spectrum.";
        if (!ec.getMs1Spectra().isEmpty() || ec.getMergedMs1Spectrum() != null) {
            Jobs.runInBackgroundAndLoad(owner, "Detecting adducts...", () -> {
                final Ms1Preprocessor pp = ApplicationCore.SIRIUS_PROVIDER.sirius().getMs1Preprocessor();
                MutableMs2Experiment experiment = new MutableMs2Experiment(ec.getExperiment(), true);
                DetectedAdducts detectedAdducts = experiment.getAnnotationOrNull(DetectedAdducts.class);
                if (detectedAdducts != null) {
                    //copy DetectedAdducts, to remove previously detected adducts and to make sure the following preprocess does not already alter this annotation (probably not copy-safe)
                    DetectedAdducts daWithoutMS1Detect = new DetectedAdducts();
                    for (String source : detectedAdducts.getSourceStrings()) {
                        if (source != DetectedAdducts.Source.MS1_PREPROCESSOR.name()) {
                            daWithoutMS1Detect.put(source, detectedAdducts.get(source));
                        }
                    }
                    experiment.setAnnotation(DetectedAdducts.class, daWithoutMS1Detect);
                }
                ProcessedInput pi = pp.preprocess(experiment);

                pi.getAnnotation(PossibleAdducts.class).
                        ifPresentOrElse(pa -> {
                                    adductList.checkBoxList.uncheckAll();
                                    pa.getAdducts().stream().map(PrecursorIonType::toString).forEach(adductList.checkBoxList::check);
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

    public PossibleAdducts getSelectedAdducts() {
        return adductList.checkBoxList.getCheckedItems().stream().map(PrecursorIonType::parsePrecursorIonType)
                .flatMap(Optional::stream).collect(Collectors.collectingAndThen(Collectors.toSet(), PossibleAdducts::new));
    }

    public JCheckboxListPanel<CustomDataSources.Source> getSearchDBList() {
        return formulaSearchStrategy.getSearchDBList();
    }

    public List<CustomDataSources.Source> getFormulaSearchDBs() {
        return formulaSearchStrategy.getFormulaSearchDBs();
    }
}
