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
import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import io.sirius.ms.sdk.model.AllowedFeatures;
import io.sirius.ms.sdk.model.SearchableDatabase;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
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


public class DatabaseDialog extends JFrame {
    /**
     * Icon size of the functional buttons. All icons are scalable SVGs.
     */
    private static final int BUTTON_ICON_SIZE = 24;

    /**
     * Fixed size of this window. The content does not grow with the number of databases, so there is
     * nothing to compute here.
     */
    private static final Dimension WINDOW_SIZE = new Dimension(450, 450);

    /**
     * Custom databases are a global resource of the SIRIUS service and not bound to a project. So
     * like the jobs dialog, this window exists only once for all gui instances. It is a frame and not
     * a dialog, so it is an independent window that does not stay in front of one specific main frame.
     */
    private static DatabaseDialog INSTANCE = null;

    /**
     * Shows the single database window with the given context. If it is already open, it is brought to
     * the front and moved to the given window instead of opening a second copy.
     *
     * @param relativeTo window to center this window on, usually the main frame it was opened from
     */
    public synchronized static void showInstance(@NotNull CustomDbContext context, @Nullable Window relativeTo) {
        if (INSTANCE == null)
            INSTANCE = new DatabaseDialog();
        INSTANCE.showWithContext(context, relativeTo);
    }

    /**
     * Disposes the single instance. Needs to be called when the shared gui infrastructure (client,
     * browser panel provider) is shut down.
     */
    public synchronized static void disposeInstance() {
        if (INSTANCE != null) {
            INSTANCE.dispose();
            INSTANCE = null;
        }
    }

    /**
     * Reloads the database list of the single instance if it is currently open. Used by windows that
     * may have modified the databases, e.g. the transformation product tool.
     */
    public synchronized static void refreshInstance() {
        if (INSTANCE != null && INSTANCE.isVisible())
            INSTANCE.refreshDatabaseList();
    }

    /**
     * Disposes the single instance if it currently runs its command jobs in the given project. Needs
     * to be called when a project is closed, so that the window cannot keep a dead project around.
     * Can be removed as soon as custom database commands are project independent.
     */
    public synchronized static void disposeInstance(@NotNull String commandProjectId) {
        if (INSTANCE != null && INSTANCE.context != null && commandProjectId.equals(INSTANCE.context.commandProjectId()))
            disposeInstance();
    }

    /**
     * Services this window operates on. Set when the window is shown, see
     * {@link #showInstance(CustomDbContext, Window)}.
     */
    @Getter
    protected CustomDbContext context;

    /** Whether the active subscription includes the transformation-products feature. Resolved on show;
     *  gates the transformation-product tool button (the server enforces this too). */
    private boolean transformationProductsAllowed = false;

    protected JList<SearchableDatabase> dbList;
    protected List<SearchableDatabase> customDatabases;

    protected DatabaseView dbView;

    private DatabaseDialog() {
        setTitle("Custom Databases");
        setLayout(new BorderLayout());

        JPanel header = new DialogHeader(Icons.DB.derive(64, 64));
        add(header, BorderLayout.NORTH);

        dbList = new JList<>();
        dbList.setCellRenderer(new ErrorDatabaseCellRenderer());
        dbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dbView = new DatabaseView();

        JButton addCustomDb = Buttons.getAddButton(BUTTON_ICON_SIZE, "Create custom database", true);
        JButton deleteDB = Buttons.getRemoveButton(BUTTON_ICON_SIZE, "Delete custom database", true);
        JButton editDB = Buttons.getEditButton(BUTTON_ICON_SIZE, "Edit custom database", true);
        JButton openDB = Buttons.getPlainFolderButton(BUTTON_ICON_SIZE, "Add existing database", true);
        JButton exportDB = Buttons.getExportButton(BUTTON_ICON_SIZE, "Export database", true);
        JButton transformationDB = new ToolbarButton(Icons.TRANSFORMATION_DB.derive(BUTTON_ICON_SIZE, BUTTON_ICON_SIZE), "Create Transformation product database", true);
        JButton showContentsDB = new ToolbarButton(Icons.DB_LENS.derive(BUTTON_ICON_SIZE, BUTTON_ICON_SIZE), "Show database contents", true);
        JButton downloadableDBs = Buttons.getDownloadButton(BUTTON_ICON_SIZE, "Download curated custom databases for local use", true);

        downloadableDBs.addActionListener(e -> new DownloadableDBsDialog(this));

        Action showSelectedDb = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchableDatabase db = dbList.getSelectedValue();
                if (db != null)
                    CustomDbWebViews.showDatabaseContent(db, context, DatabaseDialog.this);
            }
        };

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

                if (JOptionPane.showConfirmDialog(DatabaseDialog.this, deleteDialogBox, "", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    try {
                        Jobs.runInBackgroundAndLoad(DatabaseDialog.this,
                                "Deleting database '" + name + "'...", () ->
                                        context.client().databases().removeDatabase(name, deleteFromDisk.isSelected())
                        ).awaitResult();
                    } catch (ExecutionException ex) {
                        LoggerFactory.getLogger(getClass()).error("Error during Custom DB removal.", ex);
                        Jobs.runEDTLater(() -> new ErrorWithDetailsDialog(DatabaseDialog.this, context.client().unwrapErrorMessage(ex), ex));
                    } catch (Exception ex2) {
                        LoggerFactory.getLogger(getClass()).error("Fatal Error during Custom DB removal.", ex2);
                        new ErrorWithDetailsDialog(DatabaseDialog.this, "Fatal Error during Custom DB removal.", ex2);
                    }

                    //no window may be left showing a database that does not exist anymore
                    CustomDbWebViews.disposeInstances(name);
                    loadDatabaseList();
                }
            }
        };

        Action exportSelectedDb = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (dbList.getSelectedIndex() != -1) {
                    SearchableDatabase db = dbList.getSelectedValue();

                    ExecutionDialog<DatabaseExportConfigPanel> d = new ExecutionDialog<>(context.client(), context.commandProjectId(),
                            new DatabaseExportConfigPanel(db), null, DatabaseDialog.this, "Export " + db.getDisplayName(), true, false);
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
            // gate the transformation-product tool on the license feature (button stays visible but is
            // not executable without it); the server enforces the same via allowedFeature:transformationProducts
            transformationDB.setEnabled(editSelectedDb.isEnabled() && transformationProductsAllowed);
            transformationDB.setToolTipText(transformationProductsAllowed
                    ? "Create Transformation product database"
                    : GuiUtils.formatAndStripToolTip("The Transformation product tool is not included in your subscription (or you are not logged in)."));
            showContentsDB.setEnabled(db != null && !db.isUpdateNeeded());
        });

        JScrollPane scroll = new JScrollPane(dbList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        TextHeaderBoxPanel listPane = new TextHeaderBoxPanel("Custom Databases", scroll);
        listPane.setBorder(BorderFactory.createEmptyBorder(GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP));

        // buttons that add or remove databases from the list belong to the list itself
        listPane.addFooter(createButtonBar(FlowLayout.CENTER, addCustomDb, openDB, downloadableDBs, exportDB, deleteDB));

        JPanel detailsPane = new JPanel(new BorderLayout());
        detailsPane.setBorder(BorderFactory.createEmptyBorder(GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP));

        detailsPane.add(dbView, BorderLayout.CENTER);
        // buttons that modify the selected database belong to the detail view
        detailsPane.add(createButtonBar(FlowLayout.RIGHT, showContentsDB, editDB, transformationDB), BorderLayout.SOUTH);

        editDB.setEnabled(false);
        deleteDB.setEnabled(false);
        exportDB.setEnabled(false);
        transformationDB.setEnabled(false);
        showContentsDB.setEnabled(false);

        add(listPane, BorderLayout.CENTER);
        add(detailsPane, BorderLayout.EAST);

        addCustomDb.addActionListener(e -> new ImportDatabaseDialog(this));
        editDB.addActionListener(editSelectedDb);
        deleteDB.addActionListener(deleteSelectedDb);
        exportDB.addActionListener(exportSelectedDb);

        showContentsDB.addActionListener(showSelectedDb);

        transformationDB.addActionListener(e -> {
            if (!transformationProductsAllowed) // defense-in-depth: the button should already be disabled
                return;
            SearchableDatabase db = dbList.getSelectedValue();
            if (db != null) //the tool may have created a database, so refresh the list when it is closed
                CustomDbWebViews.showReactionTool(db, context, this, DatabaseDialog::refreshInstance);
        });

        JFileChooser openDbFileChooser = new JFileChooser();
        openDbFileChooser.setFileFilter(new FileNameExtensionFilter("SIRIUS custom database files", CustomDatabases.CUSTOM_DB_SUFFIX.replace(".", "")));
        openDbFileChooser.setMultiSelectionEnabled(true);
        openDB.addActionListener(e -> {
            if (openDbFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                List<File> files = Arrays.stream(openDbFileChooser.getSelectedFiles()).toList();
                // error handling and duplicate checking is performed on the server side
                try {
                    List<SearchableDatabase> newDbs = Jobs.runInBackgroundAndLoad(this,
                            "Adding '" + files.size() + "' database(s) ...", () ->
                                    context.client().databases().addDatabases(files.stream().map(File::getAbsolutePath).toList())).awaitResult();
                    if (newDbs == null || newDbs.isEmpty())
                        throw new RuntimeException("Not Database returned from Job. Open Databases probably failed.");
                    whenCustomDbIsAdded(newDbs.getFirst().getDatabaseId());
                } catch (ExecutionException ex) {
                    context.client().unwrapErrorResponse(ex).ifPresentOrElse(
                            err -> JOptionPane.showMessageDialog(this, err.getDetail(), "Error " + err.getStatus() + ": " + err.getTitle(), JOptionPane.ERROR_MESSAGE),
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
                        showSelectedDb.actionPerformed(null);
                    }
                }
            }
        });

        String deleteDbActionName = "deleteCurrentDb";
        dbList.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), deleteDbActionName);
        dbList.getActionMap().put(deleteDbActionName, deleteSelectedDb);

        GuiUtils.closeOnEscape(this);

        // the instance is reused, so closing just hides it to keep position and selection
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setIconImage(Icons.SIRIUS_APP_IMAGE); //own window in the task bar, so it needs the app icon
        Dimension windowSize = GuiUtils.shrinkToUsableScreen(WINDOW_SIZE);
        setSize(windowSize);
        setMinimumSize(windowSize);
    }

    /**
     * Sets the context to work with and shows this window centered on the given window. An already
     * visible window is moved there and brought to the front instead of opening a second copy.
     */
    private void showWithContext(@NotNull CustomDbContext context, @Nullable Window relativeTo) {
        this.context = context;
        // resolve the license feature once per open; the selection listener reads this to gate the button
        transformationProductsAllowed = context.getAllowedFeatures()
                .map(AllowedFeatures::isTransformationProducts).orElse(false);

        if (relativeTo != null)
            setLocationRelativeTo(relativeTo);

        if (!isVisible())
            setVisible(true);

        toFront();
        requestFocus();

        // databases might have been modified elsewhere since this window was shown the last time.
        // Done last, so the loading popup has a visible parent window.
        refreshDatabaseList();
    }

    /**
     * Creates a bar of toolbar buttons to be used as footer of a panel.
     *
     * @param alignment horizontal {@link FlowLayout} alignment of the buttons within the bar
     */
    private static JPanel createButtonBar(int alignment, JButton... buttons) {
        final JPanel bar = new JPanel(new FlowLayout(alignment, GuiUtils.SMALL_GAP, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(GuiUtils.SMALL_GAP, 0, 0, 0));
        for (JButton button : buttons)
            bar.add(button);
        return bar;
    }

    private void loadDatabaseList() {
        customDatabases = Jobs.runInBackgroundAndLoad(this, "Loading DBs...",
                () -> context.client().databases().getCustomDatabases(true, true)
        ).getResult();

        customDatabases.sort(Comparator.comparing(SearchableDatabase::getDatabaseId));
        dbList.setListData(customDatabases.toArray(SearchableDatabase[]::new));
    }

    /**
     * Reloads the database list from the server keeping the currently selected database selected.
     */
    private void refreshDatabaseList() {
        SearchableDatabase selected = dbList.getSelectedValue();
        if (selected != null) {
            whenCustomDbIsAdded(selected.getDatabaseId());
        } else {
            loadDatabaseList();
        }

        if (dbList.getSelectedIndex() < 0)
            dbList.setSelectedIndex(0);
    }

    protected Optional<SearchableDatabase> whenCustomDbIsAdded(final String dbIdToSelect) {
        loadDatabaseList();
        // try to scroll to the newly added Database.
        Optional<SearchableDatabase> dbOpt = dbIdToSelect == null ? Optional.empty() : customDatabases.stream()
                .filter(db -> dbIdToSelect.equals(db.getDatabaseId())).findFirst();

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
