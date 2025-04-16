package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.SearchableDatabase;
import it.unimi.dsi.fastutil.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class GlobalConfigPanel extends ConfigPanel {

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

    protected JSpinner ppmSpinner;

    protected DBSelectionListPanel searchDBList;


    protected final List<InstanceBean> allInstances;
    protected final List<InstanceBean> ecs;

    protected final Dialog owner;
    protected final SiriusGui gui;

    protected boolean hasMs2;

    public GlobalConfigPanel(SiriusGui gui, Dialog owner, List<InstanceBean> ecs, boolean ms2) {
        super();
        this.allInstances = gui.getMainFrame().getCompounds();
        this.ecs = ecs;
        this.owner = owner;
        this.gui = gui;
        this.hasMs2 = ms2;

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        final JPanel center = applyDefaultLayout(new JPanel());
        add(center);
        add(Box.createRigidArea(new Dimension(0, GuiUtils.LARGE_GAP)));

        {
            final TwoColumnPanel smallParameters = new TwoColumnPanel();
            center.add(smallParameters);
//            center.add(new TextHeaderBoxPanel("General", smallParameters));

            profileSelector = makeParameterComboBox("AlgorithmProfile", List.of(Instrument.values()), Instrument::asProfile);
            smallParameters.addNamed("Instrument", profileSelector);

            ppmSpinner = makeParameterSpinner("MS2MassDeviation.allowedMassDeviation",
                    PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class).allowedMassDeviation.getPpm(),
                    0.25, 50, 0.25, m -> m.getNumber().doubleValue() + "ppm");
            parameterBindings.put("SpectralMatchingMassDeviation.allowedPeakDeviation", () -> ((SpinnerNumberModel) ppmSpinner.getModel()).getNumber().doubleValue() + "ppm");
            parameterBindings.put("SpectralMatchingMassDeviation.allowedPrecursorDeviation", () -> ((SpinnerNumberModel) ppmSpinner.getModel()).getNumber().doubleValue() + "ppm");

            if (hasMs2) {
                smallParameters.addNamed("MS2 mass accuracy (ppm)", ppmSpinner);
            }

            //sync profile with ppm spinner
            profileSelector.addItemListener(e -> {
                final Instrument i = (Instrument) e.getItem();
                final double recommendedPPM = i.ppm;
                ppmSpinner.setValue(recommendedPPM);
            });
        }


        //configure adduct panel
        {
            if (isBatchDialog()) {
                adductList = new JCheckboxListPanel<>(new JCheckBoxList<>(), "Fallback Adducts",
                        GuiUtils.formatToolTip("Select fallback adducts to be used if no adducts could be detected. By default, all adducts detected in this project are selected."));
            } else {
                adductList = new JCheckboxListPanel<>(new JCheckBoxList<>(), "Possible Adducts",
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
        }

        //configure db selection panel
        {
            searchDBList = DBSelectionListPanel.newInstance("Search DBs", gui.getSiriusClient(), Collections::emptyList);
//            GuiUtils.assignParameterToolTip(searchDBList.checkBoxList, ""); //todo add description
            searchDBList.selectDefaultDatabases();
            center.add(searchDBList);
        }

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


    public PossibleAdducts getSelectedAdducts() {
        return new PossibleAdducts(adductList.checkBoxList.getCheckedItems());
    }

    public JCheckboxListPanel<SearchableDatabase> getSearchDBList() {
        return searchDBList;
    }


    public List<SearchableDatabase> getSearchDBs() {
        return searchDBList.checkBoxList.getCheckedItems();
    }

    public List<String> getSearchDBStrings() {
        return getSearchDBs().stream().map(SearchableDatabase::getDatabaseId).collect(Collectors.toList());
    }

    public void applyValuesFromPreset(Map<String, String> preset) {
        String profileString = preset.get("AlgorithmProfile");
        Instrument instrument = Arrays.stream(Instrument.values()).filter(i -> i.profile.equalsIgnoreCase(profileString)).findFirst()
                .orElseThrow(() -> new RuntimeException("Could not parse algorithm profile " + profileString + "."));
        profileSelector.setSelectedItem(instrument);


        if (preset.get("MS2MassDeviation.allowedMassDeviation").equals(preset.get("SpectralMatchingMassDeviation.allowedPeakDeviation"))
                && preset.get("MS2MassDeviation.allowedMassDeviation").equals(preset.get("SpectralMatchingMassDeviation.allowedPrecursorDeviation"))) {
            Deviation d = Deviation.fromString(preset.get("MS2MassDeviation.allowedMassDeviation"));
            ppmSpinner.setValue(d.getPpm());
        } else {
            throw new UnsupportedOperationException("Properties MS2MassDeviation.allowedMassDeviation, SpectralMatchingMassDeviation.allowedPeakDeviation, SpectralMatchingMassDeviation.allowedPrecursorDeviation should all have the same value.");
        }

        Set<PrecursorIonType> fallbackAdducts;
        Set<PrecursorIonType> enforcedAdducts;
        try {
            fallbackAdducts = Arrays.stream(ParameterConfig.convertToCollection(PrecursorIonType.class, preset.get("AdductSettings.fallback")))
                    .collect(Collectors.toSet());
            enforcedAdducts = Arrays.stream(ParameterConfig.convertToCollection(PrecursorIonType.class, preset.get("AdductSettings.enforced")))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException("Could not parse adducts: " + e.getMessage());
        }

        enforceAdducts.setSelected(fallbackAdducts.equals(enforcedAdducts));
        if (!fallbackAdducts.equals(enforcedAdducts) && !enforcedAdducts.isEmpty()) {
            throw new UnsupportedOperationException("Enforced adducts differ from fallback adducts.");
        }

        Pair<Set<PrecursorIonType>, Set<PrecursorIonType>> possibleAndSelected = getAdducts(fallbackAdducts, isBatchDialog(), !isBatchDialog()); //in batch-mode we always use the fallback adducts (only) - adding detected adducts was no good idea, since there are too many of them.
        refreshAdducts(possibleAndSelected.left(), possibleAndSelected.right());
    }

    protected boolean isBatchDialog() {
        return ecs.size() != 1;
    }

    /**
     * @param fallbackSelection            default candidates (pos and neg) to use if no adducts could be detected or the unknown adduct indicates adding them
     * @param forceFallback                forces the selection of all fallbackSelection candidates (with the correct charge)
     * @param addBaseIonizationForDetected add the base ionization for each detected adduct to the list of selected
     * @return set of all selectable adducts, set of all selected adducts
     */
    private Pair<Set<PrecursorIonType>, Set<PrecursorIonType>> getAdducts(Set<PrecursorIonType> fallbackSelection, boolean forceFallback, boolean addBaseIonizationForDetected) {

        if (ecs.isEmpty()) {  // get adducts from settings
            Set<PrecursorIonType> possible = PeriodicTable.getInstance().getAdducts().stream().filter(ion -> !ion.isMultimere() && !ion.isMultipleCharged()).collect(Collectors.toCollection(HashSet::new));
            possible.addAll(fallbackSelection);
            return Pair.of(possible, fallbackSelection);
        }

        Set<PrecursorIonType> detectedAdductsOrCharge = ecs.stream()
                .map(InstanceBean::getDetectedAdductsOrCharge)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        Set<PrecursorIonType> detectedUnknowns = detectedAdductsOrCharge.stream()
                .filter(PrecursorIonType::isIonizationUnknown)
                .collect(Collectors.toSet()); //selected the [M+?]+ or [M+?]- PrecursorIonTypes

        Set<PrecursorIonType> detectedAdductsNoMulti = detectedAdductsOrCharge.stream()
                .filter(ion -> !ion.isIonizationUnknown() && !ion.isMultimere() && !ion.isMultipleCharged())
                .collect(Collectors.toSet());

        // list of adducts to be shown in the Compute panel
        Set<PrecursorIonType> possibleAdducts = gui.getProjectManager().INSTANCE_LIST.stream()
                .map(InstanceBean::getDetectedAdducts)
                .flatMap(Set::stream)
                .filter(ion -> !ion.isIonizationUnknown() && !ion.isMultimere() && !ion.isMultipleCharged())
                .collect(Collectors.toSet());

        // Subset of possibleAdducts where the checkboxes are pre-selected (checked) in the compute panel.
        Set<PrecursorIonType> selectedAdducts = new HashSet<>();

        if (!forceFallback) {
            selectedAdducts.addAll(detectedAdductsNoMulti);
            if (addBaseIonizationForDetected) {
                detectedAdductsNoMulti.stream().map(p -> PrecursorIonType.getPrecursorIonType(p.getIonization())).forEach(selectedAdducts::add);
            }
        }

        if (detectedAdductsOrCharge.stream().anyMatch(PrecursorIonType::isPositive)) {
            PeriodicTable.getInstance().getPositiveAdducts().stream().filter(ion -> !ion.isMultimere() && !ion.isMultipleCharged())
                    .forEach(possibleAdducts::add);
            if (forceFallback || detectedAdductsNoMulti.isEmpty() || detectedUnknowns.contains(PrecursorIonType.unknownPositive())) {
                fallbackSelection.stream().filter(PrecursorIonType::isPositive).forEach(selectedAdducts::add);
                possibleAdducts.addAll(selectedAdducts);
            }
        }

        if (detectedAdductsOrCharge.stream().anyMatch(PrecursorIonType::isNegative)) {
            PeriodicTable.getInstance().getNegativeAdducts().stream().filter(ion -> !ion.isMultimere() && !ion.isMultipleCharged())
                    .forEach(possibleAdducts::add);
            if (forceFallback || detectedAdductsNoMulti.isEmpty() || detectedUnknowns.contains(PrecursorIonType.unknownNegative())) {
                fallbackSelection.stream().filter(PrecursorIonType::isNegative).forEach(selectedAdducts::add);
                possibleAdducts.addAll(selectedAdducts);
            }
        }

        return Pair.of(possibleAdducts, selectedAdducts);
    }
}
