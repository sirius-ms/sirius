package de.unijena.bioinf.ms.gui.utils.softwaretour;

import de.unijena.bioinf.ms.gui.utils.ToolbarButton;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Optional;

@Getter
public class SoftwareTourDecorator extends JPanel implements SoftwareTourElement {
    private final JComponent wrappedComponent;
    private Optional<Border> originalBorder;

    private final String tutorialDescription;
    private final int orderImportance;
    private final SoftwareTourInfo.LocationHorizontal locationHorizontal;
    private final SoftwareTourInfo.LocationVertical locationVertical;
    private final String scope;

    protected SoftwareTourDecorator(JComponent wrappedComponent, SoftwareTourInfo info) {
        this.wrappedComponent = wrappedComponent;
        this.tutorialDescription = info.getTutorialDescription();
        this.orderImportance = info.getOrderImportance();
        this.locationHorizontal = info.getLocationHorizontal();
        this.locationVertical = info.getLocationVertical();
        this.scope = info.getScope();

        setLayout(new BorderLayout(0,0));
        setOpaque(false);

        // Ensure component does NOT resize unexpectedly
        setPreferredSize(wrappedComponent.getPreferredSize());
        setMaximumSize(wrappedComponent.getPreferredSize());
        setMinimumSize(wrappedComponent.getPreferredSize());

        add(wrappedComponent, BorderLayout.CENTER);
    }


    /**
     * Note: for some components, this can produce unwanted side effects. For these, use specific classes.
     * @param wrappedComponent
     * @param softwareTourInfo
     * @return
     */
    public static SoftwareTourDecorator decorate(JComponent wrappedComponent, SoftwareTourInfo softwareTourInfo) {
        if (wrappedComponent instanceof ToolbarButton) {
            LoggerFactory.getLogger(SoftwareTourDecorator.class).debug("Please use specific class for ToolbarButton and not general decorator.");
        }
        return new SoftwareTourDecorator(wrappedComponent, softwareTourInfo);
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
}
