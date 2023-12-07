package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.ms.frontend.io.FileChooserPanel;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.DragAndDrop;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.JListDropImage;
import de.unijena.bioinf.ms.gui.utils.JTextAreaDropImage;
import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

class ImportDatabaseDialog extends JDialog {
    private final DatabaseDialog databaseDialog;
    protected JButton importButton;
    protected DatabaseImportConfigPanel configPanel;

    public ImportDatabaseDialog(DatabaseDialog databaseDialog) {
        this(databaseDialog, null);
    }

    public ImportDatabaseDialog(DatabaseDialog databaseDialog, @Nullable CustomDatabase db) {
        super(databaseDialog, db != null ? "Import into " + db.name() : "Create custom database", true);
        this.databaseDialog = databaseDialog;

        setPreferredSize(new Dimension(640, 480));
        setLayout(new BorderLayout());

        configPanel = new DatabaseImportConfigPanel(db, databaseDialog.customDatabases.stream().map(SearchableDatabase::name).collect(Collectors.toSet()));
        add(configPanel, BorderLayout.NORTH);

        final Box box = Box.createVerticalBox();
        final JLabel label = new JLabel(
                "<html>Add or drop files with structures or reference spectra to import.");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(label);

        DefaultListModel<File> fileListModel = new DefaultListModel<>();
        JList<File> fileList = new JListDropImage<>(fileListModel);

        final JScrollPane pane = new JScrollPane(fileList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(pane);

        ToolbarButton addFiles = Buttons.getAddButton16("Add files");
        ToolbarButton removeFiles = Buttons.getRemoveButton16("Remove files");
        removeFiles.setEnabled(false);

        Box fileListButtons = Box.createHorizontalBox();
        fileListButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        fileListButtons.add(Box.createHorizontalGlue());
        fileListButtons.add(addFiles);
        fileListButtons.add(removeFiles);

        box.add(fileListButtons);

        box.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Import compounds"));
        add(box, BorderLayout.CENTER);

        importButton = new JButton("Create/Open database and import compounds");
        importButton.setEnabled(false);
        add(importButton, BorderLayout.SOUTH);

        JFileChooser importFileChooser = new JFileChooser();
        importFileChooser.setMultiSelectionEnabled(true);

        addFiles.addActionListener(e -> {
            if (importFileChooser.showOpenDialog(ImportDatabaseDialog.this) == JFileChooser.APPROVE_OPTION) {
                File[] files = importFileChooser.getSelectedFiles();

                for (File f : files) {
                    fileListModel.addElement(f);
                }
            }
        });

        fileList.addListSelectionListener(e -> removeFiles.setEnabled(!fileList.isSelectionEmpty()));

        Action removeSelectedFiles = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedIndices = fileList.getSelectedIndices();
                for (int i = selectedIndices.length-1; i >=0; i--) {
                    fileListModel.removeElementAt(selectedIndices[i]);
                }
            }
        };

        removeFiles.addActionListener(removeSelectedFiles);

        String removeFilesActionName = "removeSelectedFiles";
        fileList.getInputMap().put(KeyStroke.getKeyStroke("DELETE"),removeFilesActionName);
        fileList.getActionMap().put(removeFilesActionName, removeSelectedFiles);

//        configPanel.dbLocationField.field.getDocument().addDocumentListener(new DocumentListener() {
//            @Override
//            public void insertUpdate(DocumentEvent e) {
//                onTextChanged();
//            }
//
//            @Override
//            public void removeUpdate(DocumentEvent e) {
//                onTextChanged();
//            }
//
//            @Override
//            public void changedUpdate(DocumentEvent e) {
//                onTextChanged();
//            }
//
//            public void onTextChanged() {
//                if (configPanel.dbLocationField.getFilePath() == null) return;
//                importButton.setEnabled(!configPanel.dbLocationField.getFilePath().isEmpty() && configPanel.dbLocationField.getFilePath().replaceAll("\\s", "").equals(configPanel.dbLocationField.getFilePath()) && databaseDialog.customDatabases.stream().noneMatch(k -> k.name().equalsIgnoreCase(configPanel.dbLocationField.getFilePath())));
//            }
//        });


//        importButton.addActionListener(e -> {
//            dispose();
//            String t = textArea.getText();
//            runImportJob(null,
//                    t != null && !t.isBlank()
//                            ? Arrays.asList(t.split("\n"))
//                            : null
//            );
//        });

//        final DropTarget dropTarget = new DropTarget() {
//            @Override
//            public synchronized void drop(DropTargetDropEvent evt) {
//                dispose();
//                String t = textArea.getText();
//                runImportJob(
//                        DragAndDrop.getFileListFromDrop(evt).stream().map(File::toPath).collect(Collectors.toList()),
//                        t != null && !t.isBlank()
//                                ? Arrays.asList(t.split("\n"))
//                                : null
//                );
//            }
//        };

        GuiUtils.closeOnEscape(this);
//        setDropTarget(dropTarget);
//        textArea.setDropTarget(dropTarget);
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);

    }

    protected void runImportJob(java.util.List<Path> sources) {
        try {
            List<String> command = new ArrayList<>();
            command.add(configPanel.toolCommand());
            command.addAll(configPanel.asParameterList());

            InputFilesOptions input = new InputFilesOptions();
            input.msInput = new InputFilesOptions.MsInput();
            input.msInput.setInputPath(sources);

            Jobs.runCommandAndLoad(command, null,
                            input, this,
                            "Importing into '" + configPanel.dbLocationField.getFilePath() + "'...",
                            false)
                    .awaitResult();

            databaseDialog.whenCustomDbIsAdded(configPanel.getDbFilePath());
        } catch (ExecutionException ex) {
            LoggerFactory.getLogger(getClass()).error("Error during Custom DB import.", ex);

            if (ex.getCause() != null)
                new StacktraceDialog(this, ex.getCause().getMessage(), ex.getCause());
            else
                new StacktraceDialog(this, "Unexpected error when importing custom DB!", ex);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Fatal Error during Custom DB import.", e);
            new StacktraceDialog(MF, "Fatal Error during Custom DB import.", e);
        }
    }
}
