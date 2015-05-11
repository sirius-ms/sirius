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
package de.unijena.bioinf.treeviewer.pcom;

import de.unijena.bioinf.treeviewer.FileListPane;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Logger;

public class IOConnection {

    private static Logger logger = Logger.getLogger(IOConnection.class.getSimpleName());

    private final FileListPane pane;

    public IOConnection(FileListPane pane) {
        this.pane = pane;
    }

    public void readInputInParallel(final InputStream in, final OutputStream out) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    readInput(in, out);
                } catch (IOException e) {
                    logger.severe("Error while listening from IO: " + e.getMessage());
                }
            }
        }).start();

    }

    public void readInput(InputStream in, OutputStream out) throws IOException{
        final Protocol protocol = new Protocol(in, out);
        while (true) {
            final Protocol.Response response = protocol.getResponse();
            logger.info("Receive: " + response.cmd);
            switch (response.cmd) {
                case VOID: return;
                case ACTIVATE_LAST: pane.enableLast(); break;
                case INPUT: pane.addFiles(Arrays.asList(response.responseObject)); break;
                case PING: protocol.sendPingResponse(); break;
                case REFRESH: flush(); break;
                case PONG: break; // do nothing
            }
        }
    }

    private void flush() {
        // currently not necessary
    }



}
