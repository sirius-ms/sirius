package de.unijena.bioinf.treeviewer;


import de.unijena.bioinf.treeviewer.pcom.Client;
import de.unijena.bioinf.treeviewer.pcom.IOConnection;
import de.unijena.bioinf.treeviewer.pcom.Server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Main {
    private static Logger logger = Logger.getLogger(MainFrame.class.getSimpleName());
    public static void main(String[] args) {
        try {
            final List<File> files = new ArrayList<File>();
            boolean server = false;
            boolean addToServer = false;
            boolean pipe = false;
            for (String arg : args) {
                if (arg.equals("-") || arg.equals("--")) {
                    pipe = true;
                } else if (arg.equals("-s") || arg.equals("-server") || arg.equals("--server"))  {
                    server = true;
                } else if (arg.equals("-a")) {
                    addToServer = true;
                } else  {
                    files.add(new File(arg));
                }
            }
            if (addToServer) {
                if (Server.isProcessStillRunning()) {
                    try {
                        final Client client = new Client();
                        client.getProtocol().sendFiles(files);
                        client.close();
                        return;
                    } catch (IOException e) {
                        logger.warning("Can't create client: " + e.getMessage());
                    }
                } else if (!server) {
                    logger.warning("Can't find another java process");
                }
            }
            final MainFrame frame = new MainFrame();
            if (pipe)
                try {
                    new IOConnection(frame.fileListPane).readInputInParallel(System.in, null);
                } catch (IOException e) {
                    logger.warning("Can't create pipe: " + e.getMessage());
                }
            if (!files.isEmpty())
                frame.setTrees(files);
            if (server) {
                frame.openServer();
            }
        } catch (RuntimeException e) {
            logger.severe(e.getMessage());
        }
    }
}
