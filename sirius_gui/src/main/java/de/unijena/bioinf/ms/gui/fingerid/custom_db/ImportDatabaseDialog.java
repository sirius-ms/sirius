/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.DragAndDrop;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.JListDropImage;
import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.io.File;
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

    private final DefaultListModel<File> fileListModel;

    public ImportDatabaseDialog(DatabaseDialog databaseDialog) {
        this(databaseDialog, null);
    }

    public ImportDatabaseDialog(DatabaseDialog databaseDialog, @Nullable CustomDatabase db) {
        super(databaseDialog, db != null ? "Import into " + db.name() : "Create custom database", true);
        this.databaseDialog = databaseDialog;

        setPreferredSize(new Dimension(640, 480));
        setLayout(new BorderLayout());

        configPanel = new DatabaseImportConfigPanel(db, databaseDialog.customDatabases.stream().map(SearchableDatabase::name).collect(Collectors.toSet()));
        configPanel.onValidityChange(this::refreshImportButton);
        add(configPanel, BorderLayout.NORTH);

        final Box box = Box.createVerticalBox();
        final JLabel label = new JLabel(
                "<html>Add or drop files with structures or reference spectra to import.");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(label);

        fileListModel = new DefaultListModel<>();
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
        fileListButtons.add(removeFiles);
        fileListButtons.add(addFiles);

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

        importButton.addActionListener(e -> {
            dispose();
            List<Path> sources = Arrays.stream(fileListModel.toArray()).map(f -> ((File) f).toPath()).distinct().toList();
            runImportJob(sources);
        });

        GuiUtils.closeOnEscape(this);
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    protected void runImportJob(List<Path> sources) {
        try {
            List<String> command = new ArrayList<>();
            command.add(configPanel.toolCommand());
            command.addAll(configPanel.asParameterList());

            InputFilesOptions input = new InputFilesOptions();
            input.msInput = new InputFilesOptions.MsInput();
            input.msInput.setInputPath(sources);

            Jobs.runCommandAndLoad(command, null,
                            input, this,
                            "Importing into '" + configPanel.getDbFilePath() + "'...",
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

    private void refreshImportButton() {
        importButton.setEnabled(!fileListModel.isEmpty() && configPanel.validDbLocation());
    }
}
