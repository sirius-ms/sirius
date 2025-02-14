package de.unijena.bioinf.ms.gui.utils.softwaretour;

import java.awt.*;
import java.util.Objects;

public interface SoftwareTourElement {

    public String getTutorialDescription();

    /**
     * lower comes first
     * @return
     */
    public int getOrderImportance();

    public SoftwareTourInfo.LocationHorizontal getLocationHorizontal();
    public SoftwareTourInfo.LocationVertical getLocationVertical();

    default boolean isActive() {
        return true;
    }

    public void highlightComponent(Color color, int thickness);

    public void resetHighlight();

    /**
     *
     * @return the property key of the software tour dialog which should present this information. If null any parent tour dialog may use it.
     *
     */
    default String getScope() {
        return null;
    }

    default boolean isInScope(String tourRootPropertyKey) {
        return Objects.isNull(getScope()) || getScope().equals(tourRootPropertyKey);
    }
}
