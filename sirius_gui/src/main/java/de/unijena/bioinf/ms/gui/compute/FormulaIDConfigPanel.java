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
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ms.frontend.subtools.sirius.SiriusOptions;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.RelativeLayout;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.SearchableDatabase;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
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
public class
FormulaIDConfigPanel extends SubToolConfigPanelAdvancedParams<SiriusOptions> {
    protected Logger logger = LoggerFactory.getLogger(FormulaIDConfigPanel.class);

    public enum Instrument {
        QTOF("Q-TOF", MsInstrumentation.Instrument.QTOF, "qtof", 10),
        ORBI("Orbitrap", MsInstrumentation.Instrument.ORBI, "orbitrap", 5);
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
    protected JCheckBox isotopeSettingsFilter, enforceElGordo;

    public enum Strategy {IGNORE, SCORE} //todo remove if Filter is implemented

    protected JComboBox<Strategy> ms2IsotpeSetting;

    @Getter
    protected FormulaSearchStrategy formulaSearchStrategy;


    protected final List<InstanceBean> allInstances;
    protected final List<InstanceBean> ecs;


    protected final Dialog owner;
    protected final SiriusGui gui;

    protected boolean hasMs2;


    public FormulaIDConfigPanel(SiriusGui gui, Dialog owner, List<InstanceBean> ecs, boolean ms2, boolean displayAdvancedParameters) {
        super(SiriusOptions.class, displayAdvancedParameters);
        this.allInstances = gui.getMainFrame().getCompounds();
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

            isotopeSettingsFilter = makeParameterCheckBox("IsotopeSettings.filter");
            addAdvancedParameter(smallParameters, "Filter by isotope pattern", isotopeSettingsFilter);

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

            enforceElGordo = makeParameterCheckBox("EnforceElGordoFormula");  //El Gordo detects lipids and by default fixes the formula
            smallParameters.addNamed("Fix formula for detected lipid", enforceElGordo);


            //sync profile with ppm spinner
            profileSelector.addItemListener(e -> {
                final Instrument i = (Instrument) e.getItem();
                final double recommendedPPM = i.ppm;
                ppmSpinner.setValue(recommendedPPM);
            });
        }

        //configure adduct panel
        if(isBatchDialog()){
            adductList = new JCheckboxListPanel<>(new JCheckBoxList<>(), "Fallback Adducts",
                    GuiUtils.formatToolTip("Select fallback adducts to be used if no adducts could be detected. By default, all adducts detected in this project are selected."));
        }else {
            adductList = new JCheckboxListPanel<>(new JCheckBoxList<>(),"Possible Adducts",
                    GuiUtils.formatToolTip("Select possible adducts to be used for formula identification. By default, the detected adducts of this feature are selected."));
        }

        adductList.checkBoxList.setPrototypeCellValue(new CheckBoxListItem<>(PrecursorIonType.fromString("[M + Na]+"), false));
        center.add(adductList);
        parameterBindings.put("AdductSettings.fallback", () -> getSelectedAdducts().toString());

        enforceAdducts = new JToggleButton("enforce", false);
        enforceAdducts.setToolTipText(GuiUtils.formatToolTip("Enforce the selected adducts instead of using them only as fallback only."));
        if (isBatchDialog()) {
            adductList.buttons.add(enforceAdducts);
            parameterBindings.put("AdductSettings.enforced", () -> enforceAdducts.isSelected() ? getSelectedAdducts().toString() : PossibleAdducts.empty().toString());
            parameterBindings.put("AdductSettings.ignoreDetectedAdducts", () -> "false");
        } else {
            //always enforce adducts for single feature.
            parameterBindings.put("AdductSettings.enforced", () -> getSelectedAdducts().toString());
            parameterBindings.put("AdductSettings.detectable", () -> "");
            parameterBindings.put("AdductSettings.ignoreDetectedAdducts", () -> "true");
        }

        formulaSearchStrategy = new FormulaSearchStrategy(gui, owner, ecs, hasMs2, isBatchDialog(), parameterBindings, this);
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

    private Pair<Set<PrecursorIonType>, Set<PrecursorIonType>> getAdducts(PrecursorIonType[] fallbackAdducts, PrecursorIonType[] enforcedAdducts) {
        Set<PrecursorIonType> detectedAdductsOrCharge = ecs.stream().map(InstanceBean::getDetectedAdductsOrCharge).flatMap(Set::stream).collect(Collectors.toSet());
        Set<PrecursorIonType> detectedUnknowns = detectedAdductsOrCharge.stream().filter(PrecursorIonType::isIonizationUnknown).collect(Collectors.toSet());
        Set<PrecursorIonType> detectedAdductsNoMulti = detectedAdductsOrCharge.stream().filter(ion -> !ion.isIonizationUnknown() && !ion.isMultimere() && !ion.isMultipleCharged()).collect(Collectors.toSet());

        Set<PrecursorIonType> possibleAdducts = gui.getProjectManager().INSTANCE_LIST.stream().map(InstanceBean::getDetectedAdducts).flatMap(Set::stream).filter(ion -> !ion.isIonizationUnknown() && !ion.isMultimere() && !ion.isMultipleCharged()).collect(Collectors.toSet());
        Set<PrecursorIonType> selectedAdducts = new HashSet<>(detectedAdductsNoMulti);

        if (detectedAdductsOrCharge.stream().anyMatch(PrecursorIonType::isPositive)) {
            PeriodicTable.getInstance().getPositiveAdducts().stream().filter(ion -> !ion.isMultimere() && !ion.isMultipleCharged()).forEach(possibleAdducts::add);
            if (detectedAdductsNoMulti.isEmpty() || detectedUnknowns.contains(PrecursorIonType.unknownPositive())) {
                Arrays.stream(fallbackAdducts).filter(PrecursorIonType::isPositive).filter(possibleAdducts::contains).forEach(selectedAdducts::add);
                Arrays.stream(enforcedAdducts).filter(PrecursorIonType::isPositive).filter(possibleAdducts::contains).forEach(selectedAdducts::add);
            }
        }

        if (detectedAdductsOrCharge.stream().anyMatch(PrecursorIonType::isNegative)) {
            PeriodicTable.getInstance().getNegativeAdducts().stream().filter(ion -> !ion.isMultimere() && !ion.isMultipleCharged()).forEach(possibleAdducts::add);
            if (detectedAdductsNoMulti.isEmpty() || detectedUnknowns.contains(PrecursorIonType.unknownNegative())) {
                Arrays.stream(fallbackAdducts).filter(PrecursorIonType::isNegative).filter(possibleAdducts::contains).forEach(selectedAdducts::add);
                Arrays.stream(enforcedAdducts).filter(PrecursorIonType::isNegative).filter(possibleAdducts::contains).forEach(selectedAdducts::add);
            }
        }

        return Pair.of(possibleAdducts, selectedAdducts);
    }

    private void refreshAdducts(Set<PrecursorIonType> possibleAdducts, Set<PrecursorIonType> selectedAdducts) {
        adductList.checkBoxList.replaceElements(possibleAdducts.stream().sorted().toList());
        adductList.checkBoxList.uncheckAll();
        selectedAdducts.forEach(adductList.checkBoxList::check);
        adductList.setEnabled(true);
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

    @Override
    public void applyValuesFromPreset(Map<String, String> preset) {
        String profileString = preset.get("AlgorithmProfile");
        Instrument instrument = Arrays.stream(Instrument.values()).filter(i -> i.profile.equalsIgnoreCase(profileString)).findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Could not parse algorithm profile " + profileString));
        profileSelector.setSelectedItem(instrument);

        isotopeSettingsFilter.setSelected(Boolean.parseBoolean(preset.get("IsotopeSettings.filter")));

        String isotopeMs2Setting = preset.get("IsotopeMs2Settings");
        try {
            ms2IsotpeSetting.setSelectedItem(Strategy.valueOf(isotopeMs2Setting));
        } catch (IllegalArgumentException e) {
            throw new UnsupportedOperationException("Could not parse MS/MS isotope scorer " + isotopeMs2Setting);
        }

        if (preset.get("MS2MassDeviation.allowedMassDeviation").equals(preset.get("SpectralMatchingMassDeviation.allowedPeakDeviation"))
                && preset.get("MS2MassDeviation.allowedMassDeviation").equals(preset.get("SpectralMatchingMassDeviation.allowedPrecursorDeviation"))) {
            Deviation d = Deviation.fromString(preset.get("MS2MassDeviation.allowedMassDeviation"));
            ppmSpinner.setValue(d.getPpm());
        } else {
            throw new UnsupportedOperationException("Properties MS2MassDeviation.allowedMassDeviation, SpectralMatchingMassDeviation.allowedPeakDeviation, SpectralMatchingMassDeviation.allowedPrecursorDeviation should all have the same value");
        }

        candidatesSpinner.setValue(Integer.parseInt(preset.get("NumberOfCandidates")));
        candidatesPerIonSpinner.setValue(Integer.parseInt(preset.get("NumberOfCandidatesPerIonization")));

        enforceElGordo.setSelected(Boolean.parseBoolean(preset.get("EnforceElGordoFormula")));

        try {
            PrecursorIonType[] fallbackAdducts = ParameterConfig.convertToCollection(PrecursorIonType.class, preset.get("AdductSettings.fallback"));
            PrecursorIonType[] enforcedAdducts = ParameterConfig.convertToCollection(PrecursorIonType.class, preset.get("AdductSettings.enforced"));
            Pair<Set<PrecursorIonType>, Set<PrecursorIonType>> possibleAndSelected = getAdducts(fallbackAdducts, enforcedAdducts);
            refreshAdducts(possibleAndSelected.left(), possibleAndSelected.right());
            enforceAdducts.setSelected(preset.get("AdductSettings.fallback").equals(preset.get("AdductSettings.enforced")));
        } catch (Exception e) {
            throw new UnsupportedOperationException("Could not parse adducts: " + e.getMessage());
        }

        treeTimeout.setValue(Integer.parseInt(preset.get("Timeout.secondsPerTree")));
        comoundTimeout.setValue(Integer.parseInt(preset.get("Timeout.secondsPerInstance")));
        mzHeuristic.setValue(Integer.parseInt(preset.get("UseHeuristic.useHeuristicAboveMz")));
        mzHeuristicOnly.setValue(Integer.parseInt(preset.get("UseHeuristic.useOnlyHeuristicAboveMz")));

        formulaSearchStrategy.applyValuesFromPreset(preset);
    }
}
