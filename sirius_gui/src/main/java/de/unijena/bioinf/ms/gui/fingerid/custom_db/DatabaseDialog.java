/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.chemdb.DataSources;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseImporter;
import de.unijena.bioinf.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.ms.frontend.Run;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.gui.GuiComputeRoot;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.frontend.workfow.GuiInstanceBufferFactory;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.*;
import de.unijena.bioinf.ms.gui.dialogs.input.DragAndDrop;
import de.unijena.bioinf.ms.gui.logging.TextAreaJJobContainer;
import de.unijena.bioinf.ms.gui.utils.ListAction;
import de.unijena.bioinf.ms.gui.utils.PlaceholderTextField;
import org.jetbrains.annotations.NotNull;

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
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

public class DatabaseDialog extends JDialog {

    //todo: we should separate the Dialog from the Database Managing part.
    //todo: we should use the import mechanisms from cli or library so that we do not nee
    //todo: prptected acces to th importer classes
    protected JList<String> dbList;
    protected Map<String, CustomDatabase> customDatabases;
    protected JButton addCustomDb;
    protected DatabaseView dbView;
    protected PlaceholderTextField nameField;
    private final JDialog owner = this;

    public DatabaseDialog(final Frame owner) {
        super(owner, true);
        setTitle("Custom Databases");
        setLayout(new BorderLayout());

        //============= NORTH (Header) =================
        JPanel header = new DialogHaeder(Icons.DB_64);
        add(header, BorderLayout.NORTH);


        this.customDatabases = Jobs.runInBackgroundAndLoad(owner, "Loading DBs...", (Callable<List<CustomDatabase>>) SearchableDatabases::getCustomDatabases).getResult()
                .stream().collect(Collectors.toMap(CustomDatabase::name, k -> k));
        this.dbList = new DatabaseList(customDatabases.keySet().stream().sorted().collect(Collectors.toList()));
        final Box box = Box.createVerticalBox();

        JScrollPane pane = new JScrollPane(dbList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        box.add(pane);
        this.nameField = new PlaceholderTextField(16);
        nameField.setPlaceholder("Enter name of custom database");
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onTextChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onTextChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onTextChanged();
            }

            public void onTextChanged() {
                addCustomDb.setEnabled(nameField.getText().length() > 0 && customDatabases.keySet().stream().noneMatch(k -> k.equalsIgnoreCase(nameField.getText())));
            }
        });

        this.addCustomDb = Buttons.getAddButton16("create custom DB");
        addCustomDb.setEnabled(false);
        final Box but = Box.createHorizontalBox();
        but.add(nameField);
        but.add(addCustomDb);

        add(but, BorderLayout.SOUTH);
        add(box, BorderLayout.CENTER);
        but.add(Box.createHorizontalGlue());

        this.dbView = new DatabaseView();

        add(dbView, BorderLayout.EAST);

        dbList.addListSelectionListener(e -> {
            final int i = dbList.getSelectedIndex();
            if (i >= 0) {
                final String s = dbList.getModel().getElementAt(i);
                if (s.equalsIgnoreCase("pubchem"))
                    dbView.update(s);
                else if (customDatabases.containsKey(s))
                    dbView.updateContent(customDatabases.get(s));
            }
        });

        dbList.setSelectedIndex(0);

        addCustomDb.addActionListener(e -> new ImportDatabaseDialog(nameField.getText()));

        new ListAction(dbList, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int k = dbList.getSelectedIndex();
                if (k > 0 && k < dbList.getModel().getSize())
                    new ImportDatabaseDialog(dbList.getModel().getElementAt(k));

            }
        });

        dbView.edit.addActionListener(e -> {
            final int k = dbList.getSelectedIndex();
            if (k > 0 && k < dbList.getModel().getSize())
                new ImportDatabaseDialog(dbList.getModel().getElementAt(k));
        });

        dbView.deleteCache.addActionListener(e -> {
            final int index = dbList.getSelectedIndex();
            if (index < 0 || index >= dbList.getModel().getSize())
                return;
            final String name = dbList.getModel().getElementAt(index);
            final String msg = (index > 0) ?
                    "Do you really want to delete the custom database '" + name + "'?" : "Do you really want to clear the cache of the PubChem database?";

            if (new QuestionDialog(getOwner(), msg).isSuccess()) {
                if (index > 0) {
                    new CustomDatabase(name, new File(SearchableDatabases.getCustomDatabaseDirectory(), name)).deleteDatabase();
                    customDatabases.remove(name);
                    final String[] dbs = Jobs.runInBackgroundAndLoad(owner, "Loading DBs...", (Callable<List<CustomDatabase>>) SearchableDatabases::getCustomDatabases).getResult()
                            .stream().map(CustomDatabase::name).toArray(String[]::new);
                    dbList.setListData(dbs);
                } else {
                    new WarningDialog(getOwner(), "Cannot delete integrated PubChem copy");
                }
            }

        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(350, getMinimumSize().height));
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    protected void whenCustomDbIsAdded(final String dbName) {
        SearchableDatabases.getCustomDatabaseByName(dbName).ifPresent(db -> {
            this.customDatabases.put(db.name(), db);

            dbList.setListData(this.customDatabases.keySet().stream().sorted().toArray(String[]::new));
            dbList.setSelectedValue(db.name(),true);
            nameField.setText(null);
        });
    }

    protected static class DatabaseView extends JPanel {
        JLabel content;
        JButton deleteCache, edit;

        protected DatabaseView() {
            this.content = new JLabel("placeholder");
            content.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
            this.deleteCache = new JButton("delete cache");
            setLayout(new BorderLayout());
            add(content, BorderLayout.CENTER);
            final Box hor = Box.createHorizontalBox();
            hor.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
            edit = new JButton("edit");
            hor.add(Box.createHorizontalGlue());
            hor.add(edit);
            hor.add(deleteCache);
            hor.add(Box.createHorizontalGlue());
            add(hor, BorderLayout.SOUTH);
            setPreferredSize(new Dimension(200, 240));
        }


        protected void update(String database) {
            if (database.equals("PubChem")) {
                content.setText("<html>Our in-house mirror of PubChem (last update was at 1st June, 2016).<br> Compounds are requested via webservice and cached locally on your hard drive.</html>");
                content.setMaximumSize(new Dimension(200, 240));
                deleteCache.setText("Delete cache");
                deleteCache.setEnabled(false);
                edit.setEnabled(false);
            }
        }


        public void updateContent(CustomDatabase c) {
            if (c.getNumberOfCompounds() > 0) {
                content.setText("<html>Custom database. Containing " + c.getNumberOfCompounds() + " compounds with " + c.getNumberOfFormulas() + " different molecular formulas. Consumes " + c.getMegabytes() + " mb on the hard drive." + ((c.isDeriveFromRestDb()) ? "<br>This database will also include all compounds from `"+ DataSources.getDataSourcesFromBitFlags(c.getFilterFlag()) +"`." : "") + (c.needsUpgrade() ? "<br><b>This database schema is outdated. You have to upgrade the database before you can use it.</b>" : "") + "</html>");
            } else {
                content.setText("Empty custom database.");
            }
            deleteCache.setText("Delete database");
            deleteCache.setEnabled(true);
            edit.setEnabled(!c.needsUpgrade());
        }
    }

    protected static class DatabaseList extends JList<String> {
        protected DatabaseList(List<String> databaseList) {
            super(new Vector<>(databaseList));
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

    }

    protected static class ImportList extends JList<InChI> implements ListCellRenderer<InChI> {
        private final Box cell;
        private final JLabel left, right;
        protected HashSet<String> importedCompounds;
        protected DefaultListModel<InChI> model;

        public ImportList() {
            super();
            this.model = new DefaultListModel<>();
            setCellRenderer(this);
            setModel(model);
            importedCompounds = new HashSet<>();
            cell = Box.createHorizontalBox();
            left = new JLabel("GZCGUPFRVQAUEE");
            right = new JLabel("InChI=1S/C6H12O6/c7-1-3(9)5(11)6(12)4(10)2-8/h1,3-6,8-12H,2H2");
            cell.add(left);
            cell.add(Box.createHorizontalStrut(32));
            cell.add(right);

            right.setFont(Fonts.FONT_BOLD);
            left.setFont(Fonts.FONT_BOLD.deriveFont(Font.BOLD));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends InChI> list, InChI value, int index, boolean isSelected, boolean cellHasFocus) {
            left.setText(value.key2D());
            right.setText(value.in2D);
            return cell;
        }
    }

    protected class ImportCompoundsDialog extends JDialog {
        protected JLabel statusText;
        protected JProgressBar progressBar;
        protected JTextArea details;
        protected CustomDatabaseImporter importer;
        protected JButton close;

        public ImportCompoundsDialog(CustomDatabaseImporter importer) {
            super(owner, "Import compounds", false);
            this.importer = importer;
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            panel.setPreferredSize(new Dimension(480, 320));
            add(panel);
            JPanel inner = new JPanel();
            inner.setLayout(new BorderLayout());
            progressBar = new JProgressBar();
            statusText = new JLabel();
            statusText.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(statusText, BorderLayout.NORTH);
            panel.add(inner, BorderLayout.CENTER);
            inner.add(progressBar, BorderLayout.NORTH);
            details = new JTextArea();
            details.setEditable(false);
            details.setMinimumSize(new Dimension(200, 200));
            JScrollPane pane = new JScrollPane(details, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            inner.add(pane, BorderLayout.CENTER);
            close = new JButton("close");
            inner.add(close, BorderLayout.SOUTH);
            close.setEnabled(false);
            close.addActionListener(e -> dispose());

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setLocationRelativeTo(getParent());
        }

        @Override
        public void dispose() {
            super.dispose();
        }
    }

    protected class ImportDatabaseDialog extends JDialog {
        protected JButton importButton;

        protected DatabaseImportConfigPanel configPanel;
        private String name;

        public ImportDatabaseDialog(String name) {
            super(owner, "Create '" + name + "' database", false);

            this.name = name;
            setPreferredSize(new Dimension(640, 480));
            setLayout(new BorderLayout());

            final JLabel explain = new JLabel("<html>You can inherit compounds from PubChem or our biological database. If you do so, all compounds in these databases are implicitly added to your custom database.");
            final Box hbox = Box.createHorizontalBox();
            hbox.add(explain);
            final Box vbox = Box.createVerticalBox();
            vbox.add(hbox);
            vbox.add(Box.createVerticalStrut(4));

            final Box box = Box.createVerticalBox();
            box.setAlignmentX(Component.LEFT_ALIGNMENT);
            final JLabel label = new JLabel("<html>Please insert the compounds of your custom database here (one compound per line). You can use SMILES and InChI to describe your compounds. It is also possible to drag and drop files with InChI, SMILES or in other molecule formats (e.g. MDL) into this text field.");
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(label);
            final JTextArea textArea = new JTextArea();
            textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            final JScrollPane pane = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            pane.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(pane);
            box.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Import compounds"));

            importButton = new JButton("Import compounds");
            importButton.setAlignmentX(Component.RIGHT_ALIGNMENT);

            configPanel = new DatabaseImportConfigPanel(name);

            add(configPanel, BorderLayout.NORTH);
            add(box, BorderLayout.CENTER);
            add(importButton, BorderLayout.SOUTH);

            importButton.addActionListener(e -> {
                dispose();
                Path p = Jobs.runInBackgroundAndLoad(this, "Processing input Data...", () -> {
                    Path f = FileUtils.newTempFile("custom-db-import", ".csv");
                    try {
                        Files.write(f, Arrays.asList(textArea.getText().split("\n")));
                        return f;
                    } catch (IOException ioException) {
                        new ErrorReportDialog(this, "Could not write input data to '" + f.toString() + "'.");
                        return null;
                    }
                }).getResult();
                if (p != null)
                    runImportJob(List.of(p));
            });

            final DropTarget dropTarget = new DropTarget() {
                @Override
                public synchronized void drop(DropTargetDropEvent evt) {
                    dispose();
                    runImportJob(DragAndDrop.getFileListFromDrop(evt).stream().map(File::toPath).collect(Collectors.toList()));
                }
            };

            setDropTarget(dropTarget);
            textArea.setDropTarget(dropTarget);
            setLocationRelativeTo(getOwner());
            pack();
            setVisible(true);

        }

        protected void runImportJob(@NotNull List<Path> source) {
            try {
                final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader();
                final WorkflowBuilder<GuiComputeRoot> wfBuilder = new WorkflowBuilder<>(new GuiComputeRoot(MF.ps(), null), configOptionLoader, new GuiInstanceBufferFactory());
                wfBuilder.rootOptions.setNonCompoundInput(source);
                final Run computation = new Run(wfBuilder);

                List<String> command = new ArrayList<>();
                command.add(configPanel.toolCommand());
                command.addAll(configPanel.asParameterList());

                computation.parseArgs(command.toArray(String[]::new));

                if (computation.isWorkflowDefined()) {
                    final TextAreaJJobContainer<Boolean> j = Jobs.runWorkflow(computation.getFlow(), List.of());
                    LoadingBackroundTask.connectToJob(this, "Importing into '" + name + "'...", false, j);
                    whenCustomDbIsAdded(name);
                }
                //todo else some error message with pico cli output
            } catch (Exception e) {
                new ExceptionDialog(MF, e.getMessage());
            }
        }
    }
}
