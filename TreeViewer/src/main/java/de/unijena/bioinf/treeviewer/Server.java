package de.unijena.bioinf.treeviewer;

import de.unijena.bioinf.treeviewer.MainFrame;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class Server extends IOConnection implements Runnable {

    final static int PORT = 3721;

    private ServerSocket server;
    private Socket listener;
    private boolean isRunning;
    MainFrame main;

    public Server(MainFrame m) throws IOException {
        super(m.fileListPane);
        this.server = new ServerSocket(PORT);
        this.isRunning = true;
        this.main = m;
        new Thread(this).start();
    }

    public void close() {
        super.close();
        isRunning = false;
        try {
            listener.close();
            server.close();
        } catch (IOException e) {
            MainFrame.logger.severe(e.getMessage());
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                this.listener = server.accept();
            } catch (IOException e) {
                MainFrame.logger.severe(e.getMessage());
            }
            final BufferedReader stream;
            try {
                stream = new BufferedReader(new InputStreamReader(listener.getInputStream()));
                readInput(stream);
            } catch (IOException e) {
                MainFrame.logger.severe(e.getMessage());
            }
        }
    }
}
