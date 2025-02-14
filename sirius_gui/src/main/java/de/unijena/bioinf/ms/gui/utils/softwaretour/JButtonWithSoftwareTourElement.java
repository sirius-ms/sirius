package de.unijena.bioinf.ms.gui.utils.softwaretour;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

@Getter
public class JButtonWithSoftwareTourElement extends JButton implements SoftwareTourElement {

    private final String tutorialDescription;
    private final int orderImportance;
    private final SoftwareTourInfo.LocationHorizontal locationHorizontal;
    private final SoftwareTourInfo.LocationVertical locationVertical;
    private final String scope;

    private Border originalBorder;

    public JButtonWithSoftwareTourElement(String title, SoftwareTourInfo info) {
        super(title);
        this.tutorialDescription = info.getTutorialDescription();
        this.orderImportance = info.getOrderImportance();
        this.locationHorizontal = info.getLocationHorizontal();
        this.locationVertical = info.getLocationVertical();
        this.scope = info.getScope();
    }

    @Override
    public void highlightComponent(Color color, int thickness) {
        if (originalBorder == null) {
            originalBorder = getBorder(); // Save original border
        }
        setBorder(BorderFactory.createLineBorder(color, thickness));
    }

    @Override
    public void resetHighlight() {
        if (originalBorder != null) setBorder(originalBorder);
    }
}
