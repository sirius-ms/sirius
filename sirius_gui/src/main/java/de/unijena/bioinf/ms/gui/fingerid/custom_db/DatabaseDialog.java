package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import com.google.common.base.Predicate;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.fingerid.db.SearchableDatabases;
import de.unijena.bioinf.fingerid.db.custom.CustomDatabase;
import de.unijena.bioinf.fingerid.db.custom.CustomDatabaseImporter;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.DialogHaeder;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.DragAndDrop;
import de.unijena.bioinf.ms.gui.utils.ListAction;
import de.unijena.bioinf.ms.gui.utils.PlaceholderTextField;
import de.unijena.bioinf.ms.gui.utils.TwoCloumnPanel;
import de.unijena.bioinf.ms.gui.io.CsvFields;
import de.unijena.bioinf.ms.gui.io.csv.GeneralCSVDialog;
import de.unijena.bioinf.ms.gui.io.csv.SimpleCsvParser;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import org.jdesktop.swingx.JXRadioGroup;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.ReaderFactory;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.List;
import java.util.*;

public class DatabaseDialog extends JDialog {

    //todo: we should separate the Dialog from the Database Managing part.
    //todo: we should use the import mechanisms from cli or library so that we do not nee
    //todo: prptected acces to th importer classes
    protected JList<String> dbList;
    protected HashMap<String, CustomDatabase> customDatabases;
    protected JButton addCustomDb;
    protected DatabaseView dbView;
    protected PlaceholderTextField nameField;
    //    protected final Frame owner;
    private JDialog owner = this;

    public DatabaseDialog(final Frame owner) {
        super(owner, true);
        setTitle("Databases");
        setLayout(new BorderLayout());

//        this.owner = owner;

        //============= NORTH (Header) =================
        JPanel header = new DialogHaeder(Icons.DB_64);
        add(header, BorderLayout.NORTH);


        final List<String> databases = collectDatabases();
        this.dbList = new DatabaseList(databases);
        this.customDatabases = new HashMap<>();

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
                if (nameField.getText().length() > 0 && !databases.contains(nameField.getText())) {
                    addCustomDb.setEnabled(true);
                } else {
                    addCustomDb.setEnabled(false);
                }
            }
        });
        this.addCustomDb = Buttons.getAddButton16("Add custom DB");
        addCustomDb.setEnabled(false);
        final Box but = Box.createHorizontalBox();
        but.add(nameField);
        but.add(addCustomDb);

        add(but, BorderLayout.SOUTH);
        add(box, BorderLayout.CENTER);
        but.add(Box.createHorizontalGlue());

        this.dbView = new DatabaseView();

        add(dbView, BorderLayout.EAST);

        dbList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                final int i = dbList.getSelectedIndex();
                if (i >= 0) {
                    final String s = dbList.getModel().getElementAt(i);
                    if (s.equalsIgnoreCase("pubchem"))
                        dbView.update(s);
                    else if (customDatabases.containsKey(s))
                        dbView.updateContent(customDatabases.get(s));
                }
            }
        });

        dbList.setSelectedIndex(0);

        addCustomDb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                databases.add(nameField.getText());
                dbList.setListData(databases.toArray(new String[databases.size()]));
                final CustomDatabase newDb = new ImportDatabaseDialog(nameField.getText()).database;
                whenCustomDbIsAdded(newDb);
            }
        });

        new ListAction(dbList, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int k = dbList.getSelectedIndex();
                if (k > 0 && k < dbList.getModel().getSize()) {
                    whenCustomDbIsAdded(new ImportDatabaseDialog(dbList.getModel().getElementAt(k)).database);
                }
            }
        });

        dbView.edit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int k = dbList.getSelectedIndex();
                if (k > 0 && k < dbList.getModel().getSize()) {
                    whenCustomDbIsAdded(new ImportDatabaseDialog(dbList.getModel().getElementAt(k)).database);
                }
            }
        });

        dbView.deleteCache.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
                        dbList.setListData(collectDatabases().toArray(new String[0]));
                    } else {
                        // TODO: implement
                    }
                }

            }
        });

        for (String name : databases) {
            if (!name.equalsIgnoreCase("pubchem"))
                whenCustomDbIsAdded(new CustomDatabase(name, new File(SearchableDatabases.getCustomDatabaseDirectory(), name)));
        }


        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(350, getMinimumSize().height));
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    protected void whenCustomDbIsAdded(final CustomDatabase db) {
        this.customDatabases.put(db.name(), db);
        new SwingWorker<String, String>() {

            @Override
            protected String doInBackground() throws Exception {
                db.readSettings();
                LoggerFactory.getLogger(this.getClass()).debug("SETTINGS OF " + db.name() + " IS READ");
                publish(db.name());
                return db.name();
            }

            @Override
            protected void process(List<String> chunks) {
                for (String c : chunks) {
                    final CustomDatabase cd = customDatabases.get(c);
                    if (c != null && cd != null && c.equals(dbList.getSelectedValue())) {
                        dbView.updateContent(cd);
                    }
                }
            }
        }.execute();
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
            //add(Box.createVerticalGlue());
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
                content.setText("<html>Custom database. Containing " + c.getNumberOfCompounds() + " compounds with " + c.getNumberOfFormulas() + " different molecular formulas. Consumes " + c.getMegabytes() + " mb on the hard drive." + ((c.searchInBio() || c.searchInPubchem()) ? "<br>This database will also include all compounds from " + (c.searchInPubchem() ? "PubChem" : "our bio database") : "") + (c.needsUpgrade() ? "<br><b>This database schema is outdated. You have to upgrade the database before you can use it.</b>" : "") + "</html>");
            } else {
                content.setText("Empty custom database.");
            }
            deleteCache.setText("Delete database");
            deleteCache.setEnabled(true);
            edit.setEnabled(!c.needsUpgrade());
        }
    }

    private List<String> collectDatabases() {
        final List<String> databases = new ArrayList<>();
        databases.add("PubChem");
        final File root = SearchableDatabases.getDatabaseDirectory();
        final File custom = new File(root, "custom");
        if (!custom.exists()) {
            return databases;
        }
        for (File subDir : custom.listFiles()) {
            if (subDir.isDirectory()) {
                databases.add(subDir.getName());
            }
        }
        return databases;
    }

    protected class DatabaseList extends JList<String> {

        protected DatabaseList(List<String> databaseList) {
            super(new Vector<String>(databaseList));
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

            Font tempFont = null;
            try {
                InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans-Bold.ttf");
                tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(13f);
                right.setFont(tempFont);
                left.setFont(tempFont.deriveFont(Font.BOLD));
            } catch (FontFormatException | IOException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            }
        }

        public void addCompound(final InChI inchi) {
            if (importedCompounds.add(inchi.key2D())) {
                // I dont understand why this have to be run in swing thread
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        model.addElement(inchi);
                    }
                });
            }
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
        protected volatile boolean doNotCancel = false;

        protected volatile int molBufferSize, fpBufferSize;

        protected SwingWorker<List<InChI>, ImportStatus> worker;

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
            close.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setLocationRelativeTo(getParent());
        }

        @Override
        public void dispose() {
            if (worker != null) worker.cancel(!doNotCancel);
            super.dispose();
        }

        protected void closeIfNoErrorMessage() {
            if (details.getDocument().getLength() == 0) dispose();
            else close.setEnabled(true);
        }


        public void setCompounds(CustomDatabase db, final List<? extends Object> stringsOrFiles) {
            close.setEnabled(false);
            details.setText("");
            progressBar.setMinimum(0);
            progressBar.setMaximum(stringsOrFiles.size());
            progressBar.setValue(0);

            GeneralCSVDialog parser = null;
            final GeneralCSVDialog.Field inchi = new CsvFields.InChIField(0, 1), smiles = new CsvFields.SMILESField(0, 1), id = new CsvFields.IDField(0, 1);
            boolean nonCsvFile = false;
            outer:
            for (Object o : stringsOrFiles) {
                if (o instanceof File) {
                    final File f = (File) o;
                    if (f.exists()) {
                        final List<String> preview = getPreviewIfIsCsv(f);
                        if (preview != null) {

                            parser = GeneralCSVDialog.makeCsvImporterDialog(this, preview, new Predicate<GeneralCSVDialog>() {
                                @Override
                                public boolean apply(GeneralCSVDialog input) {
                                    return input.getFirstColumnFor(inchi) >= 0 || input.getFirstColumnFor(smiles) >= 0;
                                }
                            }, inchi, smiles, id);
                            break outer;
                        } else {
                            nonCsvFile = true;
                        }
                    }
                }
            }

            if (nonCsvFile) {
                if (stringsOrFiles.size() > 0 && stringsOrFiles.get(0) instanceof File) {
                    new AskForFieldsToImportDialog(DatabaseDialog.this, importer);
                    statusText.setText("Parse " + stringsOrFiles.size() + " files");
                } else {
                    statusText.setText("Predict fingerprints for " + stringsOrFiles.size() + " compounds");
                }
            }

            final GeneralCSVDialog csvDialog = parser;
            final SimpleCsvParser csvParser = csvDialog != null ? csvDialog.getParser() : null;
            final int inchiColumn = csvDialog != null ? csvDialog.getFirstColumnFor(inchi) : 0, smilesColumn = csvDialog != null ? csvDialog.getFirstColumnFor(smiles) : 0, idColumn = csvDialog != null ? csvDialog.getFirstColumnFor(id) : 0;
            worker = new SwingWorker<List<InChI>, ImportStatus>() {

                @Override
                protected void done() {
                    super.done();
                    ImportCompoundsDialog.this.closeIfNoErrorMessage();
                }

                @Override
                protected void process(List<ImportStatus> chunks) {
                    super.process(chunks);
                    for (ImportStatus status : chunks) {
                        if (status.topMessage != null) statusText.setText(status.topMessage);
                        if (status.inchi != null) {
                        }
                        progressBar.setValue(status.current);
                        progressBar.setMaximum(status.max);
                        if (status.errorMessage != null) {
                            try {
                                details.getDocument().insertString(details.getDocument().getLength(), status.errorMessage + "\n", null);
                            } catch (BadLocationException e) {
                                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                            }
                        }
                    }
                }

                @Override
                protected List<InChI> doInBackground() throws Exception {
                    final List<InChI> inchis = new ArrayList<>(stringsOrFiles.size());
                    final List<IAtomContainer> buffer = new ArrayList<>();
                    final ReaderFactory rf = new ReaderFactory();
                    int k = 0;
                    for (Object s : stringsOrFiles) {
                        if (isCancelled()) return Collections.emptyList();
                        final ImportStatus status = new ImportStatus();
                        status.max = stringsOrFiles.size() + molBufferSize + fpBufferSize;
                        status.current = k++;

                        if (s instanceof String) {
                            try {
                                importer.importFromString((String) s);
                            } catch (Throwable e) {
                                status.errorMessage = e.getMessage();
                            }
                        } else if (s instanceof File) {
                            try {
                                boolean isCsv = csvParser != null;
                                if (isCsv) {
                                    try (final InputStream sr = new FileInputStream((File) s)) {
                                        if (rf.createReader(sr) != null) {
                                            isCsv = false;
                                        }
                                    }
                                }
                                if (isCsv) {
                                    try (final BufferedReader br = FileUtils.ensureBuffering(new FileReader((File) s))) {
                                        final List<String> inchiOrSmiles = new ArrayList<>();
                                        final List<String> ids = new ArrayList<>();
                                        String line;
                                        while ((line = br.readLine()) != null) {
                                            String[] tabs = csvParser.parseLine(line);

                                            String id = null;
                                            if (idColumn >= 0) id = tabs[idColumn];
                                            if (inchiColumn >= 0 && tabs[inchiColumn].startsWith("InChI=")) {
                                                inchiOrSmiles.add(tabs[inchiColumn]);
                                                ids.add(id);
                                            } else if (smilesColumn >= 0) {
                                                inchiOrSmiles.add(tabs[smilesColumn]);
                                                ids.add(id);
                                            }
                                        }
                                        int oldCurrent = status.current;
                                        status.current = 0;
                                        status.max = inchiOrSmiles.size();
                                        String oldMessage = status.topMessage;
                                        status.topMessage = "Download/Compute " + inchiOrSmiles.size() + " structures";
                                        int batchSize = Math.min(5, inchiOrSmiles.size() / 100);
                                        publish(status);
                                        for (int i = 0; i < inchiOrSmiles.size(); ++i) {
                                            try {
                                                importer.importFromString(inchiOrSmiles.get(i), ids.get(i));
                                            } catch (Exception e) {
                                                final ImportStatus sc = status.clone();
                                                sc.current = i;
                                                sc.errorMessage = inchiOrSmiles.get(i) + ": " + e.getMessage();
                                                publish(sc);
                                                e.printStackTrace();
                                            }
                                            if (i % batchSize == 0) {
                                                final ImportStatus sc = status.clone();
                                                sc.current = i;
                                                publish(sc);
                                            }
                                        }
                                        status.current = oldCurrent;
                                        status.topMessage = oldMessage;
                                    }
                                } else {
                                    importer.importFrom((File) s);
                                }
                            } catch (Throwable e) {
                                status.errorMessage = e.getMessage();
                                e.printStackTrace();
                            }
                        }
                        publish(status);
                    }
                    try {
                        doNotCancel = true;
                        importer.flushBuffer();
                        doNotCancel = false;
                    } catch (Exception e) {
                        LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                        throw (e);
                    }
                    return inchis;
                }
            };
            worker.execute();
            pack();
            setVisible(true);
        }

        private List<String> getPreviewIfIsCsv(File f) {
            try {

                try (FileInputStream br = new FileInputStream(f)) {
                    ReaderFactory rf = new ReaderFactory();
                    if (rf.createReader(br) != null) return null;
                }

                try (BufferedReader br = FileUtils.ensureBuffering(new FileReader(f))) {
                    final ArrayList<String> lines = new ArrayList<>();
                    // parse 10 lines
                    String line;
                    while ((line = br.readLine()) != null) {
                        lines.add(line);
                        if (lines.size() > 10) break;
                    }
                    if (lines.isEmpty()) return null;

                    if (SimpleCsvParser.guessSeparator(lines).parseLine(lines.get(0)).length > 1) {
                        return lines;
                    } else return null;
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                return null;
            }
        }

    }

    protected static class ImportStatus implements Cloneable {
        private InChI inchi;
        private String errorMessage, topMessage;
        private int max, current;

        @Override
        protected ImportStatus clone() {
            try {
                return (ImportStatus) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final String NONE = "None", BIO = DataSource.BIO.realName, PUBCHEM = DataSource.PUBCHEM.realName;

    protected class ImportDatabaseDialog extends JDialog implements CustomDatabaseImporter.Listener {

        protected ImportList ilist;
        protected JButton importButton;
        protected ImportCompoundsDialog importDialog;
        protected CustomDatabaseImporter importer;
        protected Collector collector;
        protected CustomDatabase database;


        public ImportDatabaseDialog(String name) {
            super(owner, "Import " + name + " database", false);
            database = CustomDatabase.createNewDatabase(name, new File(SearchableDatabases.getCustomDatabaseDirectory(), name), ApplicationCore.WEB_API.getFingerprintVersion());
            importer = database.getImporter(ApplicationCore.WEB_API);
            importer.init();
            importer.addListener(this);
            collector = new Collector(importer);
            collector.execute();
            setPreferredSize(new Dimension(640, 480));
            setLayout(new BorderLayout());

            final JLabel explain = new JLabel("<html>You can inherit compounds from PubChem or our biological database. If you do so, all compounds in these databases are implicitly added to your custom database.");
            final Box hbox = Box.createHorizontalBox();
            hbox.add(explain);
            final Box vbox = Box.createVerticalBox();
            vbox.add(hbox);
            vbox.add(Box.createVerticalStrut(4));

            final JXRadioGroup<String> inh = new JXRadioGroup<String>(new String[]{NONE, BIO, PUBCHEM});
            inh.setLayoutAxis(BoxLayout.X_AXIS);
            vbox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Inherit compounds from"));
            final Box hbox2 = Box.createHorizontalBox();
            hbox2.add(inh);
            hbox2.add(Box.createHorizontalGlue());
            vbox.add(hbox2);
            add(vbox, BorderLayout.NORTH);
            if (database.isDeriveFromBioDb()) inh.setSelectedValue(BIO);
            else if (database.isDeriveFromPubchem()) inh.setSelectedValue(PUBCHEM);
            else inh.setSelectedValue(NONE);

            inh.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final String value = inh.getSelectedValue();
                    database.setDeriveFromBioDb(false);
                    database.setDeriveFromPubchem(false);
                    if (value.equals(BIO)) database.setDeriveFromBioDb(true);
                    else if (value.equals(PUBCHEM)) database.setDeriveFromPubchem(true);
                    try {
                        importer.writeSettings();
                    } catch (IOException e1) {
                        LoggerFactory.getLogger(this.getClass()).error(e1.getMessage(), e1);
                    }
                }
            });

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
            importButton = new JButton("Import compounds");
            importButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(importButton);


            box.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Import compounds"));

            add(box, BorderLayout.CENTER);

            final Box box2 = Box.createVerticalBox();
            box2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Recently imported"));

            ilist = new ImportList();
            box2.add(new JScrollPane(ilist, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));

            add(box2, BorderLayout.SOUTH);

            importDialog = new ImportCompoundsDialog(importer);

            importButton.addActionListener(e -> {
                if (!importDialog.isVisible()) {
                    final String[] lines = textArea.getText().split("\n");
                    importDialog.setCompounds(database, Arrays.asList(lines));
                    textArea.setText("");
                }
            });

            final DropTarget dropTarget = new DropTarget() {
                @Override
                public synchronized void drop(DropTargetDropEvent evt) {
                    final List<File> files = DragAndDrop.getFileListFromDrop(evt);
                    if (!importDialog.isVisible()) {
                        importDialog.setCompounds(database, files);
                        textArea.setText("");
                    }
                }
            };

            setDropTarget(dropTarget);
            textArea.setDropTarget(dropTarget);
            setLocationRelativeTo(getParent());
            pack();
            setVisible(true);
        }


        @Override
        public void dispose() {
            collector.cancel(true);
            super.dispose();
        }

        @Override
        public void newFingerprintBufferSize(int size) {

        }

        @Override
        public void newMoleculeBufferSize(int size) {

        }

        @Override
        public void newInChI(InChI inchi) {
            ilist.addCompound(inchi);
        }
    }

    private static class Collector extends SwingWorker<InChI, InChI> implements CustomDatabaseImporter.Listener {
        private CustomDatabaseImporter importer;

        public Collector(CustomDatabaseImporter importer) {
            this.importer = importer;
        }

        @Override
        protected void process(List<InChI> chunks) {
            for (InChI inchi : chunks) {
            }
        }

        @Override
        public void newFingerprintBufferSize(int size) {

        }

        @Override
        public void newMoleculeBufferSize(int size) {

        }

        @Override
        public void newInChI(InChI inchi) {
            publish(inchi);
        }

        @Override
        protected InChI doInBackground() throws Exception {
            importer.collect(this);
            return null;
        }
    }

    private class AskForFieldsToImportDialog extends JDialog {
        private final JTextField nameField = new JTextField("COMMON_NAME,SYSTEMATIC_NAME");
        private final JTextField idField = new JTextField("");

        AskForFieldsToImportDialog(Dialog owner, final CustomDatabaseImporter importer) {
            super(owner, "Specify fields to parse", true);

            JPanel main = new JPanel(new BorderLayout());
            add(main);

            TwoCloumnPanel panel = new TwoCloumnPanel();
            panel.add(new JLabel("<html>Please specify the names of the fields that have to be parsed in a comma separated list. <br> The list should be ordered from highest to lowest priority</html>"));
            panel.add(new JLabel("Database ID"), idField);
            panel.add(new JLabel("Name"), nameField);

            JButton okB = new JButton("OK");
            okB.addActionListener(e -> {
                importer.setCommonNameProps(nameField.getText().replaceAll("\\s+", "").split(","));
                importer.setDbIDProps(idField.getText().replaceAll("\\s+", "").split(","));
                setVisible(false);
                dispose();
            });
            JPanel bPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            bPanel.add(okB);

            main.add(panel, BorderLayout.CENTER);
            main.add(bPanel, BorderLayout.SOUTH);

            pack();
            setLocationRelativeTo(owner);
            setVisible(true);
        }
    }


}
