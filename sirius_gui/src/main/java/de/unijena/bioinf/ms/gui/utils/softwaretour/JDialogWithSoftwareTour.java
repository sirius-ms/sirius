package de.unijena.bioinf.ms.gui.utils.softwaretour;

import de.unijena.bioinf.ms.gui.properties.GuiProperties;

import javax.swing.*;
import java.awt.*;

public abstract class JDialogWithSoftwareTour extends JDialog {
    public JDialogWithSoftwareTour(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
    }
    
    public void checkAndInitTutorial(GuiProperties guiProperties) {
        SoftwareTourUtils.checkAndInitTutorial(this, getTutorialPropertyKey(), guiProperties);
    }

    public abstract String getTutorialPropertyKey();

}
