package de.unijena.bioinf.treeviewer;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                final ProcessBuilder builder = new ProcessBuilder("xprop", "-name", MainFrame.TITLE, "_NET_WM_PID");
                try {
                    final Process p = builder.start();
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        logger.severe(e.getMessage());
                    }
                    if (p.exitValue() == 0) {
                        final String line = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
                        final Pattern pidreg = Pattern.compile("(\\d+)");
                        final Matcher m = pidreg.matcher(line);
                        if (m.find()) {
                            final Socket client = new Socket("localhost", Server.PORT);
                            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                            for (File f : files) {
                                writer.write(IOConnection.FILE);
                                writer.newLine();
                                writer.write(f.getAbsolutePath());
                                writer.newLine();
                            }
                            if (pipe) {
                                final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                                while (reader.ready()) {
                                    final String ln  = reader.readLine();
                                    writer.write(ln);
                                    writer.newLine();
                                }
                                reader.close();
                            }
                            writer.write(IOConnection.ACTIVATE);
                            writer.newLine();
                            writer.close();
                            client.close();
                            return;
                        } else {
                            logger.warning("Can't find another java process");
                        }
                    }
                } catch (IOException e) {
                    logger.severe(e.getMessage());
                }
            }
            final MainFrame frame = new MainFrame();
            if (pipe)
                frame.setPipe(System.in);
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
