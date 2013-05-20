package de.unijena.bioinf.treeviewer;

import org.jdom2.JDOMException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
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
    Pipe pipe;
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
                if (pipe != null) {
                    pipe.close();
                    pipe = null;
                }
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
        if (pipe != null) {
            pipe.close();
        }
        if (stream == null) pipe = null;
        else {
            logger.info("read from input stream");
            pipe = new Pipe(stream, fileListPane);
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
            this.server = new Server(this);
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }
}
