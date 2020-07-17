package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.gui.compute.DBSelectionList;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;


public class DatabaseImportConfigPanel extends SubToolConfigPanel<CustomDBOptions> {

    JCheckboxListPanel parentDBList;
    JSpinner bufferSize;
    JTextField name;

    public DatabaseImportConfigPanel() {
        this(null);
    }

    public DatabaseImportConfigPanel(@Nullable String dbName) {
        super(CustomDBOptions.class);


        final TwoColumnPanel smalls = new TwoColumnPanel();
        add(new TextHeaderBoxPanel("Parameters", smalls));

        name = new JTextField(dbName != null ? dbName : "");
        name.setMinimumSize(new Dimension(150, name.getMinimumSize().height));
        name.setPreferredSize(new Dimension(150, name.getPreferredSize().height));
        name.setEnabled(dbName == null);
        getOptionDescriptionByName("name").ifPresent(it -> name.setToolTipText(GuiUtils.formatToolTip(it)));
        smalls.addNamed("Name", name);
        parameterBindings.put("name", name::getText);

        final String buf = "buffer";
        bufferSize = makeGenericOptionSpinner(buf,
                getOptionDefaultByName(buf).map(Integer::parseInt).orElse(1),
                1, Integer.MAX_VALUE, 1,
                (v) -> String.valueOf(v.getNumber().intValue()));
        smalls.addNamed("Buffer Size", bufferSize);



        // configure database to derive from
        parentDBList = new JCheckboxListPanel<>(new DBSelectionList(false), "Derive DB from:");
        getOptionDescriptionByName("derive-from").ifPresent(it -> parentDBList.setToolTipText(GuiUtils.formatToolTip(it)));
        add(parentDBList);
        parameterBindings.put("derive-from", () -> parentDBList.checkBoxList.getCheckedItems().isEmpty() ? null : String.join(",", ((DBSelectionList) parentDBList.checkBoxList).getSelectedFormulaSearchDBStrings()));
    }
}
