
package de.unijena.bioinf.ftalign.view;

import net.iharder.FileDrop;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

public class LoadWindow extends JPanel {

    private final ApplicationState state;

    public LoadWindow(final ApplicationWindow apW, final ApplicationState state) {
        this.state = state;
        final JButton button = new JButton();
        final Container contentPane = this;
        final JProgressBar progressBar = new JProgressBar();
        final MoleculeImporter moleculeImporter = new MoleculeImporter();
        final MsImporter msImporter = new MsImporter();
        progressBar.setPreferredSize(new Dimension(640, 64));
        button.setPreferredSize(new Dimension(640, 64));
        button.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                contentPane.remove(button);
                contentPane.add(progressBar, "span 2");
                progressBar.setVisible(true);
                contentPane.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                LoadWindow.this.revalidate();
                LoadWindow.this.repaint();
                final SwingWorker worker = new SwingWorker() {
                    @Override
                    protected Object doInBackground() throws Exception {
                        final Progress prog = new Progress() {
                            @Override
                            public void start(int max) {
                            }

                            @Override
                            public void tick(int current, int max) {
                                setProgress(Math.min(100, (int) (Math.round((100d * current) / max))));
                            }
                        };
                        try {
                            state.importFiles(new ArrayList<File>(msImporter.getFileSet()),
                                    new ArrayList<File>(moleculeImporter.getFileSet()), prog);
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw e;
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        contentPane.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        progressBar.setValue(progressBar.getMaximum());
                        apW.showPlotWindow();
                    }
                };
                worker.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName().equals("progress")) {
                            int progress = (Integer) evt.getNewValue();
                            progressBar.setIndeterminate(false);
                            progressBar.setValue(progress);
                        }
                    }
                });
                worker.execute();
            }
        });
        button.setText("START");
        setPreferredSize(new Dimension(640, 480));
        setLayout(new MigLayout("", "[center][center]"));
        add(moleculeImporter, "");
        add(msImporter, "wrap");
        add(button, "span 2");
    }

    public abstract class Importer extends JPanel {
        private JList<String> jfiles;
        private DefaultListModel<String> model;
        private TreeSet<File> fileSet;

        public Importer(String label) {
            super();
            setPreferredSize(new Dimension(320, 480));
            setBorder(BorderFactory.createDashedBorder(Color.black));
            setLayout(new BorderLayout());
            add(new JLabel(label), BorderLayout.NORTH);
            this.model = new DefaultListModel<String>();
            this.jfiles = new JList<String>(model);
            this.fileSet = new TreeSet<File>();
            final JScrollPane js = new JScrollPane(jfiles);
            add(js, BorderLayout.CENTER);
            new FileDrop(this, new FileDrop.Listener() {

                @Override
                public void filesDropped(File[] files) {
                    final TreeSet<File> fileList = new TreeSet<File>(Arrays.asList(files));
                    resolveFileList(fileList);
                    fileSet.addAll(fileList);
                    model.clear();
                    int k = 0;
                    for (File f : fileList) model.add(k++, f.getName());
                }
            });
        }

        public TreeSet<File> getFileSet() {
            return fileSet;
        }

        protected void resolveFileList(TreeSet<File> treeSet) {
            final ArrayList<File> added = new ArrayList<File>();
            final Iterator<File> tf = treeSet.iterator();
            final FileFilter filter = getFileFilter();
            while (tf.hasNext()) {
                final File f = tf.next();
                if (f.isDirectory()) {
                    tf.remove();
                    added.addAll(Arrays.asList(f.listFiles(filter)));
                } else if (filter.accept(f)) {

                } else tf.remove();
            }
            for (File f : added) treeSet.add(f);
        }

        protected abstract FileFilter getFileFilter();
    }

    public class MoleculeImporter extends Importer {

        public MoleculeImporter() {
            super("Insert Molecule files or directories here!");
        }

        @Override
        protected FileFilter getFileFilter() {
            return new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (!pathname.isFile()) return false;
                    final String n = pathname.getName();
                    return n.endsWith(".inchi") || n.endsWith(".sdf") || n.endsWith(".mol") || n.endsWith(".fpt");
                }
            };
        }
    }

    public class MsImporter extends Importer {

        public MsImporter() {
            super("Insert Tree files or directories here!");
        }

        @Override
        protected FileFilter getFileFilter() {
            return new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (!pathname.isFile()) return false;
                    final String n = pathname.getName();
                    return n.endsWith(".dot") || n.endsWith(".json");
                }
            };
        }
    }

}
