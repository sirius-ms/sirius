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

    protected JCheckboxListPanel<CustomDataSources.Source> searchDBList;


    public StructureSearchStrategy(@Nullable final JCheckBoxList<CustomDataSources.Source> syncSource) {
        createPanel(syncSource);
    }

    private void createPanel(@Nullable JCheckBoxList<CustomDataSources.Source> syncSource) {
//        this.removeAll();
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        createStrategyPanel(syncSource);
    }

    private void createStrategyPanel(@Nullable JCheckBoxList<CustomDataSources.Source> syncSource) {
        // configure database to search list
        searchDBList = createDatabasePanel();

        add(searchDBList);

        PropertyManager.DEFAULTS.createInstanceWithDefaults(StructureSearchDB.class).searchDBs
                .forEach(s -> searchDBList.checkBoxList.check(CustomDataSources.getSourceFromName(s.name())));

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
        DBSelectionList innerList = new DBSelectionList();
        JCheckboxListPanel<CustomDataSources.Source> dbList = new JCheckboxListPanel<>(innerList, "Search DBs");
        GuiUtils.assignParameterToolTip(dbList, "StructureSearchDB");
        return dbList;
    }

    public JCheckboxListPanel<CustomDataSources.Source> getSearchDBList() {
        return searchDBList;
    }

    public List<CustomDataSources.Source> getStructureSearchDBs() {
        return searchDBList.checkBoxList.getCheckedItems();
    }
}
