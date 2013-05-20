package de.unijena.bioinf.treeviewer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.File;
import java.io.IOError;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FileListPane extends JPanel {

    private final static Logger logger = Logger.getLogger(FileListPane.class.getSimpleName());

    private final CurrentFileAction action;
    private final FileList listPane;
    private final DefaultListModel<DotSource> listModel;
    private File lastDir;
    private DotSource selectedFile;
    MainFrame mf;

    public FileListPane(MainFrame mf, CurrentFileAction action) {
        super();
        this.mf = mf;
        this.action = action;
        this.selectedFile = null;
        this.lastDir = new File(".");
        this.setLayout(new BorderLayout());
        this.listModel = new DefaultListModel<DotSource>();
        this.listPane = new FileList(listModel);
        final KeyAdapter removeItem = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                        final List<DotSource> files = listPane.getSelectedValuesList();
                        if (files == null) return;
                        for (DotSource f : files) {
                            if (f.equals(selectedFile)) disableSelection();
                            listModel.removeElement(f);
                        }
                    }
                } catch (RuntimeException x) {
                    logger.severe(x.getMessage());
                }
            }
        };
        listPane.setCellRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                try {
                    final Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setToolTipText(((DotSource)value).getSource());
                    return c;
                } catch (RuntimeException e) {
                    logger.severe(e.getMessage());
                    return null;
                }
            }
        });
        final JScrollPane scroll = new JScrollPane(listPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
        listPane.addKeyListener(removeItem);
        listPane.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (listPane.getSelectedIndices().length==1) {
                    setActive(listModel.get(listPane.getSelectedIndex()));
                }
            }
        });
        final JButton button = new JButton("Load Tree");
        add(button, BorderLayout.SOUTH);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final JFileChooser fileChooser = new JFileChooser(lastDir);
                    final FileFilter filter = new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            return f.isDirectory() || (f.canRead() && (f.getName().endsWith(".dot") || f.getName().endsWith(".gv")));
                        }

                        @Override
                        public String getDescription() {
                            return ".dot, .gv";
                        }
                    };
                    fileChooser.addChoosableFileFilter(filter);
                    fileChooser.setDragEnabled(true);
                    fileChooser.setFileFilter(filter);
                    fileChooser.setMultiSelectionEnabled(true);
                    final int result = fileChooser.showOpenDialog(FileListPane.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        final File[] files = fileChooser.getSelectedFiles();
                        if (files.length==0) return;
                        lastDir = files[0].getParentFile();
                        for (File f : files) {
                            addFileOrDirectory(f);
                        }
                    }
                } catch (RuntimeException err) {
                    logger.severe(err.getMessage());
                }
            }
        });
    }

    public synchronized void addFileOrDirectory(File f) {
        if (f.isDirectory()) {
            for (File g : f.listFiles()) {
                if (g.getName().endsWith(".gz") || g.getName().endsWith(".dot")) {
                    addFile(new DotFile(g));
                }
            }
        } else {
            if (f.getName().endsWith(".gz") || f.getName().endsWith(".dot")) {
                addFile(new DotFile(f));
            }
        }
    }

    public static String beautifyName(File f) {
        if (f.getAbsolutePath().startsWith("/tmp")) {
            return f.getName().replaceFirst("\\d+\\.(?:dot|gv)$", "");
        } else {
            final String s = f.getName();
            final int i = f.getName().lastIndexOf('.');
            if (i >= 0)
                return s.substring(0, i);
            else return s;
        }
    }

    public synchronized void addFile(DotSource f) {
        if (f instanceof DotFile)logger.info("load file '" + f + "'");
        else logger.info("load instance: " + f.getSource());
        listModel.addElement(f);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                mf.repaint();
            }
        });

    }

    private synchronized void disableSelection() {
        selectedFile = null;
        action.setFile(null);
    }

    private synchronized void setActive(DotSource f) {
        if (f.equals(selectedFile)) return;
        logger.info("activate " + f);
        selectedFile = f;
        action.setFile(f);
    }

    public synchronized void enableLast() {
        setActive(listModel.lastElement());
    }
}
