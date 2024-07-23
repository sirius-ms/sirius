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
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.utils.*;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.nightsky.sdk.model.SearchableDatabase;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
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

    protected JCheckboxListPanel<PrecursorIonType> adductList;
    protected JToggleButton enforceAdducts;
    protected JComboBox<Instrument> profileSelector;
    protected JSpinner ppmSpinner, candidatesSpinner, candidatesPerIonSpinner, treeTimeout, comoundTimeout, mzHeuristic, mzHeuristicOnly;

    public enum Strategy {IGNORE, SCORE} //todo remove if Filter is implemented

    protected JComboBox<Strategy> ms2IsotpeSetting;

    protected FormulaSearchStrategy formulaSearchStrategy;


    protected final List<InstanceBean> ecs;


    protected final Dialog owner;
    protected final SiriusGui gui;

    protected boolean hasMs2;


    public FormulaIDConfigPanel(SiriusGui gui, Dialog owner, List<InstanceBean> ecs, boolean ms2, boolean displayAdvancedParameters) {
        super(SiriusOptions.class, displayAdvancedParameters);
        this.ecs = ecs;
        this.owner = owner;
        this.gui = gui;
        this.hasMs2 = ms2;

        createPanel();
    }

    private void createPanel() {

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        final JPanel center = applyDefaultLayout(new JPanel());
        add(center);
        add(Box.createRigidArea(new Dimension(0, GuiUtils.LARGE_GAP)));

        parameterBindings.put("AdductSettings.prioritizeInputFileAdducts", () -> Boolean.toString(isBatchDialog()));

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
            parameterBindings.put("SpectralMatchingMassDeviation.allowedPeakDeviation", () -> ((SpinnerNumberModel) ppmSpinner.getModel()).getNumber().doubleValue() + "ppm");
            parameterBindings.put("SpectralMatchingMassDeviation.allowedPrecursorDeviation", () -> ((SpinnerNumberModel) ppmSpinner.getModel()).getNumber().doubleValue() + "ppm");

            if (hasMs2) {
                smallParameters.addNamed("MS2 mass accuracy (ppm)", ppmSpinner);
                addAdvancedParameter(smallParameters, "MS/MS isotope scorer", ms2IsotpeSetting);
            }

            candidatesSpinner = makeIntParameterSpinner("NumberOfCandidates", 1, 10000, 1);
            addAdvancedParameter(smallParameters, "Candidates stored", candidatesSpinner);

            candidatesPerIonSpinner = makeIntParameterSpinner("NumberOfCandidatesPerIonization", 0, 10000, 1);
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
        adductList.checkBoxList.setPrototypeCellValue(new CheckBoxListItem<>(PrecursorIonType.fromString("[M + Na]+"), false));
        center.add(adductList);
        parameterBindings.put("AdductSettings.fallback", () -> getSelectedAdducts().toString());

        enforceAdducts = new JToggleButton("enforce", false);
        enforceAdducts.setToolTipText(GuiUtils.formatToolTip("Enforce the selected adducts instead of using them only as fallback only."));
        if (isBatchDialog()) {
            adductList.buttons.add(enforceAdducts);
            parameterBindings.put("AdductSettings.enforced", () -> enforceAdducts.isSelected() ? getSelectedAdducts().toString() : PossibleAdducts.empty().toString());
        } else {
            //alway enforce adducts for single feature.
            parameterBindings.put("AdductSettings.enforced", () -> getSelectedAdducts().toString());
            parameterBindings.put("AdductSettings.detectable", () -> "");
        }

        formulaSearchStrategy = new FormulaSearchStrategy(gui, owner, ecs, hasMs2, isBatchDialog(), parameterBindings);
        add(formulaSearchStrategy);
        treeTimeout = makeIntParameterSpinner("Timeout.secondsPerTree", 0, Integer.MAX_VALUE, 1);
        comoundTimeout = makeIntParameterSpinner("Timeout.secondsPerInstance", 0, Integer.MAX_VALUE, 1);
        mzHeuristic = makeIntParameterSpinner("UseHeuristic.useHeuristicAboveMz", 0, 3000, 5);
        mzHeuristicOnly = makeIntParameterSpinner("UseHeuristic.useOnlyHeuristicAboveMz", 0, 3000, 5);

        // ilp timeouts
        if (hasMs2) {
            final TwoColumnPanel ilpOptions = new TwoColumnPanel();

            ilpOptions.addNamed("Tree timeout", treeTimeout);

            ilpOptions.addNamed("Compound timeout", comoundTimeout);

            ilpOptions.addNamed("Use heuristic above m/z", mzHeuristic);

            ilpOptions.addNamed("Use heuristic only above m/z", mzHeuristicOnly);

            final JPanel technicalParameters = new JPanel();
            RelativeLayout rl = new RelativeLayout(RelativeLayout.Y_AXIS, 0);
            rl.setAlignment(RelativeLayout.LEADING);
            technicalParameters.setLayout(rl);
            technicalParameters.add(Box.createRigidArea(new Dimension(0, GuiUtils.LARGE_GAP)));
            technicalParameters.add(new TextHeaderBoxPanel("Fragmentation tree computation", ilpOptions));
            technicalParameters.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));
            add(technicalParameters);
            addAdvancedComponent(technicalParameters);
        }

        // add adducts, either detect adducts for single compound or fallback adducts for correct charge (pos / neg) in batch compute
        refreshPossibleAdducts(isBatchDialog() ? ecs.stream().map(InstanceBean::getIonType).map(it -> it.getCharge()>0?PrecursorIonType.unknownPositive() : PrecursorIonType.unknownNegative()).distinct().collect(Collectors.toSet())  : ecs.stream().map(InstanceBean::getDetectedAdductsOrCharge).flatMap(Set::stream).collect(Collectors.toSet()), true);
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

    public void refreshPossibleAdducts(Set<PrecursorIonType> detectedAdductsOrCharge, boolean enabled) {
        Set<PrecursorIonType> adducts = new HashSet<>();
        Set<PrecursorIonType> adductsEnabled = new HashSet<>();
        Set<PrecursorIonType> detectedAdducteWithoutCharge = detectedAdductsOrCharge.stream().filter(it -> !it.isIonizationUnknown()).collect(Collectors.toSet());

        AdductSettings settings = PropertyManager.DEFAULTS.createInstanceWithDefaults(AdductSettings.class);
        if (!detectedAdductsOrCharge.isEmpty()) {
            if (detectedAdductsOrCharge.stream().anyMatch(PrecursorIonType::isPositive)) {
                adducts.addAll(PeriodicTable.getInstance().getPositiveAdducts());
                if (detectedAdductsOrCharge.contains(PrecursorIonType.unknownPositive())) {
                    adductsEnabled.addAll(
                            Stream.concat(settings.getFallback().stream().filter(PrecursorIonType::isPositive),
                                            settings.getEnforced().stream().filter(PrecursorIonType::isPositive))
                                    .collect(Collectors.toSet()));
                }
            }

            if (detectedAdductsOrCharge.stream().anyMatch(PrecursorIonType::isNegative)) {
                adducts.addAll(PeriodicTable.getInstance().getNegativeAdducts());
                if (detectedAdductsOrCharge.contains(PrecursorIonType.unknownNegative())) {
                    adductsEnabled.addAll(
                            Stream.concat(settings.getFallback().stream().filter(PrecursorIonType::isNegative),
                                            settings.getEnforced().stream().filter(PrecursorIonType::isNegative))
                                    .collect(Collectors.toSet()));
                }
            }

            adductsEnabled.addAll(detectedAdducteWithoutCharge);
            adducts.addAll(adductsEnabled);
        }


        if (adducts.isEmpty()) {
            adductList.checkBoxList.replaceElements(detectedAdductsOrCharge.stream().sorted().collect(Collectors.toList()));
            adductList.checkBoxList.checkAll();
            adductList.setEnabled(false);
        } else {
            adductList.checkBoxList.replaceElements(adducts.stream().sorted().toList());
            adductList.checkBoxList.uncheckAll();
            if (!isBatchDialog()) {
                if (detectedAdducteWithoutCharge.isEmpty())
                    settings.getFallback().forEach(adductList.checkBoxList::check);
                else
                    detectedAdducteWithoutCharge.forEach(adductList.checkBoxList::check);
            } else {
                adductsEnabled.forEach(adductList.checkBoxList::check);
            }
            adductList.setEnabled(enabled);
        }
    }

    public FormulaSearchStrategy getFormulaSearchStrategy() {
        return formulaSearchStrategy;
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
        return new PossibleAdducts(adductList.checkBoxList.getCheckedItems());
    }

    public JCheckboxListPanel<SearchableDatabase> getSearchDBList() {
        return formulaSearchStrategy.getSearchDBList();
    }

    public List<SearchableDatabase> getFormulaSearchDBs() {
        return formulaSearchStrategy.getFormulaSearchDBs();
    }
}
