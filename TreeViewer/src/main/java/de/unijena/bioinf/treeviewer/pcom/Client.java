package de.unijena.bioinf.treeviewer.pcom;

import java.io.IOException;
import java.net.Socket;

public class Client {

    private final Socket socket;
    private final Protocol protocol;

    public Client() throws IOException {
        this.socket = new Socket("localhost", Server.PORT);
        this.protocol = new Protocol(socket.getInputStream(), socket.getOutputStream());
    }

    public void close() throws IOException {
        socket.close();
    }

    public Protocol getProtocol() {
        return protocol;
    }
}
