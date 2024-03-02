package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.List;

public class StructureSearchStrategy extends JPanel {

    protected JCheckboxListPanel<CustomDataSources.Source> searchDBList;


    public StructureSearchStrategy(@Nullable final JCheckBoxList<CustomDataSources.Source> syncSource) {
        createPanel(syncSource);
    }

    private void createPanel(@Nullable JCheckBoxList<CustomDataSources.Source> syncSource) {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        createStrategyPanel(syncSource);
    }

    private void createStrategyPanel(@Nullable JCheckBoxList<CustomDataSources.Source> syncSource) {
        searchDBList = createDatabasePanel();
        add(searchDBList);

        if (syncSource != null) {
            syncSource.getCheckedItems().forEach(searchDBList.checkBoxList::check);
            syncSource.addCheckBoxListener(e -> {
                @SuppressWarnings("unchecked")
                CustomDataSources.Source item = (CustomDataSources.Source) ((CheckBoxListItem<Object>) e.getItem()).getValue();
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    searchDBList.checkBoxList.check(item);
                } else {
                    searchDBList.checkBoxList.uncheck(item);
                }
            });
        }
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
