package de.unijena.bioinf.ms.gui.utils.softwaretour;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SoftwareTourInfo {

    public enum LocationHorizontal {LEFT_ALIGN_TO_RIGHT, CENTER, RIGHT_SPACE, LEFT_SPACE_TO_LEFT}

    public enum LocationVertical {ON_TOP, TOP_TOP_ALIGN, CENTER, BOTTOM_BOTTOM_ALIGN, BELOW_BOTTOM}

    private final String tutorialDescription;

    /**
     * lower comes first
     */
    private final int orderImportance;

    private final LocationHorizontal locationHorizontal;
    private final LocationVertical locationVertical;


    /**
     * the property key of the software tour dialog which should present this information. If null any parent tour dialog may use it.
     */
    private String scope;


    public SoftwareTourInfo(String tutorialDescription, int orderImportance, LocationHorizontal locationHorizontal, LocationVertical locationVertical) {
        this.tutorialDescription = tutorialDescription;
        this.orderImportance = orderImportance;
        this.locationHorizontal = locationHorizontal;
        this.locationVertical = locationVertical;
    }

    public boolean isInScope(String propertyKey) {
        return scope == null || scope.equals(propertyKey);
    }
}
