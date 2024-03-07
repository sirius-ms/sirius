package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.gui.dialogs.InfoDialog;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

public class StructureSearchStrategy extends JPanel {

    protected JCheckboxListPanel<CustomDataSources.Source> searchDBList;
    public static final String DO_NOT_SHOW_DIVERGING_DATABASES_NOTE = "de.unijena.bioinf.sirius.computeDialog.divergingDatabases.dontAskAgain";

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
            addDivergingDatabasesNote();
        }
    }

    private void addDivergingDatabasesNote() {
        if (!PropertyManager.getBoolean(DO_NOT_SHOW_DIVERGING_DATABASES_NOTE, false)) {
            ItemListener dbSelectionChangeListener = new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    new InfoDialog(MainFrame.MF, "Note that you are searching in different databases for formula and structure.", DO_NOT_SHOW_DIVERGING_DATABASES_NOTE);
                    searchDBList.checkBoxList.removeCheckBoxListener(this);
                }
            };
            searchDBList.checkBoxList.addCheckBoxListener(dbSelectionChangeListener);
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

    public List<String> getStructureSearchDBStrings() {
        return getStructureSearchDBs().stream().map(CustomDataSources.Source::name).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
