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

import de.unijena.bioinf.chemdb.DataSources;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.DialogHeader;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

public class DatabaseDialog extends JDialog {

    protected JList<CustomDatabase> dbList;
    protected List<CustomDatabase> customDatabases;

    protected DatabaseView dbView;

    public DatabaseDialog(final Frame owner) {
        super(owner, true);
        setTitle("Custom Databases");
        setLayout(new BorderLayout());

        JPanel header = new DialogHeader(Icons.DB_64);
        add(header, BorderLayout.NORTH);

        dbList = new JList<>();
        dbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dbView = new DatabaseView();

        JButton addCustomDb = Buttons.getAddButton16("Create custom Database");
        JButton deleteDB = Buttons.getRemoveButton16("Delete Custom Database");
        JButton editDB = Buttons.getEditButton16("Edit Custom Database");

        loadDatabaseList();

        dbList.addListSelectionListener(e -> {
            CustomDatabase db = dbList.getSelectedValue();
            dbView.updateContent(db);
            if (db != null) {
                editDB.setEnabled(!db.needsUpgrade());
                deleteDB.setEnabled(true);
            } else {
                editDB.setEnabled(false);
                deleteDB.setEnabled(false);
            }
        });

        JScrollPane scroll = new JScrollPane(dbList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        TextHeaderBoxPanel pane = new TextHeaderBoxPanel("Custom Databases", scroll);
        pane.setBorder(BorderFactory.createEmptyBorder(GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP, 0, 0));

        final Box but = Box.createHorizontalBox();
        but.add(Box.createHorizontalGlue());
        but.add(deleteDB);
        but.add(editDB);
        but.add(addCustomDb);
        editDB.setEnabled(false);
        deleteDB.setEnabled(false);

        add(but, BorderLayout.SOUTH);
        add(pane, BorderLayout.CENTER);
        add(dbView, BorderLayout.EAST);

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
                CustomDatabase db = dbList.getSelectedValue();
                if (db == null) {
                    return;
                }
                final String name = db.name();
                final String msg = "Do you really want to remove the custom database '" + name + "'?\n(will not be deleted from disk) ";

                if (JOptionPane.showConfirmDialog(getOwner(), msg, "", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

                    try {
                        Jobs.runCommandAndLoad(Arrays.asList(
                                                CustomDBOptions.class.getAnnotation(CommandLine.Command.class).name(),
                                                "--remove", name), null, null, owner,
                                        "Deleting database '" + name + "'...", true)
                                .awaitResult();
                    } catch (ExecutionException ex) {
                        LoggerFactory.getLogger(getClass()).error("Error during Custom DB removal.", ex);

                        if (ex.getCause() != null)
                            new StacktraceDialog(DatabaseDialog.this, ex.getCause().getMessage(), ex.getCause());
                        else
                            new StacktraceDialog(DatabaseDialog.this, "Unexpected error when removing custom DB!", ex);
                    } catch (Exception ex2) {
                        LoggerFactory.getLogger(getClass()).error("Fatal Error during Custom DB removal.", ex2);
                        new StacktraceDialog(MF, "Fatal Error during Custom DB removal.", ex2);
                    }

                    loadDatabaseList();
                }
            }
        };

        addCustomDb.addActionListener(e -> new ImportDatabaseDialog(this));
        editDB.addActionListener(editSelectedDb);
        deleteDB.addActionListener(deleteSelectedDb);

        String editDbActionName = "editCurrentDb";
        dbList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), editDbActionName);
        dbList.getInputMap().put(KeyStroke.getKeyStroke("SPACE"),editDbActionName);
        dbList.getActionMap().put(editDbActionName, editSelectedDb);

        dbList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int i = dbList.getSelectedIndex();
                            if (i >= 0 && dbList.getCellBounds(i, i).contains(e.getPoint())) {
                        editSelectedDb.actionPerformed(null);
                    }
                }
            }
        });

        String deleteDbActionName = "deleteCurrentDb";
        dbList.getInputMap().put(KeyStroke.getKeyStroke("DELETE"),deleteDbActionName);
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
        customDatabases = Jobs.runInBackgroundAndLoad(getOwner(), "Loading DBs...", (Callable<List<CustomDatabase>>) SearchableDatabases::getCustomDatabases).getResult();
        customDatabases.sort(Comparator.comparing(CustomDatabase::name));
        dbList.setListData(customDatabases.toArray(new CustomDatabase[0]));
    }

    protected void whenCustomDbIsAdded(final String dbName) {
        loadDatabaseList();
        CustomDatabase newDb = customDatabases.stream().filter(db -> db.storageLocation().equals(dbName)).findFirst().orElseThrow();
        dbList.setSelectedValue(newDb, true);
        dbList.requestFocusInWindow();
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

        public void updateContent(CustomDatabase c) {
            if (c == null) {
                content.setText("No Database selected.");
                content.setToolTipText(null);
            } else if (c.getStatistics().getCompounds() > 0) {
                content.setText("<html><b>" + c.name() + "</b>"
                        + "<br><b>"
                        + c.getStatistics().getCompounds() + "</b> compounds with <b>" + c.getStatistics().getFormulas()
                        + "</b> different molecular formulas"
                        + (c.getStatistics().getSpectra() > 0 ? " and <b>" + c.getStatistics().getSpectra() + "</b> reference spectra." : ".")
                        + "<br>"
                        + ((c.getSettings().isInheritance() ? "<br>This database will also include all compounds from '" + DataSources.getDataSourcesFromBitFlags(c.getFilterFlag()).stream().filter(n -> !SearchableDatabases.NON_SLECTABLE_LIST.contains(n)).collect(Collectors.joining("', '")) + "'." : "")
                                + (c.needsUpgrade() ? "<br><b>This database schema is outdated. You have to upgrade the database before you can use it.</b>" : "")
                                + "</html>"));

                content.setToolTipText(c.storageLocation());
            } else {
                content.setText("Empty custom database.");
                content.setToolTipText(null);
            }
        }
    }
}
