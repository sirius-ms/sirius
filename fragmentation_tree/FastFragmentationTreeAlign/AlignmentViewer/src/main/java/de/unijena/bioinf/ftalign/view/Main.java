
package de.unijena.bioinf.ftalign.view;

import javax.swing.*;

public class Main implements Runnable {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Main());
    }


    public void run() {
        final ApplicationState state = new ApplicationState();
        final ApplicationWindow loadWindow = new ApplicationWindow(state);
        loadWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loadWindow.showLoadWindow();
    }
}
