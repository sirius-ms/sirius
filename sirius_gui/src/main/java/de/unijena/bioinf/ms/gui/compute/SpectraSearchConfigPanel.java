package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.AnalogueSearchSettings;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.IdentitySearchSettings;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.SpectraSearchOptions;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.sirius.ms.sdk.model.SearchableDatabase;

import javax.swing.*;
import java.util.Map;
import java.util.stream.Collectors;

public class SpectraSearchConfigPanel extends SubToolConfigPanelAdvancedParams<SpectraSearchOptions> {
    private final SiriusGui gui;
    private final GlobalConfigPanel globalConfigPanel;
    private final boolean ms2;


    protected JSpinner identityPrecursorDev, identityMinSimilarity, identityMinPeaks, identityMaxHits, analogMinSimilarity, analogMinPeaks, analogMaxHits;
    private JCheckBox enableAnalogSearch;


    public SpectraSearchConfigPanel(SiriusGui gui, GlobalConfigPanel globalConfigPanel, boolean ms2, boolean displayAdvancedParameters) {
        super(SpectraSearchOptions.class, displayAdvancedParameters);
        this.gui = gui;
        this.globalConfigPanel = globalConfigPanel;
        this.ms2 = ms2;

        createPanel();
    }


    private void createPanel() {
        // todo add retention time matching search her if avalaiblabel

        parameterBindings.put("SpectralSearchDB", () ->
                globalConfigPanel.getSearchDBs().stream()
                        .filter(db -> db.isCustomDb())
                        .map(SearchableDatabase::getDatabaseId)
                        .collect(Collectors.joining(",")));

        parameterBindings.put("SpectralMatchingMassDeviation.allowedPeakDeviation", () -> ((SpinnerNumberModel) globalConfigPanel.ppmSpinner.getModel()).getNumber().doubleValue() + "ppm");

        final TwoColumnPanel identitySearch = new TwoColumnPanel();

        //identity search
        identityPrecursorDev = makeParameterSpinner("IdentitySearchSettings.precursorDeviation",
                PropertyManager.DEFAULTS.createInstanceWithDefaults(IdentitySearchSettings.class).getPrecursorDeviation().getPpm(),
                0.25, 50, 0.25, m -> m.getNumber().floatValue() + "ppm");
        identitySearch.addNamed("Precursor Deviation (ppm)", identityPrecursorDev);


        identityMinSimilarity = makeParameterSpinner("IdentitySearchSettings.minSimilarity",
                PropertyManager.DEFAULTS.createInstanceWithDefaults(IdentitySearchSettings.class).getMinSimilarity(),
                0.2, 1, 0.05, m -> String.valueOf(m.getNumber().floatValue()));
        addAdvancedParameter(identitySearch, "Minimum Similarity", identityMinSimilarity);


        identityMinPeaks = makeParameterSpinner("IdentitySearchSettings.minNumOfPeaks",
                PropertyManager.DEFAULTS.createInstanceWithDefaults(IdentitySearchSettings.class).getMinNumOfPeaks(),
                1, 100, 1, m -> String.valueOf(m.getNumber().intValue()));
        addAdvancedParameter(identitySearch, "Minimum Matched Peaks", identityMinPeaks);

        identityMaxHits = makeParameterSpinner("IdentitySearchSettings.maxNumOfHits",
                PropertyManager.DEFAULTS.createInstanceWithDefaults(IdentitySearchSettings.class).getMaxNumOfHits(),
                1, 500, 1, m -> String.valueOf(m.getNumber().intValue()));
        addAdvancedParameter(identitySearch, "Matches stored", identityMaxHits);


        TextHeaderBoxPanel generalPanel = new TextHeaderBoxPanel("Identity Search", identitySearch);
        add(generalPanel);

        // Analogue Search params
        final TwoColumnPanel analogSearch = new TwoColumnPanel();
        enableAnalogSearch = makeParameterCheckBox("AnalogueSearchSettings.enabled"); //includes binding
        analogSearch.addNamed("Perform Analogue Search", enableAnalogSearch);

        analogMinSimilarity = makeParameterSpinner("AnalogueSearchSettings.minSimilarity",
                PropertyManager.DEFAULTS.createInstanceWithDefaults(AnalogueSearchSettings.class).getMinSimilarity(),
                0.2, 1, 0.05, m -> String.valueOf(m.getNumber().floatValue()));
        addAdvancedParameter(analogSearch, "Minimum Similarity", analogMinSimilarity);

        analogMinPeaks = makeParameterSpinner("AnalogueSearchSettings.minNumOfPeaks",
                PropertyManager.DEFAULTS.createInstanceWithDefaults(AnalogueSearchSettings.class).getMinNumOfPeaks(),
                1, 100, 1, m -> String.valueOf(m.getNumber().intValue()));
        addAdvancedParameter(analogSearch, "Minimum Matched Peaks", analogMinPeaks);

        analogMaxHits = makeParameterSpinner("AnalogueSearchSettings.maxNumOfHits",
                PropertyManager.DEFAULTS.createInstanceWithDefaults(IdentitySearchSettings.class).getMaxNumOfHits(),
                1, 500, 1, m -> String.valueOf(m.getNumber().intValue()));
        addAdvancedParameter(analogSearch, "Matches stored", analogMaxHits);

        TextHeaderBoxPanel edgePanel = new TextHeaderBoxPanel("Analogue Search", analogSearch);
        add(edgePanel);
    }


    @Override
    public void applyValuesFromPreset(Map<String, String> preset) {
        identityPrecursorDev.setValue(Deviation.valueOf(preset.get("IdentitySearchSettings.precursorDeviation")).getPpm());
        identityMinSimilarity.setValue(Float.parseFloat(preset.get("IdentitySearchSettings.minSimilarity")));
        identityMinPeaks.setValue(Integer.parseInt(preset.get("IdentitySearchSettings.minNumOfPeaks")));
        identityMaxHits.setValue(Integer.parseInt(preset.get("IdentitySearchSettings.maxNumOfHits")));

        enableAnalogSearch.setSelected(Boolean.parseBoolean(preset.get("AnalogueSearchSettings.enabled")));
        analogMinSimilarity.setValue(Float.parseFloat(preset.get("AnalogueSearchSettings.minSimilarity")));
        analogMinPeaks.setValue(Integer.parseInt(preset.get("AnalogueSearchSettings.minNumOfPeaks")));
        analogMaxHits.setValue(Integer.parseInt(preset.get("AnalogueSearchSettings.maxNumOfHits")));
    }
}
