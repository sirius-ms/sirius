package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class DatabaseImportConfigPanel extends SubToolConfigPanel<CustomDBOptions> {
    public final FileChooserPanel dbLocationField;

    JSpinner bufferSize;

    public DatabaseImportConfigPanel(@Nullable CustomDatabase db) {
        super(CustomDBOptions.class);

        final TwoColumnPanel smalls = new TwoColumnPanel();
        add(new TextHeaderBoxPanel("Parameters", smalls));

        this.dbLocationField = new FileChooserPanel(JFileChooser.DIRECTORIES_ONLY);
        if (db == null) {
            dbLocationField.field.setPlaceholder("Enter location (no whitespaces)");
        } else {
            dbLocationField.field.setText(db.storageLocation());

        }
        dbLocationField.setEnabled(db == null);


        getOptionDescriptionByName("import").ifPresent(it -> dbLocationField.setToolTipText(GuiUtils.formatToolTip(it)));
        smalls.addNamed("DB location", dbLocationField);
        parameterBindings.put("import", dbLocationField::getFilePath);

        final String buf = "buffer";
        bufferSize = makeGenericOptionSpinner(buf,
                getOptionDefaultByName(buf).map(Integer::parseInt).orElse(1),
                1, Integer.MAX_VALUE, 1,
                (v) -> String.valueOf(v.getNumber().intValue()));
        smalls.addNamed("Buffer Size", bufferSize);
    }
}