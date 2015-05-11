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
