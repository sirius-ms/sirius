
package de.unijena.bioinf.ftalign.view;

import javax.swing.*;

public class ApplicationWindow extends JFrame {

    private final ApplicationState state;

    public ApplicationWindow(ApplicationState state) {
        this.state = state;
    }

    public void showLoadWindow() {
        getContentPane().removeAll();
        getContentPane().add(new LoadWindow(this, state));
        pack();
        setVisible(true);
    }

    public void showPlotWindow() {
        getContentPane().removeAll();
        getContentPane().add(new PlotWindow(this, state));
        pack();
        setVisible(true);
    }

    public void showAlignmentWindow() {
        getContentPane().removeAll();
        getContentPane().add(new AlignmentWindow(this, state));
        pack();
        setVisible(true);
    }

}
