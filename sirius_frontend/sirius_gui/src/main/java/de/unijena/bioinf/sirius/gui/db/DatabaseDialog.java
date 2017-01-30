package de.unijena.bioinf.sirius.gui.db;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.sirius.gui.compute.ProgressDialog;
import de.unijena.bioinf.sirius.gui.configs.ConfigStorage;
import de.unijena.bioinf.sirius.gui.ext.ConfirmDialog;
import de.unijena.bioinf.sirius.gui.ext.DragAndDrop;
import de.unijena.bioinf.sirius.gui.ext.ListAction;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.utils.Buttons;
import de.unijena.bioinf.sirius.gui.utils.SwingUtils;
import org.jdesktop.swingx.JXRadioGroup;
import org.jdesktop.swingx.StackLayout;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.List;

public class DatabaseDialog extends JDialog {

    //todo we should saperade the Dialog from the Database Managing part.
    protected JList<String> dbList;
    protected JButton addCustomDb;
    protected DatabaseView dbView;
    protected JTextField nameField;
    protected final Frame owner;

    public DatabaseDialog(final Frame owner) {
        super(owner, "Databases", true);
        this.owner = owner;
        setLayout(new BorderLayout());

        final List<String> databases = collectDatabases();
        this.dbList = new DatabaseList(databases);

        final Box box = Box.createVerticalBox();
        JScrollPane pane = new JScrollPane(dbList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        box.add(pane);
        this.nameField = new JTextField(16);
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
                if (nameField.getText().length()>0 && !databases.contains(nameField.getText())) {
                    addCustomDb.setEnabled(true);
                } else {
                    addCustomDb.setEnabled(false);
                }
            }
        });
        this.addCustomDb = Buttons.getAddButton16("add custom db");
        addCustomDb.setEnabled(false);
        final Box but = Box.createHorizontalBox();
        but.add(nameField);
        but.add(addCustomDb);

        add(but, BorderLayout.SOUTH);
        add(box, BorderLayout.WEST);
        but.add(Box.createHorizontalGlue());

        this.dbView = new DatabaseView();

        add(dbView, BorderLayout.CENTER);

        dbList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                final int i = dbList.getSelectedIndex();
                if (i >= 0)
                    dbView.update(dbList.getModel().getElementAt(i));
            }
        });

        dbList.setSelectedIndex(0);

        addCustomDb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                databases.add(nameField.getText());
                dbList.setListData(databases.toArray(new String[databases.size()]));
                new ImportDatabaseDialog(nameField.getText());
            }
        });

        new ListAction(dbList, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int k=dbList.getSelectedIndex();
                if (k > 0 && k < dbList.getModel().getSize()) {
                    new ImportDatabaseDialog(dbList.getModel().getElementAt(k));
                }
            }
        });

        dbView.edit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int k=dbList.getSelectedIndex();
                if (k > 0 && k < dbList.getModel().getSize()) {
                    new ImportDatabaseDialog(dbList.getModel().getElementAt(k));
                }
            }
        });

        dbView.deleteCache.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int index = dbList.getSelectedIndex();
                final String name = dbList.getModel().getElementAt(index);
                final String msg = (index>0) ?
                        "Do you really want to delete the custom database '" + name + "'?" : "Do you really want to clear the cache of the PubChem database?";
                if (ConfirmDialog.confirm(owner, "Delete database", msg)) {
                    if (index>0) {
                        new CustomDatabase(name, new File(Workspace.CONFIG_STORAGE.getCustomDatabaseDirectory(), name)).getImporter().deleteDatabase();
                        dbList.setListData(collectDatabases().toArray(new String[0]));
                    } else {
                        // TODO: implement
                    }
                }
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setMinimumSize(new Dimension(320, 240));
        pack();
    }

    protected static class DatabaseView extends JPanel  {
        JLabel content;
        JButton deleteCache, edit;
        protected DatabaseView() {
            this.content = new JLabel("placeholder");
            content.setBorder(BorderFactory.createEmptyBorder(4,8,4,4));
            this.deleteCache = new JButton("delete cache");
            setLayout(new BorderLayout());
            add(content, BorderLayout.CENTER);
            //add(Box.createVerticalGlue());
            final Box hor = Box.createHorizontalBox();
            hor.setBorder(BorderFactory.createEmptyBorder(0,0,16,0));
            edit = new JButton("edit");
            hor.add(Box.createHorizontalGlue());
            hor.add(edit);
            hor.add(deleteCache);
            hor.add(Box.createHorizontalGlue());
            add(hor, BorderLayout.SOUTH);
            setPreferredSize(new Dimension(200,240));
        }



        protected void update(String database) {
            if (database.equals("PubChem")) {
                content.setText("<html>Our in-house mirror of PubChem (last update was at 1st June, 2016).<br> Compounds are requested via webservice and cached locally on your hard drive.</html>");
                content.setMaximumSize(new Dimension(200, 240));
                deleteCache.setText("Delete cache");
                deleteCache.setEnabled(false);
                edit.setEnabled(false);
            } else {
                content.setText("Custom database.");
                deleteCache.setText("Delete database");
                deleteCache.setEnabled(true);
                edit.setEnabled(true);
            }
        }


    }

    private List<String> collectDatabases() {
        final List<String> databases = new ArrayList<>();
        databases.add("PubChem");
        final File root = Workspace.CONFIG_STORAGE.getDatabaseDirectory();
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
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
            }
        }

        public void addCompound(InChI inchi) {
            if (importedCompounds.add(inchi.key2D())) {
                model.addElement(inchi);
            }
        }


        @Override
        public Component getListCellRendererComponent(JList<? extends InChI> list, InChI value, int index, boolean isSelected, boolean cellHasFocus) {
            left.setText(value.key2D());
            right.setText(value.in2D);
            return cell;
        }
    }

    protected static class ImportCompoundsDialog extends JDialog {
        protected JLabel statusText;
        protected JProgressBar progressBar;
        protected CompoundImportedListener listener;
        protected JTextArea details;
        protected CustomDatabase.Importer importer;
        protected JButton close;
        protected volatile boolean doNotCancel=false;
        protected SwingWorker<List<InChI>, ImportStatus> worker;
        public ImportCompoundsDialog(Frame owner, CustomDatabase.Importer importer) {
            super(owner, "Import compounds", true);
            this.importer = importer;
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
            panel.setPreferredSize(new Dimension(480,320));
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
        }

        @Override
        public void dispose() {
            if (worker!=null) worker.cancel(!doNotCancel);
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
            if (stringsOrFiles.size()>0 && stringsOrFiles.get(0) instanceof File) {
                statusText.setText("Parse " + stringsOrFiles.size() + " files");
            } else {
                statusText.setText("Predict fingerprints for " + stringsOrFiles.size() + " compounds");
            }
            worker = new SwingWorker<List<InChI>, ImportStatus>(){

                @Override
                protected void done() {
                    super.done();
                    ImportCompoundsDialog.this.closeIfNoErrorMessage();
                }

                @Override
                protected void process(List<ImportStatus> chunks) {
                    super.process(chunks);
                    for (ImportStatus status : chunks) {
                        if (status.topMessage!=null) statusText.setText(status.topMessage);
                        if (status.inchi!=null) {
                            if (listener!=null)
                                listener.compoundImported(status.inchi);
                        }
                        progressBar.setValue(status.current);
                        progressBar.setMaximum(status.max);
                        if (status.errorMessage!=null) {
                            try {
                                details.getDocument().insertString(details.getDocument().getLength(), status.errorMessage + "\n", null);
                            } catch (BadLocationException e) {
                                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                            }
                        }
                    }
                }

                @Override
                protected List<InChI> doInBackground() throws Exception {
                    final List<InChI> inchis = new ArrayList<>(stringsOrFiles.size());
                    int k=0;
                    for (Object s : stringsOrFiles) {
                        final ImportStatus status = new ImportStatus();
                        status.max = stringsOrFiles.size();
                        status.current = k++;

                        if (s instanceof String) {
                            try {
                                InChI inchi = importer.importCompound((String)s);
                                inchis.add(inchi);
                                status.inchi = inchi;
                            } catch (Throwable e) {
                                status.errorMessage = e.getMessage();
                            }
                        } else if (s instanceof File) {
                            try {
                                final List<IAtomContainer> mols = importer.importFrom((File)s);
                                if (mols.size()>0) {
                                    status.max = mols.size();
                                    status.topMessage = "Predict fingerprints for  " + mols.size() + " compounds";
                                    status.current=0;
                                    publish(status);
                                    for (IAtomContainer mol : mols) {
                                        final ImportStatus status2 = status.clone();
                                        try {
                                        status2.current++;
                                        InChI inchi = importer.importCompound(mol,null);
                                        inchis.add(inchi);
                                            status2.inchi = inchi;} catch (Throwable t) {
                                            status2.errorMessage = t.getMessage();
                                        }
                                        publish(status2);
                                    }
                                    status.max = stringsOrFiles.size();
                                    status.current = k;
                                    status.topMessage = "Parse " + stringsOrFiles.size() + " files";
                                    publish(status);

                                }
                            } catch (Throwable e) {
                                status.errorMessage = e.getMessage();
                            }
                        }

                        publish(status);
                    }
                    try {
                        doNotCancel=true;
                        importer.flushBuffer();
                        doNotCancel=false;
                    } catch (Exception e) {
                        LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                        throw(e);
                    }
                    return inchis;
                }
            };
            worker.execute();
            pack();
            setVisible(true);
        }

    }

    protected static class ImportStatus implements Cloneable{
        private InChI inchi;
        private String errorMessage, topMessage;
        private int max, current;

        @Override
        protected ImportStatus clone() {
            try {
                return (ImportStatus)super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected class ImportDatabaseDialog extends JDialog implements CompoundImportedListener{

        protected ImportList ilist;
        protected JButton importButton;
        protected ImportCompoundsDialog importDialog;
        protected CustomDatabase.Importer importer;
        protected Collector collector;
        protected CustomDatabase database;

        private static final String NONE="None", BIO = "Biological database", PUBCHEM="PubChem";

        public ImportDatabaseDialog(String name) {
            super(owner, "Import " + name + " database", true);

            database = new CustomDatabase(name, new File(Workspace.CONFIG_STORAGE.getCustomDatabaseDirectory(), name));
            importer = database.getImporter();
            importer.init();
            collector = new Collector(importer, this);
            collector.execute();
            setPreferredSize(new Dimension(640, 480));
            setLayout(new BorderLayout());

            final JXRadioGroup<String> inh = new JXRadioGroup<String>(new String[]{NONE, BIO, PUBCHEM});
            inh.setLayoutAxis(BoxLayout.X_AXIS);
            inh.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Inherit compounds from"));

            add(inh, BorderLayout.NORTH);
            if (database.deriveFromBioDb) inh.setSelectedValue(BIO);
            else if (database.deriveFromPubchem) inh.setSelectedValue(PUBCHEM);
            else inh.setSelectedValue(NONE);

            inh.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final String value = inh.getSelectedValue();
                    database.deriveFromBioDb=database.deriveFromPubchem=false;
                    if (value.equals(BIO)) database.deriveFromBioDb=true;
                    else if (value.equals(PUBCHEM)) database.deriveFromPubchem=true;
                    try {
                        importer.writeSettings();
                    } catch (IOException e1) {
                        LoggerFactory.getLogger(this.getClass()).error(e1.getMessage(),e1);
                    }
                }
            });

            final Box box = Box.createVerticalBox();
            box.setAlignmentX(Component.LEFT_ALIGNMENT);
            final JLabel label = new JLabel("<html>Please insert the compounds of your custom database here (one compound per line). You can use SMILES and InChI to describe your compounds. It is also possible to drag and drop files with InChI, SMILES or other molecule formats (e.g. MDL) into this text field.");
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


            box.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "import compounds"));

            add(box, BorderLayout.CENTER);

            final Box box2 = Box.createVerticalBox();
            box2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "already imported"));

            ilist = new ImportList();
            box2.add(new JScrollPane(ilist, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));

            add(box2, BorderLayout.SOUTH);

            importDialog = new ImportCompoundsDialog(owner, importer);
            importDialog.listener = this;

            importButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!importDialog.isVisible()) {
                        final String[] lines = textArea.getText().split("\n");
                        importDialog.setCompounds(database, Arrays.asList(lines));
                        textArea.setText("");
                    }
                }
            });

            final DropTarget dropTarget = new DropTarget(){
                @Override
                public synchronized void drop(DropTargetDropEvent evt) {
                    final List<File> files = DragAndDrop.getFileListFromDrop(evt);
                    if (!importDialog.isVisible()) {
                        importDialog.setCompounds(database,files);
                        textArea.setText("");
                    }
                }
            };

            setDropTarget(dropTarget);
            textArea.setDropTarget(dropTarget);

            pack();
            setVisible(true);
        }


        @Override
        public void dispose() {
            collector.cancel(true);
            super.dispose();
        }

        @Override
        public void compoundImported(InChI inchi) {
            ilist.addCompound(inchi);
        }
    }

    private static class Collector extends SwingWorker<InChI, InChI> implements CompoundImportedListener {
        private CustomDatabase.Importer importer;
        private CompoundImportedListener listener;
        public Collector(CustomDatabase.Importer importer, CompoundImportedListener listener) {
            this.importer = importer;
            this.listener = listener;
        }

        @Override
        protected void process(List<InChI> chunks) {
            for (InChI inchi : chunks) {
                listener.compoundImported(inchi);
            }
        }

        @Override
        public void compoundImported(InChI inchi) {
            publish(inchi);
        }

        @Override
        protected InChI doInBackground() throws Exception {
            importer.collect(this);
            return null;
        }
    }

}
