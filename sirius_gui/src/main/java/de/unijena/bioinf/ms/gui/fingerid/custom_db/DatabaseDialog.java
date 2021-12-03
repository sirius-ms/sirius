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

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.chemdb.DataSources;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.ms.frontend.Run;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import de.unijena.bioinf.ms.frontend.subtools.gui.GuiComputeRoot;
import de.unijena.bioinf.ms.frontend.workflow.WorkflowBuilder;
import de.unijena.bioinf.ms.frontend.workfow.GuiInstanceBufferFactory;
import de.unijena.bioinf.ms.gui.compute.DBSelectionList;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.DialogHeader;
import de.unijena.bioinf.ms.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.DragAndDrop;
import de.unijena.bioinf.ms.gui.logging.TextAreaJJobContainer;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.JTextAreaDropImage;
import de.unijena.bioinf.ms.gui.utils.ListAction;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
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
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

public class DatabaseDialog extends JDialog {

    //todo: we should separate the Dialog from the Database Managing part.
    protected JList<String> dbList;
    protected Map<String, CustomDatabase<?>> customDatabases;

    protected DatabaseView dbView;
    private final JDialog owner = this;
    JButton deleteDB, editDB, addCustomDb;


    public DatabaseDialog(final Frame owner) {
        super(owner, true);
        setTitle("Custom Databases");
        setLayout(new BorderLayout());

        //============= NORTH (Header) =================
        JPanel header = new DialogHeader(Icons.DB_64);
        add(header, BorderLayout.NORTH);


        this.customDatabases = Jobs.runInBackgroundAndLoad(owner, "Loading DBs...", (Callable<List<CustomDatabase<?>>>) SearchableDatabases::getCustomDatabases).getResult()
                .stream().collect(Collectors.toMap(CustomDatabase::name, k -> k));
        this.dbList = new DatabaseList(customDatabases.keySet().stream().sorted().collect(Collectors.toList()));
        JScrollPane scroll = new JScrollPane(dbList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        TextHeaderBoxPanel pane = new TextHeaderBoxPanel("Custom Databases", scroll);
        pane.setBorder(BorderFactory.createEmptyBorder(GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP,0,0));

        addCustomDb = Buttons.getAddButton16("Create custom DB");
        deleteDB = Buttons.getRemoveButton16("Delete Custom Database");
        editDB = Buttons.getEditButton16("Edit Custom Database");

        final Box but = Box.createHorizontalBox();
        but.add(Box.createHorizontalGlue());
        but.add(deleteDB);
        but.add(editDB);
        but.add(addCustomDb);
        editDB.setEnabled(false);
        deleteDB.setEnabled(false);

        this.dbView = new DatabaseView();

        add(but, BorderLayout.SOUTH);
        add(pane, BorderLayout.CENTER);
        add(dbView, BorderLayout.EAST);


        dbList.addListSelectionListener(e -> {
            final int i = dbList.getSelectedIndex();
            if (i >= 0) {
                final String s = dbList.getModel().getElementAt(i);
                if (customDatabases.containsKey(s)) {
                    final CustomDatabase<?> c = customDatabases.get(s);
                    dbView.updateContent(c);
                    editDB.setEnabled(!c.needsUpgrade());
                    deleteDB.setEnabled(true);
                } else {
                    editDB.setEnabled(false);
                    deleteDB.setEnabled(false);
                }

            }
        });

        dbList.setSelectedIndex(0);

        addCustomDb.addActionListener(e ->  new ImportDatabaseDialog());


        //klick on Entry ->  open import dialog
        new ListAction(dbList, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int k = dbList.getSelectedIndex();
                if (k >= 0 && k < dbList.getModel().getSize()){
                    String key = dbList.getModel().getElementAt(k);
                    CustomDatabase<?> db = customDatabases.get(key);
                    new ImportDatabaseDialog(db);
                }

            }
        });

        //edit button ->  open import dialog
        editDB.addActionListener(e -> {
            final int k = dbList.getSelectedIndex();
            if (k >= 0 && k < dbList.getModel().getSize()){
                String key = dbList.getModel().getElementAt(k);
                CustomDatabase<?> db = customDatabases.get(key);
                new ImportDatabaseDialog(db);
            }
        });

        deleteDB.addActionListener(e -> {
            final int index = dbList.getSelectedIndex();
            if (index < 0 || index >= dbList.getModel().getSize())
                return;
            final String name = dbList.getModel().getElementAt(index);
            final String msg = "Do you really want to delete the custom database '" + name + "'?";
            if (new QuestionDialog(getOwner(), msg).isSuccess()) {
//                CustomDatabase.openDatabase(name).deleteDatabase();;
                customDatabases.remove(name);
                final String[] dbs = Jobs.runInBackgroundAndLoad(owner, "Loading DBs...", (Callable<List<CustomDatabase<?>>>) SearchableDatabases::getCustomDatabases).getResult()
                        .stream().map(CustomDatabase::name).toArray(String[]::new);
                dbList.setListData(dbs);
            }

        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(375, getMinimumSize().height));
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    protected void whenCustomDbIsAdded(final String dbName) {
        //todo
        SearchableDatabases.getCustomDatabaseByName(dbName).ifPresent(db -> {
            this.customDatabases.put(db.name(), db);

            dbList.setListData(this.customDatabases.keySet().stream().sorted().toArray(String[]::new));
            dbList.setSelectedValue(db.name(),true);
        });
    }

    protected static class DatabaseView extends JPanel {
        JLabel content;

        protected DatabaseView() {
            this.content = new JLabel("No DB selected!");
            content.setHorizontalAlignment(JLabel.CENTER);
            content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setLayout(new BorderLayout());
            add(content, BorderLayout.CENTER);
            setPreferredSize(new Dimension(200, 240));
        }

        public void updateContent(CustomDatabase<?> c) {
            if (c.getStatistics().getCompounds() > 0) {
                content.setText("<html>Custom database. Containing "
                        + c.getStatistics().getCompounds() + " compounds with " + c.getStatistics().getFormulas()
                        + " different molecular formulas." +
                        ((c.getSettings().isInheritance() ? "<br>This database will also include all compounds from '" + DataSources.getDataSourcesFromBitFlags(c.getFilterFlag()).stream().filter(n -> !DBSelectionList.BLACK_LIST.contains(n)).collect(Collectors.joining("', '")) + "'." : "")
                                + (c.needsUpgrade() ? "<br><b>This database schema is outdated. You have to upgrade the database before you can use it.</b>" : "") + "</html>"));
            } else {
                content.setText("Empty custom database.");
            }
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

    protected class ImportDatabaseDialog extends JDialog {
        protected JButton importButton;
        protected DatabaseImportConfigPanel configPanel;

        public ImportDatabaseDialog() {
            this(null);
        }

        public ImportDatabaseDialog(@Nullable CustomDatabase<?> db) {
            super(owner, db != null ? "Import into '" + db.name() + "' database" : "Create custom database", false);

            setPreferredSize(new Dimension(640, 480));
            setLayout(new BorderLayout());

            final JLabel explain = new JLabel("<html>You can inherit compounds from PubChem or our biological databases. If you do so, all compounds in these databases are implicitly added to your custom database.");
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
            final JTextArea textArea = new JTextAreaDropImage();
            textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            final JScrollPane pane = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            pane.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(pane);
            box.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Import compounds"));

            importButton = new JButton("Import compounds");
            importButton.setAlignmentX(Component.RIGHT_ALIGNMENT);

            configPanel = new DatabaseImportConfigPanel(db);
            importButton.setEnabled(db != null && ! configPanel.nameField.getText().isBlank());
            configPanel.nameField.getDocument().addDocumentListener(new DocumentListener() {
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
                    importButton.setEnabled(configPanel.nameField.getText().length() > 0 && configPanel.nameField.getText().replaceAll("\\s", "").equals(configPanel.nameField.getText()) && customDatabases.keySet().stream().noneMatch(k -> k.equalsIgnoreCase(configPanel.nameField.getText())));
                }
            });

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
            pack();
            setLocationRelativeTo(getOwner());
            setVisible(true);

        }
        protected void runImportJob(@NotNull List<Path> source) {
            try {
                final DefaultParameterConfigLoader configOptionLoader = new DefaultParameterConfigLoader(PropertyManager.DEFAULTS.newIndependentInstance("DB_IMPORT"));
                final WorkflowBuilder<GuiComputeRoot> wfBuilder = new WorkflowBuilder<>(new GuiComputeRoot(MF.ps(), null), configOptionLoader, new GuiInstanceBufferFactory());
                wfBuilder.rootOptions.setNonCompoundInput(source);
                final Run computation = new Run(wfBuilder);

                List<String> command = new ArrayList<>();
                command.add(configPanel.toolCommand());
                command.addAll(configPanel.asParameterList());

                computation.parseArgs(command.toArray(String[]::new));

                if (computation.isWorkflowDefined()) {
                    final TextAreaJJobContainer<Boolean> j = Jobs.runWorkflow(computation.getFlow(), List.of(), command, configPanel.toolCommand());
                    LoadingBackroundTask.connectToJob(this, "Importing into '" + configPanel.nameField.getText() + "'...", false, j);
                    whenCustomDbIsAdded(configPanel.nameField.getText());
                }
                //todo else some error message with pico cli output
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("Unexpected Error during Custom DB import.", e);
                new StacktraceDialog(MF, "Unexpected Error during Custom DB import.", e);
            }
        }
    }
}
