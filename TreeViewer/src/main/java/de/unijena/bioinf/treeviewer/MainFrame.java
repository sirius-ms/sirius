/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.treeviewer;

import de.unijena.bioinf.treeviewer.pcom.IOConnection;
import de.unijena.bioinf.treeviewer.pcom.Server;
import org.jdom2.JDOMException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class MainFrame extends JFrame {
    public static String TITLE = "Fragmentation Tree Viewer";
    public static Logger logger = Logger.getLogger(MainFrame.class.getSimpleName());
    final FTCanvas canvas;
    final FileListPane fileListPane;
    final ViewSelectionPane viewSelectionPane;
    Server server;

    private final List<Profile> profiles;

    public MainFrame() {
        super(TITLE);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        // help menu
        final JMenuBar menu = new JMenuBar();
        final JMenu help = new JMenu("Help", false);
        final JMenuItem shortcuts = new JMenuItem("Shortcuts");
        this.profiles = new ArrayList<Profile>();
        try {
            this.profiles.addAll(new ProfileParser().parseProfiles());
        } catch (JDOMException e) {
            logger.severe(e.getMessage());
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
        help.add(shortcuts);
        menu.add(help);
        setJMenuBar(menu);
        shortcuts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(MainFrame.this, "Ctrl+LeftButton - Zoom Box\n" +
                        "Shift+RightButton - Zoom (with instant feedback)\n" +
                        "Shift+LeftButton - Pan\n" +
                        "Ctrl+RightButton - Rotate\n" +
                        "Ctrl+Shift+RightButton - Reset transform (also known as \"Original View\") ", "Shortcuts", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        // frame
        this.fileListPane = new FileListPane(this, new CurrentFileAction() {
            @Override
            public void setFile(final DotSource f) {
                System.out.println("SET FILE " + f.getName());
                if (f == null) {
                    logger.info("render nothing");
                    canvas.dispose();
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            canvas.setFile(f);
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(MainFrame.this, "Can't read '" + f + "' due following reason:\n " + e.getMessage(), "Input error", JOptionPane.ERROR_MESSAGE);
                        } catch (RuntimeException e) {
                            logger.severe(e.getMessage());
                        }
                    }
                }).start();
            }
        });
        this.canvas = new FTCanvas();
        canvas.setActiveProfile(profiles.size() > 0 ? profiles.get(0) : null);
        this.viewSelectionPane = new ViewSelectionPane(profiles.size() > 0 ? profiles.get(0) : null, new Runnable() {
            @Override
            public void run() {
                try {
                    canvas.refreshFilters();
                } catch (RuntimeException e) {
                    logger.severe(e.getMessage());
                }
            }
        });

        final JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, fileListPane, viewSelectionPane);
        final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, verticalSplit, canvas);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (server != null) server.close();
            }
        });
        final Log log = new Log();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(split, BorderLayout.CENTER);
        getContentPane().add(log, BorderLayout.SOUTH);
        setSize(640, 480);
        setVisible(true);
        verticalSplit.setDividerLocation(0.7);
        getRootLogger().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                log.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });

    }

    public void setPipe(InputStream stream) {
        try {
            logger.info("read from input stream");
            new IOConnection(fileListPane).readInputInParallel(System.in, null);
        } catch (IOException e) {
            logger.warning("Can't create pipe: " + e.getMessage());
        }
    }

    public void setTree(String filename) {
        fileListPane.addFileOrDirectory(new File(filename));
    }

    public void setTrees(List<File> files) {
        for (File f : files) fileListPane.addFileOrDirectory(f);
        fileListPane.enableLast();
    }

    static Logger getRootLogger() {
        Logger l = logger;
        while (l.getParent() != null) l = l.getParent();
        return l;
    }


    public void openServer() {
        try {
            this.server = new Server(fileListPane);
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }
}
