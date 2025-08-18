/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.chemdb.custom.CustomDatabases;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.DialogHeader;
import de.unijena.bioinf.ms.gui.dialogs.ErrorWithDetailsDialog;
import de.unijena.bioinf.ms.gui.dialogs.ExecutionDialog;
import de.unijena.bioinf.ms.gui.table.SiriusListCellRenderer;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import io.sirius.ms.sdk.model.SearchableDatabase;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;


public class DatabaseDialog extends JDialog {
    @Getter
    protected final SiriusGui gui;

    protected JList<SearchableDatabase> dbList;
    protected List<SearchableDatabase> customDatabases;

    protected DatabaseView dbView;

    public DatabaseDialog(SiriusGui gui) {
        this(gui, gui.getMainFrame());
    }

    public DatabaseDialog(SiriusGui gui, @Nullable Frame owner) {
        super(owner, true);
        this.gui = gui;
        setTitle("Custom Databases");
        setLayout(new BorderLayout());

        JPanel header = new DialogHeader(Icons.DB.derive(64,64));
        add(header, BorderLayout.NORTH);

        dbList = new JList<>();
        dbList.setCellRenderer(new ErrorDatabaseCellRenderer());
        dbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dbView = new DatabaseView();

        JButton addCustomDb = Buttons.getAddButton16("Create custom Database");
        JButton deleteDB = Buttons.getRemoveButton16("Delete Custom Database");
        JButton editDB = Buttons.getEditButton16("Edit Custom Database");
        JButton openDB = Buttons.getFileChooserButton16("Add existing Database");
        JButton exportDB = Buttons.getExportButton16("Export Database");

        loadDatabaseList();

        Action editSelectedDb = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (dbList.getSelectedIndex() != -1) {
                    new ImportDatabaseDialog(DatabaseDialog.this, dbList.getSelectedValue());
                }
            }
        };

        Action deleteSelectedDb = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchableDatabase db = dbList.getSelectedValue();
                if (db == null) {
                    return;
                }
                final String name = db.getDatabaseId();

                Box deleteDialogBox = Box.createVerticalBox();
                deleteDialogBox.add(new JLabel("Do you really want to remove '" + name + "'?"));
                JCheckBox deleteFromDisk = new JCheckBox("Delete from disk");
                deleteDialogBox.add(Box.createRigidArea(new Dimension(0, 10)));
                deleteDialogBox.add(deleteFromDisk);

                if (JOptionPane.showConfirmDialog(getOwner(), deleteDialogBox, "", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    try {
                        Jobs.runInBackgroundAndLoad(gui.getMainFrame(),
                                "Deleting database '" + name + "'...", () ->
                                        gui.acceptSiriusClient((c, pid) -> c.databases().removeDatabase(name, deleteFromDisk.isSelected()))
                        ).awaitResult();
                    } catch (ExecutionException ex) {
                        LoggerFactory.getLogger(getClass()).error("Error during Custom DB removal.", ex);
                        Jobs.runEDTLater(() -> new ErrorWithDetailsDialog(DatabaseDialog.this, gui.getSiriusClient().unwrapErrorMessage(ex), ex));
                    } catch (Exception ex2) {
                        LoggerFactory.getLogger(getClass()).error("Fatal Error during Custom DB removal.", ex2);
                        new ErrorWithDetailsDialog(getOwner(), "Fatal Error during Custom DB removal.", ex2);
                    }

                    loadDatabaseList();
                }
            }
        };

        Action exportSelectedDb = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (dbList.getSelectedIndex() != -1) {
                    SearchableDatabase db = dbList.getSelectedValue();

                    ExecutionDialog<DatabaseExportConfigPanel> d = new ExecutionDialog<>(gui, new DatabaseExportConfigPanel(db), null, DatabaseDialog.this, "Export " + db.getDisplayName(), true, false);
                    d.setIndeterminateProgress(false);
                    d.start();
                }
            }
        };

        dbList.addListSelectionListener(e -> {
            SearchableDatabase db = dbList.getSelectedValue();
            dbView.updateContent(db);
            if (db != null) {
                editSelectedDb.setEnabled(!db.isUpdateNeeded() && (db.getErrorMessage() == null || db.getErrorMessage().isBlank()));
                deleteSelectedDb.setEnabled(true);
            } else {
                editSelectedDb.setEnabled(false);
                deleteSelectedDb.setEnabled(false);
            }
            editDB.setEnabled(editSelectedDb.isEnabled());
            deleteDB.setEnabled(deleteSelectedDb.isEnabled());
            exportDB.setEnabled(deleteSelectedDb.isEnabled());
        });

        JScrollPane scroll = new JScrollPane(dbList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        TextHeaderBoxPanel pane = new TextHeaderBoxPanel("Custom Databases", scroll);
        pane.setBorder(BorderFactory.createEmptyBorder(GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP, 0, 0));

        final Box but = Box.createHorizontalBox();
        but.add(Box.createHorizontalGlue());
        but.add(deleteDB);
        but.add(editDB);
        but.add(exportDB);
        but.add(openDB);
        but.add(addCustomDb);
        editDB.setEnabled(false);
        deleteDB.setEnabled(false);
        exportDB.setEnabled(false);

        add(but, BorderLayout.SOUTH);
        add(pane, BorderLayout.CENTER);
        add(dbView, BorderLayout.EAST);

        addCustomDb.addActionListener(e -> new ImportDatabaseDialog(this));
        editDB.addActionListener(editSelectedDb);
        deleteDB.addActionListener(deleteSelectedDb);
        exportDB.addActionListener(exportSelectedDb);

        JFileChooser openDbFileChooser = new JFileChooser();
        openDbFileChooser.setFileFilter(new FileNameExtensionFilter("SIRIUS custom database files", CustomDatabases.CUSTOM_DB_SUFFIX.replace(".", "")));
        openDbFileChooser.setMultiSelectionEnabled(true);
        openDB.addActionListener(e -> {
            if (openDbFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                List<File> files = Arrays.stream(openDbFileChooser.getSelectedFiles()).toList();
                // error handling and duplicate checking is performed on the server side
                try {
                    Jobs.runInBackgroundAndLoad(gui.getMainFrame(),
                            "Adding '" + files.size() + "' database(s) ...", () -> {
                                List<SearchableDatabase> newDbs = gui.applySiriusClient((c, pid) ->
                                        c.databases().addDatabases(files.stream().map(File::getAbsolutePath).toList()));
                                if (newDbs == null || newDbs.isEmpty())
                                    throw new RuntimeException("Not Database returned from Job. Open Databases probably failed.");
                                whenCustomDbIsAdded(newDbs.getFirst().getDatabaseId());
                            }).awaitResult();
                } catch (ExecutionException ex) {
                    getGui().getSiriusClient().unwrapErrorResponse(ex).ifPresentOrElse(
                            err -> JOptionPane.showMessageDialog(this, err.getMessage(), "Error " + err.getStatus() + ": " + err.getError(), JOptionPane.ERROR_MESSAGE),
                            () -> JOptionPane.showMessageDialog(this, ex.getCause().getMessage(), "Unexpected Error", JOptionPane.ERROR_MESSAGE)
                    );
                    loadDatabaseList();
                }
            }
        });

        String editDbActionName = "editCurrentDb";
        dbList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), editDbActionName);
        dbList.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), editDbActionName);
        dbList.getActionMap().put(editDbActionName, editSelectedDb);

        dbList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int i = dbList.getSelectedIndex();
                    if (i >= 0 && dbList.getCellBounds(i, i).contains(e.getPoint()) && editSelectedDb.isEnabled()) {
                        editSelectedDb.actionPerformed(null);
                    }
                }
            }
        });

        String deleteDbActionName = "deleteCurrentDb";
        dbList.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), deleteDbActionName);
        dbList.getActionMap().put(deleteDbActionName, deleteSelectedDb);

        dbList.setSelectedIndex(0);

        GuiUtils.closeOnEscape(this);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(375, getMinimumSize().height));
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private void loadDatabaseList() {
        customDatabases = Jobs.runInBackgroundAndLoad(getOwner(), "Loading DBs...",
                () -> gui.applySiriusClient((c, pid) -> c.databases().getCustomDatabases(true, true))
        ).getResult();

        customDatabases.sort(Comparator.comparing(SearchableDatabase::getDatabaseId));
        dbList.setListData(customDatabases.toArray(SearchableDatabase[]::new));
    }

    protected Optional<SearchableDatabase> whenCustomDbIsAdded(final String dbIdToSelect) {
        loadDatabaseList();
        // try to scroll to the newly added Database.
        Optional<SearchableDatabase> dbOpt =dbIdToSelect == null ? Optional.empty() : customDatabases.stream()
                .filter(db -> dbIdToSelect.equals(db.getLocation())).findFirst();

        dbOpt.ifPresent(db -> {
            dbList.setSelectedValue(db, true);
            dbList.requestFocusInWindow();
        });
        return dbOpt;
    }

    protected static class DatabaseView extends JPanel {
        JLabel content;

        protected DatabaseView() {
            this.content = new JLabel();
            content.setHorizontalAlignment(JLabel.CENTER);
            content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setLayout(new BorderLayout());
            add(content, BorderLayout.CENTER);
            setPreferredSize(new Dimension(200, 240));
            updateContent(null);
        }

        public void updateContent(SearchableDatabase c) {
            if (c == null) {
                content.setText("No Database selected.");
                content.setToolTipText(null);
            } else if (c.getNumberOfStructures() != null && c.getNumberOfStructures() > 0) {
                content.setText("<html><b>" + c.getDisplayName() + "</b>"
                        + "<br><b>"
                        + c.getNumberOfStructures() + "</b> compounds with <b>" + c.getNumberOfFormulas()
                        + "</b> different molecular formulas"
                        + (Optional.ofNullable(c.getNumberOfReferenceSpectra()).orElse(0L) > 0 ? " and <b>" + c.getNumberOfReferenceSpectra() + "</b> reference spectra." : ".")
                        + "<br>"
                        + ((c.isUpdateNeeded() ? "<br><b>This database schema is outdated. You have to upgrade the database before you can use it.</b>" : "")
                        + "</html>"));

                content.setToolTipText(c.getLocation());
            } else if (c.getErrorMessage() != null && !c.getErrorMessage().isBlank()) {
                content.setText("<html><p>" + c.getErrorMessage() + "</p></html>");
                content.setToolTipText(c.getLocation());
            } else {
                content.setText("<html><b>" + c.getDisplayName() + "</b><br>Empty custom database.");
                content.setToolTipText(null);
            }
        }
    }

    protected static class ErrorDatabaseCellRenderer extends SiriusListCellRenderer {

        public ErrorDatabaseCellRenderer() {
            super(v -> ((SearchableDatabase) v).getDatabaseId());
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            SearchableDatabase db = (SearchableDatabase) value;
            if (db.getErrorMessage() != null && !db.getErrorMessage().isBlank()) {
                setForeground(Colors.TEXT_ERROR);
            }
            return this;
        }
    }
}
