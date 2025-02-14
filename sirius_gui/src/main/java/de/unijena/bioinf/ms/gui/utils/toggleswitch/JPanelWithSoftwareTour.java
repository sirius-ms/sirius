package de.unijena.bioinf.ms.gui.utils.toggleswitch;

import de.unijena.bioinf.ms.gui.properties.GuiProperties;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourUtils;

import javax.swing.*;
import java.awt.*;

public abstract class JPanelWithSoftwareTour extends JPanel {
    public JPanelWithSoftwareTour(LayoutManager layout) {
        super(layout);
    }
    
    public void checkAndInitTutorial(GuiProperties guiProperties) {
        SoftwareTourUtils.checkAndInitTutorial(this, getTutorialPropertyKey(), guiProperties);
    }

    public abstract String getTutorialPropertyKey();

}
