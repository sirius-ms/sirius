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

import de.unijena.bioinf.treeviewer.DotFile;
import de.unijena.bioinf.treeviewer.DotMemory;
import de.unijena.bioinf.treeviewer.DotSource;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Protocol {

    // RECEIVE
    public static String FILE = "<!file>";
    public static String DOT = "<!dot>";
    public static String ACTIVATE = "<!enableLast>";
    public static String FLUSH = "<!flush>";
    public static String ANYONETHERE = "<!ping>";
    public static String YESIAMTHERE = "<!pong>";
    public static Pattern CMD_PATTERN = Pattern.compile("<!(?:file|dot|enableLast|flush|ping)>");

    private static Logger logger = Logger.getLogger(Protocol.class.getSimpleName());

    private BufferedReader input;
    private BufferedWriter output;

    public Protocol(InputStream input, OutputStream output) {
        this.input = new BufferedReader(new InputStreamReader(input));
        this.output = new BufferedWriter(new OutputStreamWriter(output));
    }

    public void sendPingRequest() throws IOException {
        logger.info("send PING");
        output.write(ANYONETHERE);
        output.write('\n');
        output.flush();
    }
    public void sendPingResponse() throws IOException {
        logger.info("send PONG");
        output.write(YESIAMTHERE);
        output.write('\n');
        output.flush();
    }

    public void sendRefreshRequest() throws IOException {
        logger.info("send REFRESH");
        output.write(FLUSH); output.write('\n');
        output.flush();
    }

    public void sendActivateLastRequest() throws IOException {
        logger.info("send ACTIVATE");
        output.write(ACTIVATE); output.write('\n');
        output.flush();
    }

    public void sendFile(File f) throws IOException {
        sendFiles(Collections.singletonList(f));
    }

    public void sendFiles(List<File> fs) throws IOException {
        logger.info("send FILES");
        output.write(FILE);
        output.write('\n');
        for (File f : fs) {
            output.write(f.getAbsolutePath());
            output.write('\n');
        }
        output.flush();
    }

    public void sendDotString(CharSequence dotFile) throws IOException {
        logger.info("send DOT source");
        output.write(DOT);
        output.write('\n');
        output.write(dotFile.toString());
        if (dotFile.charAt(dotFile.length()-1) != '\n') output.write('\n');
        output.flush();
    }

    public Response getResponse() throws IOException {
        String cmdLine = null;
        while (true) {
            String l = input.readLine();
            if (l == null) break;
            if (!l.isEmpty()) {
                cmdLine = l;
                break;
            }
        }
        if (cmdLine == null) return new Response(CMD.VOID, new DotSource[0]);
        if (cmdLine.equals(FILE)) {
            final List<String> content = readUntilNextCommand();
            final DotSource[] sources = new DotSource[content.size()];
            for (int i=0; i < content.size(); ++i) sources[i] = new DotFile(new File(content.get(i)));
            logger.info("receive FILES");
            return new Response(CMD.INPUT, sources);
        } else if (cmdLine.equals(DOT)) {
            final List<String> content = readUntilNextCommand();
            int length = -content.get(0).length();
            for (String s : content) length += s.length()+1;
            final StringBuffer buffer = new StringBuffer(length);
            for (String s : content.subList(1, content.size())) buffer.append(s).append("\n");
            final DotSource[] sources = new DotSource[]{new DotMemory(content.get(0), buffer.toString())};
            logger.info("receive DOT");
            return new Response(CMD.INPUT, sources);
        } else if (cmdLine.equals(ACTIVATE)) {
            logger.info("receive ACTIVATE");
            return new Response(CMD.ACTIVATE_LAST, new DotSource[0]);
        } else if (cmdLine.equals(FLUSH)) {
            logger.info("receive REFRESH");
            return new Response(CMD.REFRESH, new DotSource[0]);
        } else if (cmdLine.equals(ANYONETHERE)) {
            logger.info("receive PING");
            return new Response(CMD.PING, new DotSource[0]);
        } else if (cmdLine.equals(YESIAMTHERE)) {
            logger.info("receive PONG");
            return new Response(CMD.PONG, new DotSource[0]);
        } else {
            throw new IOException("unknown command: " + cmdLine);
        }

    }

    private List<String> readUntilNextCommand() throws IOException {
        final ArrayList<String> lines = new ArrayList<String>();
        while (input.ready()) {
            final String line = input.readLine();
            if (line.startsWith("<!") && CMD_PATTERN.matcher(line).find()) {
                return lines;
            } else {
                lines.add(line);
            }
        }
        return lines;
    }

    public static enum CMD {
        REFRESH, INPUT, ACTIVATE_LAST, PING, VOID, PONG;
    }

    public static class Response {
        public final CMD cmd;
        public final DotSource[] responseObject;

        Response(CMD cmd, DotSource[] responseObject) {
            this.cmd = cmd;
            this.responseObject = responseObject;
        }
    }




}
