package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseFactory;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.gui.compute.SubToolConfigPanel;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.dialogs.input.DragAndDrop;
import de.unijena.bioinf.ms.gui.utils.*;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


public class DatabaseImportConfigPanel extends SubToolConfigPanel<CustomDBOptions> {

    private PlaceholderTextField dbNameField;
    private FileChooserPanel dbLocationField;
    private DefaultListModel<File> fileListModel;
    public JButton importButton;

    private boolean validDbName;
    private boolean validDbDirectory;

    public DatabaseImportConfigPanel(@Nullable CustomDatabase db, Set<String> existingNames) {
        super(CustomDBOptions.class);
        setLayout(new BorderLayout());

        add(createParametersPanel(db, existingNames), BorderLayout.NORTH);
        add(createCompoundsBox(), BorderLayout.CENTER);
        add(createImportButton(), BorderLayout.SOUTH);
    }

    private JPanel createParametersPanel(@Nullable CustomDatabase db, Set<String> existingNames) {
        final TwoColumnPanel smalls = new TwoColumnPanel();

        dbNameField = new PlaceholderTextField("");
        smalls.addNamed("DB name", dbNameField);

        String dbDirectory = db != null ? Path.of(db.storageLocation()).getParent().toString()
                : PropertyManager.getProperty(SiriusProperties.DEFAULT_SAVE_DIR_PATH, null, "");

        dbLocationField = new FileChooserPanel(dbDirectory, JFileChooser.DIRECTORIES_ONLY);
        smalls.addNamed("DB location", dbLocationField);
        parameterBindings.put("import", this::getDbFilePath);
        validDbDirectory = !dbDirectory.isEmpty();

        if (db != null) {
            dbNameField.setText(db.name());
            dbNameField.setEnabled(false);
            dbLocationField.setEnabled(false);
            validDbName = true;
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
                    refreshImportButton();
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
                    refreshImportButton();
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

        return smalls;
    }

    private Box createCompoundsBox() {
        final Box box = Box.createVerticalBox();

        String inputParameter = "input";
        fileListModel = new DefaultListModel<>();
        JList<File> fileList = new JListDropImage<>(fileListModel);
        getOptionDescriptionByName(inputParameter).ifPresent(description -> box.setToolTipText(GuiUtils.formatToolTip(300, description)));
        parameterBindings.put(inputParameter, this::getFiles);

        final JScrollPane pane = new JScrollPane(fileList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        box.add(pane);

        ToolbarButton addFiles = Buttons.getAddButton16("Add files");
        ToolbarButton removeFiles = Buttons.getRemoveButton16("Remove files");
        removeFiles.setEnabled(false);

        Box fileListButtons = Box.createHorizontalBox();
        fileListButtons.add(Box.createHorizontalGlue());
        fileListButtons.add(removeFiles);
        fileListButtons.add(addFiles);

        box.add(fileListButtons);

        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 0, 0, 0),
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Drop or add compound files")
        );
        box.setBorder(border);


        JFileChooser importFileChooser = new JFileChooser();
        importFileChooser.setMultiSelectionEnabled(true);

        addFiles.addActionListener(e -> {
            if (importFileChooser.showOpenDialog(DatabaseImportConfigPanel.this) == JFileChooser.APPROVE_OPTION) {
                File[] files = importFileChooser.getSelectedFiles();

                for (File f : files) {
                    fileListModel.addElement(f);
                }
                refreshImportButton();
            }
        });

        DropTarget dropTarget = new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent evt) {
                fileListModel.addAll(DragAndDrop.getFileListFromDrop(evt));
                refreshImportButton();
            }
        };
        setDropTarget(dropTarget);

        fileList.addListSelectionListener(e -> removeFiles.setEnabled(!fileList.isSelectionEmpty()));

        Action removeSelectedFiles = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedIndices = fileList.getSelectedIndices();
                for (int i = selectedIndices.length-1; i >=0; i--) {
                    fileListModel.removeElementAt(selectedIndices[i]);
                }
                refreshImportButton();
            }
        };

        removeFiles.addActionListener(removeSelectedFiles);

        String removeFilesActionName = "removeSelectedFiles";
        fileList.getInputMap().put(KeyStroke.getKeyStroke("DELETE"),removeFilesActionName);
        fileList.getActionMap().put(removeFilesActionName, removeSelectedFiles);

        return box;
    }

    private JButton createImportButton() {
        importButton = new JButton("Create/Open database and import compounds");
        importButton.setEnabled(false);
        return importButton;
    }

    private void refreshImportButton() {
        importButton.setEnabled(validDbDirectory && validDbName && !fileListModel.isEmpty());
    }

    public String getDbFilePath() {
        return Path.of(dbLocationField.getFilePath(), dbNameField.getText()).toString();
    }

    /**
     * @return comma-separated list of absolute paths to files to import
     */
    public String getFiles() {
        return Arrays.stream(fileListModel.toArray()).map(f -> ((File) f).getAbsolutePath()).distinct().collect(Collectors.joining(","));
    }
}
