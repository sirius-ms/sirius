package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.dialogs.InfoDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.nightsky.sdk.model.SearchableDatabase;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

public class StructureSearchStrategy extends JPanel {

    protected JCheckboxListPanel<SearchableDatabase> searchDBList;
    protected SiriusGui gui;
    public static final String DO_NOT_SHOW_DIVERGING_DATABASES_NOTE = "de.unijena.bioinf.sirius.computeDialog.divergingDatabases.dontAskAgain";

    public StructureSearchStrategy(SiriusGui gui, @Nullable final FormulaSearchStrategy syncStrategy) {
        this.gui = gui;
        createPanel(syncStrategy);
    }

    private void createPanel(@Nullable final FormulaSearchStrategy syncStrategy) {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        createStrategyPanel(syncStrategy);
    }

    private void createStrategyPanel(@Nullable final FormulaSearchStrategy syncStrategy) {
        searchDBList = createDatabasePanel();
        add(searchDBList);

        if (syncStrategy != null) {
            JCheckBoxList<SearchableDatabase> syncCheckBoxList = syncStrategy.getSearchDBList().checkBoxList;

            syncCheckBoxList.getCheckedItems().forEach(searchDBList.checkBoxList::check);
            syncCheckBoxList.addCheckBoxListener(e -> {
                @SuppressWarnings("unchecked")
                SearchableDatabase item = (SearchableDatabase) ((CheckBoxListItem<Object>) e.getItem()).getValue();
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    searchDBList.checkBoxList.check(item);
                } else {
                    searchDBList.checkBoxList.uncheck(item);
                }
            });
            addDivergingDatabasesNote(syncStrategy);
        }
    }

    private void addDivergingDatabasesNote(FormulaSearchStrategy syncStrategy) {
        if (!PropertyManager.getBoolean(DO_NOT_SHOW_DIVERGING_DATABASES_NOTE, false)) {
            ItemListener dbSelectionChangeListener = new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (syncStrategy.getSelectedStrategy() == FormulaSearchStrategy.Strategy.DATABASE && !syncStrategy.getSearchDBList().checkBoxList.isSelectionEqual(searchDBList.checkBoxList)) {
                        new InfoDialog(gui.getMainFrame(), "Note that you are searching in different databases for formula and structure.", DO_NOT_SHOW_DIVERGING_DATABASES_NOTE);
                        searchDBList.checkBoxList.removeCheckBoxListener(this);
                    }
                }
            };
            searchDBList.checkBoxList.addCheckBoxListener(dbSelectionChangeListener);
        }
    }


    private JCheckboxListPanel<SearchableDatabase> createDatabasePanel() {
        DBSelectionListPanel dbList = DBSelectionListPanel.newInstance("Search DBs", gui.getSiriusClient());
        GuiUtils.assignParameterToolTip(dbList, "StructureSearchDB");
        return dbList;
    }

    public JCheckboxListPanel<SearchableDatabase> getSearchDBList() {
        return searchDBList;
    }

    public List<SearchableDatabase> getStructureSearchDBs() {
        return searchDBList.checkBoxList.getCheckedItems();
    }
}
