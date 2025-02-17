package de.unijena.bioinf.ms.gui.utils.softwaretour;

import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Optional;

@Getter
public class SoftwareTourDecorator<C extends JComponent> extends JPanel implements SoftwareTourElement {
    private final C wrappedComponent;
    private Optional<Border> originalBorder;

    private final String tutorialDescription;
    private final int orderImportance;
    private final SoftwareTourInfo.LocationHorizontal locationHorizontal;
    private final SoftwareTourInfo.LocationVertical locationVertical;
    private final String scope;

    @Setter
    private boolean active;

    /**
     * Note: for some components, this can produce unwanted side effects. For these, use specific classes.
     * @param wrappedComponent
     * @param tourInfo
     * @return
     */
    public SoftwareTourDecorator(C wrappedComponent, SoftwareTourInfo tourInfo) {
        this(wrappedComponent, tourInfo, true);
    }
    public SoftwareTourDecorator(C wrappedComponent, SoftwareTourInfo tourInfo, boolean active) {
        if (wrappedComponent instanceof ToolbarButton) {
            LoggerFactory.getLogger(SoftwareTourDecorator.class).debug("Please use specific class for ToolbarButton and not general decorator.");
        }
        this.wrappedComponent = wrappedComponent;
        this.tutorialDescription = tourInfo.getTutorialDescription();
        this.orderImportance = tourInfo.getOrderImportance();
        this.locationHorizontal = tourInfo.getLocationHorizontal();
        this.locationVertical = tourInfo.getLocationVertical();
        this.scope = tourInfo.getScope();
        this.active = active;

        setLayout(new BorderLayout(0,0));
        setOpaque(false);

        // Ensure component does NOT resize unexpectedly
        setPreferredSize(wrappedComponent.getPreferredSize());
        setMaximumSize(wrappedComponent.getPreferredSize());
        setMinimumSize(wrappedComponent.getPreferredSize());

        add(wrappedComponent, BorderLayout.CENTER);
    }


    /** Ensure the decorator has the same size as the wrapped component */
    @Override
    public Dimension getPreferredSize() {
        return wrappedComponent.getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return wrappedComponent.getMinimumSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return wrappedComponent.getMaximumSize();
    }


    /** Apply a temporary thick border that does NOT change layout */
    public void highlightComponent(Color color, int thickness) {
        if (originalBorder == null) {
            originalBorder = Optional.ofNullable(wrappedComponent.getBorder()); // Save original border
        }
        wrappedComponent.setBorder(BorderFactory.createLineBorder(color, thickness));
    }

    @Override
    public void resetHighlight() {
        if (originalBorder != null) wrappedComponent.setBorder(originalBorder.orElse(null));
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
