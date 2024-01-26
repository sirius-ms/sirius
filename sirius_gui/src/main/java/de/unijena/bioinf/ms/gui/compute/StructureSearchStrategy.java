package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ChemistryBase.utils.DescriptiveOptions;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.annotations.StructureSearchDB;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StructureSearchStrategy extends JPanel {

    public enum Strategy implements DescriptiveOptions {
        PUBCHEM_AS_FALLBACK("Search in the specified set of databases and use the PubChem database as fallback if no good hit is available"), //todo Workflow: this should forbid PubChem in the list of DBs
        NO_FALLBACK("Search in the specified set of databases");

        private final String description;

        Strategy(String description) {
            this.description = description;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    protected final Strategy strategy;
    protected final ParameterBinding parameterBindings;
    private boolean isEnabled;
    protected JCheckboxListPanel<CustomDataSources.Source> searchDBList;


    public StructureSearchStrategy(Strategy strategy, ParameterBinding parameterBindings, @Nullable final JCheckBoxList<CustomDataSources.Source> syncSource) {
        this.strategy = strategy;
        this.parameterBindings = parameterBindings;

        createPanel(syncSource);

    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        revalidate();
    }

    private void createPanel(@Nullable JCheckBoxList<CustomDataSources.Source> syncSource) {
        this.removeAll();
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        createStrategyPanel(strategy, this, syncSource);

    }

    private void createStrategyPanel(Strategy strategy, JPanel main, @Nullable JCheckBoxList<CustomDataSources.Source> syncSource) {
        // configure database to search list
        DBSelectionList innerList = new DBSelectionList();
        searchDBList = createDatabasePanel();
        add(searchDBList);

//        add(new TextHeaderBoxPanel("General", additionalOptions));

        PropertyManager.DEFAULTS.createInstanceWithDefaults(StructureSearchDB.class).searchDBs
                .forEach(s -> searchDBList.checkBoxList.check(CustomDataSources.getSourceFromName(s.name())));

        //todo NewWorkflow: not sure, if this works properly
        if (syncSource != null)
            syncSource.addListSelectionListener(e -> {
                searchDBList.checkBoxList.uncheckAll();
                if (syncSource.getCheckedItems().isEmpty())
                    searchDBList.checkBoxList.check(CustomDataSources.getSourceFromName(DataSource.BIO.realName()));
                else
                    searchDBList.checkBoxList.checkAll(syncSource.getCheckedItems());
            });

    }


    private JCheckboxListPanel<CustomDataSources.Source> createDatabasePanel() {
        //todo NewWorkflow: should this be identical to the panel in FormulaSearchStrategy? probably yes. But not Sync it?
        if (this.searchDBList != null) return this.searchDBList;
        // configure database to search list
        DBSelectionList innerList = new DBSelectionList();
        searchDBList = new JCheckboxListPanel<>(innerList, "Search DBs");
        GuiUtils.assignParameterToolTip(searchDBList, "StructureSearchDB");
        parameterBindings.put("StructureSearchDB", () -> searchDBList.checkBoxList.getCheckedItems().isEmpty() ? null : String.join(",", getStructureSearchDBStrings()));
        return searchDBList;
    }

    public JCheckboxListPanel<CustomDataSources.Source> getSearchDBList() {
        return searchDBList;
    }

    public List<CustomDataSources.Source> getStructureSearchDBs() {
        return searchDBList.checkBoxList.getCheckedItems();
    }

    public List<String> getStructureSearchDBStrings() {
        return getStructureSearchDBs().stream().map(CustomDataSources.Source::id).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
