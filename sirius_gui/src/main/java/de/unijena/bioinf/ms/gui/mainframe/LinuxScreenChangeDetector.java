package de.unijena.bioinf.ms.gui.mainframe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class LinuxScreenChangeDetector extends ComponentAdapter {
    JFrame sourceFrame;
    GraphicsDevice lastKnownScreenDevice;

    public LinuxScreenChangeDetector(JFrame sourceFrame) {
        this.sourceFrame = sourceFrame;
        this.sourceFrame.addComponentListener(this);
    }
    @Override
    public void componentShown(ComponentEvent e) {
        // Good place for initial device check if not done earlier
        if (lastKnownScreenDevice == null) {
            updateLastKnownScreenDevice();
            System.out.println("Screen on componentShown: " + (lastKnownScreenDevice != null ? lastKnownScreenDevice.getIDstring() : "Unknown"));
        }
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        // No need to get graphics configuration if the window isn't displayable yet
        // (e.g. during initial setup before setVisible(true))
        if (!sourceFrame.isDisplayable() || sourceFrame.getGraphicsConfiguration() == null) {
            return;
        }

        GraphicsDevice currentDevice = sourceFrame.getGraphicsConfiguration().getDevice();
        // Check again, in case of rapid moves back and forth or if it settled on the same screen
        if (!currentDevice.equals(lastKnownScreenDevice)) {
            final Dimension d = sourceFrame.getSize();
            sourceFrame.setSize(d.width + 1, d.height + 1);

            Timer timer = new Timer(100, evt -> sourceFrame.setSize(d));
            timer.setRepeats(false); // Ensure it only runs once
            timer.start();

            lastKnownScreenDevice = currentDevice; // Update the baseline *after* re-render
        }
    }


    private void updateLastKnownScreenDevice() {
        if (sourceFrame.isDisplayable() && sourceFrame.getGraphicsConfiguration() != null) {
            lastKnownScreenDevice = sourceFrame.getGraphicsConfiguration().getDevice();
        } else {
            // Cannot get graphics configuration if not displayable
            // This might happen if called too early.
            lastKnownScreenDevice = null;
        }
    }
}
