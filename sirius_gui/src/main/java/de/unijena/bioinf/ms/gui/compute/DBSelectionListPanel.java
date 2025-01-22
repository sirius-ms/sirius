package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.annotations.FormulaSearchDB;
import de.unijena.bioinf.chemdb.annotations.StructureSearchDB;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.model.SearchableDatabase;
import de.unijena.bioinf.ms.properties.PropertyManager;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

public class DBSelectionListPanel extends JCheckboxListPanel<SearchableDatabase> {
    private final SiriusClient client;
    private final SearchableDatabase bioDB;
    private final JButton bioButton;

    protected DBSelectionListPanel(JCheckBoxList<SearchableDatabase> sourceList, String headline, SiriusClient client, Supplier<Collection<SearchableDatabase>> deactivatedDBs) {
        super(sourceList, headline);
        this.client = client;
        bioDB = client.databases().getDatabase(DataSource.BIO.name(), false);

        bioButton = new JButton("bio");
        buttons.add(bioButton);

        bioButton.addActionListener(e -> {
            checkBoxList.uncheckAll();
            select(bioDB);});

        Arrays.stream(all.getActionListeners()).forEach(all::removeActionListener);
        all.addActionListener(e -> {
            checkBoxList.checkAll();
            deactivatedDBs.get().forEach(checkBoxList::uncheck);
        });
    }

    public static DBSelectionListPanel newInstance(String headline, SiriusClient client, Supplier<Collection<SearchableDatabase>> deactivatedDBs) {
        SearchableDatabase bioDB = client.databases().getDatabase(DataSource.BIO.name(), false);
        return new DBSelectionListPanel(DBSelectionList.fromSearchableDatabases(client, Collections.singleton(bioDB)), headline, client, deactivatedDBs);
    }

    public void select(Collection<CustomDataSources.Source> databases){
        databases.forEach(s -> {
            SearchableDatabase db =client.databases().getDatabase(s.name(), false);
            select(db);
        });
    }

    public void select(SearchableDatabase database){
        if (database.equals(bioDB)) {
            //select all bio dbs
            Arrays.stream(DataSource.values()).filter(DataSource::isBioOnly).forEach(ds -> checkBoxList.check(client.databases().getDatabase(ds.name(), false)));
        } else {
            checkBoxList.check(database);
        }
    }

    public void selectDefaultDatabases() {
        selectFormulaSearchDBs();
        selectStructureSearchDBs();
    }

    public void selectFormulaSearchDBs() {
        select(PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaSearchDB.class).searchDBs);
    }

    public void selectStructureSearchDBs() {
        select(PropertyManager.DEFAULTS.createInstanceWithDefaults(StructureSearchDB.class).searchDBs);
    }



}
