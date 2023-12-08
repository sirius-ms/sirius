package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseFactory;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.utils.ErrorReportingInputVerifier;
import de.unijena.bioinf.ms.gui.utils.PlaceholderTextField;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.nio.file.Path;
import java.util.Set;


public class DatabaseImportConfigPanel extends SubToolConfigPanel<CustomDBOptions> {

    public final PlaceholderTextField dbNameField;

    public final FileChooserPanel dbLocationField;

    private boolean validDbName;
    private boolean validDbDirectory;

    private Runnable validityChangeListener;

    public DatabaseImportConfigPanel(@Nullable CustomDatabase db, Set<String> existingNames) {
        super(CustomDBOptions.class);

        final TwoColumnPanel smalls = new TwoColumnPanel();
        add(new TextHeaderBoxPanel("Parameters", smalls));

        dbNameField = new PlaceholderTextField("");
        smalls.addNamed("DB name", dbNameField);

        String dbDirectory = db != null ? Path.of(db.storageLocation()).getParent().toString()
                : PropertyManager.getProperty(SiriusProperties.DEFAULT_SAVE_DIR_PATH, null, "");

        dbLocationField = new FileChooserPanel(dbDirectory, JFileChooser.DIRECTORIES_ONLY);
        smalls.addNamed("DB location", dbLocationField);
        parameterBindings.put("import", this::getDbFilePath);

        if (db != null) {
            dbNameField.setText(db.name());
            dbNameField.setEnabled(false);
            dbLocationField.setEnabled(false);
            validDbName = true;
            validDbDirectory = true;
        } else {
            dbNameField.setPlaceholder("my_database" +  CustomDatabaseFactory.NOSQL_SUFFIX);
        }

        dbNameField.setToolTipText("Filename for the new custom database, should end in " + CustomDatabaseFactory.NOSQL_SUFFIX);

        dbNameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String name = dbNameField.getText();
                if (!name.isEmpty() && !name.endsWith(CustomDatabaseFactory.NOSQL_SUFFIX)) {
                    dbNameField.setText(name + CustomDatabaseFactory.NOSQL_SUFFIX);
                }
            }
        });

        dbNameField.setInputVerifier(new ErrorReportingInputVerifier() {
            @Override
            public String getErrorMessage(JComponent input) {
                String name = ((JTextField)input).getText();
                String error = null;
                if (name == null || name.isBlank()) {
                    error = "DB name missing";
                } else if (existingNames.contains(name)
                        || (!name.endsWith(CustomDatabaseFactory.NOSQL_SUFFIX) && existingNames.contains(name + CustomDatabaseFactory.NOSQL_SUFFIX))) {
                    error = "This name is already in use";
                }
                if (validDbName != (error == null)) {
                    validDbName = error == null;
                    fireValidityChange();
                }
                return error;
            }
        });

        dbLocationField.field.setInputVerifier(new ErrorReportingInputVerifier() {
            @Override
            public String getErrorMessage(JComponent input) {
                String text = ((JTextField)input).getText();
                String error = null;
                if (text == null || text.isBlank()) {
                    error = "DB location not set";
                }
                if (validDbDirectory != (error == null)) {
                    validDbDirectory = error == null;
                    fireValidityChange();
                }
                return error;
            }
        });

        final String buf = "buffer";
        JSpinner bufferSize = makeGenericOptionSpinner(buf,
                getOptionDefaultByName(buf).map(Integer::parseInt).orElse(1),
                1, Integer.MAX_VALUE, 1,
                (v) -> String.valueOf(v.getNumber().intValue()));
        smalls.addNamed("Buffer Size", bufferSize);
    }

    public String getDbFilePath() {
        return Path.of(dbLocationField.getFilePath(), dbNameField.getText()).toString();
    }

    public boolean validDbLocation() {
        return validDbName && validDbDirectory;
    }

    public void onValidityChange(Runnable listener) {
        validityChangeListener = listener;
    }

    private void fireValidityChange() {
        if (validityChangeListener != null) {
            validityChangeListener.run();
        }
    }
}