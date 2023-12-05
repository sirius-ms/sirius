package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Path;


public class DatabaseImportConfigPanel extends SubToolConfigPanel<CustomDBOptions> {

    public final JTextField dbNameField;

    public final FileChooserPanel dbLocationField;

    JSpinner bufferSize;

    public DatabaseImportConfigPanel(@Nullable CustomDatabase db) {
        super(CustomDBOptions.class);

        final TwoColumnPanel smalls = new TwoColumnPanel();
        add(new TextHeaderBoxPanel("Parameters", smalls));

        dbNameField = new JTextField("");
        smalls.addNamed("DB Name", dbNameField);

        String dbDirectory = db != null ? Path.of(db.storageLocation()).getParent().toString()
                : PropertyManager.getProperty(SiriusProperties.DEFAULT_SAVE_DIR_PATH, null, "");

        dbLocationField = new FileChooserPanel(dbDirectory, JFileChooser.DIRECTORIES_ONLY);

        if (db != null) {
            dbNameField.setText(db.name());
            dbNameField.setEnabled(false);
            dbLocationField.setEnabled(false);
        }


        getOptionDescriptionByName("import").ifPresent(it -> dbLocationField.setToolTipText(GuiUtils.formatToolTip(it)));
        smalls.addNamed("DB location", dbLocationField);
        parameterBindings.put("import", this::getDbFilePath);

        final String buf = "buffer";
        bufferSize = makeGenericOptionSpinner(buf,
                getOptionDefaultByName(buf).map(Integer::parseInt).orElse(1),
                1, Integer.MAX_VALUE, 1,
                (v) -> String.valueOf(v.getNumber().intValue()));
        smalls.addNamed("Buffer Size", bufferSize);
    }

    public String getDbFilePath() {
        return Path.of(dbLocationField.getFilePath(), dbNameField.getText()).toString();
    }
}